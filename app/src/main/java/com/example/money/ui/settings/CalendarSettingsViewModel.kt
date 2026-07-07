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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalendarSettingsViewModel(
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
            preferencesStore.updateCalendarSettings(transform)
        }
    }

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
                return CalendarSettingsViewModel(app.preferencesStore, app.debtCalendarSyncService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
