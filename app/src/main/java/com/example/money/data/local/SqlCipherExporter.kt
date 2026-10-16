package com.example.money.data.local

import java.io.File
import net.zetetic.database.sqlcipher.SQLiteDatabase

internal object SqlCipherExporter {

    fun export(plainCopy: File, encryptedFile: File, passphrase: ByteArray) {
        System.loadLibrary("sqlcipher")
        val database = SQLiteDatabase.openOrCreateDatabase(
            encryptedFile,
            passphrase,
            null,
            null,
            null,
        )
        try {
            val path = plainCopy.absolutePath.replace("'", "''")
            database.rawExecSQL("ATTACH DATABASE '$path' AS plaintext KEY ''")
            database.rawExecSQL("SELECT sqlcipher_export('main', 'plaintext')")
            database.rawExecSQL("DETACH DATABASE plaintext")
            database.version = SqlCipherPlaintextMigrator.userVersion(plainCopy)
        } finally {
            database.close()
        }
    }
}
