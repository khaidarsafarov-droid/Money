package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Stable sync ids for goal tasks and notes so backup and full snapshots can
 * rebuild them on another device. Historical migrations before this one stay
 * untouched.
 */
val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = OFF")
        rebuildGoalActions(db)
        rebuildNotes(db)
        db.execSQL("PRAGMA foreign_keys = ON")
    }

    private fun rebuildGoalActions(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `goal_action_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `goal_id` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `completed` INTEGER NOT NULL,
                `sort_order` INTEGER NOT NULL,
                `sync_id` TEXT NOT NULL,
                `updated_at` INTEGER NOT NULL,
                FOREIGN KEY(`goal_id`) REFERENCES `savings_goal`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO goal_action_new (id, goal_id, title, completed, sort_order, sync_id, updated_at)
            SELECT id, goal_id, title, completed, sort_order,
                   lower(hex(randomblob(16))) || '-' || id,
                   0
            FROM goal_action
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE goal_action")
        db.execSQL("ALTER TABLE goal_action_new RENAME TO goal_action")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_action_goal_id` ON `goal_action` (`goal_id`)")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_goal_action_sync_id` ON `goal_action` (`sync_id`)",
        )
    }

    private fun rebuildNotes(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `finance_notes_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
                `body` TEXT NOT NULL,
                `mark` TEXT NOT NULL,
                `closed` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `sync_id` TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO finance_notes_new (
                id, title, kind, body, mark, closed, created_at, updated_at, sync_id
            )
            SELECT id, title, kind, body, mark, closed, created_at, updated_at,
                   lower(hex(randomblob(16))) || '-' || id
            FROM finance_notes
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `finance_note_lines_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `note_id` INTEGER NOT NULL,
                `label` TEXT NOT NULL,
                `amount_cents` INTEGER NOT NULL,
                `sort_order` INTEGER NOT NULL,
                `sync_id` TEXT NOT NULL,
                `updated_at` INTEGER NOT NULL,
                FOREIGN KEY(`note_id`) REFERENCES `finance_notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO finance_note_lines_new (
                id, note_id, label, amount_cents, sort_order, sync_id, updated_at
            )
            SELECT id, note_id, label, amount_cents, sort_order,
                   lower(hex(randomblob(16))) || '-' || id,
                   0
            FROM finance_note_lines
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE finance_note_lines")
        db.execSQL("DROP TABLE finance_notes")
        db.execSQL("ALTER TABLE finance_notes_new RENAME TO finance_notes")
        db.execSQL("ALTER TABLE finance_note_lines_new RENAME TO finance_note_lines")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_finance_notes_sync_id` ON `finance_notes` (`sync_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_finance_note_lines_note_id` ON `finance_note_lines` (`note_id`)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_finance_note_lines_sync_id` ON `finance_note_lines` (`sync_id`)",
        )
    }
}
