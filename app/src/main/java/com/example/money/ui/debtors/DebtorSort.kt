package com.example.money.ui.debtors

import com.example.money.domain.model.Debtor
import com.example.money.domain.util.DebtDueDateUtils

/** Sort: active → overdue → paid; within groups nearest due date first. */
object DebtorSort {

    fun sort(debtors: List<Debtor>, now: Long = System.currentTimeMillis()): List<Debtor> =
        debtors.sortedWith(
            compareBy<Debtor> { rank(it, now) }
                .thenBy { it.dateDue }
                .thenByDescending { it.id },
        )

    internal fun rank(debtor: Debtor, now: Long): Int = when {
        debtor.isPaid -> 2
        DebtDueDateUtils.isOverdue(debtor.dateDue, now) -> 1
        else -> 0
    }
}
