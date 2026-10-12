package com.example.money.data.local.migration

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.util.UUID
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class Migration23To24Test {

    @Test
    fun migration23To24_createsNotesAndLines() {
        val context: Context = RuntimeEnvironment.getApplication()
        val databaseName = "migration-${UUID.randomUUID()}.db"
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(23) {
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
            MIGRATION_23_24.migrate(database)
            val tables = mutableListOf<String>()
            database.query(
                "SELECT name FROM sqlite_master WHERE type = 'table' AND name IN ('finance_notes', 'finance_note_lines')",
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    tables += cursor.getString(0)
                }
            }
            assertTrue(tables.contains("finance_notes"))
            assertTrue(tables.contains("finance_note_lines"))
        } finally {
            helper.close()
            context.deleteDatabase(databaseName)
        }
    }
}
