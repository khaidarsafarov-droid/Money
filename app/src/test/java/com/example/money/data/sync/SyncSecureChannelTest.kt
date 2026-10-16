package com.example.money.data.sync

import com.example.money.data.local.dao.SyncQueueDao
import com.example.money.data.local.entity.SyncQueueEntity
import com.example.money.domain.repository.FinanceSnapshotRepository
import com.example.money.domain.sync.FinanceSnapshot
import com.example.money.domain.sync.SnapshotGoal
import com.example.money.domain.sync.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class SyncSecureChannelTest {

    @Test
    fun acceptorWithoutKeyDoesNotSendSnapshot() = runBlocking {
        val sent = mutableListOf<String>()
        var encoded = false
        val result = runCatching {
            SyncSecureChannel.runAcceptor(
                localDeviceId = "host",
                vault = MemorySyncKeyVault(),
                hostOffer = null,
                local = snapshot("host"),
                encode = {
                    encoded = true
                    error("snapshot must stay local")
                },
                decode = { error("nothing to decode") },
                sendText = { sent += it },
                receiveText = { "hello:1:stranger" },
            )
        }

        assertTrue(result.isFailure)
        assertFalse(encoded)
        assertEquals(SyncSecureChannel.UNPAIRED, sent.single())
    }

    @Test
    fun tamperedMessageIsRejectedBeforeLocalSnapshotIsSent() = runBlocking {
        val key = ByteArray(32) { 4 }
        val vault = MemorySyncKeyVault().apply {
            save(PairedPeer("joiner", key, SyncPeerRole.HOST))
        }
        val sealed = SyncSessionCrypto.seal(key, "not-a-snapshot".toByteArray())
        val tampered = flipLastChar(sealed)
        val frames = ArrayDeque(listOf("hello:1:joiner", "enc:1:$tampered"))
        val sent = mutableListOf<String>()
        var encoded = false
        val result = runCatching {
            SyncSecureChannel.runAcceptor(
                localDeviceId = "host",
                vault = vault,
                hostOffer = null,
                local = snapshot("host"),
                encode = {
                    encoded = true
                    FinanceSnapshotCodec.encode(snapshot("host"))
                },
                decode = FinanceSnapshotCodec::decode,
                sendText = { sent += it },
                receiveText = { frames.removeFirst() },
            )
        }

        assertTrue(result.isFailure)
        assertFalse(encoded)
        assertTrue(sent.isEmpty())
    }

    @Test
    fun sealRoundTripAndTamper() {
        val key = ByteArray(32) { 3 }
        val sealed = SyncSessionCrypto.seal(key, "hello".toByteArray())
        assertEquals("hello", SyncSessionCrypto.open(key, sealed).decodeToString())
        val broken = runCatching { SyncSessionCrypto.open(key, flipLastChar(sealed)) }
        assertTrue(broken.isFailure)
    }

    @Test
    fun pinPairingAgreesOnTheSameKeyAndRejectsABadMac() {
        val offer = SyncPairing.createOffer(randomPin = 123456)
        val joiner = SyncPairing.fromPin("123456")
        assertNotNull(joiner)
        val message = SyncPairing.joinerMessage(joiner!!)
        val reply = SyncPairing.hostReply(offer, message)
        assertNotNull(reply)
        val joinerKey = SyncPairing.finishJoiner(joiner, reply!!.message)
        assertNotNull(joinerKey)
        assertTrue(reply.sharedKey.contentEquals(joinerKey))
        assertNull(SyncPairing.hostReply(offer, tamperMac(message)))
    }

    @Test
    fun disabledSyncDoesNotOpenTheServer() = runBlocking {
        var clientsCreated = 0
        val service = SyncService(
            readDeviceId = { "local" },
            financeSnapshotRepository = object : FinanceSnapshotRepository {
                override suspend fun buildSnapshot() = snapshot("local")
                override fun encodeSnapshot(snapshot: FinanceSnapshot) = FinanceSnapshotCodec.encode(snapshot)
                override fun decodeSnapshot(raw: String) = FinanceSnapshotCodec.decode(raw)
            },
            applySnapshot = {},
            syncQueueDao = object : SyncQueueDao {
                override suspend fun getAllPending(): List<SyncQueueEntity> = emptyList()
                override fun observePendingCount(): Flow<Int> = flowOf(0)
                override suspend fun insert(item: SyncQueueEntity) = Unit
                override suspend fun deleteById(id: String) = Unit
                override suspend fun deleteByEntity(entityId: Long, entityType: String) = Unit
                override suspend fun clearAll() = Unit
            },
            scope = this,
            keyVault = MemorySyncKeyVault(),
            syncEnabled = { false },
            httpClients = SyncHttpClients { clientsCreated += 1; SyncHttpClients.create() },
            discoveryFactory = PeerDiscoveryFactory { _, _, _ ->
                object : PeerDiscovery {
                    override fun registerService(port: Int) = Unit
                    override fun startDiscovery() = Unit
                    override fun stop() = Unit
                }
            },
        )

        service.start()
        assertEquals(0, clientsCreated)
        assertEquals(SyncStatus.Offline, service.connectionState.value.status)
    }

    private fun flipLastChar(value: String): String {
        val chars = value.toCharArray()
        chars[chars.lastIndex] = if (chars[chars.lastIndex] == 'A') 'B' else 'A'
        return String(chars)
    }

    private fun tamperMac(message: String): String {
        val macStart = message.lastIndexOf(':') + 1
        val chars = message.toCharArray()
        val index = macStart + (chars.size - macStart) / 2
        chars[index] = if (chars[index] == 'A') 'B' else 'A'
        return String(chars)
    }

    private fun snapshot(deviceId: String) = FinanceSnapshot(
        schemaVersion = FinanceSnapshot.CURRENT_SCHEMA_VERSION,
        deviceId = deviceId,
        updatedAt = 1L,
        goals = listOf(
            SnapshotGoal(
                id = "goal-$deviceId",
                name = deviceId,
                targetAmountCents = 100L,
                currency = "USD",
                dateDeadline = null,
                syncUpdatedAt = 1L,
            ),
        ),
        debts = emptyList(),
        debtPayments = emptyList(),
        transactions = emptyList(),
        voiceSettings = null,
    )
}
