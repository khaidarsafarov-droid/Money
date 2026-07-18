package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Hot-query indexes for debt lists, budget totals, and sync queue cleanup. */
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS index_debtor_status ON debtor(status)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_debtor_date_due ON debtor(date_due)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_debtor_debt_type ON debtor(debt_type)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_isConfirmed ON expenses(isConfirmed)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_debt_id ON expenses(debt_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transfers_isConfirmed ON transfers(isConfirmed)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transfers_goalId ON transfers(goalId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transfers_debt_id ON transfers(debt_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_sync_queue_entity ON sync_queue(entityType, entityId)")
    }
}
