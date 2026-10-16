package com.example.money.data.sync

import android.content.Context
import androidx.room.Room
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.domain.sync.FinanceSnapshot
import com.example.money.domain.sync.SnapshotDebt
import com.example.money.domain.sync.SnapshotDebtPayment
import com.example.money.domain.sync.SnapshotGoal
import com.example.money.domain.sync.SnapshotTransaction
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
class FinanceSnapshotApplierTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var applier: FinanceSnapshotApplier

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        applier = FinanceSnapshotApplier(database, MoneyPreferencesStore(context))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun apply_deletesLiveRowsForEveryTombstoneType() = runBlocking {
        val goalId = database.savingsGoalDao().insert(
            SavingsGoalEntity(syncId = "goal-1", name = "Goal", targetAmountCents = 1_000L, updatedAt = 100L),
        )
        val debtorId = database.debtorDao().insert(
            DebtorEntity(
                syncId = "debt-1",
                name = "Debt",
                targetAmountCents = 2_000L,
                dateGiven = 100L,
                dateDue = 200L,
            ),
        )
        val paymentId = database.debtPaymentDao().insert(
            DebtPaymentEntity(
                syncId = "payment-1",
                debtorId = debtorId,
                amountCents = 500L,
                currency = "USD",
                paidAt = 100L,
            ),
        )
        database.transferDao().insert(
            TransferEntity(
                syncId = "transfer-1",
                amountCents = 1_000L,
                date = 100L,
                isConfirmed = true,
                goalId = goalId,
                goalSyncId = "goal-1",
            ),
        )
        database.expenseDao().insert(
            ExpenseEntity(
                syncId = "expense-1",
                amountCents = 500L,
                category = "Other",
                date = 100L,
                isConfirmed = true,
                debtId = debtorId,
                debtPaymentId = paymentId,
            ),
        )

        applier.apply(tombstoneSnapshot())

        assertNull(database.savingsGoalDao().getBySyncId("goal-1"))
        assertNull(database.debtorDao().getBySyncId("debt-1"))
        assertNull(database.debtPaymentDao().getBySyncId("payment-1"))
        assertNull(database.transferDao().getBySyncId("transfer-1"))
        assertNull(database.expenseDao().getBySyncId("expense-1"))
        assertEquals(5, database.syncTombstoneDao().getAll().size)
    }

    @Test
    fun apply_newerLiveRecordsRemoveOlderTombstones() = runBlocking {
        val deleted = tombstoneSnapshot()
        applier.apply(deleted)
        val recreated = deleted.copy(
            updatedAt = 300L,
            goals = deleted.goals.map { it.copy(syncUpdatedAt = 300L, deleted = false) },
            debts = deleted.debts.map { it.copy(syncUpdatedAt = 300L, deleted = false) },
            debtPayments = deleted.debtPayments.map { it.copy(syncUpdatedAt = 300L, deleted = false) },
            transactions = deleted.transactions.map { it.copy(syncUpdatedAt = 300L, deleted = false) },
        )

        applier.apply(recreated)

        assertTrue(database.syncTombstoneDao().getAll().isEmpty())
        assertNotNull(database.savingsGoalDao().getBySyncId("goal-1"))
        assertNotNull(database.debtorDao().getBySyncId("debt-1"))
        assertNotNull(database.debtPaymentDao().getBySyncId("payment-1"))
        assertNotNull(database.transferDao().getBySyncId("transfer-1"))
        assertNotNull(database.expenseDao().getBySyncId("expense-1"))
    }

    private fun tombstoneSnapshot() = FinanceSnapshot(
        schemaVersion = FinanceSnapshot.CURRENT_SCHEMA_VERSION,
        deviceId = "device-b",
        updatedAt = 200L,
        goals = listOf(
            SnapshotGoal(
                id = "goal-1",
                name = "Goal",
                targetAmountCents = 1_000L,
                currency = "USD",
                dateDeadline = null,
                syncUpdatedAt = 200L,
                deleted = true,
            ),
        ),
        debts = listOf(
            SnapshotDebt(
                id = "debt-1",
                name = "Debt",
                targetAmountCents = 2_000L,
                currentAmountCents = 0L,
                currency = "USD",
                status = "ACTIVE",
                dateGiven = "1970-01-01",
                dateDue = "1970-01-01",
                syncUpdatedAt = 200L,
                deleted = true,
            ),
        ),
        debtPayments = listOf(
            SnapshotDebtPayment(
                id = "payment-1",
                debtorId = "debt-1",
                amountCents = 500L,
                paidAt = "1970-01-01T00:00:00.200Z",
                syncUpdatedAt = 200L,
                deleted = true,
            ),
        ),
        transactions = listOf(
            SnapshotTransaction(
                id = "transfer-1",
                kind = "GOAL_TRANSFER",
                amountCents = 1_000L,
                goalId = "goal-1",
                createdAt = "1970-01-01T00:00:00.200Z",
                note = null,
                category = null,
                syncUpdatedAt = 200L,
                deleted = true,
            ),
            SnapshotTransaction(
                id = "expense-1",
                kind = "EXPENSE",
                amountCents = 500L,
                goalId = null,
                debtId = "debt-1",
                debtPaymentId = "payment-1",
                createdAt = "1970-01-01T00:00:00.200Z",
                note = null,
                category = "Other",
                syncUpdatedAt = 200L,
                deleted = true,
            ),
        ),
        voiceSettings = null,
    )
}
