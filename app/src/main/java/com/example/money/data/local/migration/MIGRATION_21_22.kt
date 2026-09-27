package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Persists P2P deletion records after the transient sync queue is cleared. */
val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sync_tombstone` (
                `entity_type` TEXT NOT NULL,
                `entity_id` TEXT NOT NULL,
                `payload_json` TEXT NOT NULL,
                `sync_updated_at` INTEGER NOT NULL,
                PRIMARY KEY(`entity_type`, `entity_id`)
            )
            """.trimIndent(),
        )
    }
}
