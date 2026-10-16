package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Existing rows stay in the personal mode. Clients are a new professional-only table.
 */
val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `client` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sync_id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `phone` TEXT NOT NULL,
                `company` TEXT NOT NULL,
                `email` TEXT,
                `last_meeting_at` INTEGER,
                `note` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `sync_updated_at` INTEGER NOT NULL,
                `deleted` INTEGER NOT NULL,
                `mode` TEXT NOT NULL DEFAULT 'PROFESSIONAL'
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_client_sync_id` ON `client` (`sync_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_client_mode` ON `client` (`mode`)")

        listOf("debtor", "debt_payment", "transfers", "expenses", "savings_goal", "finance_notes").forEach { table ->
            db.execSQL("ALTER TABLE `$table` ADD COLUMN `mode` TEXT NOT NULL DEFAULT 'PERSONAL'")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_${table}_mode` ON `$table` (`mode`)")
        }
        db.execSQL("ALTER TABLE `debtor` ADD COLUMN `client_id` INTEGER REFERENCES `client`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_debtor_client_id` ON `debtor` (`client_id`)")
    }
}
