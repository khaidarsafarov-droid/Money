package com.example.money.data.sync

import android.content.Context
import androidx.room.Room
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.domain.sync.SyncEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
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
class ThreePeerTombstoneSyncTest {

    private lateinit var context: Context
    private lateinit var databases: List<AppDatabase>

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        databases = List(3) {
            Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        }
    }

    @After
    fun tearDown() {
        databases.forEach(AppDatabase::close)
    }

    @Test
    fun deletingGoalAndDebt_survivesSyncFromAToBThenBToStaleC() = runBlocking {
        val (deviceA, deviceB, deviceC) = databases
        for (database in databases) {
            seedStaleRows(database)
        }
        val deletedGoal = deviceA.savingsGoalDao().getBySyncId("goal-1")!!.copy(updatedAt = 300L)
        val deletedDebt = deviceA.debtorDao().getBySyncId("debt-1")!!
        val recorderA = recorder(deviceA)
        deviceA.savingsGoalDao().delete(deletedGoal)
        deviceA.debtorDao().delete(deletedDebt)
        recorderA.recordGoal(deletedGoal, deleted = true)
        recorderA.recordDebt(deletedDebt, syncUpdatedAt = 300L, deleted = true)

        val repositoryA = repository(deviceA)
        val repositoryB = repository(deviceB)
        val repositoryC = repository(deviceC)
        val afterAB = SyncEngine.mergeSymmetric(
            repositoryA.buildSnapshot().copy(deviceId = "device-a"),
            repositoryB.buildSnapshot().copy(deviceId = "device-b"),
        )
        FinanceSnapshotApplier(deviceA, MoneyPreferencesStore(context)).apply(afterAB)
        FinanceSnapshotApplier(deviceB, MoneyPreferencesStore(context)).apply(afterAB)
        deviceA.syncQueueDao().clearAll()
        deviceB.syncQueueDao().clearAll()

        val afterBC = SyncEngine.mergeSymmetric(
            repositoryB.buildSnapshot().copy(deviceId = "device-b"),
            repositoryC.buildSnapshot().copy(deviceId = "device-c"),
        )
        FinanceSnapshotApplier(deviceB, MoneyPreferencesStore(context)).apply(afterBC)
        FinanceSnapshotApplier(deviceC, MoneyPreferencesStore(context)).apply(afterBC)

        databases.forEach { database ->
            assertNull(database.savingsGoalDao().getBySyncId("goal-1"))
            assertNull(database.debtorDao().getBySyncId("debt-1"))
        }
        listOf(repositoryA, repositoryB, repositoryC).forEach { repository ->
            val snapshot = repository.buildSnapshot()
            assertTrue(snapshot.goals.single().deleted)
            assertTrue(snapshot.debts.single().deleted)
        }
    }

    private suspend fun seedStaleRows(database: AppDatabase) {
        database.savingsGoalDao().insert(
            SavingsGoalEntity(
                syncId = "goal-1",
                name = "Stale goal",
                targetAmountCents = 1_000L,
                updatedAt = 100L,
            ),
        )
        database.debtorDao().insert(
            DebtorEntity(
                syncId = "debt-1",
                name = "Stale debt",
                targetAmountCents = 2_000L,
                dateGiven = 100L,
                dateDue = 200L,
            ),
        )
    }

    private fun recorder(database: AppDatabase) = SyncQueueRecorder(
        syncQueueDao = database.syncQueueDao(),
        syncTombstoneDao = database.syncTombstoneDao(),
        debtorDao = database.debtorDao(),
        debtPaymentDao = database.debtPaymentDao(),
    )

    private fun repository(database: AppDatabase) = FinanceSnapshotRepositoryImpl(
        database = database,
        preferencesStore = MoneyPreferencesStore(context),
        deviceIdProvider = DeviceIdProvider(context),
    )
}
