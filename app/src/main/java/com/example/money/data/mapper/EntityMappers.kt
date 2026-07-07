package com.example.money.data.mapper

import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.DebtorWithPayments
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.domain.model.DebtPayment
import com.example.money.domain.model.DebtStatus
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.Expense
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.model.Transfer
import com.example.money.domain.util.MoneyMath

fun DebtorWithPayments.toDomain(): Debtor = debtor.toDomain(payments)

fun DebtorEntity.toDomain(payments: List<DebtPaymentEntity> = emptyList()): Debtor = Debtor(
    id = id,
    syncId = syncId,
    name = name,
    targetAmountCents = targetAmountCents,
    currentAmountCents = currentAmountCents,
    currency = currency,
    status = status.toDebtStatus(),
    debtType = debtType.toDebtType(),
    affectsBalance = affectsBalance,
    note = note,
    dateGiven = dateGiven,
    dateDue = dateDue,
    loanCategory = loanCategory.toLoanCategory(),
    interestRateBps = interestRateBps,
    paymentHistory = payments.map { it.toDomain() },
    calendarEventId = calendarEventId,
    calendarSyncFailed = calendarSyncFailed,
    contactLookupKey = contactLookupKey,
    contactUri = contactUri,
    contactPhotoUri = contactPhotoUri,
)

fun Debtor.toEntity(): DebtorEntity = DebtorEntity(
    id = id,
    syncId = syncId.ifBlank { com.example.money.data.sync.SyncIds.newId() },
    name = name,
    targetAmountCents = targetAmountCents,
    currentAmountCents = currentAmountCents,
    currency = currency,
    status = status.name,
    debtType = debtType.name,
    affectsBalance = affectsBalance,
    note = note,
    dateGiven = dateGiven,
    dateDue = dateDue,
    loanCategory = loanCategory.name,
    interestRateBps = interestRateBps,
    calendarEventId = calendarEventId,
    calendarSyncFailed = calendarSyncFailed,
    contactLookupKey = contactLookupKey,
    contactUri = contactUri,
    contactPhotoUri = contactPhotoUri,
)

fun DebtPaymentEntity.toDomain(): DebtPayment = DebtPayment(
    id = id,
    debtorId = debtorId,
    amountCents = amountCents,
    currency = currency,
    paidAt = paidAt,
    affectsBalance = affectsBalance,
)

fun DebtPayment.toEntity(): DebtPaymentEntity = DebtPaymentEntity(
    id = id,
    debtorId = debtorId,
    amountCents = amountCents,
    currency = currency,
    paidAt = paidAt,
    affectsBalance = affectsBalance,
)

fun TransferEntity.toDomain(): Transfer = Transfer(
    id = id,
    syncId = syncId,
    amount = amount,
    date = date,
    isConfirmed = isConfirmed,
    goalId = goalId,
    debtId = debtId,
    debtPaymentId = debtPaymentId,
    note = note,
)

fun Transfer.toEntity(): TransferEntity = TransferEntity(
    id = id,
    syncId = syncId.ifBlank { com.example.money.data.sync.SyncIds.newId() },
    amount = amount,
    date = date,
    isConfirmed = isConfirmed,
    goalId = goalId,
    debtId = debtId,
    debtPaymentId = debtPaymentId,
    note = note,
)

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    syncId = syncId,
    amount = amount,
    category = category,
    date = date,
    note = note,
    isConfirmed = isConfirmed,
    debtId = debtId,
    debtPaymentId = debtPaymentId,
)

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    syncId = syncId.ifBlank { com.example.money.data.sync.SyncIds.newId() },
    amount = amount,
    category = category,
    date = date,
    note = note,
    isConfirmed = isConfirmed,
    debtId = debtId,
    debtPaymentId = debtPaymentId,
)

fun SavingsGoalEntity.toDomain(): SavingsGoal = SavingsGoal(
    id = id,
    syncId = syncId,
    name = name,
    targetAmountCents = targetAmountCents,
    currentAmountCents = currentAmountCents,
    currency = currency,
    updatedAt = updatedAt,
    deadlineMillis = deadlineMillis,
)

fun SavingsGoal.toEntity(): SavingsGoalEntity = SavingsGoalEntity(
    id = id,
    syncId = syncId.ifBlank { com.example.money.data.sync.SyncIds.newId() },
    name = name,
    targetAmountCents = targetAmountCents,
    currentAmountCents = currentAmountCents,
    currency = currency,
    updatedAt = updatedAt,
    deadlineMillis = deadlineMillis,
)

/** Конвертация legacy double amount в cents при добавлении долга из UI. */
fun Double.toAmountCents(): Long = MoneyMath.toCents(this)

private fun String.toDebtStatus(): DebtStatus =
    runCatching { DebtStatus.valueOf(this) }.getOrDefault(DebtStatus.ACTIVE)

private fun String.toDebtType(): DebtType =
    runCatching { DebtType.valueOf(this) }.getOrDefault(DebtType.CREDITOR)

private fun String.toLoanCategory(): LoanCategory =
    LoanCategory.fromStored(this)
