package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE debtor ADD COLUMN contact_lookup_key TEXT")
        db.execSQL("ALTER TABLE debtor ADD COLUMN contact_uri TEXT")
        db.execSQL("ALTER TABLE debtor ADD COLUMN contact_photo_uri TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_debtor_contact_lookup_key ON debtor(contact_lookup_key)")
    }
}
