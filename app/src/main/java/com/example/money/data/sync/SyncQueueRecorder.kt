package com.example.money.data.sync

import com.example.money.data.local.dao.DebtPaymentDao
import com.example.money.data.local.dao.DebtorDao
import com.example.money.data.local.dao.SyncQueueDao
import com.example.money.data.local.entity.DebtPaymentEntity
import com.example.money.data.local.entity.DebtorEntity
import com.example.money.data.local.entity.ExpenseEntity
import com.example.money.data.local.entity.SavingsGoalEntity
import com.example.money.data.local.entity.SyncQueueEntity
import com.example.money.data.local.entity.TransferEntity
import com.example.money.domain.sync.SyncEntityType
import com.example.money.voice.VoiceReportSettings
import java.util.UUID

class SyncQueueRecorder(
    private val syncQueueDao: SyncQueueDao,
    private val debtorDao: DebtorDao,
    private val debtPaymentDao: DebtPaymentDao,
) {

    suspend fun recordGoal(entity: SavingsGoalEntity, deleted: Boolean = false) {
        enqueue(
            entityType = SyncEntityType.GOAL,
            entityId = entity.id,
            payloadJson = FinanceSnapshotCodec.encodeGoal(entity, deleted),
        )
    }

    suspend fun recordDebt(entity: DebtorEntity, syncUpdatedAt: Long, deleted: Boolean = false) {
        enqueue(
            entityType = SyncEntityType.DEBT,
            entityId = entity.id,
            payloadJson = FinanceSnapshotCodec.encodeDebt(entity, syncUpdatedAt, deleted),
        )
    }

    suspend fun recordDebtPayment(entity: DebtPaymentEntity, deleted: Boolean = false) {
        val debtorSyncId = debtorDao.getDebtorById(entity.debtorId)?.syncId ?: return
        enqueue(
            entityType = SyncEntityType.DEBT_PAYMENT,
            entityId = entity.id,
            payloadJson = FinanceSnapshotCodec.encodeDebtPayment(entity, debtorSyncId, deleted),
        )
    }

    suspend fun recordTransfer(entity: TransferEntity, deleted: Boolean = false) {
        val debtSyncId = entity.debtId?.let { debtorDao.getDebtorById(it)?.syncId }
        val debtPaymentSyncId = entity.debtPaymentId?.let { debtPaymentDao.getById(it)?.syncId }
        enqueue(
            entityType = SyncEntityType.TRANSACTION,
            entityId = entity.id,
            payloadJson = FinanceSnapshotCodec.encodeTransactionFromTransfer(
                entity,
                debtSyncId = debtSyncId,
                debtPaymentSyncId = debtPaymentSyncId,
                deleted = deleted,
            ),
        )
    }

    suspend fun recordExpense(entity: ExpenseEntity, deleted: Boolean = false) {
        val debtSyncId = entity.debtId?.let { debtorDao.getDebtorById(it)?.syncId }
        val debtPaymentSyncId = entity.debtPaymentId?.let { debtPaymentDao.getById(it)?.syncId }
        enqueue(
            entityType = SyncEntityType.TRANSACTION,
            entityId = entity.id,
            payloadJson = FinanceSnapshotCodec.encodeTransactionFromExpense(
                entity,
                debtSyncId = debtSyncId,
                debtPaymentSyncId = debtPaymentSyncId,
                deleted = deleted,
            ),
        )
    }

    suspend fun recordVoiceSettings(settings: VoiceReportSettings, syncUpdatedAt: Long) {
        enqueue(
            entityType = SyncEntityType.VOICE_SETTINGS,
            entityId = VOICE_SETTINGS_ENTITY_ID,
            payloadJson = FinanceSnapshotCodec.encodeVoiceSettings(settings, syncUpdatedAt),
        )
    }

    suspend fun recordFullSnapshot(payloadJson: String) {
        enqueue(
            entityType = SyncEntityType.FULL_SNAPSHOT,
            entityId = FULL_SNAPSHOT_ENTITY_ID,
            payloadJson = payloadJson,
        )
    }

    private suspend fun enqueue(
        entityType: SyncEntityType,
        entityId: Long,
        payloadJson: String,
    ) {
        if (SyncWriteGuard.isRecordingSuppressed) return
        syncQueueDao.insert(
            SyncQueueEntity(
                id = UUID.randomUUID().toString(),
                entityType = entityType.name,
                entityId = entityId,
                payloadJson = payloadJson,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    private companion object {
        const val VOICE_SETTINGS_ENTITY_ID = -1L
        const val FULL_SNAPSHOT_ENTITY_ID = -2L
    }
}
