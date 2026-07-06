package com.example.money.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.money.ui.home.HomeLaunchConfig
import com.example.money.ui.home.HomeScreen
import com.example.money.ui.settings.AppLanguageScreen
import com.example.money.ui.settings.VoiceSettingsScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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

    if (showAppLanguage) {
        AppLanguageScreen(
            onBack = { showAppLanguage = false },
            modifier = modifier,
        )
        return
    }

    if (showVoiceSettings) {
        VoiceSettingsScreen(
            onBack = { showVoiceSettings = false },
            modifier = modifier,
        )
        return
    }

    HomeScreen(
        launchConfig = launchConfig,
        notificationsEnabled = notificationsEnabled,
        onRequestNotificationPermission = onRequestNotificationPermission,
        onLaunchConfigConsumed = onLaunchConfigConsumed,
        onOpenVoiceSettings = { showVoiceSettings = true },
        onOpenAppLanguage = { showAppLanguage = true },
        modifier = modifier,
    )
}
