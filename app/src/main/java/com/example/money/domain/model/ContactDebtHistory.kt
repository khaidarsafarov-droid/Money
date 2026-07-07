package com.example.money.domain.model

data class ContactDebtPaymentEntry(
    val debtorId: Long,
    val debtorName: String,
    val debtType: DebtType,
    val payment: DebtPayment,
)

data class ContactDebtHistory(
    val lookupKey: String,
    val displayName: String,
    val debts: List<Debtor>,
    val payments: List<ContactDebtPaymentEntry>,
    val activeRemainingCents: Long,
    val activeDebtCount: Int,
    val closedDebtCount: Int,
)
