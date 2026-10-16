package com.example.money.data.mapper

import com.example.money.data.local.entity.GoalActionEntity
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.DebtorWithPayments
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.domain.model.GoalAction
import com.example.money.domain.model.GoalKind
import com.example.money.domain.model.DebtPayment
import com.example.money.domain.model.DebtStatus
import com.example.money.domain.model.DebtType
import com.example.money.domain.model.Debtor
import com.example.money.domain.model.Expense
import com.example.money.domain.model.LoanCategory
import com.example.money.domain.model.SavingsGoal
import com.example.money.domain.model.Transfer
import com.example.money.domain.util.MoneyMath
import com.example.money.settings.AppMode

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
    phone = phone,
    email = email,
    mode = AppMode.fromStored(mode),
    clientId = clientId,
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
    phone = phone,
    email = email,
    mode = mode.name,
    clientId = clientId,
)

fun DebtPaymentEntity.toDomain(): DebtPayment = DebtPayment(
    id = id,
    debtorId = debtorId,
    amountCents = amountCents,
    currency = currency,
    paidAt = paidAt,
    affectsBalance = affectsBalance,
    mode = AppMode.fromStored(mode),
)

fun DebtPayment.toEntity(): DebtPaymentEntity = DebtPaymentEntity(
    id = id,
    debtorId = debtorId,
    amountCents = amountCents,
    currency = currency,
    paidAt = paidAt,
    affectsBalance = affectsBalance,
    mode = mode.name,
)

fun TransferEntity.toDomain(): Transfer = Transfer(
    id = id,
    syncId = syncId,
    amountCents = amountCents,
    date = date,
    isConfirmed = isConfirmed,
    goalId = goalId,
    debtId = debtId,
    debtPaymentId = debtPaymentId,
    note = note,
    mode = AppMode.fromStored(mode),
)

fun Transfer.toEntity(): TransferEntity = TransferEntity(
    id = id,
    syncId = syncId.ifBlank { com.example.money.data.sync.SyncIds.newId() },
    amountCents = amountCents,
    date = date,
    isConfirmed = isConfirmed,
    goalId = goalId,
    debtId = debtId,
    debtPaymentId = debtPaymentId,
    note = note,
    mode = mode.name,
)

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    syncId = syncId,
    amountCents = amountCents,
    category = category,
    date = date,
    note = note,
    isConfirmed = isConfirmed,
    debtId = debtId,
    debtPaymentId = debtPaymentId,
    goalId = goalId,
    mode = AppMode.fromStored(mode),
)

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    syncId = syncId.ifBlank { com.example.money.data.sync.SyncIds.newId() },
    amountCents = amountCents,
    category = category,
    date = date,
    note = note,
    isConfirmed = isConfirmed,
    debtId = debtId,
    debtPaymentId = debtPaymentId,
    goalId = goalId,
    mode = mode.name,
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
    kind = GoalKind.fromStored(kind),
    mode = AppMode.fromStored(mode),
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
    kind = kind.name,
    mode = mode.name,
)

fun GoalActionEntity.toDomain(): GoalAction = GoalAction(
    id = id,
    goalId = goalId,
    title = title,
    completed = completed,
    sortOrder = sortOrder,
)

/** Конвертация legacy double amount в cents при добавлении долга из UI. */
fun Double.toAmountCents(): Long = MoneyMath.toCents(this)

private fun String.toDebtStatus(): DebtStatus =
    runCatching { DebtStatus.valueOf(this) }.getOrDefault(DebtStatus.ACTIVE)

private fun String.toDebtType(): DebtType =
    runCatching { DebtType.valueOf(this) }.getOrDefault(DebtType.CREDITOR)

private fun String.toLoanCategory(): LoanCategory =
    LoanCategory.fromStored(this)
