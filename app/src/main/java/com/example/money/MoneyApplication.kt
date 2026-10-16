package com.example.money

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.money.di.AppContainer
import com.example.money.data.backup.GoogleDriveAuthorizationManager
import com.example.money.data.backup.LocalBackupDocumentGateway
import com.example.money.data.locale.AppLanguageManager
import com.example.money.data.locale.AppLanguageStorage
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.AppDatabase
import com.example.money.data.sync.DeviceIdProvider
import com.example.money.data.sync.SyncQueueRecorder
import com.example.money.data.sync.SyncService
import com.example.money.domain.repository.BudgetRepository
import com.example.money.domain.repository.ClientRepository
import com.example.money.domain.repository.BackupRepository
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.repository.FinanceSnapshotRepository
import com.example.money.domain.repository.GoalRepository
import com.example.money.domain.backup.BackupStorageProvider
import com.example.money.calendar.DebtCalendarSyncService
import com.example.money.notification.DebtReminderScheduler
import com.example.money.notification.PersonalDebtMessageScheduler
import com.example.money.notification.NotificationChannels
import com.example.money.notification.WeeklyCheckInScheduler
import com.example.money.voice.VoiceReportScheduler
import com.example.money.autopilot.MicroPaymentScheduler
import com.example.money.widget.GoalWidgetSnapshotWriter
import com.example.money.widget.GoalWidgetUpdater
import com.example.money.widget.WidgetGoalRepository
import com.example.money.ui.util.AppLocaleContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MoneyApplication : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(
            AppLocaleContext.wrap(base, AppLanguageStorage.read(base)),
        )
    }

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Uncaught coroutine exception", throwable)
    }

    val applicationScope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + coroutineExceptionHandler,
    )

    val container: AppContainer by lazy { AppContainer(this, applicationScope) }

    val database: AppDatabase get() = container.database
    val deviceIdProvider: DeviceIdProvider get() = container.deviceIdProvider
    val syncQueueRecorder: SyncQueueRecorder get() = container.syncQueueRecorder
    val preferencesStore: MoneyPreferencesStore get() = container.preferencesStore
    val financeSnapshotRepository: FinanceSnapshotRepository get() = container.financeSnapshotRepository
    val syncService: SyncService get() = container.syncService
    val debtorRepository: DebtorRepository get() = container.debtorRepository
    val budgetRepository: BudgetRepository get() = container.budgetRepository
    val goalRepository: GoalRepository get() = container.goalRepository
    val backupRepository: BackupRepository get() = container.backupRepository
    val localBackupDocumentGateway: LocalBackupDocumentGateway
        get() = container.localBackupDocumentGateway
    val googleDriveAuthorizationManager: GoogleDriveAuthorizationManager
        get() = container.googleDriveAuthorizationManager
    val luckDiaryRepository: com.example.money.domain.repository.LuckDiaryRepository
        get() = container.luckDiaryRepository
    val noteRepository: com.example.money.domain.repository.NoteRepository
        get() = container.noteRepository
    val clientRepository: ClientRepository
        get() = container.clientRepository
    val debtReminderScheduler: DebtReminderScheduler get() = container.debtReminderScheduler
    val debtCalendarSyncService: DebtCalendarSyncService get() = container.debtCalendarSyncService
    val personalDebtMessageScheduler: PersonalDebtMessageScheduler
        get() = container.personalDebtMessageScheduler

    fun googleDriveBackupStorageProvider(accessToken: String): BackupStorageProvider =
        container.googleDriveBackupStorageProvider(accessToken)

    private val widgetGoalRepository: WidgetGoalRepository by lazy {
        WidgetGoalRepository(this)
    }

    private val goalWidgetSnapshotWriter: GoalWidgetSnapshotWriter by lazy {
        GoalWidgetSnapshotWriter(
            context = this,
            goalRepository = goalRepository,
            debtorRepository = debtorRepository,
            pendingTransactionCount = budgetRepository.observePendingCount(),
            preferencesStore = preferencesStore,
            currencyRegion = preferencesStore.currencyRegion,
        )
    }

    private val goalWidgetUpdater: GoalWidgetUpdater by lazy {
        GoalWidgetUpdater(this, widgetGoalRepository)
    }

    override fun onCreate() {
        super.onCreate()
        AppLanguageManager.applyStored(this)
        NotificationChannels.create(this)
        rescheduleDebtReminders()
        syncPersonalDebtMessageSchedule()
        startWidgetPipeline()
        syncAutopilotSchedule()
        syncWeeklyCheckInSchedule()
        syncVoiceReportSchedule()
        syncAutomaticBackupSchedule()
        startPeerSync()
    }

    private fun startPeerSync() {
        applicationScope.launch {
            preferencesStore.peerSyncEnabled.collect { enabled ->
                try {
                    if (enabled) {
                        syncService.start()
                    } else {
                        syncService.stop()
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Log.w(TAG, "Failed to update P2P sync", exception)
                }
            }
        }
    }

    private fun syncAutopilotSchedule() {
        applicationScope.launch {
            MicroPaymentScheduler.syncSchedule(this@MoneyApplication)
        }
    }

    private fun syncWeeklyCheckInSchedule() {
        applicationScope.launch {
            WeeklyCheckInScheduler.syncSchedule(this@MoneyApplication)
        }
    }

    private fun syncVoiceReportSchedule() {
        applicationScope.launch {
            VoiceReportScheduler.syncSchedule(this@MoneyApplication)
        }
    }

    private fun syncAutomaticBackupSchedule() {
        applicationScope.launch {
            val settings = container.backupStateStore.settings.first()
            container.automaticBackupScheduler.syncSchedule(
                enabled = settings.automaticBackupEnabled,
                wifiOnly = settings.wifiOnly,
            )
        }
    }

    private fun startWidgetPipeline() {
        applicationScope.launch {
            goalWidgetSnapshotWriter.observeSnapshotChanges().collect { snapshot ->
                try {
                    goalWidgetSnapshotWriter.persist(snapshot)
                } catch (exception: Exception) {
                    Log.w(TAG, "Failed to persist widget snapshot", exception)
                }
            }
        }
        goalWidgetUpdater.start(applicationScope)
    }

    suspend fun refreshWidgetSnapshot() {
        val snapshot = goalWidgetSnapshotWriter.observeSnapshotChanges().first()
        goalWidgetSnapshotWriter.persist(snapshot)
        goalWidgetUpdater.refreshAllWidgets()
    }

    private companion object {
        const val TAG = "MoneyApplication"
    }

    fun rescheduleDebtReminders() {
        applicationScope.launch {
            val settings = preferencesStore.personalBranchSettings.first()
            debtorRepository.getActiveDebtors().first().forEach { debtor ->
                if (!settings.debtNotificationsEnabled) {
                    debtReminderScheduler.cancel(debtor.id)
                } else {
                    debtReminderScheduler.schedule(debtor)
                    debtReminderScheduler.scheduleAdvanceReminders(debtor, settings)
                }
            }
        }
    }

    fun syncPersonalDebtMessageSchedule() {
        applicationScope.launch {
            val settings = preferencesStore.personalBranchSettings.first()
            personalDebtMessageScheduler.syncSchedule(settings)
        }
    }
}
