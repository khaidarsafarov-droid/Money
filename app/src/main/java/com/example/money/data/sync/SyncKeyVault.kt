package com.example.money.data.sync

import java.security.KeyPair

enum class SyncPeerRole {
    HOST,
    JOINER,
}

data class PairedPeer(
    val peerId: String,
    val sharedKey: ByteArray,
    val role: SyncPeerRole,
)

interface SyncKeyVault {
    fun find(peerId: String): PairedPeer?
    fun save(peer: PairedPeer)
}

class MemorySyncKeyVault : SyncKeyVault {
    private val peers = linkedMapOf<String, PairedPeer>()

    override fun find(peerId: String): PairedPeer? = synchronized(peers) { peers[peerId] }

    override fun save(peer: PairedPeer) {
        synchronized(peers) {
            peers[peer.peerId] = peer
        }
    }
}

data class PairingOffer(
    val pin: String,
    val keyPair: KeyPair,
) {
    val qrPayload: String
        get() = "money-pair:1:${SyncSessionCrypto.base64(keyPair.public.encoded)}:$pin"
}

data class OutgoingPairing(
    val pin: String,
    val keyPair: KeyPair,
    val expectedHostPublic: ByteArray?,
)

object SyncPairing {
    fun createOffer(randomPin: Int = java.security.SecureRandom().nextInt(900_000) + 100_000): PairingOffer =
        PairingOffer(
            pin = randomPin.toString(),
            keyPair = SyncSessionCrypto.generateKeyPair(),
        )

    fun parseQr(payload: String): OutgoingPairing? {
        val parts = payload.trim().split(":")
        if (parts.size != 4 || parts[0] != "money-pair" || parts[1] != "1") return null
        val publicKey = runCatching { SyncSessionCrypto.base64Decode(parts[2]) }.getOrNull() ?: return null
        val pin = parts[3]
        if (pin.length !in 4..8 || pin.any { !it.isDigit() }) return null
        return OutgoingPairing(
            pin = pin,
            keyPair = SyncSessionCrypto.generateKeyPair(),
            expectedHostPublic = publicKey,
        )
    }

    fun fromPin(pin: String): OutgoingPairing? {
        val trimmed = pin.trim()
        if (trimmed.length !in 4..8 || trimmed.any { !it.isDigit() }) return null
        return OutgoingPairing(
            pin = trimmed,
            keyPair = SyncSessionCrypto.generateKeyPair(),
            expectedHostPublic = null,
        )
    }

    fun joinerMessage(pairing: OutgoingPairing): String {
        val publicKey = pairing.keyPair.public.encoded
        val mac = SyncSessionCrypto.pinMac(pairing.pin, publicKey)
        return "pair:1:${SyncSessionCrypto.base64(publicKey)}:${SyncSessionCrypto.base64(mac)}"
    }

    fun hostReply(offer: PairingOffer, joinerMessage: String): HostPairingReply? {
        val parts = joinerMessage.split(":")
        if (parts.size != 4 || parts[0] != "pair" || parts[1] != "1") return null
        val joinerPublic = runCatching { SyncSessionCrypto.base64Decode(parts[2]) }.getOrNull() ?: return null
        val mac = runCatching { SyncSessionCrypto.base64Decode(parts[3]) }.getOrNull() ?: return null
        if (!SyncSessionCrypto.pinMacMatches(offer.pin, joinerPublic, mac)) return null
        val hostPublic = offer.keyPair.public.encoded
        val hostMac = SyncSessionCrypto.pinMac(offer.pin, hostPublic)
        return HostPairingReply(
            message = "pair-ok:1:${SyncSessionCrypto.base64(hostPublic)}:${SyncSessionCrypto.base64(hostMac)}",
            joinerPublic = joinerPublic,
            sharedKey = SyncSessionCrypto.sharedKey(
                offer.keyPair.private,
                SyncSessionCrypto.publicKeyFrom(joinerPublic),
            ),
        )
    }

    fun finishJoiner(pairing: OutgoingPairing, hostMessage: String): ByteArray? {
        val parts = hostMessage.split(":")
        if (parts.size != 4 || parts[0] != "pair-ok" || parts[1] != "1") return null
        val hostPublic = runCatching { SyncSessionCrypto.base64Decode(parts[2]) }.getOrNull() ?: return null
        val mac = runCatching { SyncSessionCrypto.base64Decode(parts[3]) }.getOrNull() ?: return null
        if (!SyncSessionCrypto.pinMacMatches(pairing.pin, hostPublic, mac)) return null
        val expected = pairing.expectedHostPublic
        if (expected != null && !MessageDigestEquals(expected, hostPublic)) return null
        return SyncSessionCrypto.sharedKey(
            pairing.keyPair.private,
            SyncSessionCrypto.publicKeyFrom(hostPublic),
        )
    }

    private fun MessageDigestEquals(left: ByteArray, right: ByteArray): Boolean =
        java.security.MessageDigest.isEqual(left, right)
}

data class HostPairingReply(
    val message: String,
    val joinerPublic: ByteArray,
    val sharedKey: ByteArray,
)
