package com.example.money.data.sync

import com.example.money.domain.sync.FinanceSnapshot
import com.example.money.domain.sync.SyncEngine

object SyncSnapshotGuard {
    const val MAX_RECORDS = 10_000

    fun checkFrame(raw: String) {
        if (raw.length > SyncSessionCrypto.MAX_FRAME_CHARS) {
            throw IllegalArgumentException("Snapshot exceeds maximum size")
        }
    }

    fun check(snapshot: FinanceSnapshot) {
        if (snapshot.schemaVersion != FinanceSnapshot.CURRENT_SCHEMA_VERSION) {
            throw IllegalArgumentException("Unsupported snapshot schema")
        }
        if (snapshot.deviceId.isBlank() || snapshot.deviceId.length > 80) {
            throw IllegalArgumentException("Snapshot device id is invalid")
        }
        val recordCount = snapshot.goals.size +
            snapshot.debts.size +
            snapshot.debtPayments.size +
            snapshot.transactions.size +
            (snapshot.goalActions?.size ?: 0) +
            (snapshot.notes?.size ?: 0) +
            (snapshot.noteLines?.size ?: 0)
        if (recordCount > MAX_RECORDS) {
            throw IllegalArgumentException("Snapshot has too many records")
        }
        snapshot.goals.forEach { goal ->
            checkId(goal.id)
            checkCents(goal.targetAmountCents)
        }
        snapshot.debts.forEach { debt ->
            checkId(debt.id)
            checkCents(debt.targetAmountCents)
            checkCents(debt.currentAmountCents)
        }
        snapshot.debtPayments.forEach { payment ->
            checkId(payment.id)
            checkId(payment.debtorId)
            checkCents(payment.amountCents)
        }
        snapshot.transactions.forEach { transaction ->
            checkId(transaction.id)
            checkCents(transaction.amountCents)
            transaction.goalId?.let(::checkId)
        }
        snapshot.goalActions?.forEach { action ->
            checkId(action.id)
            checkId(action.goalId)
        }
        snapshot.notes?.forEach { note -> checkId(note.id) }
        snapshot.noteLines?.forEach { line ->
            checkId(line.id)
            checkId(line.noteId)
            checkCents(line.amountCents)
        }
    }

    private fun checkId(id: String) {
        if (id.isBlank() || id.length > 80 || id.any { it.isISOControl() }) {
            throw IllegalArgumentException("Snapshot id is invalid")
        }
    }

    private fun checkCents(cents: Long) {
        if (cents < 0L || cents > SyncSessionCrypto.MAX_AMOUNT_CENTS) {
            throw IllegalArgumentException("Snapshot amount is out of range")
        }
    }
}

/**
 * Authenticated sync over an already open text channel.
 * The TCP client is the joiner. The listener is the host. Device id is not used to pick a role.
 */
object SyncSecureChannel {
    const val UNPAIRED = "unpaired"
    private const val HELLO_PREFIX = "hello:1:"
    private const val ENC_PREFIX = "enc:1:"

    suspend fun runInitiator(
        localDeviceId: String,
        expectedPeerId: String?,
        vault: SyncKeyVault,
        pairing: OutgoingPairing?,
        local: FinanceSnapshot,
        encode: (FinanceSnapshot) -> String,
        decode: (String) -> FinanceSnapshot,
        sendText: suspend (String) -> Unit,
        receiveText: suspend () -> String,
    ): FinanceSnapshot {
        val key = if (pairing != null) {
            pairAsJoiner(localDeviceId, pairing, vault, sendText, receiveText)
        } else {
            val peerId = expectedPeerId ?: error("Missing peer id")
            val paired = vault.find(peerId) ?: error("Unpaired")
            sendText("$HELLO_PREFIX$localDeviceId")
            paired.sharedKey
        }
        return exchangeAsSender(key, local, encode, decode, sendText, receiveText)
    }

