package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE transfers ADD COLUMN isConfirmed INTEGER NOT NULL DEFAULT 1",
        )
        db.execSQL(
            "ALTER TABLE expenses ADD COLUMN isConfirmed INTEGER NOT NULL DEFAULT 1",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sync_queue (
                id TEXT NOT NULL PRIMARY KEY,
                entityType TEXT NOT NULL,
                entityId INTEGER NOT NULL,
                payloadJson TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS debtor_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                target_amount_cents INTEGER NOT NULL,
                current_amount_cents INTEGER NOT NULL DEFAULT 0,
                currency TEXT NOT NULL DEFAULT 'USD',
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                date_given INTEGER NOT NULL,
                date_due INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO debtor_new (
                id, name, target_amount_cents, current_amount_cents,
                currency, status, date_given, date_due
            )
            SELECT
                id,
                name,
                CAST(ROUND(amount * 100) AS INTEGER),
                CASE WHEN is_paid = 1 THEN CAST(ROUND(amount * 100) AS INTEGER) ELSE 0 END,
                'USD',
                CASE WHEN is_paid = 1 THEN 'COMPLETED' ELSE 'ACTIVE' END,
                date_given,
                date_due
            FROM debtor
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE debtor")
        db.execSQL("ALTER TABLE debtor_new RENAME TO debtor")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS debt_payment (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                debtor_id INTEGER NOT NULL,
                amount_cents INTEGER NOT NULL,
                currency TEXT NOT NULL,
                paid_at INTEGER NOT NULL,
                FOREIGN KEY(debtor_id) REFERENCES debtor(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_payment_debtor_id ON debt_payment(debtor_id)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS savings_goal_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL DEFAULT '',
                target_amount_cents INTEGER NOT NULL,
                current_amount_cents INTEGER NOT NULL DEFAULT 0,
                currency TEXT NOT NULL DEFAULT 'USD',
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            INSERT INTO savings_goal_new (
                id, name, target_amount_cents, current_amount_cents, currency, updated_at
            )
            SELECT
                id,
                'Финансовая цель',
                CAST(ROUND(target_amount * 100) AS INTEGER),
                0,
                'USD',
                updated_at
            FROM savings_goal
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE savings_goal")
        db.execSQL("ALTER TABLE savings_goal_new RENAME TO savings_goal")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE debtor SET currency = 'USD' WHERE currency != 'USD'")
        db.execSQL("UPDATE debt_payment SET currency = 'USD' WHERE currency != 'USD'")
        db.execSQL("UPDATE savings_goal SET currency = 'USD' WHERE currency != 'USD'")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE savings_goal ADD COLUMN deadline_ms INTEGER DEFAULT NULL",
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE transfers ADD COLUMN goalId INTEGER DEFAULT NULL",
        )

        db.query("SELECT id, current_amount_cents FROM savings_goal WHERE current_amount_cents > 0").use { cursor ->
            while (cursor.moveToNext()) {
                val goalId = cursor.getLong(0)
                val cents = cursor.getLong(1)
                val amount = cents / 100.0
                val now = System.currentTimeMillis()
                db.execSQL(
                    """
                    INSERT INTO transfers (amount, date, isConfirmed, goalId)
                    VALUES (?, ?, 1, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(amount, now, goalId),
                )
            }
        }

        db.query(
            """
            SELECT COALESCE(SUM(current_amount_cents), 0)
            FROM savings_goal
            WHERE current_amount_cents > 0
            """.trimIndent(),
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                val totalCents = cursor.getLong(0)
                if (totalCents > 0) {
                    val amount = totalCents / 100.0
                    val now = System.currentTimeMillis()
                    db.execSQL(
                        """
                        INSERT INTO transfers (amount, date, isConfirmed, goalId)
                        VALUES (?, ?, 1, NULL)
                        """.trimIndent(),
                        arrayOf<Any?>(amount, now),
                    )
                }
            }
        }
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateSavingsGoal(db)
        migrateDebtor(db)
        migrateDebtPayment(db)
        migrateTransfers(db)
        migrateExpenses(db)
    }

    private fun migrateSavingsGoal(db: SupportSQLiteDatabase) {
        val hasSyncId = columnExists(db, "savings_goal", "sync_id")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS savings_goal_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                name TEXT NOT NULL,
                target_amount_cents INTEGER NOT NULL,
                current_amount_cents INTEGER NOT NULL,
                currency TEXT NOT NULL,
                updated_at INTEGER NOT NULL,
                deadline_ms INTEGER
            )
            """.trimIndent(),
        )
        val query = if (hasSyncId) {
            """
            SELECT id, sync_id, name, target_amount_cents, current_amount_cents, currency, updated_at, deadline_ms
            FROM savings_goal
            """.trimIndent()
        } else {
            """
            SELECT id, name, target_amount_cents, current_amount_cents, currency, updated_at, deadline_ms
            FROM savings_goal
            """.trimIndent()
        }
        db.query(query).use { cursor ->
            while (cursor.moveToNext()) {
                var index = 0
                val id = cursor.getLong(index++)
                val syncId = if (hasSyncId) {
                    cursor.getString(index++)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                } else {
                    UUID.randomUUID().toString()
                }
                val name = cursor.getString(index++)
                val targetCents = cursor.getLong(index++)
                val currentCents = cursor.getLong(index++)
                val currency = cursor.getString(index++)
                val updatedAt = cursor.getLong(index++)
                val deadline = if (cursor.isNull(index)) null else cursor.getLong(index)
                db.execSQL(
                    """
                    INSERT INTO savings_goal_new (
                        id, sync_id, name, target_amount_cents, current_amount_cents, currency, updated_at, deadline_ms
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(id, syncId, name, targetCents, currentCents, currency, updatedAt, deadline),
                )
            }
        }
        db.execSQL("DROP TABLE savings_goal")
        db.execSQL("ALTER TABLE savings_goal_new RENAME TO savings_goal")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_savings_goal_sync_id ON savings_goal(sync_id)")
    }

    private fun migrateDebtor(db: SupportSQLiteDatabase) {
        val hasSyncId = columnExists(db, "debtor", "sync_id")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS debtor_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                name TEXT NOT NULL,
                target_amount_cents INTEGER NOT NULL,
                current_amount_cents INTEGER NOT NULL,
                currency TEXT NOT NULL,
                status TEXT NOT NULL,
                date_given INTEGER NOT NULL,
                date_due INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        val query = if (hasSyncId) {
            """
            SELECT id, sync_id, name, target_amount_cents, current_amount_cents, currency, status, date_given, date_due
            FROM debtor
            """.trimIndent()
        } else {
            """
            SELECT id, name, target_amount_cents, current_amount_cents, currency, status, date_given, date_due
            FROM debtor
            """.trimIndent()
        }
        db.query(query).use { cursor ->
            while (cursor.moveToNext()) {
                var index = 0
                val id = cursor.getLong(index++)
                val syncId = if (hasSyncId) {
                    cursor.getString(index++)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                } else {
                    UUID.randomUUID().toString()
                }
                val name = cursor.getString(index++)
                val targetCents = cursor.getLong(index++)
                val currentCents = cursor.getLong(index++)
                val currency = cursor.getString(index++)
                val status = cursor.getString(index++)
                val dateGiven = cursor.getLong(index++)
                val dateDue = cursor.getLong(index)
                db.execSQL(
                    """
                    INSERT INTO debtor_new (
                        id, sync_id, name, target_amount_cents, current_amount_cents, currency, status, date_given, date_due
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(id, syncId, name, targetCents, currentCents, currency, status, dateGiven, dateDue),
                )
            }
        }
        db.execSQL("DROP TABLE debtor")
        db.execSQL("ALTER TABLE debtor_new RENAME TO debtor")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_debtor_sync_id ON debtor(sync_id)")
    }

    private fun migrateDebtPayment(db: SupportSQLiteDatabase) {
        val hasSyncId = columnExists(db, "debt_payment", "sync_id")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS debt_payment_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                debtor_id INTEGER NOT NULL,
                amount_cents INTEGER NOT NULL,
                currency TEXT NOT NULL,
                paid_at INTEGER NOT NULL,
                FOREIGN KEY(debtor_id) REFERENCES debtor(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_payment_new_debtor_id ON debt_payment_new(debtor_id)")
        val query = if (hasSyncId) {
            """
            SELECT id, sync_id, debtor_id, amount_cents, currency, paid_at
            FROM debt_payment
            """.trimIndent()
        } else {
            """
            SELECT id, debtor_id, amount_cents, currency, paid_at
            FROM debt_payment
            """.trimIndent()
        }
        db.query(query).use { cursor ->
            while (cursor.moveToNext()) {
                var index = 0
                val id = cursor.getLong(index++)
                val syncId = if (hasSyncId) {
                    cursor.getString(index++)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                } else {
                    UUID.randomUUID().toString()
                }
                val debtorId = cursor.getLong(index++)
                val amountCents = cursor.getLong(index++)
                val currency = cursor.getString(index++)
                val paidAt = cursor.getLong(index++)
                db.execSQL(
                    """
                    INSERT INTO debt_payment_new (id, sync_id, debtor_id, amount_cents, currency, paid_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(id, syncId, debtorId, amountCents, currency, paidAt),
                )
            }
        }
        db.execSQL("DROP TABLE debt_payment")
        db.execSQL("ALTER TABLE debt_payment_new RENAME TO debt_payment")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_payment_debtor_id ON debt_payment(debtor_id)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_debt_payment_sync_id ON debt_payment(sync_id)")
    }

    private fun migrateTransfers(db: SupportSQLiteDatabase) {
        val hasSyncId = columnExists(db, "transfers", "sync_id")
        val hasGoalSyncId = columnExists(db, "transfers", "goal_sync_id")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transfers_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                amount REAL NOT NULL,
                date INTEGER NOT NULL,
                isConfirmed INTEGER NOT NULL,
                goalId INTEGER,
                goal_sync_id TEXT
            )
            """.trimIndent(),
        )
        val query = when {
            hasSyncId && hasGoalSyncId ->
                "SELECT id, sync_id, amount, date, isConfirmed, goalId, goal_sync_id FROM transfers"
            hasSyncId ->
                "SELECT id, sync_id, amount, date, isConfirmed, goalId FROM transfers"
            else ->
                "SELECT id, amount, date, isConfirmed, goalId FROM transfers"
        }
        db.query(query).use { cursor ->
            while (cursor.moveToNext()) {
                var index = 0
                val id = cursor.getLong(index++)
                val syncId = if (hasSyncId) {
                    cursor.getString(index++)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                } else {
                    UUID.randomUUID().toString()
                }
                val amount = cursor.getDouble(index++)
                val date = cursor.getLong(index++)
                val isConfirmed = cursor.getInt(index++) == 1
                val goalId: Long? = if (cursor.isNull(index)) {
                    index++
                    null
                } else {
                    cursor.getLong(index++)
                }
                val goalSyncId = when {
                    hasGoalSyncId -> {
                        if (cursor.isNull(index)) {
                            index++
                            null
                        } else {
                            cursor.getString(index++)
                        }
                    }
                    goalId != null -> lookupGoalSyncId(db, goalId)
                    else -> null
                }
                db.execSQL(
                    """
                    INSERT INTO transfers_new (id, sync_id, amount, date, isConfirmed, goalId, goal_sync_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(id, syncId, amount, date, if (isConfirmed) 1 else 0, goalId, goalSyncId),
                )
            }
        }
        db.execSQL("DROP TABLE transfers")
        db.execSQL("ALTER TABLE transfers_new RENAME TO transfers")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transfers_sync_id ON transfers(sync_id)")
    }

    private fun migrateExpenses(db: SupportSQLiteDatabase) {
        val hasSyncId = columnExists(db, "expenses", "sync_id")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS expenses_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                amount REAL NOT NULL,
                category TEXT NOT NULL,
                date INTEGER NOT NULL,
                note TEXT NOT NULL,
                isConfirmed INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        val query = if (hasSyncId) {
            "SELECT id, sync_id, amount, category, date, note, isConfirmed FROM expenses"
        } else {
            "SELECT id, amount, category, date, note, isConfirmed FROM expenses"
        }
        db.query(query).use { cursor ->
            while (cursor.moveToNext()) {
                var index = 0
                val id = cursor.getLong(index++)
                val syncId = if (hasSyncId) {
                    cursor.getString(index++)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                } else {
                    UUID.randomUUID().toString()
                }
                val amount = cursor.getDouble(index++)
                val category = cursor.getString(index++)
                val date = cursor.getLong(index++)
                val note = cursor.getString(index++)
                val isConfirmed = cursor.getInt(index++) == 1
                db.execSQL(
                    """
                    INSERT INTO expenses_new (id, sync_id, amount, category, date, note, isConfirmed)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(id, syncId, amount, category, date, note, if (isConfirmed) 1 else 0),
                )
            }
        }
        db.execSQL("DROP TABLE expenses")
        db.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_expenses_sync_id ON expenses(sync_id)")
    }

    private fun lookupGoalSyncId(db: SupportSQLiteDatabase, goalId: Long): String? {
        db.query("SELECT sync_id FROM savings_goal WHERE id = ?", arrayOf(goalId)).use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getString(0)
            }
        }
        return null
    }

    private fun columnExists(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return true
            }
        }
        return false
    }
}
