package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE debtor ADD COLUMN phone TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE debtor ADD COLUMN email TEXT NOT NULL DEFAULT ''")
    }
}
