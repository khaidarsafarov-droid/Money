package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE debtor ADD COLUMN loan_category TEXT NOT NULL DEFAULT 'OTHER'",
        )
        db.execSQL(
            "ALTER TABLE debtor ADD COLUMN interest_rate_bps INTEGER NOT NULL DEFAULT 0",
        )
    }
}
