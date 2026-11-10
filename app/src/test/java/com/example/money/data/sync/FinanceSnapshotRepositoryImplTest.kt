package com.example.money.data.sync

import android.content.Context
import androidx.room.Room
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.FinanceNoteEntity
import com.example.money.data.local.entity.FinanceNoteLineEntity
import com.example.money.data.local.entity.GoalActionEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.data.repository.BudgetRepositoryImpl
import com.example.money.data.repository.GoalRepositoryImpl
import com.example.money.data.repository.NoteRepositoryImpl
import com.example.money.domain.model.Expense
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.NoteKind
import com.example.money.domain.model.SavingsGoal
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
                amountCents = 1_000L,
                date = 100L,
                isConfirmed = true,
                goalSyncId = "goal-1",
            ),
        )
        val transfer = database.transferDao().getById(transferId)!!
        val expenseId = database.expenseDao().insert(
            ExpenseEntity(
                syncId = "expense-1",
                amountCents = 500L,
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

    @Test
    fun goalExpenseTasksAndNotesSurviveFullSnapshotOnAnotherDevice() = runBlocking {
        val goalId = database.savingsGoalDao().insert(
            SavingsGoalEntity(
                syncId = "goal-sync",
                name = "Home",
                targetAmountCents = 50_000L,
                updatedAt = 10L,
                kind = "ACTION",
            ),
        )
        database.goalActionDao().insert(
            GoalActionEntity(
                goalId = goalId,
                title = "Buy flour",
                completed = false,
                sortOrder = 0,
                occurredAt = 11L,
                syncId = "action-sync",
                updatedAt = 11L,
            ),
        )
        database.expenseDao().insert(
            ExpenseEntity(
                syncId = "expense-sync",
                amountCents = 1_200L,
                category = "food",
                date = 20L,
                isConfirmed = true,
                goalId = goalId,
                goalSyncId = "goal-sync",
            ),
        )
        val noteId = database.financeNoteDao().insertNote(
            FinanceNoteEntity(
                syncId = "note-sync",
                title = "House",
                kind = "FINANCIAL",
                body = "",
                mark = "OWED",
                closed = false,
                createdAt = 30L,
                updatedAt = 31L,
            ),
        )
        database.financeNoteDao().insertLine(
            FinanceNoteLineEntity(
                noteId = noteId,
                label = "Flour",
                amountCents = 20_000L,
                sortOrder = 0,
                occurredAt = 32L,
                syncId = "line-sync",
                updatedAt = 32L,
            ),
        )

        val decoded = repository.decodeSnapshot(repository.encodeSnapshot(repository.buildSnapshot()))
        val expense = decoded.transactions.single { it.kind == "EXPENSE" }
        assertEquals("goal-sync", expense.goalId)
        assertEquals("ACTION", decoded.goals.single().goalKind)
        assertEquals("Buy flour", decoded.goalActions?.single()?.title)
        assertEquals(11L, decoded.goalActions?.single()?.occurredAt)
        assertEquals("House", decoded.notes?.single()?.title)
        assertEquals(20_000L, decoded.noteLines?.single()?.amountCents)
        assertEquals(32L, decoded.noteLines?.single()?.occurredAt)

        val other = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            FinanceSnapshotApplier(other, MoneyPreferencesStore(context)).apply(decoded)
            val restoredGoal = other.savingsGoalDao().getAllGoalsOnce().single()
            val restoredExpense = other.expenseDao().getAllOnce().single()
            assertEquals("ACTION", restoredGoal.kind)
            assertEquals(restoredGoal.id, restoredExpense.goalId)
            assertEquals("goal-sync", restoredExpense.goalSyncId)
            assertEquals("Buy flour", other.goalActionDao().getAllOnce().single().title)
            assertEquals(restoredGoal.id, other.goalActionDao().getAllOnce().single().goalId)
            val restoredNote = other.financeNoteDao().getAllNotesOnce().single()
            assertEquals("FINANCIAL", restoredNote.kind)
            assertEquals("OWED", restoredNote.mark)
            val restoredLine = other.financeNoteDao().getAllLinesOnce().single()
            assertEquals("Flour", restoredLine.label)
            assertEquals(32L, restoredLine.occurredAt)
            assertEquals(11L, other.goalActionDao().getAllOnce().single().occurredAt)
            assertEquals(restoredNote.id, restoredLine.noteId)
        } finally {
            other.close()
        }
    }

    @Test
    fun deleteGoal_removesLinkedMoneyAndTasksAndKeepsThemInTheSnapshot() = runBlocking {
        val goals = GoalRepositoryImpl(
            database = database,
            savingsGoalDao = database.savingsGoalDao(),
            transferDao = database.transferDao(),
            expenseDao = database.expenseDao(),
            goalActionDao = database.goalActionDao(),
            syncQueueRecorder = recorder,
            appMode = kotlinx.coroutines.flow.flowOf(com.example.money.settings.AppMode.PERSONAL),
        )
        val budget = BudgetRepositoryImpl(
            database = database,
            transferDao = database.transferDao(),
            expenseDao = database.expenseDao(),
            savingsGoalDao = database.savingsGoalDao(),
            syncQueueRecorder = recorder,
            appMode = kotlinx.coroutines.flow.flowOf(com.example.money.settings.AppMode.PERSONAL),
        )
        val goalId = goals.addGoal("Trip", 50_000L, null, GoalKind.MONEY)
        val goal = database.savingsGoalDao().getGoalById(goalId)!!
        budget.addExpense(
            Expense(amountCents = 400L, category = "Food", date = 20L, goalId = goalId),
        )
        database.transferDao().insert(
            TransferEntity(
                syncId = "transfer-goal",
                amountCents = 1_000L,
                date = 20L,
                isConfirmed = true,
                goalId = goalId,
                goalSyncId = goal.syncId,
            ),
        )
        goals.addAction(goalId, "Pack")
        assertEquals(goal.syncId, database.expenseDao().getAllOnce().single().goalSyncId)

        goals.deleteGoal(
            SavingsGoal(
                id = goalId,
                name = "Trip",
                targetAmountCents = 50_000L,
                updatedAt = goal.updatedAt,
            ),
        )

        assertTrue(database.savingsGoalDao().getAllGoalsOnce().isEmpty())
        assertTrue(database.transferDao().getAllOnce().isEmpty())
        assertTrue(database.expenseDao().getAllOnce().isEmpty())
        assertTrue(database.goalActionDao().getAllOnce().isEmpty())
        val snapshot = repository.buildSnapshot()
        assertTrue(snapshot.goals.single { it.id == goal.syncId }.deleted)
        assertTrue(snapshot.goalActions.orEmpty().single().deleted)
        assertEquals("Pack", snapshot.goalActions.orEmpty().single().title)
        assertEquals(2, snapshot.transactions.size)
        assertTrue(snapshot.transactions.all { it.deleted })
    }

    @Test
    fun deletedNote_staysInTheNextSnapshot() = runBlocking {
        val notes = NoteRepositoryImpl(
            database.financeNoteDao(),
            recorder,
            kotlinx.coroutines.flow.flowOf(com.example.money.settings.AppMode.PERSONAL),
        )
        val id = notes.addNote("List", NoteKind.TEXT)
        notes.addLine(id, "Flour", 2_000L)
        notes.deleteNote(id)
        assertTrue(database.financeNoteDao().getAllNotesOnce().isEmpty())
        assertTrue(database.financeNoteDao().getAllLinesOnce().isEmpty())
        val snapshot = repository.buildSnapshot()
        assertEquals("List", snapshot.notes.orEmpty().single().title)
        assertTrue(snapshot.notes.orEmpty().single().deleted)
        assertEquals("Flour", snapshot.noteLines.orEmpty().single().label)
        assertTrue(snapshot.noteLines.orEmpty().single().deleted)
    }
}
