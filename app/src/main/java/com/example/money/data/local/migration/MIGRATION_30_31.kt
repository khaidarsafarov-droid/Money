package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Note lines and goal actions keep a user-visible date. Existing rows inherit
 * their last update time so older records still sort and display a date.
 */
val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `finance_note_lines` ADD COLUMN `occurred_at` INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "UPDATE `finance_note_lines` SET `occurred_at` = `updated_at` WHERE `occurred_at` = 0",
        )
        db.execSQL(
            "ALTER TABLE `goal_action` ADD COLUMN `occurred_at` INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "UPDATE `goal_action` SET `occurred_at` = `updated_at` WHERE `occurred_at` = 0",
        )
    }
}
