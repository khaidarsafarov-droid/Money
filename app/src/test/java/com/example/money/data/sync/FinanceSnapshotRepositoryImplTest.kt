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
import com.example.money.voice.VoiceReportSettings
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FinanceSnapshotRepositoryImplTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var recorder: SyncQueueRecorder
    private lateinit var repository: FinanceSnapshotRepositoryImpl

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        recorder = SyncQueueRecorder(
            syncQueueDao = database.syncQueueDao(),
            syncTombstoneDao = database.syncTombstoneDao(),
            debtorDao = database.debtorDao(),
            debtPaymentDao = database.debtPaymentDao(),
        )
        repository = FinanceSnapshotRepositoryImpl(
            database = database,
            preferencesStore = MoneyPreferencesStore(context),
            deviceIdProvider = DeviceIdProvider(context),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun buildSnapshot_includesPersistedTombstonesAfterQueueIsCleared() = runBlocking {
        val goal = SavingsGoalEntity(
            syncId = "goal-1",
            name = "Goal",
            targetAmountCents = 1_000L,
            updatedAt = 100L,
        )
        database.savingsGoalDao().insert(goal)
        val debtorId = database.debtorDao().insert(
            DebtorEntity(
                syncId = "debt-1",
                name = "Debt",
                targetAmountCents = 2_000L,
                dateGiven = 100L,
                dateDue = 200L,
            ),
        )
        val debt = database.debtorDao().getDebtorById(debtorId)!!
        val paymentId = database.debtPaymentDao().insert(
            DebtPaymentEntity(
                syncId = "payment-1",
                debtorId = debtorId,
                amountCents = 500L,
                currency = "USD",
                paidAt = 100L,
            ),
        )
        val payment = database.debtPaymentDao().getById(paymentId)!!
        val transferId = database.transferDao().insert(
            TransferEntity(
                syncId = "transfer-1",
                amount = 10.0,
                date = 100L,
                isConfirmed = true,
                goalSyncId = "goal-1",
            ),
        )
        val transfer = database.transferDao().getById(transferId)!!
        val expenseId = database.expenseDao().insert(
            ExpenseEntity(
                syncId = "expense-1",
                amount = 5.0,
                category = "Other",
                date = 100L,
                isConfirmed = true,
            ),
        )
        val expense = database.expenseDao().getById(expenseId)!!

        recorder.recordGoal(goal.copy(updatedAt = 200L), deleted = true)
        recorder.recordDebt(debt, syncUpdatedAt = 200L, deleted = true)
        recorder.recordDebtPayment(payment, deleted = true)
        recorder.recordTransfer(transfer, deleted = true)
        recorder.recordExpense(expense, deleted = true)
        recorder.recordVoiceSettings(VoiceReportSettings(), syncUpdatedAt = 200L)
        database.syncQueueDao().clearAll()

        val snapshot = repository.buildSnapshot()

        assertTrue(database.syncQueueDao().getAllPending().isEmpty())
        assertEquals(5, database.syncTombstoneDao().getAll().size)
        assertTrue(snapshot.goals.single().deleted)
        assertTrue(snapshot.debts.single().deleted)
        assertTrue(snapshot.debtPayments.single().deleted)
        assertTrue(snapshot.debtPayments.single().syncUpdatedAt > payment.paidAt)
        assertEquals(2, snapshot.transactions.size)
        assertTrue(snapshot.transactions.all { it.deleted })
        assertTrue(snapshot.transactions.all { it.syncUpdatedAt > 100L })
    }
}
