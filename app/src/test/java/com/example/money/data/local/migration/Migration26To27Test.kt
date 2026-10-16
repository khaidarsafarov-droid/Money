package com.example.money.data.local.migration

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.money.data.sync.SyncIds
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
class Migration26To27Test {

    @Test
    fun migration26To27_replacesLegacyGoalIdsAndKeepsReferences() {
        val context: Context = RuntimeEnvironment.getApplication()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("migration-${UUID.randomUUID()}.db")
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(26) {
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
                    id INTEGER PRIMARY KEY NOT NULL,
                    sync_id TEXT NOT NULL,
                    name TEXT NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE transfers (
                    id INTEGER PRIMARY KEY NOT NULL,
                    sync_id TEXT NOT NULL,
                    goal_sync_id TEXT
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE sync_tombstone (
                    entity_type TEXT NOT NULL,
                    entity_id TEXT NOT NULL,
                    payload_json TEXT NOT NULL,
                    sync_updated_at INTEGER NOT NULL,
                    PRIMARY KEY(entity_type, entity_id)
                )
                """.trimIndent(),
            )
            val existing = "33333333-3333-4333-8333-333333333333"
            database.execSQL("INSERT INTO savings_goal (id, sync_id, name) VALUES (1, '1', 'Phone')")
            database.execSQL("INSERT INTO savings_goal (id, sync_id, name) VALUES (2, '$existing', 'Kept')")
            database.execSQL("INSERT INTO transfers (id, sync_id, goal_sync_id) VALUES (1, '9', '1')")
            database.execSQL(
                """
                INSERT INTO sync_tombstone (entity_type, entity_id, payload_json, sync_updated_at)
                VALUES ('GOAL', '1', '{"id":"1"}', 1)
                """.trimIndent(),
            )

            MIGRATION_26_27.migrate(database)

            val goalId = singleString(database, "SELECT sync_id FROM savings_goal WHERE name = 'Phone'")
            assertTrue(SyncIds.isUuid(goalId))
            assertEquals(existing, singleString(database, "SELECT sync_id FROM savings_goal WHERE name = 'Kept'"))
            assertEquals(goalId, singleString(database, "SELECT goal_sync_id FROM transfers"))
            assertTrue(SyncIds.isUuid(singleString(database, "SELECT sync_id FROM transfers")))
            assertEquals(goalId, singleString(database, "SELECT entity_id FROM sync_tombstone"))
            assertEquals("""{"id":"$goalId"}""", singleString(database, "SELECT payload_json FROM sync_tombstone"))
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
}
