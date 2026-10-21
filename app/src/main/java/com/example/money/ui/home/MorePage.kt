package com.example.money.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoMode
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.money.R
import com.example.money.settings.AppMode
import com.example.money.ui.theme.MoneyRadius
import com.example.money.ui.theme.MoneySpacing

@Composable
internal fun MorePage(
    appMode: AppMode,
    onAppModeChange: (AppMode) -> Unit,
    onOpenNotes: () -> Unit,
    onOpenClients: () -> Unit,
    onOpenAdvisor: () -> Unit,
    onOpenBackupSettings: () -> Unit,
    onOpenIncomeIdeas: () -> Unit,
    onOpenVoiceSettings: () -> Unit,
    onOpenAutopilotSettings: () -> Unit,
    onOpenCurrency: () -> Unit,
    onOpenAppLanguage: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onOpenDebtNotificationSettings: () -> Unit,
    onOpenCalendarSettings: () -> Unit,
    onAfterClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = MoneySpacing.lg),
        verticalArrangement = Arrangement.spacedBy(MoneySpacing.xs),
    ) {
        if (showSettings) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = MoneySpacing.sm),
            ) {
                IconButton(onClick = { showSettings = false }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_navigate_up),
                    )
                }
                Text(
                    text = stringResource(R.string.settings_open),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            DrawerGroupLabel(stringResource(R.string.drawer_group_data))
            SettingsDrawerItem(
                label = stringResource(R.string.backup_settings_title),
                icon = Icons.Outlined.CloudUpload,
                onClick = {
                    onOpenBackupSettings()
                    onAfterClick()
                },
            )
            DrawerGroupLabel(stringResource(R.string.drawer_group_autopilot))
            SettingsDrawerItem(
                label = stringResource(R.string.autopilot_settings_title),
                icon = Icons.Outlined.AutoMode,
                onClick = {
                    onOpenAutopilotSettings()
                    onAfterClick()
                },
            )
            DrawerGroupLabel(stringResource(R.string.drawer_group_settings))
            SettingsDrawerItem(
                label = stringResource(R.string.currency_settings_title),
                icon = Icons.Outlined.Payments,
                onClick = {
                    onOpenCurrency()
                    onAfterClick()
                },
            )
            SettingsDrawerItem(
                label = stringResource(R.string.app_language_menu),
                icon = Icons.Outlined.Translate,
                onClick = {
                    onOpenAppLanguage()
                    onAfterClick()
                },
            )
            SettingsDrawerItem(
                label = stringResource(R.string.sync_settings_title),
                icon = Icons.Outlined.Sync,
                onClick = {
                    onOpenSyncSettings()
                    onAfterClick()
                },
            )
            SettingsDrawerItem(
                label = stringResource(R.string.privacy_settings_title),
                icon = Icons.Outlined.Lock,
                onClick = {
                    onOpenPrivacySettings()
                    onAfterClick()
                },
            )
            SettingsDrawerItem(
                label = stringResource(R.string.debt_notification_settings_title),
                icon = Icons.Outlined.Notifications,
                onClick = {
                    onOpenDebtNotificationSettings()
                    onAfterClick()
                },
            )
            SettingsDrawerItem(
                label = stringResource(R.string.calendar_settings_title),
                icon = Icons.Outlined.CalendarMonth,
                onClick = {
                    onOpenCalendarSettings()
                    onAfterClick()
                },
            )
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MoneySpacing.lg),
                shape = RoundedCornerShape(MoneyRadius.lg),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                Column(modifier = Modifier.padding(MoneySpacing.lg)) {
                    Text(
                        text = stringResource(R.string.home_brand),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            if (appMode == AppMode.PROFESSIONAL) {
                                R.string.mode_professional_short
                            } else {
                                R.string.mode_personal_short
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MoneySpacing.lg, vertical = MoneySpacing.sm),
            ) {
                AppMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = appMode == mode,
                        onClick = { onAppModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, AppMode.entries.size),
                        icon = {},
                        label = {
                            Text(
                                stringResource(
                                    if (mode == AppMode.PROFESSIONAL) {
                                        R.string.mode_professional_short
                                    } else {
                                        R.string.mode_personal_short
                                    },
                                ),
                            )
                        },
                    )
                }
            }
            SettingsDrawerItem(
                label = stringResource(R.string.tab_notes),
                icon = Icons.Outlined.EditNote,
                onClick = {
                    onOpenNotes()
                    onAfterClick()
                },
            )
            if (appMode == AppMode.PROFESSIONAL) {
                SettingsDrawerItem(
                    label = stringResource(R.string.clients_title),
                    icon = Icons.Outlined.Work,
                    onClick = {
                        onOpenClients()
                        onAfterClick()
                    },
                )
            }
            DrawerGroupLabel(stringResource(R.string.drawer_group_tools))
            SettingsDrawerItem(
                label = stringResource(R.string.advisor_open),
                icon = Icons.Outlined.Psychology,
                onClick = {
                    onOpenAdvisor()
                    onAfterClick()
                },
            )
            SettingsDrawerItem(
                label = stringResource(R.string.income_ideas_title),
                icon = Icons.Outlined.Lightbulb,
                onClick = {
                    onOpenIncomeIdeas()
                    onAfterClick()
                },
            )
            SettingsDrawerItem(
                label = stringResource(R.string.voice_settings_menu),
                icon = Icons.Outlined.Mic,
                onClick = {
                    onOpenVoiceSettings()
                    onAfterClick()
                },
            )
            SettingsDrawerItem(
                label = stringResource(R.string.settings_open),
                icon = Icons.Outlined.Settings,
                onClick = { showSettings = true },
            )
        }
    }
}
