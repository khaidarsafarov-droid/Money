package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Expenses can be taken from a goal without touching the free wallet. */
val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE expenses ADD COLUMN goalId INTEGER")
        db.execSQL("ALTER TABLE expenses ADD COLUMN goal_sync_id TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_goalId` ON `expenses` (`goalId`)")
    }
}
