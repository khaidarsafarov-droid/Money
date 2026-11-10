package com.example.money.data.local.migration

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Migration30To31Test {

    @Test
    fun migration30To31_copiesUpdatedAtOntoNoteLinesAndGoalActions() {
        val context: Context = RuntimeEnvironment.getApplication()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("migration-${UUID.randomUUID()}.db")
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(30) {
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
                CREATE TABLE `finance_notes` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `kind` TEXT NOT NULL,
                    `body` TEXT NOT NULL DEFAULT '',
                    `mark` TEXT NOT NULL DEFAULT 'NONE',
                    `closed` INTEGER NOT NULL DEFAULT 0,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    `sync_id` TEXT NOT NULL,
                    `mode` TEXT NOT NULL DEFAULT 'PERSONAL'
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE `finance_note_lines` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `note_id` INTEGER NOT NULL,
                    `label` TEXT NOT NULL,
                    `amount_cents` INTEGER NOT NULL,
                    `sort_order` INTEGER NOT NULL,
                    `sync_id` TEXT NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    FOREIGN KEY(`note_id`) REFERENCES `finance_notes`(`id`) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE `savings_goal` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE `goal_action` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `goal_id` INTEGER NOT NULL,
                    `title` TEXT NOT NULL,
                    `completed` INTEGER NOT NULL,
                    `sort_order` INTEGER NOT NULL,
                    `sync_id` TEXT NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    FOREIGN KEY(`goal_id`) REFERENCES `savings_goal`(`id`) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                "INSERT INTO `finance_notes` (id, title, kind, created_at, updated_at, sync_id) VALUES (1, 'Aka', 'FINANCIAL', 10, 11, 'note-1')",
            )
            database.execSQL(
                "INSERT INTO `finance_note_lines` (id, note_id, label, amount_cents, sort_order, sync_id, updated_at) VALUES (2, 1, 'Zelle', 750000, 0, 'line-1', 22)",
            )
            database.execSQL("INSERT INTO `savings_goal` (id, name) VALUES (3, 'Trip')")
            database.execSQL(
                "INSERT INTO `goal_action` (id, goal_id, title, completed, sort_order, sync_id, updated_at) VALUES (4, 3, 'Pack', 0, 0, 'action-1', 33)",
            )

            MIGRATION_30_31.migrate(database)

            assertEquals(22L, singleLong(database, "SELECT occurred_at FROM finance_note_lines"))
            assertEquals(33L, singleLong(database, "SELECT occurred_at FROM goal_action"))
            assertEquals("Zelle", singleString(database, "SELECT label FROM finance_note_lines"))
            assertEquals("Pack", singleString(database, "SELECT title FROM goal_action"))
        } finally {
            helper.close()
        }
    }

    private fun singleString(database: SupportSQLiteDatabase, sql: String): String {
        database.query(sql).use { cursor ->
            cursor.moveToFirst()
            return cursor.getString(0)
        }
    }

    private fun singleLong(database: SupportSQLiteDatabase, sql: String): Long {
        database.query(sql).use { cursor ->
            cursor.moveToFirst()
            return cursor.getLong(0)
        }
    }
}
