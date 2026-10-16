package com.example.money.data.local.migration

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.entity.TransferEntity
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Migration25To26Test {

    @Test
    fun migration25To26_convertsTenthsOfADollarIntoExactCents() {
        val context: Context = RuntimeEnvironment.getApplication()
        val helper = openVersion25(context)
        try {
            val database = helper.writableDatabase
            createLegacyTables(database)
            repeat(1_000) { index ->
                database.execSQL(
                    """
                    INSERT INTO transfers (sync_id, amount, date, isConfirmed, note)
                    VALUES ('t-$index', 0.10, 1, 1, '')
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    INSERT INTO expenses (sync_id, amount, category, date, note, isConfirmed)
                    VALUES ('e-$index', 0.10, 'Food', 1, '', 1)
                    """.trimIndent(),
                )
            }

            MIGRATION_25_26.migrate(database)

            assertEquals(10_000L, sumCents(database, "transfers"))
            assertEquals(10_000L, sumCents(database, "expenses"))
        } finally {
            helper.close()
        }
    }

    @Test
    fun depositSum_ofManyTenthsIsExact() = runBlocking {
        val context: Context = RuntimeEnvironment.getApplication()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            repeat(1_000) {
                database.transferDao().insert(
                    TransferEntity(amountCents = 10L, date = 1L, isConfirmed = true),
                )
            }
            assertEquals(10_000L, database.transferDao().getDepositTotalOnce("PERSONAL"))
        } finally {
            database.close()
        }
    }

    private fun sumCents(database: SupportSQLiteDatabase, table: String): Long {
        database.query("SELECT COALESCE(SUM(amount_cents), 0) FROM $table").use { cursor ->
            cursor.moveToFirst()
            return cursor.getLong(0)
        }
    }

    private fun openVersion25(context: Context): SupportSQLiteOpenHelper =
        FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("migration-${UUID.randomUUID()}.db")
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(25) {
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

    private fun createLegacyTables(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE transfers (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                amount REAL NOT NULL,
                date INTEGER NOT NULL,
                isConfirmed INTEGER NOT NULL,
                goalId INTEGER,
                goal_sync_id TEXT,
                debt_id INTEGER,
                debt_payment_id INTEGER,
                note TEXT NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                sync_id TEXT NOT NULL,
                amount REAL NOT NULL,
                category TEXT NOT NULL,
                date INTEGER NOT NULL,
                note TEXT NOT NULL,
                isConfirmed INTEGER NOT NULL,
                debt_id INTEGER,
                debt_payment_id INTEGER,
                goalId INTEGER,
                goal_sync_id TEXT
            )
            """.trimIndent(),
        )
    }
}
