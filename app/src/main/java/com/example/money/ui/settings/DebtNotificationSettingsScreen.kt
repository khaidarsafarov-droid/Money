package com.example.money.ui.settings

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.money.MoneyApplication
import com.example.money.R
import com.example.money.ui.debtors.DebtorsViewModel
import com.example.money.ui.util.withLightHaptic
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtNotificationSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val viewModel: DebtorsViewModel = viewModel(
        factory = DebtorsViewModel.Factory(
            app.debtorRepository,
            app.debtReminderScheduler,
            app.preferencesStore,
            app.debtCalendarSyncService,
            app.personalDebtMessageScheduler,
        ),
    )
    val settings by viewModel.personalBranchSettings.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(settings.debtAutoMessageEnabled) {
        app.syncPersonalDebtMessageSchedule()
    }
    LaunchedEffect(
        settings.debtNotificationsEnabled,
        settings.clientNotificationsEnabled,
        settings.debtAdvanceRemind1Day,
        settings.debtAdvanceRemind3Days,
        settings.reminderHour,
        settings.reminderMinute,
    ) {
        app.rescheduleDebtReminders()
    }

    if (showTimePicker) {
        val pickerState = rememberTimePickerState(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.debt_notification_hour)) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updatePersonalBranchSettings {
                            it.copy(reminderHour = pickerState.hour, reminderMinute = pickerState.minute)
                        }
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debt_notification_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_up))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsToggle(
                label = stringResource(R.string.debt_notification_personal),
                checked = settings.debtNotificationsEnabled,
                onCheckedChange = { enabled ->
                    viewModel.updatePersonalBranchSettings { it.copy(debtNotificationsEnabled = enabled) }
                },
            )
            SettingsToggle(
                label = stringResource(R.string.debt_notification_clients),
                checked = settings.clientNotificationsEnabled,
                onCheckedChange = { enabled ->
                    viewModel.updatePersonalBranchSettings { it.copy(clientNotificationsEnabled = enabled) }
                },
            )
            SettingsToggle(
                label = stringResource(R.string.debt_notification_advance_3d),
                checked = settings.debtAdvanceRemind3Days,
                onCheckedChange = { enabled ->
                    viewModel.updatePersonalBranchSettings { it.copy(debtAdvanceRemind3Days = enabled) }
                },
            )
            SettingsToggle(
                label = stringResource(R.string.debt_notification_advance_1d),
                checked = settings.debtAdvanceRemind1Day,
                onCheckedChange = { enabled ->
                    viewModel.updatePersonalBranchSettings { it.copy(debtAdvanceRemind1Day = enabled) }
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { showTimePicker = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.debt_notification_hour))
                Text(
                    String.format(Locale.getDefault(), "%02d:%02d", settings.reminderHour, settings.reminderMinute),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    TextButton(onClick = { openExactAlarmSettings(context) }) {
                        Text(stringResource(R.string.debt_notification_exact))
                    }
                }
                TextButton(onClick = { openBatterySettings(context) }) {
                    Text(stringResource(R.string.debt_notification_battery))
                }
            }
            Text(
                text = stringResource(R.string.debt_auto_message_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
            SettingsToggle(
                label = stringResource(R.string.debt_auto_message_enabled),
                checked = settings.debtAutoMessageEnabled,
                onCheckedChange = { enabled ->
                    viewModel.updatePersonalBranchSettings { it.copy(debtAutoMessageEnabled = enabled) }
                },
            )
            SettingsToggle(
                label = stringResource(R.string.debt_auto_message_sms),
                checked = settings.debtAutoMessageSms,
                onCheckedChange = { enabled ->
                    viewModel.updatePersonalBranchSettings { it.copy(debtAutoMessageSms = enabled) }
                },
            )
            SettingsToggle(
                label = stringResource(R.string.debt_auto_message_whatsapp),
                checked = settings.debtAutoMessageWhatsapp,
                onCheckedChange = { enabled ->
                    viewModel.updatePersonalBranchSettings { it.copy(debtAutoMessageWhatsapp = enabled) }
                },
            )
            SettingsToggle(
                label = stringResource(R.string.debt_auto_message_telegram),
                checked = settings.debtAutoMessageTelegram,
                onCheckedChange = { enabled ->
                    viewModel.updatePersonalBranchSettings { it.copy(debtAutoMessageTelegram = enabled) }
                },
            )
            SettingsToggle(
                label = stringResource(R.string.debt_auto_message_email),
                checked = settings.debtAutoMessageEmail,
                onCheckedChange = { enabled ->
                    viewModel.updatePersonalBranchSettings { it.copy(debtAutoMessageEmail = enabled) }
                },
            )
            SettingsToggle(
                label = stringResource(R.string.debt_direct_sms),
                checked = settings.debtDirectSmsEnabled,
                onCheckedChange = { enabled ->
                    viewModel.updatePersonalBranchSettings { it.copy(debtDirectSmsEnabled = enabled) }
                },
            )
        }
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = withLightHaptic(LocalContext.current, onCheckedChange = onCheckedChange),
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(checked = checked, onCheckedChange = null)
    }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val packageUri = Uri.parse("package:${context.packageName}")
    val request = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData(packageUri)
    startSettings(context, request, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(packageUri))
}

private fun openBatterySettings(context: Context) {
    val packageUri = Uri.parse("package:${context.packageName}")
    startSettings(
        context,
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(packageUri),
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(packageUri),
    )
}

private fun startSettings(context: Context, vararg intents: Intent) {
    val host = context as? Activity ?: context
    intents.forEach { intent ->
        try {
            host.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: ActivityNotFoundException) {
            // Try the next settings screen.
        } catch (_: SecurityException) {
            // Some OEMs reject the battery-exemption intent.
        }
    }
}
