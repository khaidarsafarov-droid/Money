package com.example.money.ui.navigation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.money.MoneyApplication
import com.example.money.settings.UserProfile
import com.example.money.ui.goal.GoalScreen
import com.example.money.ui.home.HomeLaunchConfig
import com.example.money.ui.home.HomeScreen
import com.example.money.ui.onboarding.OnboardingScreen
import com.example.money.ui.settings.AppLanguageScreen
import com.example.money.ui.settings.AutopilotSettingsScreen
import com.example.money.ui.settings.CalendarSettingsScreen
import com.example.money.ui.settings.PrivacySettingsScreen
import com.example.money.ui.settings.VoiceSettingsScreen
import kotlinx.coroutines.launch

@Composable
fun MoneyNavHost(
    launchConfig: HomeLaunchConfig = HomeLaunchConfig(),
    notificationsEnabled: Boolean = true,
    onRequestNotificationPermission: () -> Unit = {},
    onLaunchConfigConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyApplication
    val scope = rememberCoroutineScope()
    val userProfile by app.preferencesStore.userProfile.collectAsStateWithLifecycle(
        initialValue = UserProfile(),
    )
    val calendarSettings by app.preferencesStore.calendarSettings.collectAsStateWithLifecycle(
        initialValue = com.example.money.settings.CalendarSettings(),
    )

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        scope.launch {
            app.preferencesStore.updateCalendarSettings {
                it.copy(permissionRequested = true)
            }
        }
    }

    LaunchedEffect(userProfile.onboardingCompleted, calendarSettings.permissionRequested) {
        if (userProfile.onboardingCompleted && !calendarSettings.permissionRequested) {
            calendarPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
            )
        }
    }

    var showVoiceSettings by remember { mutableStateOf(false) }
    var showAppLanguage by remember { mutableStateOf(false) }
    var showPrivacySettings by remember { mutableStateOf(false) }
    var showAutopilotSettings by remember { mutableStateOf(false) }
    var showCalendarSettings by remember { mutableStateOf(false) }
    var showGoalHub by remember { mutableStateOf(false) }

    when {
        !userProfile.onboardingCompleted -> OnboardingScreen(
            onComplete = { },
            modifier = modifier,
        )
        showGoalHub -> GoalScreen(onBack = { showGoalHub = false }, modifier = modifier)
        showAppLanguage -> AppLanguageScreen(onBack = { showAppLanguage = false }, modifier = modifier)
        showPrivacySettings -> PrivacySettingsScreen(onBack = { showPrivacySettings = false }, modifier = modifier)
        showAutopilotSettings -> AutopilotSettingsScreen(onBack = { showAutopilotSettings = false }, modifier = modifier)
        showCalendarSettings -> CalendarSettingsScreen(
            onBack = { showCalendarSettings = false },
            onRequestPermission = {
                calendarPermissionLauncher.launch(
                    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                )
            },
            modifier = modifier,
        )
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
            onOpenCalendarSettings = { showCalendarSettings = true },
            onOpenGoalHub = { showGoalHub = true },
            modifier = modifier,
        )
    }
}
