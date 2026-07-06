package com.example.money.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.MoneyApplication
import com.example.money.autopilot.MicroPaymentScheduler
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.settings.AutopilotSettings
import com.example.money.settings.AutopilotStrategy
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AutopilotSettingsViewModel(
    private val app: MoneyApplication,
    private val preferencesStore: MoneyPreferencesStore,
) : ViewModel() {

    val settings: StateFlow<AutopilotSettings> = preferencesStore.autopilotSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AutopilotSettings(),
    )

    fun update(transform: (AutopilotSettings) -> AutopilotSettings) {
        viewModelScope.launch {
            preferencesStore.updateAutopilotSettings(transform)
            MicroPaymentScheduler.syncSchedule(app)
        }
    }

    fun setStrategy(strategy: AutopilotStrategy) {
        update { it.copy(strategy = strategy) }
    }

    class Factory(
        private val app: MoneyApplication,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AutopilotSettingsViewModel::class.java)) {
                return AutopilotSettingsViewModel(app, app.preferencesStore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
