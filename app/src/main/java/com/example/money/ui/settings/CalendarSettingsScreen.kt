package com.example.money.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.money.ui.theme.CreamBackground
import com.example.money.ui.theme.CreamSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSettingsScreen(
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val viewModel: CalendarSettingsViewModel = viewModel(
        factory = CalendarSettingsViewModel.Factory(app),
    )
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = CreamBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calendar_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CreamSurface),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.calendar_settings_title), fontWeight = FontWeight.Bold)
                    SettingSwitch(
                        label = stringResource(R.string.calendar_auto_sync),
                        checked = settings.autoSyncEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.update { it.copy(autoSyncEnabled = enabled) }
                        },
                    )
                    SettingSwitch(
                        label = stringResource(R.string.calendar_remind_1d),
                        checked = settings.remindOneDayBefore,
                        onCheckedChange = { checked ->
                            viewModel.update { it.copy(remindOneDayBefore = checked) }
                        },
                    )
                    SettingSwitch(
                        label = stringResource(R.string.calendar_remind_3d),
                        checked = settings.remindThreeDaysBefore,
                        onCheckedChange = { checked ->
                            viewModel.update { it.copy(remindThreeDaysBefore = checked) }
                        },
                    )
                    SettingSwitch(
                        label = stringResource(R.string.calendar_remind_due),
                        checked = settings.remindOnDueDay,
                        onCheckedChange = { checked ->
                            viewModel.update { it.copy(remindOnDueDay = checked) }
                        },
                    )
                    SettingSwitch(
                        label = stringResource(R.string.calendar_use_debts_calendar),
                        checked = settings.useDebtsCalendar,
                        onCheckedChange = { checked ->
                            viewModel.update { it.copy(useDebtsCalendar = checked) }
                        },
                    )
                    Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.calendar_request_permission))
                    }
                    Button(onClick = viewModel::createDebtsCalendar, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.calendar_create_debts_calendar))
                    }
                    Text(
                        stringResource(R.string.calendar_permission_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
