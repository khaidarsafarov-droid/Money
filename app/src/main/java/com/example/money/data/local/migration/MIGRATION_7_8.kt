package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE debtor ADD COLUMN debt_type TEXT NOT NULL DEFAULT 'CREDITOR'",
        )
        db.execSQL(
            "ALTER TABLE debtor ADD COLUMN affects_balance INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "ALTER TABLE debtor ADD COLUMN note TEXT NOT NULL DEFAULT ''",
        )
        db.execSQL(
            "ALTER TABLE debt_payment ADD COLUMN affects_balance INTEGER NOT NULL DEFAULT 1",
        )
        db.execSQL(
            "ALTER TABLE transfers ADD COLUMN debt_id INTEGER DEFAULT NULL",
        )
        db.execSQL(
            "ALTER TABLE transfers ADD COLUMN debt_payment_id INTEGER DEFAULT NULL",
        )
        db.execSQL(
            "ALTER TABLE transfers ADD COLUMN note TEXT NOT NULL DEFAULT ''",
        )
        db.execSQL(
            "ALTER TABLE expenses ADD COLUMN debt_id INTEGER DEFAULT NULL",
        )
        db.execSQL(
            "ALTER TABLE expenses ADD COLUMN debt_payment_id INTEGER DEFAULT NULL",
        )
    }
}
