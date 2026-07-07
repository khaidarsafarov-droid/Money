package com.example.money.domain.util

import com.example.money.domain.model.ContactDebtHistory
import com.example.money.domain.model.ContactDebtPaymentEntry
import com.example.money.domain.model.Debtor

object ContactDebtHistoryEngine {
    fun aggregate(
        allDebts: List<Debtor>,
        lookupKey: String,
        displayName: String,
    ): ContactDebtHistory {
        val debts = allDebts
            .filter { it.contactLookupKey == lookupKey }
            .sortedByDescending { it.dateGiven }

        val payments = debts.flatMap { debtor ->
            debtor.paymentHistory.map { payment ->
                ContactDebtPaymentEntry(
                    debtorId = debtor.id,
                    debtorName = debtor.name,
                    debtType = debtor.debtType,
                    payment = payment,
                )
            }
        }.sortedByDescending { it.payment.paidAt }

        val activeDebts = debts.filter { !it.isPaid }
        val activeRemainingCents = activeDebts.sumOf { it.remainingAmountCents }

        return ContactDebtHistory(
            lookupKey = lookupKey,
            displayName = displayName,
            debts = debts,
            payments = payments,
            activeRemainingCents = activeRemainingCents,
            activeDebtCount = activeDebts.size,
            closedDebtCount = debts.size - activeDebts.size,
        )
    }

    fun countExistingDebts(allDebts: List<Debtor>, lookupKey: String): Int =
        allDebts.count { it.contactLookupKey == lookupKey }
}
