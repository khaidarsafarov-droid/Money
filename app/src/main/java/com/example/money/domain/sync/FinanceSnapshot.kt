package com.example.money.domain.sync

/** Last-write-wins record exchanged during P2P sync. */
interface LwwRecord {
    val syncUpdatedAt: Long
    val deleted: Boolean
}

data class SnapshotGoal(
    val id: String,
    val name: String,
    val targetAmountCents: Long,
    val currency: String,
    val dateDeadline: String?,
    val goalKind: String? = null,
    override val syncUpdatedAt: Long,
    override val deleted: Boolean = false,
) : LwwRecord

data class SnapshotDebt(
    val id: String,
    val name: String,
    val targetAmountCents: Long,
    val currentAmountCents: Long,
    val currency: String,
    val status: String,
    val dateGiven: String,
    val dateDue: String?,
    val debtType: String = "CREDITOR",
    val affectsBalance: Boolean = false,
    val note: String = "",
    override val syncUpdatedAt: Long,
    override val deleted: Boolean = false,
) : LwwRecord

data class SnapshotDebtPayment(
    val id: String,
    val debtorId: String,
    val amountCents: Long,
    val paidAt: String,
    val affectsBalance: Boolean = true,
    override val syncUpdatedAt: Long,
    override val deleted: Boolean = false,
) : LwwRecord

data class SnapshotTransaction(
    val id: String,
    val kind: String,
    val amountCents: Long,
    val goalId: String?,
    val debtId: String? = null,
    val debtPaymentId: String? = null,
    val createdAt: String,
    val note: String?,
    val category: String?,
    override val syncUpdatedAt: Long,
    override val deleted: Boolean = false,
) : LwwRecord

data class SnapshotVoiceSettings(
    val enabled: Boolean,
    val reportHour: Int,
    val reportMinute: Int,
    val language: String,
    val gender: String,
    val forceInSilentMode: Boolean,
    val lastVoiceReportDate: String?,
    val lastAppOpenDate: String?,
    val syncUpdatedAt: Long,
)

/**
 * Canonical offline snapshot for Project Money P2P sync (schema v3).
 * Goal balances are derived from GOAL_TRANSFER transactions, not stored on goals.
 */
data class FinanceSnapshot(
    val schemaVersion: Int,
    val deviceId: String,
    val updatedAt: Long,
    val goals: List<SnapshotGoal>,
    val debts: List<SnapshotDebt>,
    val debtPayments: List<SnapshotDebtPayment>,
    val transactions: List<SnapshotTransaction>,
    val voiceSettings: SnapshotVoiceSettings?,
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 3
    }
}
