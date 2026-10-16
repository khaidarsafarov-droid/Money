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
class Migration28To29Test {

    @Test
    fun migration28To29_archivesClosedNotesAndKeepsLines() {
        val context: Context = RuntimeEnvironment.getApplication()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("migration-${UUID.randomUUID()}.db")
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(28) {
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
                    `id` INTEGER PRIMARY KEY NOT NULL,
                    `title` TEXT NOT NULL,
                    `kind` TEXT NOT NULL,
                    `body` TEXT NOT NULL,
                    `mark` TEXT NOT NULL,
                    `closed` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    `updated_at` INTEGER NOT NULL,
                    `sync_id` TEXT NOT NULL,
                    `mode` TEXT NOT NULL DEFAULT 'PERSONAL'
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO `finance_notes`
                    (id, title, kind, body, mark, closed, created_at, updated_at, sync_id, mode)
                VALUES (1, 'Rent', 'FINANCIAL', 'kept', 'OWED', 1, 10, 20, 'note-1', 'PERSONAL')
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE `finance_note_lines` (
                    `id` INTEGER PRIMARY KEY NOT NULL,
                    `note_id` INTEGER NOT NULL,
                    `label` TEXT NOT NULL,
                    `amount_cents` INTEGER NOT NULL,
                    `sort_order` INTEGER NOT NULL,
                    `sync_id` TEXT NOT NULL,
                    `updated_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO `finance_note_lines`
                    (id, note_id, label, amount_cents, sort_order, sync_id, updated_at)
                VALUES (7, 1, 'part', 1500, 0, 'line-1', 20)
                """.trimIndent(),
            )

            MIGRATION_28_29.migrate(database)

            assertEquals("Rent", singleString(database, "SELECT title FROM finance_notes"))
            assertEquals("kept", singleString(database, "SELECT body FROM finance_notes"))
            assertEquals(1, singleInt(database, "SELECT closed FROM finance_notes"))
            assertEquals(1, singleInt(database, "SELECT archived FROM finance_notes"))
            assertEquals(0, singleInt(database, "SELECT pinned FROM finance_notes"))
            assertTrue(isNull(database, "SELECT deleted_at FROM finance_notes"))
            assertEquals("part", singleString(database, "SELECT label FROM finance_note_lines"))
            assertEquals(1500, singleInt(database, "SELECT amount_cents FROM finance_note_lines"))
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

    private fun singleInt(database: SupportSQLiteDatabase, sql: String): Int {
        database.query(sql).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }

    private fun isNull(database: SupportSQLiteDatabase, sql: String): Boolean {
        database.query(sql).use { cursor ->
            cursor.moveToFirst()
            return cursor.isNull(0)
        }
    }
}
