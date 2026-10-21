package com.example.money.domain.util

import com.example.money.domain.model.DebtPayment
import com.example.money.domain.model.DebtStatus
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.SavingsGoal
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeDebtStatusTest {

    private val now = Calendar.getInstance().apply {
        set(2026, Calendar.OCTOBER, 21, 15, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun countsOverdueTodayAndTheNextWeek() {
        val status = HomeDebtStatusEngine.summarize(
            debtors = listOf(
                debtor(1, shiftDays(0)),
                debtor(2, shiftDays(-1)),
                debtor(3, shiftDays(6)),
                debtor(4, shiftDays(8)),
                debtor(5, 0L),
            ),
            now = now,
        )
        assertEquals(1, status.overdueCount)
        assertEquals(5_000L, status.overdueCents)
        assertEquals(1, status.todayCount)
        assertEquals(2, status.weekCount)
    }

    @Test
    fun returnedThisMonthIgnoresOlderPayments() {
        val older = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MONTH, -1)
        }.timeInMillis
        val debtor = debtor(1, shiftDays(1)).copy(
            paymentHistory = listOf(
                payment(1_200L, now),
                payment(9_000L, older),
            ),
        )
        assertEquals(1_200L, HomeDebtStatusEngine.returnedThisMonthCents(listOf(debtor), now))
    }

    @Test
    fun closestGoalIsTheOneNearestCompletion() {
        val early = goal(1, saved = 100, target = 1_000)
        val close = goal(2, saved = 900, target = 1_000)
        assertEquals(2L, HomeDebtStatusEngine.closestGoal(listOf(early, close))?.id)
        assertNull(HomeDebtStatusEngine.closestGoal(listOf(goal(3, saved = 1_000, target = 1_000))))
    }

    private fun shiftDays(days: Int): Long = Calendar.getInstance().apply {
        timeInMillis = DebtDueDateUtils.startOfDayMillis(now)
        add(Calendar.DAY_OF_YEAR, days)
        set(Calendar.HOUR_OF_DAY, 12)
    }.timeInMillis

    private fun debtor(id: Long, due: Long) = Debtor(
        id = id,
        name = "Person $id",
        targetAmountCents = 5_000,
        currentAmountCents = 0,
        status = DebtStatus.ACTIVE,
        debtType = DebtType.CREDITOR,
        dateGiven = due,
        dateDue = due,
    )

    private fun payment(cents: Long, at: Long) = DebtPayment(
        debtorId = 1,
        amountCents = cents,
        currency = "USD",
        paidAt = at,
    )

    private fun goal(id: Long, saved: Long, target: Long) = SavingsGoal(
        id = id,
        name = "Goal $id",
        targetAmountCents = target,
        currentAmountCents = saved,
        updatedAt = now,
    )
}
