package com.example.money.domain.model

data class Debtor(
    val id: Long = 0,
    val syncId: String = "",
    val name: String,
    val targetAmountCents: Long,
    val currentAmountCents: Long = 0,
    val currency: String = "USD",
    val status: DebtStatus = DebtStatus.ACTIVE,
    val debtType: DebtType = DebtType.CREDITOR,
    val affectsBalance: Boolean = false,
    val note: String = "",
    val dateGiven: Long,
    val dateDue: Long,
    val loanCategory: LoanCategory = LoanCategory.OTHER,
    val interestRateBps: Int = 0,
    val paymentHistory: List<DebtPayment> = emptyList(),
    val calendarEventId: String? = null,
    val calendarSyncFailed: Boolean = false,
    val contactLookupKey: String? = null,
    val contactUri: String? = null,
    val contactPhotoUri: String? = null,
    val phone: String = "",
    val email: String = "",
) {
    val remainingAmountCents: Long
        get() = com.example.money.domain.util.MoneyMath.remainingCents(
            targetAmountCents,
            currentAmountCents,
        )

    /** Сумма для уведомлений и отображения — остаток долга. */
    val amount: Double
        get() = com.example.money.domain.util.MoneyMath.fromCents(remainingAmountCents)

    val targetAmount: Double
        get() = com.example.money.domain.util.MoneyMath.fromCents(targetAmountCents)

    val isPaid: Boolean
        get() = status == DebtStatus.COMPLETED

    val isHighPriorityDebt: Boolean
        get() = loanCategory.isHighPriority

    val progressFraction: Float
        get() = com.example.money.domain.util.MoneyMath.progressFraction(
            targetAmountCents,
            currentAmountCents,
        )
}
