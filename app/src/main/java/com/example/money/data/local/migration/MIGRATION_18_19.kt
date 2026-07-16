package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE business_sms_log ADD COLUMN channel TEXT NOT NULL DEFAULT 'SMS'",
        )
        db.execSQL(
            "ALTER TABLE business_sms_log ADD COLUMN workflow_debt_id INTEGER",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_business_sms_log_workflow_debt_id ON business_sms_log(workflow_debt_id)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_business_sms_log_channel ON business_sms_log(channel)",
        )
    }
}
