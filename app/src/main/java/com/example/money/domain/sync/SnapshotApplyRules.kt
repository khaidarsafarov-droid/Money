package com.example.money.domain.sync

/**
 * Payments whose debtor is absent from the snapshot cannot be inserted
 * (the debtor row is gone, and debt payments cascade-delete with it).
 * Applying them used to abort the whole snapshot transaction.
 */
internal fun linkableDebtPayments(
    payments: List<SnapshotDebtPayment>,
    debtorSyncIds: Set<String>,
): List<SnapshotDebtPayment> = payments.filter { it.debtorId in debtorSyncIds }

internal enum class SnapshotTransactionBucket {
    EXPENSE,
    TRANSFER,
    IGNORED,
}

internal fun snapshotTransactionBucket(kind: String): SnapshotTransactionBucket = when (kind) {
    "EXPENSE" -> SnapshotTransactionBucket.EXPENSE
    "DEPOSIT", "GOAL_TRANSFER" -> SnapshotTransactionBucket.TRANSFER
    else -> SnapshotTransactionBucket.IGNORED
}
