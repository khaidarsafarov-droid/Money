package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE debtor ADD COLUMN calendar_event_id TEXT")
        db.execSQL("ALTER TABLE debtor ADD COLUMN calendar_sync_failed INTEGER NOT NULL DEFAULT 0")
    }
}
