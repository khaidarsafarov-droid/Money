package com.example.money.data.backup

import com.example.money.domain.backup.BackupAuthenticationException
import com.example.money.domain.backup.BackupEnvelopeException
import com.example.money.domain.backup.UnsupportedBackupEnvelopeVersionException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordBackupCryptoTest {

    private val crypto = PasswordBackupCrypto()
    private val password = "correct horse battery staple".toCharArray()
    private val plaintext = """{"portable":"backup"}""".toByteArray(Charsets.UTF_8)

    @Test
    fun encryptDecrypt_roundTripsUsingVersionedEnvelope() {
        val envelope = crypto.encrypt(plaintext, password)

        val decrypted = crypto.decrypt(envelope, password)

        assertArrayEquals(plaintext, decrypted)
        assertTrue(envelope.copyOfRange(0, PasswordBackupCrypto.MAGIC_SIZE).contentEquals(PasswordBackupCrypto.MAGIC))
        assertFalse(envelope.contentEquals(plaintext))
    }

    @Test
    fun decrypt_rejectsWrongPasswordWithAuthenticationError() {
        val envelope = crypto.encrypt(plaintext, password)

        assertThrows(BackupAuthenticationException::class.java) {
            crypto.decrypt(envelope, "wrong password".toCharArray())
        }
    }

    @Test
    fun decrypt_rejectsCorruptedCiphertextWithAuthenticationError() {
        val envelope = crypto.encrypt(plaintext, password).copyOf()
        envelope[envelope.lastIndex] = (envelope.last().toInt() xor 0x01).toByte()

        assertThrows(BackupAuthenticationException::class.java) {
            crypto.decrypt(envelope, password)
        }
    }

    @Test
    fun decrypt_rejectsUnsupportedEnvelopeVersionBeforeDecryption() {
        val envelope = crypto.encrypt(plaintext, password).copyOf()
        envelope[PasswordBackupCrypto.MAGIC_SIZE] = 99

        val error = assertThrows(UnsupportedBackupEnvelopeVersionException::class.java) {
            crypto.decrypt(envelope, password)
        }

        assertTrue(error.actualVersion == 99)
    }

    @Test
    fun decrypt_rejectsInvalidMagicAndTruncatedEnvelope() {
        val invalidMagic = crypto.encrypt(plaintext, password).copyOf()
        invalidMagic[0] = (invalidMagic[0].toInt() xor 0x01).toByte()

        assertThrows(BackupEnvelopeException::class.java) {
            crypto.decrypt(invalidMagic, password)
        }
        assertThrows(BackupEnvelopeException::class.java) {
            crypto.decrypt(ByteArray(4), password)
        }
    }

    @Test
    fun encrypt_rejectsEmptyPassword() {
        assertThrows(BackupEnvelopeException::class.java) {
            crypto.encrypt(plaintext, CharArray(0))
        }
    }
}
