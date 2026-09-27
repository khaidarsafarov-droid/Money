package com.example.money.calendar

import android.content.Context
import androidx.room.Room
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.SyncQueueEntity
import com.example.money.domain.model.Debtor
import com.example.money.settings.CalendarSettings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DebtCalendarReconcilerTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context: Context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun reconcileMissingActiveEvents_persistsNewIdsWithoutQueueOrAccountingWrites() = runBlocking {
        database.debtorDao().insert(debtor(id = 1L, status = "ACTIVE"))
        database.debtorDao().insert(debtor(id = 2L, status = "COMPLETED"))
        database.syncQueueDao().insert(
            SyncQueueEntity("existing", "DEBTOR", 1L, "{}", 123L),
        )
        val gateway = RecordingCalendarGateway(
            result = CalendarSyncResult(eventId = "987", failed = false),
        )
        val reconciler = DebtCalendarReconciler(
            database = database,
            gateway = gateway,
            loadDisplayName = { "Alex" },
        )

        reconciler.reconcileMissingActiveEvents(CalendarSettings(autoSyncEnabled = true))

        assertEquals(listOf(1L), gateway.debtorIds)
        assertEquals("987", database.debtorDao().getDebtorById(1L)?.calendarEventId)
        assertEquals(false, database.debtorDao().getDebtorById(1L)?.calendarSyncFailed)
        assertNull(database.debtorDao().getDebtorById(2L)?.calendarEventId)
        assertEquals(1, database.syncQueueDao().getAllPending().size)
        assertTrue(database.transferDao().getAllOnce().isEmpty())
        assertTrue(database.expenseDao().getAllOnce().isEmpty())
    }

    @Test
    fun reconcileMissingActiveEvents_withoutPermissionDoesNothing() = runBlocking {
        database.debtorDao().insert(debtor(id = 1L, status = "ACTIVE"))
        val gateway = RecordingCalendarGateway(
            hasPermissions = false,
            result = CalendarSyncResult(eventId = "987", failed = false),
        )
        val reconciler = DebtCalendarReconciler(
            database = database,
            gateway = gateway,
            loadDisplayName = { "Alex" },
        )

        reconciler.reconcileMissingActiveEvents(CalendarSettings(autoSyncEnabled = true))

        assertTrue(gateway.debtorIds.isEmpty())
        assertNull(database.debtorDao().getDebtorById(1L)?.calendarEventId)
    }

    private fun debtor(id: Long, status: String) = DebtorEntity(
        id = id,
        syncId = "debt-$id",
        name = "Debt $id",
        targetAmountCents = 10_000,
        currentAmountCents = if (status == "COMPLETED") 10_000 else 1_000,
        status = status,
        dateGiven = 1_700_000_000_000L,
        dateDue = 1_800_000_000_000L,
    )

    private class RecordingCalendarGateway(
        private val hasPermissions: Boolean = true,
        private val result: CalendarSyncResult,
    ) : DebtCalendarEventGateway {
        val debtorIds = mutableListOf<Long>()

        override fun hasPermissions(): Boolean = hasPermissions

        override suspend fun upsertDebtEvent(
            debtor: Debtor,
            settings: CalendarSettings,
            displayName: String,
            markClosed: Boolean,
            force: Boolean,
        ): CalendarSyncResult {
            debtorIds += debtor.id
            return result
        }
    }
}
