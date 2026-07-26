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
class Migration21To22Test {

    @Test
    fun migration21To22_createsDurableSyncTombstoneTable() {
        val context: Context = RuntimeEnvironment.getApplication()
        val databaseName = "migration-${UUID.randomUUID()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(21) {
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
            MIGRATION_21_22.migrate(database)

            val columns = mutableListOf<Pair<String, Int>>()
            database.query("PRAGMA table_info(`sync_tombstone`)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                val primaryKeyIndex = cursor.getColumnIndexOrThrow("pk")
                while (cursor.moveToNext()) {
                    columns += cursor.getString(nameIndex) to cursor.getInt(primaryKeyIndex)
                }
            }

            assertEquals(
                listOf(
                    "entity_type" to 1,
                    "entity_id" to 2,
                    "payload_json" to 0,
                    "sync_updated_at" to 0,
                ),
                columns,
            )
        } finally {
            helper.close()
            context.deleteDatabase(databaseName)
        }
    }
}
