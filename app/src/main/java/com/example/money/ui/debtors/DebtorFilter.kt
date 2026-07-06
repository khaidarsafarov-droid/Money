package com.example.money.ui.debtors

import com.example.money.R
import com.example.money.domain.model.DebtType

enum class DebtTab(val titleRes: Int) {
    OwesMe(R.string.debt_tab_owes_me),
    IOwe(R.string.debt_tab_i_owe),
    ;

    companion object {
        fun forDebtType(debtType: DebtType): DebtTab = when (debtType) {
            DebtType.CREDITOR -> OwesMe
            DebtType.DEBTOR -> IOwe
        }
    }
}

enum class DebtorFilter(val labelRes: Int) {
    ALL(R.string.debt_filter_all),
    ACTIVE(R.string.debt_filter_active),
    OVERDUE(R.string.debt_filter_overdue),
    PAID(R.string.debt_filter_paid),
}
