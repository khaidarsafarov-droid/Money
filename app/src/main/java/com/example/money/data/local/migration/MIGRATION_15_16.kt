package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS business_sms_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                recipient_phone TEXT NOT NULL,
                recipient_name TEXT NOT NULL DEFAULT '',
                client_id INTEGER,
                body TEXT NOT NULL,
                template_id TEXT,
                status TEXT NOT NULL DEFAULT 'PENDING',
                created_at INTEGER NOT NULL,
                FOREIGN KEY(client_id) REFERENCES business_client(id) ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_sms_log_client_id ON business_sms_log(client_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_sms_log_created_at ON business_sms_log(created_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_sms_log_status ON business_sms_log(status)")
    }
}
