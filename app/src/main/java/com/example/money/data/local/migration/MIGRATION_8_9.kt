package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE debtor
            SET affects_balance = 1
            WHERE id IN (
                SELECT debt_id FROM expenses
                WHERE debt_id IS NOT NULL AND debt_payment_id IS NULL
                UNION
                SELECT debt_id FROM transfers
                WHERE debt_id IS NOT NULL AND debt_payment_id IS NULL
            )
            """.trimIndent(),
        )
    }
}
