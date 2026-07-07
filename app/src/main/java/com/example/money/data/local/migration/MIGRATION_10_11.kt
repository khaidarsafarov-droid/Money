package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS luck_diary_entry (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                entry_date INTEGER NOT NULL,
                note TEXT NOT NULL,
                amount_cents INTEGER,
                category TEXT NOT NULL DEFAULT 'OTHER',
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_luck_diary_entry_sync_id ON luck_diary_entry(sync_id)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_luck_diary_entry_entry_date ON luck_diary_entry(entry_date)",
        )
    }
}
