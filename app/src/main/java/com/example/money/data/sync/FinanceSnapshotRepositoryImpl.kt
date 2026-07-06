package com.example.money.data.sync

import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.AppDatabase
import com.example.money.domain.repository.FinanceSnapshotRepository
import com.example.money.domain.sync.FinanceSnapshot
import com.example.money.domain.sync.SnapshotVoiceSettings
import kotlinx.coroutines.flow.first

class FinanceSnapshotRepositoryImpl(
    private val database: AppDatabase,
    private val preferencesStore: MoneyPreferencesStore,
    private val deviceIdProvider: DeviceIdProvider,
) : FinanceSnapshotRepository {

    override suspend fun buildSnapshot(): FinanceSnapshot {
        val deviceId = deviceIdProvider.getDeviceId()
        val goals = database.savingsGoalDao().getAllGoalsOnce()
            .map(FinanceSnapshotCodec::toSnapshotGoal)
        val debtors = database.debtorDao().getAllDebtorsOnce()
        val payments = database.debtPaymentDao().getAllPaymentsOnce()
        val debtorSyncIds = debtors.associate { it.id to it.syncId }
        val paymentSyncIds = payments.associate { it.id to it.syncId }
        val paymentsByDebtor = payments.groupBy { it.debtorId }
        val debts = debtors.map { debtor ->
            val latestPayment = paymentsByDebtor[debtor.id]?.maxOfOrNull { it.paidAt }
            val syncUpdatedAt = maxOf(debtor.dateGiven, latestPayment ?: debtor.dateGiven)
            FinanceSnapshotCodec.toSnapshotDebt(debtor, syncUpdatedAt)
        }
        val debtPayments = payments.mapNotNull { payment ->
            val debtorSyncId = debtorSyncIds[payment.debtorId] ?: return@mapNotNull null
            FinanceSnapshotCodec.toSnapshotDebtPayment(payment, debtorSyncId)
        }
        val transferTransactions = database.transferDao().getAllOnce()
            .map { transfer ->
                FinanceSnapshotCodec.toSnapshotTransactionFromTransfer(
                    transfer,
                    debtSyncId = transfer.debtId?.let { debtorSyncIds[it] },
                    debtPaymentSyncId = transfer.debtPaymentId?.let { paymentSyncIds[it] },
                )
            }
        val expenseTransactions = database.expenseDao().getAllOnce()
            .map { expense ->
                FinanceSnapshotCodec.toSnapshotTransactionFromExpense(
                    expense,
                    debtSyncId = expense.debtId?.let { debtorSyncIds[it] },
                    debtPaymentSyncId = expense.debtPaymentId?.let { paymentSyncIds[it] },
                )
            }
        val transactions = (transferTransactions + expenseTransactions)
            .sortedBy { it.id }
        val voiceSettings = buildVoiceSettings()

        val updatedAt = listOfNotNull(
            goals.maxOfOrNull { it.syncUpdatedAt },
            debts.maxOfOrNull { it.syncUpdatedAt },
            debtPayments.maxOfOrNull { it.syncUpdatedAt },
            transactions.maxOfOrNull { it.syncUpdatedAt },
            voiceSettings?.syncUpdatedAt,
        ).maxOrNull() ?: System.currentTimeMillis()

        return FinanceSnapshot(
            schemaVersion = FinanceSnapshot.CURRENT_SCHEMA_VERSION,
            deviceId = deviceId,
            updatedAt = updatedAt,
            goals = goals,
            debts = debts,
            debtPayments = debtPayments,
            transactions = transactions,
            voiceSettings = voiceSettings,
        )
    }

    override fun encodeSnapshot(snapshot: FinanceSnapshot): String =
        FinanceSnapshotCodec.encode(snapshot)

    override fun decodeSnapshot(raw: String): FinanceSnapshot =
        FinanceSnapshotCodec.decode(raw)

    private suspend fun buildVoiceSettings(): SnapshotVoiceSettings? {
        val settings = preferencesStore.voiceReportSettings.first()
        val syncUpdatedAt = preferencesStore.getVoiceSettingsSyncUpdatedAt() ?: return null
        return SnapshotVoiceSettings(
            enabled = settings.enabled,
            reportHour = settings.reportHour,
            reportMinute = settings.reportMinute,
            language = settings.language.name,
            gender = settings.gender.name,
            forceInSilentMode = settings.forceInSilentMode,
            lastVoiceReportDate = settings.lastVoiceReportDate,
            lastAppOpenDate = settings.lastAppOpenDate,
            syncUpdatedAt = syncUpdatedAt,
        )
    }
}
