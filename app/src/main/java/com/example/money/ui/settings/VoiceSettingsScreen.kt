package com.example.money.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.ui.theme.CreamBackground
import com.example.money.ui.theme.CreamSurface
import com.example.money.ui.util.withLightHaptic
import com.example.money.voice.VoiceGender
import com.example.money.voice.VoiceLanguage
import com.example.money.voice.VoiceReportSpeaker
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val voiceTestSuccess = stringResource(R.string.voice_settings_test_success)
    val voiceTestSilent = stringResource(R.string.voice_settings_test_silent)
    val voiceTestDnd = stringResource(R.string.voice_settings_test_dnd)
    val voiceTestFocusDenied = stringResource(R.string.voice_settings_test_focus_denied)
    val voiceTestUnavailable = stringResource(R.string.voice_settings_test_unavailable)
    val voiceTestError = stringResource(R.string.voice_settings_test_error)
    val app = context.applicationContext as MoneyApplication
    val viewModel: VoiceSettingsViewModel = viewModel(
        factory = VoiceSettingsViewModel.Factory(app, app.preferencesStore),
    )
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isTestingVoice by viewModel.isTestingVoice.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = settings.reportHour,
            initialMinute = settings.reportMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.voice_settings_time_title)) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setReportTime(pickerState.hour, pickerState.minute)
                        showTimePicker = false
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    feedbackMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { feedbackMessage = null },
            title = { Text(stringResource(R.string.voice_settings_test_title)) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { feedbackMessage = null }) {
                    Text(stringResource(R.string.action_got_it))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = CreamBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.voice_settings_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_up))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamBackground),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CreamSurface),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.voice_settings_enabled_label),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.voice_settings_enabled_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.enabled,
                            onCheckedChange = withLightHaptic(LocalContext.current, onCheckedChange = viewModel::setEnabled),
                        )
                    }

                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        enabled = settings.enabled,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(
                            text = stringResource(
                                R.string.voice_settings_time_value,
                                formatTime(settings.reportHour, settings.reportMinute),
                            ),
                        )
                    }

                    VoiceSettingSection(
                        title = stringResource(R.string.voice_settings_language_label),
                        hint = stringResource(R.string.voice_settings_language_hint),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.language == VoiceLanguage.RU,
                                onClick = { viewModel.setLanguage(VoiceLanguage.RU) },
                                label = { Text(stringResource(R.string.voice_settings_language_ru)) },
                            )
                            FilterChip(
                                selected = settings.language == VoiceLanguage.EN,
                                onClick = { viewModel.setLanguage(VoiceLanguage.EN) },
                                label = { Text(stringResource(R.string.voice_settings_language_en)) },
                            )
                        }
                    }

                    VoiceSettingSection(
                        title = stringResource(R.string.voice_settings_gender_label),
                        hint = stringResource(R.string.voice_settings_gender_hint),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.gender == VoiceGender.MALE,
                                onClick = { viewModel.setGender(VoiceGender.MALE) },
                                label = { Text(stringResource(R.string.voice_settings_gender_male)) },
                            )
                            FilterChip(
                                selected = settings.gender == VoiceGender.FEMALE,
                                onClick = { viewModel.setGender(VoiceGender.FEMALE) },
                                label = { Text(stringResource(R.string.voice_settings_gender_female)) },
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.voice_settings_force_silent_label),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = stringResource(R.string.voice_settings_force_silent_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = settings.forceInSilentMode,
                            onCheckedChange = withLightHaptic(
                                LocalContext.current,
                                onCheckedChange = viewModel::setForceInSilentMode,
                            ),
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.testVoiceReport { result ->
                                feedbackMessage = when (result) {
                                    VoiceReportSpeaker.SpeakResult.Success -> voiceTestSuccess
                                    VoiceReportSpeaker.SpeakResult.BlockedSilent -> voiceTestSilent
                                    VoiceReportSpeaker.SpeakResult.BlockedDnd -> voiceTestDnd
                                    VoiceReportSpeaker.SpeakResult.AudioFocusDenied -> voiceTestFocusDenied
                                    VoiceReportSpeaker.SpeakResult.TtsUnavailable -> voiceTestUnavailable
                                    VoiceReportSpeaker.SpeakResult.TtsError -> voiceTestError
                                }
                            }
                        },
                        enabled = !isTestingVoice,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        if (isTestingVoice) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isTestingVoice) {
                                stringResource(R.string.voice_settings_test_running)
                            } else {
                                stringResource(R.string.voice_settings_test_button)
                            },
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.voice_settings_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

@Composable
private fun VoiceSettingSection(
    title: String,
    hint: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}
