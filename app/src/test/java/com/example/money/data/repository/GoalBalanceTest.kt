package com.example.money.data.repository

import android.content.Context
import androidx.room.Room
import com.example.money.data.local.AppDatabase
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.sync.SyncQueueRecorder
import com.example.money.domain.model.Expense
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.model.Transfer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GoalBalanceTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var budget: BudgetRepositoryImpl
    private lateinit var goals: GoalRepositoryImpl

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val recorder = SyncQueueRecorder(
            syncQueueDao = database.syncQueueDao(),
            syncTombstoneDao = database.syncTombstoneDao(),
            debtorDao = database.debtorDao(),
            debtPaymentDao = database.debtPaymentDao(),
        )
        budget = BudgetRepositoryImpl(
            database = database,
            transferDao = database.transferDao(),
            expenseDao = database.expenseDao(),
            savingsGoalDao = database.savingsGoalDao(),
            syncQueueRecorder = recorder,
            appMode = kotlinx.coroutines.flow.flowOf(com.example.money.settings.AppMode.PERSONAL),
        )
        goals = GoalRepositoryImpl(
            database = database,
            savingsGoalDao = database.savingsGoalDao(),
            transferDao = database.transferDao(),
            expenseDao = database.expenseDao(),
            goalActionDao = database.goalActionDao(),
            syncQueueRecorder = recorder,
            appMode = kotlinx.coroutines.flow.flowOf(com.example.money.settings.AppMode.PERSONAL),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addGoalTransferIfAffordable_rejectsWhenFreeBalanceIsShort() = runBlocking {
        budget.addDeposit(Transfer(amountCents = 10_000L, date = 1L))
        budget.addExpense(Expense(amountCents = 3_000L, category = "Food", date = 2L))
        val goalId = database.savingsGoalDao().insert(
            SavingsGoalEntity(name = "Trip", targetAmountCents = 50_000L, updatedAt = 1L),
        )

        assertTrue(
            budget.addGoalTransferIfAffordable(
                Transfer(amountCents = 4_000L, date = 3L, goalId = goalId),
            ),
        )
        assertFalse(
            budget.addGoalTransferIfAffordable(
                Transfer(amountCents = 4_000L, date = 4L, goalId = goalId),
            ),
        )

        val goalTransfers = database.transferDao().getAllOnce().filter { it.goalId != null }
        assertEquals(1, goalTransfers.size)
        assertEquals(4_000L, goalTransfers.single().amountCents)
    }

    @Test
    fun updateGoal_keepsStoredCurrentAmount() = runBlocking {
        val goalId = database.savingsGoalDao().insert(
            SavingsGoalEntity(
                name = "Trip",
                targetAmountCents = 50_000L,
                currentAmountCents = 2_500L,
                updatedAt = 1L,
            ),
        )
        val stored = database.savingsGoalDao().getGoalById(goalId)!!
        goals.updateGoal(
            SavingsGoal(
                id = stored.id,
                syncId = stored.syncId,
                name = "Holiday",
                targetAmountCents = 60_000L,
                currentAmountCents = 0L,
                updatedAt = stored.updatedAt,
                kind = GoalKind.MONEY,
            ),
        )

        val updated = database.savingsGoalDao().getGoalById(goalId)!!
        assertEquals("Holiday", updated.name)
        assertEquals(60_000L, updated.targetAmountCents)
        assertEquals(2_500L, updated.currentAmountCents)
    }
}
