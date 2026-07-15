package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS business_workflow_category (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS business_workflow_process (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                category_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                FOREIGN KEY(category_id) REFERENCES business_workflow_category(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_business_workflow_process_category_id ON business_workflow_process(category_id)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS business_workflow_bill (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                process_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                total_amount_cents INTEGER NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                period_label TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                FOREIGN KEY(process_id) REFERENCES business_workflow_process(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_business_workflow_bill_process_id ON business_workflow_bill(process_id)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS business_workflow_participant (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                phone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                contact_uri TEXT,
                contact_lookup_key TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_business_workflow_participant_contact_lookup_key ON business_workflow_participant(contact_lookup_key)",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS business_workflow_debt (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                bill_id INTEGER NOT NULL,
                participant_id INTEGER NOT NULL,
                amount_cents INTEGER NOT NULL,
                paid_amount_cents INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'OPEN',
                created_at INTEGER NOT NULL,
                FOREIGN KEY(bill_id) REFERENCES business_workflow_bill(id) ON DELETE CASCADE,
                FOREIGN KEY(participant_id) REFERENCES business_workflow_participant(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_business_workflow_debt_bill_id ON business_workflow_debt(bill_id)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_business_workflow_debt_participant_id ON business_workflow_debt(participant_id)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_business_workflow_debt_status ON business_workflow_debt(status)",
        )

        val categories = listOf(
            Triple("Работа с клиентами", "BUSINESS", 0),
            Triple("Торговля", "TRADE", 1),
            Triple("Рабочие процессы", "WORK_PROCESS", 2),
            Triple("Семейные расходы", "FAMILY", 3),
            Triple("Общие счета", "SHARED", 4),
            Triple("Проекты", "PROJECTS", 5),
            Triple("Услуги", "SERVICES", 6),
            Triple("Подписки", "SUBSCRIPTIONS", 7),
        )
        categories.forEach { (name, type, order) ->
            db.execSQL(
                "INSERT INTO business_workflow_category (name, type, sort_order) VALUES ('$name', '$type', $order)",
            )
        }
    }
}
