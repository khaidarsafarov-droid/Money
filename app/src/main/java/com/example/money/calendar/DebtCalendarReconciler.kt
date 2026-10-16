package com.example.money.calendar

import com.example.money.data.local.AppDatabase
import com.example.money.data.mapper.toDomain
import com.example.money.data.sync.SyncWriteGuard
import com.example.money.domain.model.DebtStatus
import com.example.money.settings.CalendarSettings

class DebtCalendarReconciler(
    private val database: AppDatabase,
    private val gateway: DebtCalendarEventGateway,
    private val loadDisplayName: suspend () -> String,
) {
    suspend fun reconcileMissingActiveEvents(settings: CalendarSettings) {
        if (!settings.autoSyncEnabled || !gateway.hasPermissions()) return

        val displayName = loadDisplayName()
        database.debtorDao().getAllDebtorsOnce()
            .asSequence()
            .filter { debtor -> debtor.status != DebtStatus.COMPLETED.name }
            .filter { debtor -> debtor.calendarEventId.isNullOrBlank() || debtor.calendarSyncFailed }
            .forEach { entity ->
                val payments = database.debtPaymentDao().getPaymentsForDebtor(entity.mode, entity.id)
                val result = gateway.upsertDebtEvent(
                    debtor = entity.toDomain(payments),
                    settings = settings,
                    displayName = displayName,
                )
                SyncWriteGuard.withWriteLock {
                    database.debtorDao().updateCalendarSyncState(
                        debtorId = entity.id,
                        eventId = result.eventId,
                        failed = result.failed,
                    )
                }
            }
    }
}
