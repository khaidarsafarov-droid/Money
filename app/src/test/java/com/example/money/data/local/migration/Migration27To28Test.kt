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
class Migration27To28Test {

    @Test
    fun migration27To28_keepsExistingRowsPersonalAndAddsClients() {
        val context: Context = RuntimeEnvironment.getApplication()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("migration-${UUID.randomUUID()}.db")
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(27) {
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
            listOf("debtor", "debt_payment", "transfers", "expenses", "savings_goal", "finance_notes").forEach { table ->
                database.execSQL("CREATE TABLE `$table` (id INTEGER PRIMARY KEY NOT NULL, name TEXT NOT NULL)")
                database.execSQL("INSERT INTO `$table` (id, name) VALUES (1, 'kept')")
            }

            MIGRATION_27_28.migrate(database)

            listOf("debtor", "debt_payment", "transfers", "expenses", "savings_goal", "finance_notes").forEach { table ->
                assertEquals("PERSONAL", singleString(database, "SELECT mode FROM `$table`"))
            }
            assertEquals(0, singleInt(database, "SELECT COUNT(*) FROM client"))
            assertEquals(
                "client",
                singleString(database, "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'client'"),
            )
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
}
