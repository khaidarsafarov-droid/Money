package com.example.money.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.MoneyApplication
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
            if (reminderSettingsChanged(before, after)) {
                app.debtorRepository.getAllDebtors().first()
                    .filter { !it.isPaid && !it.calendarEventId.isNullOrBlank() }
                    .forEach { debtor ->
                        val profile = preferencesStore.userProfile.first()
                        val result = debtCalendarSyncService.upsertDebtEvent(
                            debtor = debtor,
                            settings = after,
                            displayName = profile.resolvedDisplayName(),
                            force = true,
                        )
                        val latest = app.debtorRepository.getDebtorById(debtor.id) ?: return@forEach
                        app.debtorRepository.updateDebtor(
                            latest.copy(
                                calendarEventId = result.eventId,
                                calendarSyncFailed = result.failed,
                            ),
                            latest.affectsBalance,
                        )
                    }
            }
        }
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
                return CalendarSettingsViewModel(app, app.preferencesStore, app.debtCalendarSyncService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
