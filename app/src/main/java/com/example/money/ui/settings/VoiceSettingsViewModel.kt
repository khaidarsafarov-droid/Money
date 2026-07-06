package com.example.money.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.money.MoneyApplication
import com.example.money.data.datastore.MoneyPreferencesStore
import com.example.money.voice.VoiceGender
import com.example.money.voice.VoiceLanguage
import com.example.money.voice.VoiceReportEngine
import com.example.money.voice.VoiceReportScheduler
import com.example.money.voice.VoiceReportSettings
import com.example.money.voice.VoiceReportSpeaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VoiceSettingsViewModel(
    application: Application,
    private val preferencesStore: MoneyPreferencesStore,
) : AndroidViewModel(application) {

    val settings: StateFlow<VoiceReportSettings> = preferencesStore.voiceReportSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = VoiceReportSettings(),
        )

    private val _isTestingVoice = MutableStateFlow(false)
    val isTestingVoice: StateFlow<Boolean> = _isTestingVoice.asStateFlow()

    private val voiceEngine: VoiceReportEngine by lazy {
        VoiceReportEngine(getApplication<MoneyApplication>())
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesStore.updateVoiceReportSettings { current ->
                current.copy(enabled = enabled)
            }
            VoiceReportScheduler.syncSchedule(getApplication())
        }
    }

    fun setReportTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesStore.updateVoiceReportSettings { current ->
                current.copy(reportHour = hour, reportMinute = minute)
            }
            VoiceReportScheduler.syncSchedule(getApplication())
        }
    }

    fun setLanguage(language: VoiceLanguage) {
        viewModelScope.launch {
            preferencesStore.updateVoiceReportSettings { current ->
                current.copy(language = language)
            }
        }
    }

    fun setGender(gender: VoiceGender) {
        viewModelScope.launch {
            preferencesStore.updateVoiceReportSettings { current ->
                current.copy(gender = gender)
            }
        }
    }

    fun setForceInSilentMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesStore.updateVoiceReportSettings { current ->
                current.copy(forceInSilentMode = enabled)
            }
        }
    }

    fun testVoiceReport(onResult: (VoiceReportSpeaker.SpeakResult) -> Unit) {
        if (_isTestingVoice.value) return
        viewModelScope.launch {
            _isTestingVoice.value = true
            try {
                onResult(voiceEngine.runTestReport())
            } finally {
                _isTestingVoice.value = false
            }
        }
    }

    class Factory(
        private val application: Application,
        private val preferencesStore: MoneyPreferencesStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VoiceSettingsViewModel::class.java)) {
                return VoiceSettingsViewModel(application, preferencesStore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
