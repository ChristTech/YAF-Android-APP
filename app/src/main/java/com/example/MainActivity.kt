package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.core.notification.FellowshipBackgroundSyncWorker
import com.example.ui.navigation.MainAppScaffold
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val openNotificationsOnLaunch = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.getBooleanExtra("open_notifications", false) == true) {
            openNotificationsOnLaunch.value = true
            intent?.removeExtra("open_notifications")
        }
        setContent {
            val isDark = com.example.ui.theme.ThemeManager.isDarkMode.value
            MyApplicationTheme(darkTheme = isDark) {
                MainAppScaffold(
                    initialOpenNotifications = openNotificationsOnLaunch.value,
                    onNotificationsIntentConsumed = { openNotificationsOnLaunch.value = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("open_notifications", false)) {
            openNotificationsOnLaunch.value = true
            intent.removeExtra("open_notifications")
        }
    }

    override fun onStop() {
        super.onStop()
        FellowshipBackgroundSyncWorker.triggerImmediateSync(applicationContext)
    }
}
