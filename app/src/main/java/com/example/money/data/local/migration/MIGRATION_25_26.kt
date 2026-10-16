package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Stores transfer and expense amounts as integer cents. Existing REAL values
 * are converted with ROUND(amount * 100) so a stored 0.10 becomes 10 cents.
 */
val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys = OFF")
        rebuildTransfers(db)
        rebuildExpenses(db)
        db.execSQL("PRAGMA foreign_keys = ON")
    }

    private fun rebuildTransfers(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transfers_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sync_id` TEXT NOT NULL,
                `amount_cents` INTEGER NOT NULL,
                `date` INTEGER NOT NULL,
                `isConfirmed` INTEGER NOT NULL,
                `goalId` INTEGER,
                `goal_sync_id` TEXT,
                `debt_id` INTEGER,
                `debt_payment_id` INTEGER,
                `note` TEXT NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO transfers_new (
                id, sync_id, amount_cents, date, isConfirmed, goalId, goal_sync_id,
                debt_id, debt_payment_id, note
            )
            SELECT id, sync_id, CAST(ROUND(amount * 100) AS INTEGER), date, isConfirmed, goalId,
                   goal_sync_id, debt_id, debt_payment_id, note
            FROM transfers
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE transfers")
        db.execSQL("ALTER TABLE transfers_new RENAME TO transfers")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_transfers_sync_id` ON `transfers` (`sync_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transfers_isConfirmed` ON `transfers` (`isConfirmed`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transfers_goalId` ON `transfers` (`goalId`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_transfers_debt_id` ON `transfers` (`debt_id`)",
        )
    }

    private fun rebuildExpenses(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `expenses_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `sync_id` TEXT NOT NULL,
                `amount_cents` INTEGER NOT NULL,
                `category` TEXT NOT NULL,
                `date` INTEGER NOT NULL,
                `note` TEXT NOT NULL,
                `isConfirmed` INTEGER NOT NULL,
                `debt_id` INTEGER,
                `debt_payment_id` INTEGER,
                `goalId` INTEGER,
                `goal_sync_id` TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO expenses_new (
                id, sync_id, amount_cents, category, date, note, isConfirmed,
                debt_id, debt_payment_id, goalId, goal_sync_id
            )
            SELECT id, sync_id, CAST(ROUND(amount * 100) AS INTEGER), category, date, note,
                   isConfirmed, debt_id, debt_payment_id, goalId, goal_sync_id
            FROM expenses
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE expenses")
        db.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_expenses_sync_id` ON `expenses` (`sync_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_expenses_isConfirmed` ON `expenses` (`isConfirmed`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_expenses_debt_id` ON `expenses` (`debt_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_expenses_goalId` ON `expenses` (`goalId`)",
        )
    }
}
