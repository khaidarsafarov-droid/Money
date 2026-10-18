package com.example.money.data.local.migration

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.money.domain.util.DebtBalance
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Migration29To30Test {

    @Test
    fun migration29To30_seedsOpeningAdvanceAndKeepsPayments() {
        val context: Context = RuntimeEnvironment.getApplication()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name("migration-${UUID.randomUUID()}.db")
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(29) {
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
                CREATE TABLE `debtor` (
                    `id` INTEGER PRIMARY KEY NOT NULL,
                    `sync_id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `target_amount_cents` INTEGER NOT NULL,
                    `current_amount_cents` INTEGER NOT NULL,
                    `currency` TEXT NOT NULL,
                    `date_given` INTEGER NOT NULL,
                    `mode` TEXT NOT NULL DEFAULT 'PERSONAL'
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO `debtor`
                    (id, sync_id, name, target_amount_cents, current_amount_cents, currency, date_given, mode)
                VALUES (4, 'debt-isrofil', 'Isrofil', 150000, 20000, 'USD', 1729728000000, 'PERSONAL')
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE `debt_payment` (
                    `id` INTEGER PRIMARY KEY NOT NULL,
                    `sync_id` TEXT NOT NULL,
                    `debtor_id` INTEGER NOT NULL,
                    `amount_cents` INTEGER NOT NULL,
                    `paid_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO `debt_payment` (id, sync_id, debtor_id, amount_cents, paid_at)
                VALUES (9, 'pay-1', 4, 20000, 1729814400000)
                """.trimIndent(),
            )

            MIGRATION_29_30.migrate(database)

            assertEquals(1, singleInt(database, "SELECT COUNT(*) FROM debt_advance"))
            assertEquals("open-debt-isrofil", singleString(database, "SELECT sync_id FROM debt_advance"))
            assertEquals(150000, singleInt(database, "SELECT amount_cents FROM debt_advance"))
            assertEquals(1729728000000L, singleLong(database, "SELECT given_at FROM debt_advance"))
            assertEquals(1729728000000L, singleLong(database, "SELECT updated_at FROM debt_advance"))
            assertEquals("", singleString(database, "SELECT note FROM debt_advance"))
            assertEquals(20000, singleInt(database, "SELECT amount_cents FROM debt_payment"))
            assertEquals("Isrofil", singleString(database, "SELECT name FROM debtor"))
            assertEquals(
                130000L,
                DebtBalance.remainingCents(
                    advanceCents = listOf(singleLong(database, "SELECT amount_cents FROM debt_advance")),
                    paymentCents = listOf(singleLong(database, "SELECT amount_cents FROM debt_payment")),
                ),
            )
            assertEquals(
                singleLong(database, "SELECT target_amount_cents - current_amount_cents FROM debtor"),
                DebtBalance.remainingCents(
                    advanceCents = listOf(150000L),
                    paymentCents = listOf(20000L),
                ),
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

    private fun singleLong(database: SupportSQLiteDatabase, sql: String): Long {
        database.query(sql).use { cursor ->
            cursor.moveToFirst()
            return cursor.getLong(0)
        }
    }
}
