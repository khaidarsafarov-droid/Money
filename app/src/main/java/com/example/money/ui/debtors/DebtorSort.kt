package com.example.money.ui.debtors

import com.example.money.domain.model.Debtor
import com.example.money.domain.util.DebtDueDateUtils
import com.example.money.settings.AppMode

/**
 * Overdue first (oldest due date on top), then active by due date (nearest on top),
 * then debts with no due date, then paid.
 */
object DebtorSort {

    fun sort(debtors: List<Debtor>, now: Long = System.currentTimeMillis()): List<Debtor> =
        debtors.sortedWith(
            compareBy<Debtor> { rank(it, now) }
                .thenBy { it.dateDue }
                .thenByDescending { it.id },
        )

    internal fun rank(debtor: Debtor, now: Long): Int = if (debtor.mode == AppMode.PROFESSIONAL) {
        when {
            debtor.isPaid -> 4
            debtor.dateDue <= 0L -> 3
            DebtDueDateUtils.isDueToday(debtor.dateDue, now) -> 0
            DebtDueDateUtils.isOverdue(debtor.dateDue, now) -> 1
            else -> 2
        }
    } else {
        when {
            debtor.isPaid -> 3
            debtor.dateDue <= 0L -> 2
            DebtDueDateUtils.isOverdue(debtor.dateDue, now) -> 0
            else -> 1
        }
    }
}
