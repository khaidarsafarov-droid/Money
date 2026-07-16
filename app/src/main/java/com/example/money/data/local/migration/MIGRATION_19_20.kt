package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS business_workflow_debt")
        db.execSQL("DROP TABLE IF EXISTS business_workflow_participant")
        db.execSQL("DROP TABLE IF EXISTS business_workflow_bill")
        db.execSQL("DROP TABLE IF EXISTS business_workflow_process")
        db.execSQL("DROP TABLE IF EXISTS business_workflow_category")
        db.execSQL("DROP TABLE IF EXISTS business_sms_log")
        db.execSQL("DROP TABLE IF EXISTS business_asset")
        db.execSQL("DROP TABLE IF EXISTS business_category")
        db.execSQL("DROP TABLE IF EXISTS business_transaction")
        db.execSQL("DROP TABLE IF EXISTS business_invoice")
        db.execSQL("DROP TABLE IF EXISTS business_project")
        db.execSQL("DROP TABLE IF EXISTS business_client")
    }
}
