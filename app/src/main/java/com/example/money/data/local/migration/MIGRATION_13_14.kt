package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS business_client (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                name TEXT NOT NULL,
                contact_info TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                phone TEXT NOT NULL DEFAULT '',
                is_active INTEGER NOT NULL DEFAULT 1,
                note TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_business_client_sync_id ON business_client(sync_id)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS business_project (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                client_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                budget_cents INTEGER NOT NULL DEFAULT 0,
                note TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(client_id) REFERENCES business_client(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_business_project_sync_id ON business_project(sync_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_project_client_id ON business_project(client_id)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS business_invoice (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                client_id INTEGER NOT NULL,
                project_id INTEGER,
                issue_date INTEGER NOT NULL,
                due_date INTEGER NOT NULL,
                amount_cents INTEGER NOT NULL,
                status TEXT NOT NULL DEFAULT 'DRAFT',
                note TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(client_id) REFERENCES business_client(id) ON DELETE CASCADE,
                FOREIGN KEY(project_id) REFERENCES business_project(id) ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_business_invoice_sync_id ON business_invoice(sync_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_invoice_client_id ON business_invoice(client_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_invoice_project_id ON business_invoice(project_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_invoice_status ON business_invoice(status)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS business_transaction (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                type TEXT NOT NULL,
                amount_cents INTEGER NOT NULL,
                category TEXT NOT NULL,
                ledger_type TEXT NOT NULL DEFAULT 'OPERATING',
                client_id INTEGER,
                project_id INTEGER,
                invoice_id INTEGER,
                date INTEGER NOT NULL,
                note TEXT NOT NULL DEFAULT '',
                FOREIGN KEY(client_id) REFERENCES business_client(id) ON DELETE SET NULL,
                FOREIGN KEY(project_id) REFERENCES business_project(id) ON DELETE SET NULL,
                FOREIGN KEY(invoice_id) REFERENCES business_invoice(id) ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_business_transaction_sync_id ON business_transaction(sync_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_transaction_client_id ON business_transaction(client_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_transaction_project_id ON business_transaction(project_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_transaction_invoice_id ON business_transaction(invoice_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_transaction_date ON business_transaction(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_business_transaction_ledger_type ON business_transaction(ledger_type)")
    }
}
