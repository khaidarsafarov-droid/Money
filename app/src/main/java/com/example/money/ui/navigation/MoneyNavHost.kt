package com.example.money.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.money.MoneyApplication
import com.example.money.settings.UserProfile
import com.example.money.ui.goal.GoalScreen
import com.example.money.ui.home.HomeLaunchConfig
import com.example.money.ui.home.HomeScreen
import com.example.money.ui.onboarding.OnboardingScreen
import com.example.money.ui.settings.AppLanguageScreen
import com.example.money.ui.settings.AutopilotSettingsScreen
import com.example.money.ui.settings.BackupSettingsScreen
import com.example.money.ui.settings.CalendarSettingsScreen
import com.example.money.ui.settings.DebtNotificationSettingsScreen
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
    val navController = rememberNavController()
    val userProfile by app.preferencesStore.userProfile.collectAsStateWithLifecycle(
        initialValue = UserProfile(),
    )
    val calendarSettings by app.preferencesStore.calendarSettings.collectAsStateWithLifecycle(
        initialValue = com.example.money.settings.CalendarSettings(),
    )
    val notificationPermissionRequested by app.preferencesStore.notificationPermissionRequested
        .collectAsStateWithLifecycle(initialValue = false)

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        scope.launch {
            app.preferencesStore.updateCalendarSettings {
                it.copy(permissionRequested = true)
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        scope.launch {
            app.preferencesStore.setNotificationPermissionRequested(true)
        }
    }

    LaunchedEffect(userProfile.onboardingCompleted, calendarSettings.permissionRequested) {
        if (userProfile.onboardingCompleted && !calendarSettings.permissionRequested) {
            calendarPermissionLauncher.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
            )
        }
    }

    LaunchedEffect(userProfile.onboardingCompleted, notificationPermissionRequested, notificationsEnabled) {
        if (
            userProfile.onboardingCompleted &&
            !notificationPermissionRequested &&
            !notificationsEnabled &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (!userProfile.onboardingCompleted) {
        OnboardingScreen(onComplete = { }, modifier = modifier)
        return
    }

    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME,
        modifier = modifier,
    ) {
        composable(AppRoutes.HOME) {
            HomeScreen(
                launchConfig = launchConfig,
                notificationsEnabled = notificationsEnabled,
                onRequestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onRequestNotificationPermission()
                    }
                },
                onLaunchConfigConsumed = onLaunchConfigConsumed,
                onOpenVoiceSettings = { navController.navigate(AppRoutes.VOICE_SETTINGS) },
                onOpenAppLanguage = { navController.navigate(AppRoutes.APP_LANGUAGE) },
                onOpenPrivacySettings = { navController.navigate(AppRoutes.PRIVACY_SETTINGS) },
                onOpenAutopilotSettings = { navController.navigate(AppRoutes.AUTOPILOT_SETTINGS) },
                onOpenCalendarSettings = { navController.navigate(AppRoutes.CALENDAR_SETTINGS) },
                onOpenDebtNotificationSettings = { navController.navigate(AppRoutes.DEBT_NOTIFICATION_SETTINGS) },
                onOpenBackupSettings = { navController.navigate(AppRoutes.BACKUP_SETTINGS) },
                onOpenGoalHub = { navController.navigate(AppRoutes.GOAL_HUB) },
            )
        }
        composable(AppRoutes.GOAL_HUB) {
            GoalScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.APP_LANGUAGE) {
            AppLanguageScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.PRIVACY_SETTINGS) {
            PrivacySettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.AUTOPILOT_SETTINGS) {
            AutopilotSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.CALENDAR_SETTINGS) {
            CalendarSettingsScreen(
                onBack = { navController.popBackStack() },
                onRequestPermission = {
                    calendarPermissionLauncher.launch(
                        arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                    )
                },
            )
        }
        composable(AppRoutes.DEBT_NOTIFICATION_SETTINGS) {
            DebtNotificationSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.VOICE_SETTINGS) {
            VoiceSettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.BACKUP_SETTINGS) {
            BackupSettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
