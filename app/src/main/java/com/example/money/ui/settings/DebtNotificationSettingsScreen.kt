package com.example.money.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.money.ui.debtors.DebtorsViewModel

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

    LaunchedEffect(settings.debtAutoMessageEnabled) {
        app.syncPersonalDebtMessageSchedule()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debt_notification_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
            Text(
                text = stringResource(R.string.debt_notification_settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SettingsToggle(
                label = stringResource(R.string.debt_notification_enabled),
                checked = settings.debtNotificationsEnabled,
                onCheckedChange = { enabled ->
                    viewModel.updatePersonalBranchSettings { it.copy(debtNotificationsEnabled = enabled) }
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
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f).padding(end = 8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
