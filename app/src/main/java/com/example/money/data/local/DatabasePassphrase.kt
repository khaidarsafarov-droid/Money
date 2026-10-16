package com.example.money.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 256-bit SQLCipher passphrase sealed by a non-exportable AES-GCM key in Android Keystore.
 */
class DatabasePassphrase(
    context: Context,
) {
    private val file = context.applicationContext.getFileStreamPath(FILE_NAME)
    private val lock = Any()

    fun getOrCreate(): ByteArray = synchronized(lock) {
        if (file.exists() && file.length() > 0L) {
            return unseal(file.readText())
        }
        val passphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        file.writeText(seal(passphrase))
        passphrase
    }

    private fun masterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun seal(passphrase: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(passphrase), Base64.NO_WRAP)
    }

    private fun unseal(sealed: String): ByteArray {
        val packed = Base64.decode(sealed, Base64.NO_WRAP)
        val iv = packed.copyOfRange(0, GCM_IV_BYTES)
        val body = packed.copyOfRange(GCM_IV_BYTES, packed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(body)
    }

    companion object {
        const val FILE_NAME = "room-passphrase.bin"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "money.room.passphrase"
        private const val PASSPHRASE_BYTES = 32
        private const val GCM_IV_BYTES = 12
    }
}
