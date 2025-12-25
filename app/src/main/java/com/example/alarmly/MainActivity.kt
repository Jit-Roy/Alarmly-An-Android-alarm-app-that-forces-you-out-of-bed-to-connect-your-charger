package com.example.alarmly

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.alarmly.ui.navigation.AlarmlyNavHost
import com.example.alarmly.ui.navigation.Screen
import com.example.alarmly.ui.theme.AlarmlyTheme

class MainActivity : ComponentActivity() {

    private var navigationTrigger by mutableStateOf(0)
    private var pendingAlarmId by mutableStateOf(-1)

    // Notification permission launcher
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission granted or denied
        if (!isGranted) {
            // Optionally show a message to the user that notifications are needed
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val showAlarmScreen = intent.getBooleanExtra("SHOW_ALARM_SCREEN", false)
        val alarmId = intent.getIntExtra("ALARM_ID", -1)

        // Show over lock screen and turn on screen for alarm
        if (showAlarmScreen && alarmId != -1) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }

            pendingAlarmId = alarmId
            navigationTrigger++
        }

        setContent {
            AlarmlyTheme(darkTheme = true) {
                val navController = rememberNavController()

                LaunchedEffect(navigationTrigger) {
                    if (pendingAlarmId != -1) {
                        navController.navigate(Screen.AlarmRinging.createRoute(pendingAlarmId)) {
                            popUpTo(Screen.AlarmList.route) { inclusive = false }
                        }
                        // Don't reset pendingAlarmId here to keep the screen visible
                    }
                }

                AlarmlyNavHost(
                    navController = navController,
                    startDestination = if (showAlarmScreen && alarmId != -1) {
                        Screen.AlarmRinging.createRoute(alarmId)
                    } else {
                        Screen.AlarmList.route
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val showAlarmScreen = intent.getBooleanExtra("SHOW_ALARM_SCREEN", false)
        val alarmId = intent.getIntExtra("ALARM_ID", -1)

        if (showAlarmScreen && alarmId != -1) {
            // Show over lock screen and turn on screen for alarm
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }

            pendingAlarmId = alarmId
            navigationTrigger++
        }
    }
}

