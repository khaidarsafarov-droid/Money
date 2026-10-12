package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Notes, including financial calculator lines. */
val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `finance_notes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
                `body` TEXT NOT NULL,
                `mark` TEXT NOT NULL,
                `closed` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `finance_note_lines` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `note_id` INTEGER NOT NULL,
                `label` TEXT NOT NULL,
                `amount_cents` INTEGER NOT NULL,
                `sort_order` INTEGER NOT NULL,
                FOREIGN KEY(`note_id`) REFERENCES `finance_notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_finance_note_lines_note_id` ON `finance_note_lines` (`note_id`)",
        )
    }
}
