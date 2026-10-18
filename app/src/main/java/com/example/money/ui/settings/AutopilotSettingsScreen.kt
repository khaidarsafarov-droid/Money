package com.example.money.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.money.settings.AutopilotStrategy
import com.example.money.ui.util.Formatters
import com.example.money.ui.util.withLightHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutopilotSettingsScreen(
    onBack: () -> Unit,
    onOpenGoalHub: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val viewModel: AutopilotSettingsViewModel = viewModel(
        factory = AutopilotSettingsViewModel.Factory(app),
    )
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.autopilot_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_navigate_up))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
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
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.autopilot_enabled),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Switch(
                        checked = settings.enabled,
                        onCheckedChange = withLightHaptic(LocalContext.current) { enabled ->
                            viewModel.update { it.copy(enabled = enabled) }
                        },
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.autopilot_daily_amount), fontWeight = FontWeight.SemiBold)
                    Text(
                        Formatters.currencyFromCents(settings.dailyAmountCents),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Slider(
                        value = settings.dailyAmountCents.toFloat(),
                        onValueChange = { value ->
                            val stepped = ((value / 100f).toLong()) * 100L
                            viewModel.update { it.copy(dailyAmountCents = stepped.coerceIn(100L, 10_000L)) }
                        },
                        valueRange = 100f..10_000f,
                        steps = 20,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.autopilot_strategy), fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(
                            R.string.autopilot_strategy_current,
                            stringResource(
                                if (settings.strategy == AutopilotStrategy.AVALANCHE) {
                                    R.string.autopilot_strategy_avalanche
                                } else {
                                    R.string.autopilot_strategy_snowball
                                },
                            ),
                        ),
                    )
                    TextButton(onClick = onOpenGoalHub) {
                        Text(stringResource(R.string.autopilot_strategy_change))
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.autopilot_round_to_ten),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Switch(
                        checked = settings.roundPaymentsToTen,
                        onCheckedChange = withLightHaptic(LocalContext.current) { enabled ->
                            viewModel.update { it.copy(roundPaymentsToTen = enabled) }
                        },
                    )
                }
            }

            val weeklyCheckIn by viewModel.weeklyCheckInSettings.collectAsStateWithLifecycle()
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.weekly_checkin_enabled),
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Switch(
                        checked = weeklyCheckIn.enabled,
                        onCheckedChange = withLightHaptic(LocalContext.current) { enabled ->
                            viewModel.updateWeeklyCheckIn { it.copy(enabled = enabled) }
                        },
                    )
                }
            }
        }
    }
}
