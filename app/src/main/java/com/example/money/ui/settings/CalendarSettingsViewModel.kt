package com.example.money.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.MoneyApplication
import com.example.money.calendar.DebtCalendarReconciler
import com.example.money.calendar.DebtCalendarSyncService
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.settings.CalendarSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalendarSettingsViewModel(
    private val app: MoneyApplication,
    private val preferencesStore: MoneyPreferencesStore,
    private val debtCalendarSyncService: DebtCalendarSyncService,
    private val debtCalendarReconciler: DebtCalendarReconciler,
) : ViewModel() {

    val settings: StateFlow<CalendarSettings> = preferencesStore.calendarSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarSettings(),
    )

    fun update(transform: (CalendarSettings) -> CalendarSettings) {
        viewModelScope.launch {
            val before = settings.value
            preferencesStore.updateCalendarSettings(transform)
            val after = preferencesStore.calendarSettings.first()
            if (!before.autoSyncEnabled && after.autoSyncEnabled) {
                syncMissingDebtEvents(after)
            } else if (reminderSettingsChanged(before, after)) {
                app.debtorRepository.getAllDebtors().first()
                    .filter { !it.isPaid && !it.calendarEventId.isNullOrBlank() }
                    .forEach { debtor ->
                        upsertAndPersist(debtor, after, force = true)
                    }
            }
        }
    }

    private suspend fun syncMissingDebtEvents(settings: CalendarSettings) {
        debtCalendarReconciler.reconcileMissingActiveEvents(settings)
    }

    private suspend fun upsertAndPersist(
        debtor: com.example.money.domain.model.Debtor,
        settings: CalendarSettings,
        force: Boolean,
    ) {
        val profile = preferencesStore.userProfile.first()
        val result = debtCalendarSyncService.upsertDebtEvent(
            debtor = debtor,
            settings = settings,
            displayName = profile.resolvedDisplayName(debtCalendarSyncService.defaultDisplayName()),
            force = force,
        )
        val latest = app.debtorRepository.getDebtorById(debtor.id) ?: return
        app.debtorRepository.updateDebtor(
            latest.copy(
                calendarEventId = result.eventId,
                calendarSyncFailed = result.failed,
            ),
            latest.affectsBalance,
        )
    }

    private fun reminderSettingsChanged(before: CalendarSettings, after: CalendarSettings): Boolean =
        before.remindOneDayBefore != after.remindOneDayBefore ||
            before.remindThreeDaysBefore != after.remindThreeDaysBefore ||
            before.remindOnDueDay != after.remindOnDueDay

    fun createDebtsCalendar() {
        viewModelScope.launch {
            val current = settings.value
            val id = debtCalendarSyncService.ensureDebtsCalendar(current)
            if (id != null) {
                preferencesStore.updateCalendarSettings {
                    it.copy(debtsCalendarId = id, useDebtsCalendar = true)
                }
            }
        }
    }

    class Factory(
        private val app: MoneyApplication,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CalendarSettingsViewModel::class.java)) {
                return CalendarSettingsViewModel(
                    app,
                    app.preferencesStore,
                    app.debtCalendarSyncService,
                    app.container.debtCalendarReconciler,
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
