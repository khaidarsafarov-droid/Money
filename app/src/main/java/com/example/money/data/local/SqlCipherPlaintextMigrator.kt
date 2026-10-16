package com.example.money.data.local

import java.io.File

/**
 * Copies an existing plaintext SQLite file into SQLCipher. The original file stays
 * in place until the encrypted file is ready, and is restored if the swap fails.
 */
internal object SqlCipherPlaintextMigrator {

    fun isPlainSqlite(file: File): Boolean {
        if (!file.isFile || file.length() < HEADER.size) return false
        val header = ByteArray(HEADER.size)
        file.inputStream().use { input ->
            if (input.read(header) != HEADER.size) return false
        }
        return header.contentEquals(HEADER)
    }

    fun userVersion(file: File): Int {
        val header = ByteArray(68)
        file.inputStream().use { input ->
            check(input.read(header) == header.size)
        }
        return ((header[60].toInt() and 0xff) shl 24) or
            ((header[61].toInt() and 0xff) shl 16) or
            ((header[62].toInt() and 0xff) shl 8) or
            (header[63].toInt() and 0xff)
    }

    fun migrate(
        databaseFile: File,
        export: (plainCopy: File, encryptedFile: File) -> Unit,
    ) {
        if (!isPlainSqlite(databaseFile)) return
        val parent = databaseFile.parentFile ?: return
        val backup = File(parent, databaseFile.name + ".plaintext-backup")
        val encrypted = File(parent, databaseFile.name + ".encrypting")
        databaseFile.copyTo(backup, overwrite = true)
        try {
            if (encrypted.exists()) encrypted.delete()
            export(backup, encrypted)
            if (!encrypted.isFile || encrypted.length() == 0L) {
                error("Encrypted database was not created")
            }
            if (!databaseFile.delete()) {
                error("Could not remove the plaintext database")
            }
            if (!encrypted.renameTo(databaseFile)) {
                backup.copyTo(databaseFile, overwrite = true)
                error("Could not move the encrypted database into place")
            }
            backup.delete()
        } catch (error: Exception) {
            if (encrypted.exists()) encrypted.delete()
            if (!databaseFile.exists() && backup.exists()) {
                backup.copyTo(databaseFile, overwrite = true)
            }
            if (databaseFile.exists() && backup.exists()) {
                backup.delete()
            }
            throw error
        }
    }

    private val HEADER = "SQLite format 3\u0000".encodeToByteArray()
}
