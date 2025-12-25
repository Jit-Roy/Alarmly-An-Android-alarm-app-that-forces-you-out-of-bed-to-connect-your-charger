package com.example.alarmly.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarmly.R
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlarmRingingScreen(
    @Suppress("UNUSED_PARAMETER") alarmId: Int,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isCharging by remember { mutableStateOf(false) }

    // Update time every second
    var currentTime by remember { mutableStateOf(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())) }

    // Determine if it's day or night (6 AM to 6 PM is day, otherwise night)
    val greetingText by remember {
        derivedStateOf {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            if (hour in 6..17) "Good Day" else "Good Night"
        }
    }

    // Monitor charging state
    LaunchedEffect(Unit) {
        val intentFilter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)

        while (true) {
            val batteryStatus = context.registerReceiver(null, intentFilter)
            val status = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isChargingNow = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
                               status == android.os.BatteryManager.BATTERY_STATUS_FULL

            if (isChargingNow && !isCharging) {
                // Charger just connected - stop the alarm service and dismiss screen
                val serviceIntent = android.content.Intent(context, com.example.alarmly.alarm.AlarmService::class.java)
                context.stopService(serviceIntent)
                onDismiss()
                break
            }

            isCharging = isChargingNow
            kotlinx.coroutines.delay(500) // Check every 500ms
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background image - alarm_ringing.png
        Image(
            painter = painterResource(id = R.drawable.alarm_ringing),
            contentDescription = "Alarm Ringing Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Content overlay - main content centered
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Dynamic greeting text
                Text(
                    text = greetingText,
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Current time - Large display
                Text(
                    text = currentTime,
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // Instruction text at bottom
            Text(
                text = "Put phone on charge to\nstop alarm",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            )
        }
    }
}

