package com.example.money.di

import android.app.Application
import com.example.money.MoneyApplication
import com.example.money.autopilot.MicroPaymentScheduler
import com.example.money.calendar.DebtCalendarReconciler
import com.example.money.calendar.DebtCalendarSyncService
import com.example.money.data.backup.BackupExporter
import com.example.money.data.backup.BackupImporter
import com.example.money.data.backup.BackupRepositoryImpl
import com.example.money.data.backup.BackupRoomApplier
import com.example.money.data.backup.DebtReminderRestoreReconciler
import com.example.money.data.backup.DefaultPostRestoreReconciler
import com.example.money.data.backup.PostRestoreReconciler
import com.example.money.data.backup.VisionImageStore
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.AppDatabase
import com.example.money.data.repository.BudgetRepositoryImpl
import com.example.money.data.repository.DebtorRepositoryImpl
import com.example.money.data.repository.GoalRepositoryImpl
import com.example.money.data.repository.LuckDiaryRepositoryImpl
import com.example.money.data.sync.DeviceIdProvider
import com.example.money.data.sync.FinanceSnapshotApplier
import com.example.money.data.sync.FinanceSnapshotRepositoryImpl
import com.example.money.data.sync.SyncQueueRecorder
import com.example.money.data.sync.SyncService
import com.example.money.domain.repository.BudgetRepository
import com.example.money.domain.repository.BackupRepository
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.repository.FinanceSnapshotRepository
import com.example.money.domain.repository.GoalRepository
import com.example.money.domain.repository.LuckDiaryRepository
import com.example.money.notification.DebtReminderScheduler
import com.example.money.notification.DebtReminderSchedulerImpl
import com.example.money.notification.PersonalDebtMessageScheduler
import com.example.money.notification.PersonalDebtMessageSchedulerImpl
import com.example.money.notification.WeeklyCheckInScheduler
import com.example.money.voice.VoiceReportScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first

/**
 * Single composition root — ViewModels and workers resolve dependencies here
 * instead of reaching into [com.example.money.MoneyApplication] fields directly.
 */
class AppContainer(
    private val application: MoneyApplication,
    val applicationScope: CoroutineScope,
) {
    val database: AppDatabase by lazy { AppDatabase.getInstance(application) }

    val deviceIdProvider: DeviceIdProvider by lazy { DeviceIdProvider(application) }

    val syncQueueRecorder: SyncQueueRecorder by lazy {
        SyncQueueRecorder(
            syncQueueDao = database.syncQueueDao(),
            debtorDao = database.debtorDao(),
            debtPaymentDao = database.debtPaymentDao(),
        )
    }

    val preferencesStore: MoneyPreferencesStore by lazy {
        MoneyPreferencesStore(application, syncQueueRecorder)
    }

    val financeSnapshotRepository: FinanceSnapshotRepository by lazy {
        FinanceSnapshotRepositoryImpl(database, preferencesStore, deviceIdProvider)
    }

    val financeSnapshotApplier: FinanceSnapshotApplier by lazy {
        FinanceSnapshotApplier(database, preferencesStore)
    }

    val syncService: SyncService by lazy {
        SyncService(
            context = application,
            deviceIdProvider = deviceIdProvider,
            financeSnapshotRepository = financeSnapshotRepository,
            financeSnapshotApplier = financeSnapshotApplier,
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

    val luckDiaryRepository: LuckDiaryRepository by lazy {
        LuckDiaryRepositoryImpl(database.luckDiaryDao())
    }

    val debtReminderScheduler: DebtReminderScheduler by lazy {
        DebtReminderSchedulerImpl(application)
    }

    val debtCalendarSyncService: DebtCalendarSyncService by lazy {
        DebtCalendarSyncService(application)
    }

    val debtCalendarReconciler: DebtCalendarReconciler by lazy {
        DebtCalendarReconciler(
            database = database,
            gateway = debtCalendarSyncService,
            loadDisplayName = {
                preferencesStore.userProfile.first().resolvedDisplayName()
            },
        )
    }

    val personalDebtMessageScheduler: PersonalDebtMessageScheduler by lazy {
        PersonalDebtMessageSchedulerImpl(application)
    }

    private val debtReminderRestoreReconciler: DebtReminderRestoreReconciler by lazy {
        DebtReminderRestoreReconciler(
            scheduler = debtReminderScheduler,
            loadSettings = { preferencesStore.personalBranchSettings.first() },
            loadActiveDebtors = { debtorRepository.getActiveDebtors().first() },
        )
    }

    val postRestoreReconciler: PostRestoreReconciler by lazy {
        DefaultPostRestoreReconciler(
            reconcileDebtReminders = debtReminderRestoreReconciler::reconcile,
            reconcilePersonalDebtMessaging = {
                personalDebtMessageScheduler.syncSchedule(
                    preferencesStore.personalBranchSettings.first(),
                )
            },
            reconcileAutopilot = { MicroPaymentScheduler.syncSchedule(application) },
            reconcileWeeklyCheckIn = { WeeklyCheckInScheduler.syncSchedule(application) },
            reconcileVoiceReports = { VoiceReportScheduler.syncSchedule(application) },
            refreshWidgetSnapshot = application::refreshWidgetSnapshot,
            reconcileCalendar = {
                debtCalendarReconciler.reconcileMissingActiveEvents(
                    preferencesStore.calendarSettings.first(),
                )
            },
        )
    }

    private val visionImageStore: VisionImageStore by lazy {
        VisionImageStore(application, preferencesStore)
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepositoryImpl(
            exporter = BackupExporter(
                database = database,
                preferencesStore = preferencesStore,
                visionImageStorage = visionImageStore,
                appVersionName = application.packageManager
                    .getPackageInfo(application.packageName, 0)
                    .versionName
                    .orEmpty()
                    .ifBlank { "unknown" },
            ),
            importer = BackupImporter(
                roomApplier = BackupRoomApplier(database),
                preferencesStore = preferencesStore,
                visionImageStorage = visionImageStore,
                postRestoreReconciler = postRestoreReconciler,
            ),
        )
    }
}

fun Application.appContainer(): AppContainer =
    (this as com.example.money.MoneyApplication).container
