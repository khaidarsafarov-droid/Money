package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS business_category (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_business_category_sync_id ON business_category(sync_id)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_business_category_name_type ON business_category(name, type)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS business_asset (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL DEFAULT 'BANK',
                balance_cents INTEGER NOT NULL DEFAULT 0,
                currency TEXT NOT NULL DEFAULT 'USD',
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_business_asset_sync_id ON business_asset(sync_id)")

        db.execSQL("ALTER TABLE business_client ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE business_client ADD COLUMN created_at INTEGER NOT NULL DEFAULT $now")

        db.execSQL("ALTER TABLE business_project ADD COLUMN start_date INTEGER")
        db.execSQL("ALTER TABLE business_project ADD COLUMN end_date INTEGER")
        db.execSQL("ALTER TABLE business_project ADD COLUMN created_at INTEGER NOT NULL DEFAULT $now")

        db.execSQL("ALTER TABLE business_invoice ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD'")
        db.execSQL("ALTER TABLE business_invoice ADD COLUMN created_at INTEGER NOT NULL DEFAULT $now")

        db.execSQL("ALTER TABLE business_transaction ADD COLUMN category_id INTEGER")
        db.execSQL("ALTER TABLE business_transaction ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD'")
        db.execSQL("ALTER TABLE business_transaction ADD COLUMN payment_method TEXT NOT NULL DEFAULT 'OTHER'")
        db.execSQL("ALTER TABLE business_transaction ADD COLUMN tax_type TEXT")
        db.execSQL("ALTER TABLE business_transaction ADD COLUMN created_at INTEGER NOT NULL DEFAULT $now")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_transaction_category_id ON business_transaction(category_id)")

        seedCategories(db, now)
        db.execSQL(
            """
            UPDATE business_transaction
            SET category_id = (
                SELECT c.id FROM business_category c
                WHERE c.name = business_transaction.category
                LIMIT 1
            )
            WHERE category_id IS NULL
            """.trimIndent(),
        )

        db.execSQL(
            """
            INSERT OR IGNORE INTO business_asset (sync_id, name, type, balance_cents, currency, created_at)
            VALUES ('asset-default-cash', 'Main Cash', 'CASH', 0, 'USD', $now)
            """.trimIndent(),
        )
    }

    private fun seedCategories(db: SupportSQLiteDatabase, now: Long) {
        val income = listOf("Revenue", "Services", "Products", "Consulting", "Other income")
        val expense = listOf("COGS", "Marketing", "Software", "Travel", "Taxes", "Rent", "Payroll", "Utilities", "Other expense")
        income.forEach { name ->
            db.execSQL(
                "INSERT OR IGNORE INTO business_category (sync_id, name, type, created_at) VALUES (?, ?, 'INCOME', ?)",
                arrayOf("cat-income-$name", name, now),
            )
        }
        expense.forEach { name ->
            db.execSQL(
                "INSERT OR IGNORE INTO business_category (sync_id, name, type, created_at) VALUES (?, ?, 'EXPENSE', ?)",
                arrayOf("cat-expense-$name", name, now),
            )
        }
    }
}
