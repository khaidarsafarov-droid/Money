package com.example.money.ui.debtors

import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.util.DebtDueDateUtils

/** Pure filter/sort pipeline — easy to unit-test and reuse outside ViewModel. */
object DebtorListFilter {

    fun apply(
        debtors: List<Debtor>,
        tab: DebtTab,
        filter: DebtorFilter,
        searchQuery: String,
        now: Long = System.currentTimeMillis(),
    ): List<Debtor> {
        if (tab == DebtTab.Calendar) return emptyList()

        val debtType = when (tab) {
            DebtTab.OwesMe -> DebtType.CREDITOR
            DebtTab.IOwe -> DebtType.DEBTOR
            DebtTab.Calendar -> DebtType.DEBTOR
        }

        val byType = debtors.filter { it.debtType == debtType }
        val filtered = when (filter) {
            DebtorFilter.ALL -> byType
            // «Активные» = все непогашенные (включая просроченные)
            DebtorFilter.ACTIVE -> byType.filter { !it.isPaid }
            DebtorFilter.OVERDUE -> byType.filter {
                !it.isPaid && it.dateDue > 0L && DebtDueDateUtils.isOverdue(it.dateDue, now)
            }
            DebtorFilter.PAID -> byType.filter { it.isPaid }
        }

        val query = searchQuery.trim()
        val searched = if (query.isBlank()) {
            filtered
        } else {
            filtered.filter { debtor ->
                debtor.name.contains(query, ignoreCase = true) ||
                    debtor.note.contains(query, ignoreCase = true) ||
                    debtor.phone.contains(query, ignoreCase = true) ||
                    debtor.email.contains(query, ignoreCase = true)
            }
        }

        return DebtorSort.sort(searched, now)
    }
}
