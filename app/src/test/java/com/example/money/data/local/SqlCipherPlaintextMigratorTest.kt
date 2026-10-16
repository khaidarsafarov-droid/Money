package com.example.money.data.local

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SqlCipherPlaintextMigratorTest {

    @Test
    fun migrate_keepsTheOriginalFileWhenExportFails() {
        val directory = File.createTempFile("money-db", "").apply {
            delete()
            mkdirs()
        }
        val database = File(directory, "money.db")
        database.writeBytes(sqliteHeader(userVersion = 27) + "original".encodeToByteArray())

        val failed = runCatching {
            SqlCipherPlaintextMigrator.migrate(database) { _, _ ->
                error("export failed")
            }
        }

        assertTrue(failed.isFailure)
        assertEquals("original", database.readBytes().drop(100).toByteArray().decodeToString())
        assertFalse(File(directory, "money.db.encrypting").exists())
        assertFalse(File(directory, "money.db.plaintext-backup").exists())
        directory.deleteRecursively()
    }

    @Test
    fun migrate_replacesThePlainFileOnlyAfterExport() {
        val directory = File.createTempFile("money-db", "").apply {
            delete()
            mkdirs()
        }
        val database = File(directory, "money.db")
        database.writeBytes(sqliteHeader(userVersion = 27))

        SqlCipherPlaintextMigrator.migrate(database) { _, encrypted ->
            encrypted.writeBytes("encrypted".encodeToByteArray())
        }

        assertEquals("encrypted", database.readText())
        assertFalse(SqlCipherPlaintextMigrator.isPlainSqlite(database))
        assertFalse(File(directory, "money.db.plaintext-backup").exists())
        directory.deleteRecursively()
    }

    private fun sqliteHeader(userVersion: Int): ByteArray {
        val header = ByteArray(100)
        "SQLite format 3\u0000".encodeToByteArray().copyInto(header)
        header[60] = (userVersion ushr 24).toByte()
        header[61] = (userVersion ushr 16).toByte()
        header[62] = (userVersion ushr 8).toByte()
        header[63] = userVersion.toByte()
        return header
    }
}
