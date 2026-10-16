package com.example.money.data.sync

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * ECDH P-256 plus AES-GCM for one paired sync session.
 * The raw shared key is sealed by an Android Keystore key before it is stored.
 */
object SyncSessionCrypto {
    const val MAX_FRAME_CHARS = 1_048_576
    const val MAX_AMOUNT_CENTS = 1_000_000_000L

    private const val GCM_TAG_BITS = 128
    private const val IV_BYTES = 12
    private const val HKDF_INFO = "money-sync-v1"
    private val random = SecureRandom()

    fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"), random)
        return generator.generateKeyPair()
    }

    fun publicKeyFrom(bytes: ByteArray): PublicKey =
        KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes))

    fun sharedKey(privateKey: PrivateKey, peerPublic: PublicKey): ByteArray {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(peerPublic, true)
        return hkdfSha256(agreement.generateSecret(), HKDF_INFO.toByteArray(Charsets.UTF_8), 32)
    }

    fun seal(key: ByteArray, plaintext: ByteArray): String {
        val iv = ByteArray(IV_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        val body = cipher.doFinal(plaintext)
        return base64(iv + body)
    }

    fun open(key: ByteArray, sealedText: String): ByteArray {
        val packed = base64Decode(sealedText)
        if (packed.size < IV_BYTES + (GCM_TAG_BITS / 8)) {
            error("Truncated sync frame")
        }
        val iv = packed.copyOfRange(0, IV_BYTES)
        val body = packed.copyOfRange(IV_BYTES, packed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(body)
    }

    fun pinMac(pin: String, publicKey: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(pin.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(publicKey)
    }

    fun pinMacMatches(pin: String, publicKey: ByteArray, presented: ByteArray): Boolean {
        val expected = pinMac(pin, publicKey)
        return MessageDigest.isEqual(expected, presented)
    }

    fun base64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    fun base64Decode(text: String): ByteArray =
        Base64.decode(text, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    private fun hkdfSha256(inputKey: ByteArray, info: ByteArray, length: Int): ByteArray {
        val extract = Mac.getInstance("HmacSHA256")
        extract.init(SecretKeySpec(ByteArray(32), "HmacSHA256"))
        val prk = extract.doFinal(inputKey)
        val expand = Mac.getInstance("HmacSHA256")
        expand.init(SecretKeySpec(prk, "HmacSHA256"))
        expand.update(info)
        expand.update(1)
        return expand.doFinal().copyOf(length)
    }
}
