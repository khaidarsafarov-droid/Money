package com.example.money.widget

import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.SavingsGoal
import com.example.money.settings.CurrencyRegion
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
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
        assertEquals("Money 1", snapshot.featuredGoalName)
        assertEquals(4_000L, snapshot.featuredGoalSavedCents)
        assertFalse(snapshot.featuredGoalIsAction)
    }

    @Test
    fun debtAmountsStaySeparateFromTheGoalScore() {
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
        assertFalse(snapshot.hasFeaturedGoal)
        assertEquals("", snapshot.featuredGoalName)
    }

    @Test
    fun nearestActiveGoalIsLabeledSeparatelyFromTheAggregate() {
        val day = 86_400_000L
        val sooner = moneyGoal(id = 2, target = 30_000, current = 9_900).copy(
            name = "Отпуск",
            deadlineMillis = now + day,
        )
        val later = moneyGoal(id = 1, target = 10_000, current = 1_000).copy(
            name = "Later",
            deadlineMillis = now + day * 10,
        )
        val done = moneyGoal(id = 3, target = 5_000, current = 5_000).copy(
            name = "Done",
            deadlineMillis = now + day,
        )

        val snapshot = build(goals = listOf(later, done, sooner), debts = emptyList())

        assertEquals("Отпуск", snapshot.featuredGoalName)
        assertEquals(9_900L, snapshot.featuredGoalSavedCents)
        assertEquals(30_000L, snapshot.featuredGoalTargetCents)
        assertEquals(33, snapshot.featuredGoalPercent)
        assertTrue(snapshot.hasFeaturedGoal)
        assertEquals("33%", featuredGoalPercentLabel(snapshot.featuredGoalPercent))
    }

    @Test
    fun overdueGoalBeatsALaterDeadline() {
        val day = 86_400_000L
        val snapshot = build(
            goals = listOf(
                moneyGoal(id = 2, target = 10_000, current = 1_000).copy(
                    name = "Soon",
                    deadlineMillis = now + day,
                ),
                moneyGoal(id = 1, target = 10_000, current = 1_000).copy(
                    name = "Late",
                    deadlineMillis = now - day * 3,
                ),
            ),
            debts = emptyList(),
        )

        assertEquals("Late", snapshot.featuredGoalName)
    }

    @Test
    fun actionGoalUsesStepCounts() {
        val snapshot = build(
            goals = listOf(
                SavingsGoal(
                    id = 4,
                    name = "Steps",
                    targetAmountCents = 0,
                    currentAmountCents = 0,
                    updatedAt = now,
                    kind = GoalKind.ACTION,
                    actionDone = 1,
                    actionTotal = 3,
                    deadlineMillis = now + 86_400_000L,
                ),
            ),
            debts = emptyList(),
        )

        assertTrue(snapshot.featuredGoalIsAction)
        assertEquals(1L, snapshot.featuredGoalSavedCents)
        assertEquals(3L, snapshot.featuredGoalTargetCents)
        assertEquals(33, snapshot.featuredGoalPercent)
        assertEquals("1/3", featuredGoalRatio(snapshot))
    }

    @Test
    fun completedGoalsDoNotFillTheGoalBlock() {
        val snapshot = build(
            goals = listOf(moneyGoal(id = 1, target = 5_000, current = 5_000)),
            debts = emptyList(),
        )

        assertFalse(snapshot.hasFeaturedGoal)
    }

    @Test
    fun contentEqualityIgnoresTheTimestampAndKeepsShownFields() {
        val shown = WidgetGoalSnapshot.Empty.copy(
            owedToMeCents = 8_000,
            currencyRegion = CurrencyRegion.fromCode("EUR"),
            hideAmounts = true,
            updatedAt = 10,
        )
        assertTrue(shown.contentEquals(shown.copy(updatedAt = 99)))
        assertFalse(shown.contentEquals(shown.copy(owedToMeCents = 1)))
        assertFalse(shown.contentEquals(shown.copy(hideAmounts = false)))
        assertFalse(shown.contentEquals(shown.copy(currencyRegion = CurrencyRegion.US)))
    }

    @Test
    fun widgetRefreshSignalSkipsTimestampOnlyRepeats() = runBlocking {
        val shown = WidgetGoalSnapshot.Empty.copy(owedToMeCents = 8_000, updatedAt = 10)
        val values = flow {
            emit(shown)
            emit(shown.copy(updatedAt = 20))
            emit(shown.copy(updatedAt = 30, owedToMeCents = 9_000))
            emit(shown.copy(updatedAt = 40, owedToMeCents = 9_000))
        }.distinctWidgetContent().toList()

        assertEquals(listOf(8_000L, 9_000L), values.map { it.owedToMeCents })
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
