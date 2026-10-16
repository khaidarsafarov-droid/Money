package com.example.money.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.money.MoneyApplication
import com.example.money.settings.CurrencyRegion
import com.example.money.settings.UserProfile
import com.example.money.ui.util.Formatters
import com.example.money.ui.clients.ClientDetailScreen
import com.example.money.ui.clients.ClientEditorScreen
import com.example.money.ui.clients.ClientListScreen
import com.example.money.ui.goal.GoalScreen
import com.example.money.ui.home.HomeLaunchConfig
import com.example.money.ui.home.HomeScreen
import com.example.money.ui.home.OnboardingPreferences
import com.example.money.ui.onboarding.OnboardingScreen
import com.example.money.ui.onboarding.OnboardingSessionViewModel
import com.example.money.ui.theme.CreamBackground
import com.example.money.ui.settings.AppLanguageScreen
import com.example.money.ui.settings.AutopilotSettingsScreen
import com.example.money.ui.settings.BackupSettingsScreen
import com.example.money.ui.settings.CalendarSettingsScreen
import com.example.money.ui.settings.DebtNotificationSettingsScreen
import com.example.money.ui.settings.PrivacySettingsScreen
import com.example.money.ui.settings.SyncPairingScreen
import com.example.money.ui.settings.VoiceSettingsScreen
import kotlinx.coroutines.flow.map
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
    val currencyRegion by app.preferencesStore.currencyRegion.collectAsStateWithLifecycle(
        initialValue = CurrencyRegion.US,
    )
    SideEffect { Formatters.activeRegion = currencyRegion }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val onboardingSession: OnboardingSessionViewModel = viewModel()
    val storedProfile by remember(app) {
        app.preferencesStore.userProfile.map<UserProfile, UserProfile?> { profile -> profile }
    }.collectAsStateWithLifecycle(initialValue = null)
    val userProfile = storedProfile ?: UserProfile()
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

    val onboardingSaved = OnboardingPreferences.isCompleted(context)
    LaunchedEffect(storedProfile?.onboardingCompleted) {
        if (storedProfile?.onboardingCompleted == true) {
            OnboardingPreferences.markCompleted(context)
        }
    }
    if (storedProfile == null && !onboardingSaved) {
        Box(modifier.fillMaxSize().background(CreamBackground))
        return
    }
    val onboardingCompleted = onboardingSaved || storedProfile?.onboardingCompleted == true
    if (onboardingSession.active || !onboardingCompleted) {
        SideEffect { onboardingSession.active = true }
        OnboardingScreen(
            onComplete = { onboardingSession.active = false },
            modifier = modifier,
        )
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
                onOpenSyncSettings = { navController.navigate(AppRoutes.SYNC_SETTINGS) },
                onOpenGoalHub = { navController.navigate(AppRoutes.GOAL_HUB) },
                onOpenClients = { navController.navigate(AppRoutes.CLIENTS) },
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
        composable(AppRoutes.SYNC_SETTINGS) {
            SyncPairingScreen(onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.CLIENTS) {
            ClientListScreen(
                onBack = { navController.popBackStack() },
                onOpenClient = { id -> navController.navigate(AppRoutes.clientDetail(id)) },
                onCreateClient = { navController.navigate(AppRoutes.CLIENT_NEW) },
            )
        }
        composable(AppRoutes.CLIENT_NEW) {
            ClientEditorScreen(clientId = null, onBack = { navController.popBackStack() })
        }
        composable(AppRoutes.CLIENT_EDIT) { entry ->
            val clientId = entry.arguments?.getString("clientId")?.toLongOrNull()
            if (clientId == null) {
                navController.popBackStack()
            } else {
                ClientEditorScreen(clientId = clientId, onBack = { navController.popBackStack() })
            }
        }
        composable(AppRoutes.CLIENT_DETAIL) { entry ->
            val clientId = entry.arguments?.getString("clientId")?.toLongOrNull()
            if (clientId == null) {
                navController.popBackStack()
            } else {
                ClientDetailScreen(
                    clientId = clientId,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate(AppRoutes.clientEdit(id)) },
                )
            }
        }
    }
}
