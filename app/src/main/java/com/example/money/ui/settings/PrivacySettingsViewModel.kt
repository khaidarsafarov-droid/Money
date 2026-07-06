package com.example.money.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.MoneyApplication
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.settings.PrivacySettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PrivacySettingsViewModel(
    private val preferencesStore: MoneyPreferencesStore,
) : ViewModel() {

    val settings: StateFlow<PrivacySettings> = preferencesStore.privacySettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PrivacySettings(),
    )

    fun update(transform: (PrivacySettings) -> PrivacySettings) {
        viewModelScope.launch {
            preferencesStore.updatePrivacySettings(transform)
        }
    }

    class Factory(
        private val app: MoneyApplication,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PrivacySettingsViewModel::class.java)) {
                return PrivacySettingsViewModel(app.preferencesStore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
