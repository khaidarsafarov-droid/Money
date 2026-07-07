package com.example.money.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.money.ui.goal.GoalScreen
import com.example.money.ui.home.HomeLaunchConfig
import com.example.money.ui.home.HomeScreen
import com.example.money.ui.settings.AppLanguageScreen
import com.example.money.ui.settings.AutopilotSettingsScreen
import com.example.money.ui.settings.PrivacySettingsScreen
import com.example.money.ui.settings.VoiceSettingsScreen

@Composable
fun MoneyNavHost(
    launchConfig: HomeLaunchConfig = HomeLaunchConfig(),
    notificationsEnabled: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onLaunchConfigConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var showVoiceSettings by remember { mutableStateOf(false) }
    var showAppLanguage by remember { mutableStateOf(false) }
    var showPrivacySettings by remember { mutableStateOf(false) }
    var showAutopilotSettings by remember { mutableStateOf(false) }
    var showGoalHub by remember { mutableStateOf(false) }

    when {
        showGoalHub -> GoalScreen(onBack = { showGoalHub = false }, modifier = modifier)
        showAppLanguage -> AppLanguageScreen(onBack = { showAppLanguage = false }, modifier = modifier)
        showPrivacySettings -> PrivacySettingsScreen(onBack = { showPrivacySettings = false }, modifier = modifier)
        showAutopilotSettings -> AutopilotSettingsScreen(onBack = { showAutopilotSettings = false }, modifier = modifier)
        showVoiceSettings -> VoiceSettingsScreen(onBack = { showVoiceSettings = false }, modifier = modifier)
        else -> HomeScreen(
            launchConfig = launchConfig,
            notificationsEnabled = notificationsEnabled,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onLaunchConfigConsumed = onLaunchConfigConsumed,
            onOpenVoiceSettings = { showVoiceSettings = true },
            onOpenAppLanguage = { showAppLanguage = true },
            onOpenPrivacySettings = { showPrivacySettings = true },
            onOpenAutopilotSettings = { showAutopilotSettings = true },
            onOpenGoalHub = { showGoalHub = true },
            modifier = modifier,
        )
    }
}
