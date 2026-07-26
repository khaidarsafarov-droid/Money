package com.example.money.data.backup

import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.AppDatabase
import com.example.money.data.sync.SyncWriteGuard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LocalCalendarLink(
    val debtorId: Long,
    val eventId: String,
)

data class LocalDataResetWarning(
    val subsystem: String,
    val message: String,
)

data class LocalDataResetResult(
    val warnings: List<LocalDataResetWarning> = emptyList(),
)

fun interface LocalDataResetter {
    suspend fun reset(): LocalDataResetResult
}

class DefaultLocalDataResetter(
    private val database: AppDatabase,
    private val preferencesStore: MoneyPreferencesStore,
    private val visionImageStorage: VisionImageStorage,
    private val backupStateStore: BackupStateStore,
    private val credentialCache: BackupCredentialCache,
    private val resetAppState: suspend () -> Unit,
    private val stopPeerSync: suspend () -> Unit,
    private val cleanCalendarLinks: suspend (List<LocalCalendarLink>) -> Unit,
    private val cancelSchedules: suspend () -> Unit,
    private val cancelNotifications: suspend () -> Unit,
    private val operationDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LocalDataResetter {

    override suspend fun reset(): LocalDataResetResult =
        withContext(operationDispatcher) {
            SyncWriteGuard.withWriteLock {
                SyncWriteGuard.runWithoutRecording {
                    stopPeerSync()
                    val calendarLinks = database.debtorDao().getAllDebtorsOnce()
                        .mapNotNull { debtor ->
                            debtor.calendarEventId
                                ?.takeIf(String::isNotBlank)
                                ?.let { eventId -> LocalCalendarLink(debtor.id, eventId) }
                        }

                    visionImageStorage.replaceCurrent(null)
                    database.clearAllTables()
                    preferencesStore.clearAll()
                    backupStateStore.clear()
                    resetAppState()

                    LocalDataResetResult(
                        warnings = buildList {
                            runCleanup("backup_credential", credentialCache::clear)?.let(::add)
                            runCleanup("work_schedules", cancelSchedules)?.let(::add)
                            runCleanup("notifications", cancelNotifications)?.let(::add)
                            runCleanup("calendar") { cleanCalendarLinks(calendarLinks) }?.let(::add)
                        },
                    )
                }
            }
        }

    private suspend fun runCleanup(
        subsystem: String,
        cleanup: suspend () -> Unit,
    ): LocalDataResetWarning? =
        try {
            cleanup()
            null
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            LocalDataResetWarning(
                subsystem = subsystem,
                message = error.message ?: error::class.java.simpleName,
            )
        }
}
