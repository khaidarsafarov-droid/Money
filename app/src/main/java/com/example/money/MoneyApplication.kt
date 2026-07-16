package com.example.money

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.money.data.locale.AppLanguageManager
import com.example.money.data.locale.AppLanguageStorage
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.AppDatabase
import com.example.money.data.repository.BudgetRepositoryImpl
import com.example.money.data.repository.DebtorRepositoryImpl
import com.example.money.data.repository.GoalRepositoryImpl
import com.example.money.data.sync.DeviceIdProvider
import com.example.money.data.sync.FinanceSnapshotApplier
import com.example.money.data.sync.FinanceSnapshotRepositoryImpl
import com.example.money.data.sync.SyncQueueRecorder
import com.example.money.data.sync.SyncService
import com.example.money.domain.repository.BudgetRepository
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.repository.FinanceSnapshotRepository
import com.example.money.domain.repository.GoalRepository
import com.example.money.calendar.DebtCalendarSyncService
import com.example.money.notification.DebtReminderScheduler
import com.example.money.notification.DebtReminderSchedulerImpl
import com.example.money.notification.PersonalDebtMessageScheduler
import com.example.money.notification.PersonalDebtMessageSchedulerImpl
import com.example.money.notification.NotificationChannels
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

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    val deviceIdProvider: DeviceIdProvider by lazy { DeviceIdProvider(this) }

    val syncQueueRecorder: SyncQueueRecorder by lazy {
        SyncQueueRecorder(
            syncQueueDao = database.syncQueueDao(),
            debtorDao = database.debtorDao(),
            debtPaymentDao = database.debtPaymentDao(),
        )
    }

    val preferencesStore: MoneyPreferencesStore by lazy {
        MoneyPreferencesStore(this, syncQueueRecorder)
    }

    val financeSnapshotRepository: FinanceSnapshotRepository by lazy {
        FinanceSnapshotRepositoryImpl(database, preferencesStore, deviceIdProvider)
    }

    val syncService: SyncService by lazy {
        SyncService(
            context = this,
            deviceIdProvider = deviceIdProvider,
            financeSnapshotRepository = financeSnapshotRepository,
            financeSnapshotApplier = FinanceSnapshotApplier(database, preferencesStore),
            syncQueueDao = database.syncQueueDao(),
            scope = applicationScope,
        )
    }

    val debtorRepository: DebtorRepository by lazy {
        DebtorRepositoryImpl(
            database = database,
            debtorDao = database.debtorDao(),
            debtPaymentDao = database.debtPaymentDao(),
            transferDao = database.transferDao(),
            expenseDao = database.expenseDao(),
            syncQueueRecorder = syncQueueRecorder,
        )
    }

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepositoryImpl(
            database = database,
            transferDao = database.transferDao(),
            expenseDao = database.expenseDao(),
            savingsGoalDao = database.savingsGoalDao(),
            syncQueueRecorder = syncQueueRecorder,
        )
    }

    val goalRepository: GoalRepository by lazy {
        GoalRepositoryImpl(
            savingsGoalDao = database.savingsGoalDao(),
            transferDao = database.transferDao(),
            syncQueueRecorder = syncQueueRecorder,
        )
    }

    val debtReminderScheduler: DebtReminderScheduler by lazy {
        DebtReminderSchedulerImpl(this)
    }

    val debtCalendarSyncService: DebtCalendarSyncService by lazy {
        DebtCalendarSyncService(this)
    }

    val personalDebtMessageScheduler: PersonalDebtMessageScheduler by lazy {
        PersonalDebtMessageSchedulerImpl(this)
    }

    private val widgetGoalRepository: WidgetGoalRepository by lazy {
        WidgetGoalRepository(this)
    }

    private val goalWidgetSnapshotWriter: GoalWidgetSnapshotWriter by lazy {
        GoalWidgetSnapshotWriter(
            goalRepository = goalRepository,
            debtorRepository = debtorRepository,
            budgetRepository = budgetRepository,
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
        syncVoiceReportSchedule()
        startPeerSync()
    }

    private fun startPeerSync() {
        applicationScope.launch {
            try {
                syncService.start()
            } catch (exception: Exception) {
                Log.w(TAG, "Failed to start P2P sync", exception)
            }
        }
    }

    private fun syncAutopilotSchedule() {
        applicationScope.launch {
            MicroPaymentScheduler.syncSchedule(this@MoneyApplication)
        }
    }

    private fun syncVoiceReportSchedule() {
        applicationScope.launch {
            VoiceReportScheduler.syncSchedule(this@MoneyApplication)
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

    private companion object {
        const val TAG = "MoneyApplication"
    }

    fun rescheduleDebtReminders() {
        applicationScope.launch {
            val settings = preferencesStore.personalBranchSettings.first()
            debtorRepository.getActiveDebtors().first().forEach { debtor ->
                debtReminderScheduler.schedule(debtor)
                debtReminderScheduler.scheduleAdvanceReminders(debtor, settings)
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
