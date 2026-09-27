package com.example.money.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupCredentialException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

interface BackupCredentialCache {
    suspend fun save(password: CharArray)

    suspend fun load(): CharArray?

    suspend fun clear()
}

internal data class WrappedBackupCredential(
    val ciphertext: ByteArray,
    val iv: ByteArray,
)

internal interface BackupCredentialCipher {
    fun encrypt(plaintext: ByteArray): WrappedBackupCredential

    fun decrypt(credential: WrappedBackupCredential): ByteArray

    fun clearKey()
}

class EncryptedBackupCredentialCache internal constructor(
    private val preferences: SharedPreferences,
    private val cipher: BackupCredentialCipher,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BackupCredentialCache {

    constructor(context: Context) : this(
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE),
        cipher = AndroidKeystoreBackupCredentialCipher(),
    )

    override suspend fun save(password: CharArray) = withContext(ioDispatcher) {
        if (password.isEmpty()) {
            throw BackupCredentialException("Backup password must not be empty")
        }
        val plaintext = password.toUtf8Bytes()
        try {
            val wrapped = cipher.encrypt(plaintext)
            val saved = preferences.edit()
                .putString(KEY_CIPHERTEXT, Base64.encodeToString(wrapped.ciphertext, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(wrapped.iv, Base64.NO_WRAP))
                .commit()
            if (!saved) {
                throw BackupCredentialException("Unable to persist encrypted backup credential")
            }
        } catch (error: BackupCredentialException) {
            throw error
        } catch (error: Exception) {
            throw BackupCredentialException("Unable to protect backup credential", error)
        } finally {
            plaintext.fill(0)
        }
    }

    override suspend fun load(): CharArray? = withContext(ioDispatcher) {
        val encodedCiphertext = preferences.getString(KEY_CIPHERTEXT, null)
        val encodedIv = preferences.getString(KEY_IV, null)
        if (encodedCiphertext == null && encodedIv == null) return@withContext null
        if (encodedCiphertext == null || encodedIv == null) {
            throw BackupCredentialException("Encrypted backup credential is incomplete")
        }
        val plaintext = try {
            cipher.decrypt(
                WrappedBackupCredential(
                    ciphertext = Base64.decode(encodedCiphertext, Base64.NO_WRAP),
                    iv = Base64.decode(encodedIv, Base64.NO_WRAP),
                ),
            )
        } catch (error: Exception) {
            throw BackupCredentialException("Unable to unlock backup credential", error)
        }
        try {
            plaintext.toUtf8Chars()
        } catch (error: Exception) {
            throw BackupCredentialException("Unable to decode backup credential", error)
        } finally {
            plaintext.fill(0)
        }
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        val cleared = preferences.edit().remove(KEY_CIPHERTEXT).remove(KEY_IV).commit()
        cipher.clearKey()
        if (!cleared) {
            throw BackupCredentialException("Unable to clear encrypted backup credential")
        }
    }

    private fun CharArray.toUtf8Bytes(): ByteArray {
        val encoded = Charsets.UTF_8.newEncoder().encode(CharBuffer.wrap(this))
        return ByteArray(encoded.remaining()).also(encoded::get)
    }

    private fun ByteArray.toUtf8Chars(): CharArray {
        val decoded = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(this))
        return CharArray(decoded.remaining()).also(decoded::get)
    }

    private companion object {
        const val PREFERENCES_NAME = "backup_credentials"
        const val KEY_CIPHERTEXT = "wrapped_password"
        const val KEY_IV = "wrapped_password_iv"
    }
}

private class AndroidKeystoreBackupCredentialCipher : BackupCredentialCipher {

    override fun encrypt(plaintext: ByteArray): WrappedBackupCredential {
        val encryptionCipher = Cipher.getInstance(TRANSFORMATION)
        encryptionCipher.init(Cipher.ENCRYPT_MODE, loadOrCreateKey())
        return WrappedBackupCredential(
            ciphertext = encryptionCipher.doFinal(plaintext),
            iv = encryptionCipher.iv.copyOf(),
        )
    }

    override fun decrypt(credential: WrappedBackupCredential): ByteArray {
        val key = loadKey()
            ?: throw BackupCredentialException("Backup credential key is unavailable")
        val decryptionCipher = Cipher.getInstance(TRANSFORMATION)
        decryptionCipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(GCM_TAG_BITS, credential.iv),
        )
        return decryptionCipher.doFinal(credential.ciphertext)
    }

    override fun clearKey() {
        keyStore().deleteEntry(KEY_ALIAS)
    }

    private fun loadOrCreateKey(): SecretKey = loadKey() ?: generateKey()

    private fun loadKey(): SecretKey? =
        keyStore().getKey(KEY_ALIAS, null) as? SecretKey

    private fun generateKey(): SecretKey {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                return generateKey(strongBoxBacked = true)
            } catch (_: StrongBoxUnavailableException) {
                // Android Keystore remains non-exportable when StrongBox is unavailable.
            }
        }
        return generateKey(strongBoxBacked = false)
    }

    private fun generateKey(strongBoxBacked: Boolean): SecretKey {
        val specBuilder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            specBuilder.setIsStrongBoxBacked(strongBoxBacked)
        }
        val key = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(specBuilder.build())
            generateKey()
        }
        return key
    }

    private fun keyStore(): KeyStore =
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "money_backup_password_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
    }
}
