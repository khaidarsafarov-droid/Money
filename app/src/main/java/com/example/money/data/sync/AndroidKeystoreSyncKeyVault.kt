package com.example.money.data.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject

/**
 * The ECDH shared key cannot be imported into Android Keystore. A non-exportable
 * AES-256-GCM key is generated there and used to seal each peer key. The sealed
 * blob stays in app-private storage.
 */
class AndroidKeystoreSyncKeyVault(
    context: Context,
) : SyncKeyVault {
    private val file = context.applicationContext.getFileStreamPath(FILE_NAME)
    private val lock = Any()

    override fun find(peerId: String): PairedPeer? = synchronized(lock) {
        readAll().firstOrNull { it.peerId == peerId }
    }

    override fun save(peer: PairedPeer) {
        synchronized(lock) {
            val updated = readAll().filterNot { it.peerId == peer.peerId } + peer
            writeAll(updated)
        }
    }

    private fun readAll(): List<PairedPeer> {
        if (!file.exists()) return emptyList()
        val raw = file.readText()
        if (raw.isBlank()) return emptyList()
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val role = when (item.getString("role")) {
                    SyncPeerRole.HOST.name -> SyncPeerRole.HOST
                    SyncPeerRole.JOINER.name -> SyncPeerRole.JOINER
                    else -> error("Unknown sync peer role")
                }
                add(
                    PairedPeer(
                        peerId = item.getString("peerId"),
                        sharedKey = unseal(item.getString("sealed")),
                        role = role,
                    ),
                )
            }
        }
    }

    private fun writeAll(peers: List<PairedPeer>) {
        val array = JSONArray()
        peers.forEach { peer ->
            array.put(
                JSONObject()
                    .put("peerId", peer.peerId)
                    .put("role", peer.role.name)
                    .put("sealed", seal(peer.sharedKey)),
            )
        }
        file.writeText(array.toString())
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

    private fun seal(key: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey())
        val iv = cipher.iv
        val body = cipher.doFinal(key)
        return Base64.encodeToString(iv + body, Base64.NO_WRAP)
    }

    private fun unseal(sealed: String): ByteArray {
        val packed = Base64.decode(sealed, Base64.NO_WRAP)
        val iv = packed.copyOfRange(0, GCM_IV_BYTES)
        val body = packed.copyOfRange(GCM_IV_BYTES, packed.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(body)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "money.sync.peer-key"
        const val FILE_NAME = "sync-peer-keys.bin"
        const val GCM_IV_BYTES = 12
    }
}
