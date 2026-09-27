package com.example.money.data.backup

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EncryptedBackupCredentialCacheTest {

    @Test
    fun roundTripStoresOnlyWrappedBytesAndClearRemovesCredential() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences("credential-test", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
        val cipher = FakeCredentialCipher()
        val cache = EncryptedBackupCredentialCache(
            preferences = preferences,
            cipher = cipher,
            ioDispatcher = Dispatchers.Unconfined,
        )
        val password = "backup secret \uD83D\uDD10".toCharArray()

        cache.save(password)

        val storedText = preferences.all.values.joinToString()
        assertFalse(storedText.contains("backup secret"))
        assertArrayEquals(password, cache.load())

        cache.clear()

        assertNull(cache.load())
        assertTrue(preferences.all.isEmpty())
        assertTrue(cipher.keyCleared)
        password.fill('\u0000')
    }

    private class FakeCredentialCipher : BackupCredentialCipher {
        var keyCleared = false

        override fun encrypt(plaintext: ByteArray): WrappedBackupCredential =
            WrappedBackupCredential(
                ciphertext = plaintext.map { (it.toInt() xor MASK).toByte() }.toByteArray(),
                iv = byteArrayOf(1, 2, 3, 4),
            )

        override fun decrypt(credential: WrappedBackupCredential): ByteArray =
            credential.ciphertext.map { (it.toInt() xor MASK).toByte() }.toByteArray()

        override fun clearKey() {
            keyCleared = true
        }

        private companion object {
            const val MASK = 0x5a
        }
    }
}
