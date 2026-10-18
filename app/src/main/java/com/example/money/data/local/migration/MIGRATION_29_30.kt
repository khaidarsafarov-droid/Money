package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Each disbursement of a debt is stored separately. Existing debts keep their
 * amount as the first disbursement, dated with the debt's creation date.
 * The opening sync id is derived from the debtor sync id so two devices that
 * migrate the same debt do not duplicate the row when they sync.
 */
val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `debt_advance` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sync_id` TEXT NOT NULL,
                `debtor_id` INTEGER NOT NULL,
                `amount_cents` INTEGER NOT NULL,
                `currency` TEXT NOT NULL,
                `given_at` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `mode` TEXT NOT NULL DEFAULT 'PERSONAL',
                `updated_at` INTEGER NOT NULL,
                FOREIGN KEY(`debtor_id`) REFERENCES `debtor`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_debt_advance_debtor_id` ON `debt_advance` (`debtor_id`)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_debt_advance_sync_id` ON `debt_advance` (`sync_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_debt_advance_mode` ON `debt_advance` (`mode`)",
        )
        db.execSQL(
            """
            INSERT INTO `debt_advance` (
                `sync_id`, `debtor_id`, `amount_cents`, `currency`, `given_at`, `note`, `mode`, `updated_at`
            )
            SELECT 'open-' || `sync_id`, `id`, `target_amount_cents`, `currency`, `date_given`, '', `mode`, `date_given`
            FROM `debtor`
            """.trimIndent(),
        )
    }
}
