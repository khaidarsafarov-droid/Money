package com.example.money.di

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import com.example.money.MoneyApplication
import com.example.money.autopilot.MicroPaymentScheduler
import com.example.money.calendar.DebtCalendarReconciler
import com.example.money.calendar.DebtCalendarSyncService
import com.example.money.data.backup.AutomaticBackupExecutor
import com.example.money.data.backup.AutomaticBackupScheduler
import com.example.money.data.backup.BackupExporter
import com.example.money.data.backup.BackupCredentialCache
import com.example.money.data.backup.BackupDeviceStateStore
import com.example.money.data.backup.BackupImporter
import com.example.money.data.backup.BackupRepositoryImpl
import com.example.money.data.backup.BackupRoomApplier
import com.example.money.data.backup.BackupStateStore
import com.example.money.data.backup.DebtReminderRestoreReconciler
import com.example.money.data.backup.DefaultLocalDataResetter
import com.example.money.data.backup.DefaultPostRestoreReconciler
import com.example.money.data.backup.EncryptedBackupCredentialCache
import com.example.money.data.backup.GoogleDriveAuthorizationManager
import com.example.money.data.backup.GoogleDriveBackupStorageProvider
import com.example.money.data.backup.GoogleDriveRestClient
import com.example.money.data.backup.GoogleDriveStorageProviderFactory
import com.example.money.data.backup.LocalBackupDocumentGateway
import com.example.money.data.backup.LocalDataResetter
import com.example.money.data.backup.PostRestoreReconciler
import com.example.money.data.backup.UrlConnectionGoogleDriveHttpTransport
import com.example.money.data.backup.VisionImageStore
import com.example.money.data.backup.WorkManagerAutomaticBackupScheduler
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.data.local.AppDatabase
import com.example.money.data.locale.AppLanguageStorage
import com.example.money.data.repository.BudgetRepositoryImpl
import com.example.money.data.repository.DebtorRepositoryImpl
import com.example.money.data.repository.GoalRepositoryImpl
import com.example.money.data.repository.NoteRepositoryImpl
import com.example.money.data.repository.LuckDiaryRepositoryImpl
import com.example.money.data.sync.AndroidKeystoreSyncKeyVault
import com.example.money.data.sync.DeviceIdProvider
import com.example.money.data.sync.FinanceSnapshotApplier
import com.example.money.data.sync.FinanceSnapshotRepositoryImpl
import com.example.money.data.sync.NsdPeerDiscovery
import com.example.money.data.sync.PeerDiscoveryFactory
import com.example.money.data.sync.SyncQueueRecorder
import com.example.money.data.sync.SyncService
import com.example.money.domain.repository.BudgetRepository
import com.example.money.domain.repository.BackupRepository
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.repository.FinanceSnapshotRepository
import com.example.money.domain.repository.GoalRepository
import com.example.money.domain.repository.NoteRepository
import com.example.money.domain.repository.LuckDiaryRepository
import com.example.money.notification.DebtReminderScheduler
import com.example.money.notification.DebtReminderSchedulerImpl
import com.example.money.notification.PersonalDebtMessageScheduler
import com.example.money.notification.PersonalDebtMessageSchedulerImpl
import com.example.money.notification.WeeklyCheckInScheduler
import com.example.money.ui.home.OnboardingPreferences
import com.example.money.ui.home.WelcomePreferences
import com.example.money.voice.VoiceReportScheduler
import com.google.android.gms.auth.api.identity.Identity
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

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
            syncTombstoneDao = database.syncTombstoneDao(),
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
            readDeviceId = { deviceIdProvider.getDeviceId() },
            financeSnapshotRepository = financeSnapshotRepository,
            applySnapshot = { snapshot -> financeSnapshotApplier.apply(snapshot) },
            syncQueueDao = database.syncQueueDao(),
            scope = applicationScope,
            keyVault = AndroidKeystoreSyncKeyVault(application),
            syncEnabled = { preferencesStore.peerSyncEnabled.first() },
            discoveryFactory = PeerDiscoveryFactory { serviceName, onPeerResolved, onPeerLost ->
                NsdPeerDiscovery(
                    context = application,
                    localServiceName = serviceName,
                    onPeerResolved = onPeerResolved,
                    onPeerLost = onPeerLost,
                )
            },
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
            expenseDao = database.expenseDao(),
            goalActionDao = database.goalActionDao(),
            syncQueueRecorder = syncQueueRecorder,
        )
    }

    val luckDiaryRepository: LuckDiaryRepository by lazy {
        LuckDiaryRepositoryImpl(database.luckDiaryDao())
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(
            noteDao = database.financeNoteDao(),
            syncQueueRecorder = syncQueueRecorder,
        )
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

    val localDataResetter: LocalDataResetter by lazy {
        DefaultLocalDataResetter(
            database = database,
            preferencesStore = preferencesStore,
            visionImageStorage = visionImageStore,
            backupStateStore = backupStateStore,
            credentialCache = backupCredentialCache,
            resetAppState = {
                deviceIdProvider.clear()
                AppLanguageStorage.clear(application)
                WelcomePreferences.clear(application)
                OnboardingPreferences.clear(application)
            },
            stopPeerSync = {
                withContext(Dispatchers.IO) {
                    syncService.stop()
                }
            },
            cleanCalendarLinks = { links ->
                val failedLinks = links.filterNot { link ->
                    debtCalendarSyncService.deleteDebtEvent(link.eventId, link.debtorId)
                }
                if (failedLinks.isNotEmpty()) {
                    throw IOException(
                        "Unable to remove ${failedLinks.size} linked calendar event(s)",
                    )
                }
            },
            cancelSchedules = {
                withContext(Dispatchers.IO) {
                    WorkManager.getInstance(application).cancelAllWork().result.get()
                }
            },
            cancelNotifications = {
                NotificationManagerCompat.from(application).cancelAll()
            },
        )
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

    val localBackupDocumentGateway: LocalBackupDocumentGateway by lazy {
        LocalBackupDocumentGateway(application.contentResolver)
    }

    val googleDriveAuthorizationManager: GoogleDriveAuthorizationManager by lazy {
        GoogleDriveAuthorizationManager(Identity.getAuthorizationClient(application))
    }

    val backupStateStore: BackupStateStore by lazy {
        BackupDeviceStateStore(application)
    }

    val backupCredentialCache: BackupCredentialCache by lazy {
        EncryptedBackupCredentialCache(application)
    }

    val automaticBackupScheduler: AutomaticBackupScheduler by lazy {
        WorkManagerAutomaticBackupScheduler(application)
    }

    private val googleDriveRestClient: GoogleDriveRestClient by lazy {
        GoogleDriveRestClient(UrlConnectionGoogleDriveHttpTransport())
    }

    val googleDriveStorageProviderFactory: GoogleDriveStorageProviderFactory by lazy {
        GoogleDriveStorageProviderFactory(::googleDriveBackupStorageProvider)
    }

    val automaticBackupExecutor: AutomaticBackupExecutor by lazy {
        AutomaticBackupExecutor(
            backupRepository = backupRepository,
            stateStore = backupStateStore,
            credentialCache = backupCredentialCache,
            authorizer = googleDriveAuthorizationManager,
            providerFactory = googleDriveStorageProviderFactory,
        )
    }

    fun googleDriveBackupStorageProvider(
        accessToken: String,
    ): GoogleDriveBackupStorageProvider =
        GoogleDriveBackupStorageProvider(accessToken, googleDriveRestClient)
}

fun Application.appContainer(): AppContainer =
    (this as com.example.money.MoneyApplication).container
