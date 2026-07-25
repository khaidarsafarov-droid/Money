package com.example.money.data.backup

import com.example.money.domain.backup.BackupAuthenticationException
import com.example.money.domain.backup.BackupEnvelopeException
import com.example.money.domain.backup.UnsupportedBackupEnvelopeVersionException
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Pure-JVM password envelope. Android Keystore wrapping is deliberately a
 * separate concern so replacement devices can always decrypt with a password.
 */
class PasswordBackupCrypto(
    private val secureRandom: SecureRandom = SecureRandom(),
    private val maxPlaintextBytes: Int = BackupBundleCodec.DEFAULT_MAX_BUNDLE_BYTES,
) {

    init {
        require(maxPlaintextBytes >= 0) { "maxPlaintextBytes must not be negative" }
    }

    fun encrypt(plaintext: ByteArray, password: CharArray): ByteArray {
        validatePassword(password)
        if (plaintext.size > maxPlaintextBytes) {
            throw BackupEnvelopeException(
                "Backup plaintext exceeds maximum size of $maxPlaintextBytes bytes",
            )
        }

        val salt = ByteArray(SALT_SIZE).also(secureRandom::nextBytes)
        val iv = ByteArray(IV_SIZE).also(secureRandom::nextBytes)
        val header = createHeader(salt, iv)
        val keyBytes = deriveKey(password, salt)
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(keyBytes, AES),
                GCMParameterSpec(GCM_TAG_BITS, iv),
            )
            cipher.updateAAD(header)
            header + cipher.doFinal(plaintext)
        } catch (error: GeneralSecurityException) {
            throw BackupEnvelopeException("Unable to encrypt backup", error)
        } finally {
            keyBytes.fill(0)
        }
    }

    fun decrypt(envelope: ByteArray, password: CharArray): ByteArray {
        validatePassword(password)
        if (envelope.size < MIN_ENVELOPE_SIZE) {
            throw BackupEnvelopeException("Encrypted backup envelope is truncated")
        }
        if (envelope.size > maxPlaintextBytes + HEADER_SIZE + GCM_TAG_BYTES) {
            throw BackupEnvelopeException("Encrypted backup envelope exceeds the maximum size")
        }

        val magic = envelope.copyOfRange(0, MAGIC_SIZE)
        if (!MessageDigest.isEqual(MAGIC_BYTES, magic)) {
            throw BackupEnvelopeException("Encrypted backup envelope has invalid magic")
        }
        val version = envelope[MAGIC_SIZE].toInt() and 0xff
        if (version != CURRENT_ENVELOPE_VERSION) {
            throw UnsupportedBackupEnvelopeVersionException(version)
        }

        val saltStart = MAGIC_SIZE + VERSION_SIZE
        val ivStart = saltStart + SALT_SIZE
        val ciphertextStart = ivStart + IV_SIZE
        val salt = envelope.copyOfRange(saltStart, ivStart)
        val iv = envelope.copyOfRange(ivStart, ciphertextStart)
        val header = envelope.copyOfRange(0, ciphertextStart)
        val ciphertext = envelope.copyOfRange(ciphertextStart, envelope.size)
        val keyBytes = deriveKey(password, salt)
        return try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyBytes, AES),
                GCMParameterSpec(GCM_TAG_BITS, iv),
            )
            cipher.updateAAD(header)
            cipher.doFinal(ciphertext)
        } catch (error: BadPaddingException) {
            throw BackupAuthenticationException(error)
        } catch (error: GeneralSecurityException) {
            throw BackupEnvelopeException("Unable to decrypt backup envelope", error)
        } finally {
            keyBytes.fill(0)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): ByteArray {
        val keySpec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
                .generateSecret(keySpec)
                .encoded
        } catch (error: GeneralSecurityException) {
            throw BackupEnvelopeException("Unable to derive backup encryption key", error)
        } finally {
            keySpec.clearPassword()
        }
    }

    private fun validatePassword(password: CharArray) {
        if (password.isEmpty()) {
            throw BackupEnvelopeException("Backup password must not be empty")
        }
    }

    private fun createHeader(salt: ByteArray, iv: ByteArray): ByteArray =
        ByteArray(HEADER_SIZE).also { header ->
            MAGIC_BYTES.copyInto(header, destinationOffset = 0)
            header[MAGIC_SIZE] = CURRENT_ENVELOPE_VERSION.toByte()
            salt.copyInto(header, destinationOffset = MAGIC_SIZE + VERSION_SIZE)
            iv.copyInto(header, destinationOffset = MAGIC_SIZE + VERSION_SIZE + SALT_SIZE)
        }

    companion object {
        const val CURRENT_ENVELOPE_VERSION = 1
        const val MAGIC_SIZE = 8

        val MAGIC: ByteArray
            get() = MAGIC_BYTES.copyOf()

        private const val VERSION_SIZE = 1
        private const val SALT_SIZE = 16
        private const val IV_SIZE = 12
        private const val GCM_TAG_BITS = 128
        private const val GCM_TAG_BYTES = GCM_TAG_BITS / 8
        private const val AES_KEY_BITS = 256
        private const val PBKDF2_ITERATIONS = 210_000
        private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val AES = "AES"
        private const val HEADER_SIZE = MAGIC_SIZE + VERSION_SIZE + SALT_SIZE + IV_SIZE
        private const val MIN_ENVELOPE_SIZE = HEADER_SIZE + GCM_TAG_BYTES

        private val MAGIC_BYTES = byteArrayOf(
            'M'.code.toByte(),
            'O'.code.toByte(),
            'N'.code.toByte(),
            'E'.code.toByte(),
            'Y'.code.toByte(),
            'B'.code.toByte(),
            'K'.code.toByte(),
            'P'.code.toByte(),
        )
    }
}
