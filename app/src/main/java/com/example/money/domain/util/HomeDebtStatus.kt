package com.example.money.domain.util

import com.example.money.domain.model.Debtor
import com.example.money.domain.model.DebtPayment
import java.util.Calendar

data class HomeDebtStatus(
    val overdueCount: Int,
    val overdueCents: Long,
    val todayCount: Int,
    val weekCount: Int,
)

object HomeDebtStatusEngine {
    const val WEEK_DAYS = 7

    fun summarize(debtors: List<Debtor>, now: Long = System.currentTimeMillis()): HomeDebtStatus {
        val open = debtors.filter { !it.isPaid && it.dateDue > 0L }
        val overdue = open.filter { DebtDueDateUtils.isOverdue(it.dateDue, now) }
        val today = open.filter { DebtDueDateUtils.isDueToday(it.dateDue, now) }
        val week = open.filter { DebtDueDateUtils.isDueWithinDays(it.dateDue, WEEK_DAYS, now) }
        return HomeDebtStatus(
            overdueCount = overdue.size,
            overdueCents = overdue.sumOf { it.remainingAmountCents },
            todayCount = today.size,
            weekCount = week.size,
        )
    }

    fun returnedThisMonthCents(debtors: List<Debtor>, now: Long = System.currentTimeMillis()): Long {
        val start = startOfMonthMillis(now)
        return debtors.sumOf { paymentsSince(it.paymentHistory, start) }
    }

    fun closestGoal(goals: List<com.example.money.domain.model.SavingsGoal>): com.example.money.domain.model.SavingsGoal? =
        goals.filter { !it.isCompleted }.maxWithOrNull(
            compareBy<com.example.money.domain.model.SavingsGoal> { it.progressFraction }
                .thenBy { it.deadlineMillis ?: Long.MAX_VALUE },
        )
}

fun startOfMonthMillis(now: Long): Long {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

fun paymentsSince(payments: List<DebtPayment>, since: Long): Long =
    payments.sumOf { if (it.paidAt >= since) it.amountCents else 0L }
