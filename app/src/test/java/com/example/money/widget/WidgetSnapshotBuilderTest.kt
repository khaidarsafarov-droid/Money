package com.example.money.widget

import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.settings.CurrencyRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSnapshotBuilderTest {

    private val now = 1_700_000_000_000L

    @Test
    fun actionGoalsStayOutOfMoneyTotals() {
        val snapshot = build(
            goals = listOf(
                moneyGoal(id = 1, target = 10_000, current = 4_000),
                SavingsGoal(
                    id = 2,
                    name = "Steps",
                    targetAmountCents = 99_000,
                    currentAmountCents = 50_000,
                    updatedAt = now,
                    kind = GoalKind.ACTION,
                    actionDone = 1,
                    actionTotal = 4,
                ),
            ),
            debts = emptyList(),
        )

        assertEquals(40.0, snapshot.totalAccumulated, 0.001)
        assertEquals(60.0, snapshot.totalRemainingToGoal, 0.001)
        assertFalse(snapshot.scoreUsesDebts)
        assertEquals(40, snapshot.progressPercent)
        assertEquals(WidgetItemType.GOAL, snapshot.activeItems.first().type)
        assertEquals("1/4", snapshot.activeItems.first().remainingLabel)
    }

    @Test
    fun debtScoreMatchesHomeEngineAndOverdueDebtComesFirst() {
        val debts = listOf(
            debt(id = 1, name = "Later", type = DebtType.DEBTOR, target = 10_000, current = 2_000, due = now + 86_400_000L),
            debt(id = 2, name = "Late", type = DebtType.DEBTOR, target = 4_000, current = 1_000, due = now - 86_400_000L * 3),
            debt(id = 3, name = "Owes me", type = DebtType.CREDITOR, target = 8_000, current = 0, due = now + 86_400_000L),
        )

        val snapshot = build(goals = emptyList(), debts = debts)

        assertFalse(snapshot.scoreUsesDebts)
        assertEquals(0, snapshot.progressPercent)
        assertEquals(8_000L, snapshot.owedToMeCents)
        assertEquals(11_000L, snapshot.iOweCents)
        assertEquals(1, snapshot.overdueDebtCount)
        assertEquals("Late", snapshot.activeItems.first().name)
        assertTrue(snapshot.activeItems.first().isOverdue)
    }

    @Test
    fun moneyOwedToMeDoesNotReplaceTheGoalScore() {
        val snapshot = build(
            goals = listOf(moneyGoal(id = 1, target = 10_000, current = 4_000)),
            debts = listOf(
                debt(
                    id = 3,
                    name = "Owes me",
                    type = DebtType.CREDITOR,
                    target = 8_000,
                    current = 0,
                    due = now + 86_400_000L,
                ),
            ),
        )

        assertFalse(snapshot.scoreUsesDebts)
        assertEquals(40, snapshot.progressPercent)
    }

    private fun build(
        goals: List<SavingsGoal>,
        debts: List<Debtor>,
    ) = buildWidgetGoalSnapshot(
        goals = goals,
        debts = debts,
        pendingCount = 0,
        region = CurrencyRegion.US,
        hideAmounts = false,
        streakCount = 0,
        lastWinMessage = null,
        now = now,
        goalFallbackName = "Goal",
        formatCents = { cents -> cents.toString() },
        actionStepsLabel = { done, total -> "$done/$total" },
    )

    private fun moneyGoal(id: Long, target: Long, current: Long) = SavingsGoal(
        id = id,
        name = "Money $id",
        targetAmountCents = target,
        currentAmountCents = current,
        updatedAt = now,
        kind = GoalKind.MONEY,
    )

    private fun debt(
        id: Long,
        name: String,
        type: DebtType,
        target: Long,
        current: Long,
        due: Long,
    ) = Debtor(
        id = id,
        name = name,
        targetAmountCents = target,
        currentAmountCents = current,
        debtType = type,
        dateGiven = now - 86_400_000L,
        dateDue = due,
    )
}