    suspend fun runAcceptor(
        localDeviceId: String,
        vault: SyncKeyVault,
        hostOffer: PairingOffer?,
        local: FinanceSnapshot,
        encode: (FinanceSnapshot) -> String,
        decode: (String) -> FinanceSnapshot,
        sendText: suspend (String) -> Unit,
        receiveText: suspend () -> String,
    ): FinanceSnapshot {
        val first = receiveText()
        val key = when {
            first.startsWith("pair:1:") -> {
                val offer = hostOffer ?: reject(sendText)
                val reply = SyncPairing.hostReply(offer, first) ?: reject(sendText)
                sendText(reply.message)
                val joinerId = readIdentity(reply.sharedKey, receiveText)
                sendIdentity(reply.sharedKey, localDeviceId, sendText)
                vault.save(PairedPeer(joinerId, reply.sharedKey, SyncPeerRole.HOST))
                reply.sharedKey
            }
            first.startsWith(HELLO_PREFIX) -> {
                val peerId = first.removePrefix(HELLO_PREFIX)
                vault.find(peerId)?.sharedKey ?: reject(sendText)
            }
            else -> reject(sendText)
        }
        return exchangeAsReceiver(key, local, encode, decode, sendText, receiveText)
    }

    private suspend fun pairAsJoiner(
        localDeviceId: String,
        pairing: OutgoingPairing,
        vault: SyncKeyVault,
        sendText: suspend (String) -> Unit,
        receiveText: suspend () -> String,
    ): ByteArray {
        sendText(SyncPairing.joinerMessage(pairing))
        val shared = SyncPairing.finishJoiner(pairing, receiveText()) ?: error("Pairing rejected")
        sendIdentity(shared, localDeviceId, sendText)
        val peerId = readIdentity(shared, receiveText)
        vault.save(PairedPeer(peerId, shared, SyncPeerRole.JOINER))
        return shared
    }

    private suspend fun exchangeAsSender(
        key: ByteArray,
        local: FinanceSnapshot,
        encode: (FinanceSnapshot) -> String,
        decode: (String) -> FinanceSnapshot,
        sendText: suspend (String) -> Unit,
        receiveText: suspend () -> String,
    ): FinanceSnapshot {
        val encoded = encode(local)
        SyncSnapshotGuard.checkFrame(encoded)
        SyncSnapshotGuard.check(local)
        sendText(ENC_PREFIX + SyncSessionCrypto.seal(key, encoded.toByteArray(Charsets.UTF_8)))
        val remoteRaw = readEncrypted(key, receiveText)
        val remote = decode(remoteRaw)
        SyncSnapshotGuard.check(remote)
        return SyncEngine.mergeSymmetric(local, remote)
    }

    private suspend fun exchangeAsReceiver(
        key: ByteArray,
        local: FinanceSnapshot,
        encode: (FinanceSnapshot) -> String,
        decode: (String) -> FinanceSnapshot,
        sendText: suspend (String) -> Unit,
        receiveText: suspend () -> String,
    ): FinanceSnapshot {
        val remoteRaw = readEncrypted(key, receiveText)
        val remote = decode(remoteRaw)
        SyncSnapshotGuard.check(remote)
        val encoded = encode(local)
        SyncSnapshotGuard.checkFrame(encoded)
        SyncSnapshotGuard.check(local)
        sendText(ENC_PREFIX + SyncSessionCrypto.seal(key, encoded.toByteArray(Charsets.UTF_8)))
        return SyncEngine.mergeSymmetric(local, remote)
    }

    private suspend fun sendIdentity(
        key: ByteArray,
        deviceId: String,
        sendText: suspend (String) -> Unit,
    ) {
        sendText(ENC_PREFIX + SyncSessionCrypto.seal(key, deviceId.toByteArray(Charsets.UTF_8)))
    }

    private suspend fun readIdentity(
        key: ByteArray,
        receiveText: suspend () -> String,
    ): String {
        val deviceId = readEncrypted(key, receiveText)
        if (deviceId.isBlank() || deviceId.length > 80) error("Invalid peer identity")
        return deviceId
    }

    private suspend fun readEncrypted(
        key: ByteArray,
        receiveText: suspend () -> String,
    ): String {
        val frame = receiveText()
        if (!frame.startsWith(ENC_PREFIX) || frame.length > SyncSessionCrypto.MAX_FRAME_CHARS) {
            error("Rejected sync frame")
        }
        val opened = SyncSessionCrypto.open(key, frame.removePrefix(ENC_PREFIX))
        val text = opened.toString(Charsets.UTF_8)
        SyncSnapshotGuard.checkFrame(text)
        return text
    }

    private suspend fun reject(sendText: suspend (String) -> Unit): Nothing {
        sendText(UNPAIRED)
        error("Unpaired")
    }
}
