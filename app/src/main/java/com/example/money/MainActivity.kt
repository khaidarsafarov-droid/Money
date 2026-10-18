package com.example.money

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.money.data.locale.AppLanguageStorage
import com.example.money.navigation.AppIntents
import com.example.money.ui.home.HomeLaunchConfig
import com.example.money.ui.navigation.MoneyNavHost
import com.example.money.ui.theme.MoneyTheme
import com.example.money.ui.util.AppLocaleContext
import com.example.money.ui.util.NotificationPermissions
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val launchConfigState = mutableStateOf(HomeLaunchConfig())
    private val notificationsEnabledState = mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsEnabledState.value = granted || NotificationPermissions.areEnabled(this)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(
            AppLocaleContext.wrap(newBase, AppLanguageStorage.read(newBase)),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launchConfigState.value = AppIntents.parseLaunchConfig(intent)
        notificationsEnabledState.value = NotificationPermissions.areEnabled(this)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val localeTag = LocalConfiguration.current.locales[0].toLanguageTag()
            key(localeTag) {
                MoneyTheme {
                    MoneyNavHost(
                        launchConfig = launchConfigState.value,
                        notificationsEnabled = notificationsEnabledState.value,
                        onRequestNotificationPermission = ::requestNotificationPermission,
                        onLaunchConfigConsumed = {
                            launchConfigState.value = launchConfigState.value.withoutOneShotActions()
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchConfigState.value = AppIntents.parseLaunchConfig(intent)
    }

    override fun onResume() {
        super.onResume()
        notificationsEnabledState.value = NotificationPermissions.areEnabled(this)
        markAppOpenedToday()
    }

    private fun markAppOpenedToday() {
        val app = application as MoneyApplication
        val cal = Calendar.getInstance()
        val iso = String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
        lifecycleScope.launch {
            app.preferencesStore.markAppOpenedToday(iso)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
