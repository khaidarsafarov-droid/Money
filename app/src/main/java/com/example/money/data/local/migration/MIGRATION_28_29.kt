package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Notes gain pin, archive, soft-delete, and a link to a debt or goal.
 * kind, mark, closed, and finance_note_lines stay so older backups and the
 * migration wizard can still read them.
 */
val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `finance_notes` ADD COLUMN `pinned` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `finance_notes` ADD COLUMN `archived` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `finance_notes` SET `archived` = `closed`")
        db.execSQL("ALTER TABLE `finance_notes` ADD COLUMN `deleted_at` INTEGER")
        db.execSQL("ALTER TABLE `finance_notes` ADD COLUMN `linked_type` TEXT")
        db.execSQL("ALTER TABLE `finance_notes` ADD COLUMN `linked_sync_id` TEXT")
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_finance_notes_deleted_at_archived_pinned_updated_at`
            ON `finance_notes` (`deleted_at`, `archived`, `pinned`, `updated_at`)
            """.trimIndent(),
        )
    }
}
