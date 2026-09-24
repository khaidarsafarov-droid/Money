package com.example.money.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Goal kind (money or actions) and checklist steps stored on each goal. */
val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE savings_goal ADD COLUMN kind TEXT NOT NULL DEFAULT 'MONEY'",
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS goal_action (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                goal_id INTEGER NOT NULL,
                title TEXT NOT NULL,
                completed INTEGER NOT NULL,
                sort_order INTEGER NOT NULL,
                FOREIGN KEY(goal_id) REFERENCES savings_goal(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_goal_action_goal_id ON goal_action(goal_id)",
        )
    }
}
