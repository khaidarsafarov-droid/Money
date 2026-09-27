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
class Migration22To23Test {

    @Test
    fun migration22To23_addsGoalColumnsOnExpenses() {
        val context: Context = RuntimeEnvironment.getApplication()
        val databaseName = "migration-${UUID.randomUUID()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(22) {
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
                CREATE TABLE IF NOT EXISTS `expenses` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `sync_id` TEXT NOT NULL,
                    `amount` REAL NOT NULL,
                    `category` TEXT NOT NULL,
                    `date` INTEGER NOT NULL,
                    `note` TEXT NOT NULL,
                    `isConfirmed` INTEGER NOT NULL,
                    `debt_id` INTEGER,
                    `debt_payment_id` INTEGER
                )
                """.trimIndent(),
            )
            MIGRATION_22_23.migrate(database)

            val columns = mutableListOf<String>()
            database.query("PRAGMA table_info(`expenses`)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    columns += cursor.getString(nameIndex)
                }
            }
            assertTrue(columns.contains("goalId"))
            assertTrue(columns.contains("goal_sync_id"))

            val indexes = mutableListOf<String>()
            database.query("PRAGMA index_list(`expenses`)").use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    indexes += cursor.getString(nameIndex)
                }
            }
            assertEquals(true, indexes.contains("index_expenses_goalId"))
        } finally {
            helper.close()
            context.deleteDatabase(databaseName)
        }
    }
}
