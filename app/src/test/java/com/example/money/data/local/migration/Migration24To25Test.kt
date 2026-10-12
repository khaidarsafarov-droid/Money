package com.example.money.data.local.migration

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Migration24To25Test {

    @Test
    fun migration24To25_keepsTasksAndNoteLinesAndAssignsSyncIds() {
        val context: Context = RuntimeEnvironment.getApplication()
        val databaseName = "migration-${UUID.randomUUID()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(24) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int,
                        ) = Unit
                    },
                )
                .build(),
        )

        try {
            val database = helper.writableDatabase
            database.execSQL(
                """
                CREATE TABLE savings_goal (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sync_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    target_amount_cents INTEGER NOT NULL,
                    current_amount_cents INTEGER NOT NULL,
                    currency TEXT NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE goal_action (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    goal_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    completed INTEGER NOT NULL,
                    sort_order INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE finance_notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL,
                    kind TEXT NOT NULL,
                    body TEXT NOT NULL,
                    mark TEXT NOT NULL,
                    closed INTEGER NOT NULL,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE finance_note_lines (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    note_id INTEGER NOT NULL,
                    label TEXT NOT NULL,
                    amount_cents INTEGER NOT NULL,
                    sort_order INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL("INSERT INTO savings_goal (sync_id, name, target_amount_cents, current_amount_cents, currency, updated_at) VALUES ('g', 'Goal', 1, 0, 'USD', 1)")
            database.execSQL("INSERT INTO goal_action (goal_id, title, completed, sort_order) VALUES (1, 'Task', 1, 2)")
            database.execSQL("INSERT INTO finance_notes (title, kind, body, mark, closed, created_at, updated_at) VALUES ('House', 'FINANCIAL', '', 'NONE', 0, 3, 4)")
            database.execSQL("INSERT INTO finance_note_lines (note_id, label, amount_cents, sort_order) VALUES (1, 'Flour', 20000, 0)")

            MIGRATION_24_25.migrate(database)

            database.query("SELECT title, completed, sync_id FROM goal_action").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Task", cursor.getString(0))
                assertEquals(1, cursor.getInt(1))
                assertTrue(cursor.getString(2).isNotBlank())
            }
            database.query("SELECT label, amount_cents, note_id, sync_id FROM finance_note_lines").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Flour", cursor.getString(0))
                assertEquals(20_000, cursor.getInt(1))
                assertEquals(1, cursor.getInt(2))
                assertTrue(cursor.getString(3).isNotBlank())
            }
            database.query("SELECT sync_id FROM finance_notes").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.getString(0).isNotBlank())
            }
        } finally {
            helper.close()
            context.deleteDatabase(databaseName)
        }
    }
}
