package com.example.money.di

import android.app.Application
import com.example.money.calendar.DebtCalendarSyncService
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
import com.example.money.domain.repository.DebtorRepository
import com.example.money.domain.repository.FinanceSnapshotRepository
import com.example.money.domain.repository.GoalRepository
import com.example.money.domain.repository.LuckDiaryRepository
import com.example.money.notification.DebtReminderScheduler
import com.example.money.notification.DebtReminderSchedulerImpl
import com.example.money.notification.PersonalDebtMessageScheduler
import com.example.money.notification.PersonalDebtMessageSchedulerImpl
import kotlinx.coroutines.CoroutineScope

/**
 * Single composition root — ViewModels and workers resolve dependencies here
 * instead of reaching into [com.example.money.MoneyApplication] fields directly.
 */
class AppContainer(
    private val application: Application,
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

    val personalDebtMessageScheduler: PersonalDebtMessageScheduler by lazy {
        PersonalDebtMessageSchedulerImpl(application)
    }
}

fun Application.appContainer(): AppContainer =
    (this as com.example.money.MoneyApplication).container
