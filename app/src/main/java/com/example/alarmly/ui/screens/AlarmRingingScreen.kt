package com.example.alarmly.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a1a),  // Dark gray top
                        Color(0xFF000000)   // Black bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Good Night text
            Text(
                text = "Good Night",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Current time - Large display
            Text(
                text = currentTime,
                color = Color.White,
                fontSize = 84.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 60.dp)
            )

            // Moon and clouds illustration - MATCHING YOUR DESIGN
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .padding(horizontal = 32.dp)
            ) {
                val width = size.width
                val height = size.height
                val centerX = width / 2f

                // Large crescent moon in center (like your design)
                val moonRadius = width * 0.28f
                val moonCenterY = height * 0.35f

                // Full moon circle
                drawCircle(
                    color = Color(0xFFE8E8E8),  // Light gray moon
                    radius = moonRadius,
                    center = Offset(centerX, moonCenterY)
                )

                // Shadow to create crescent shape
                drawCircle(
                    color = Color(0xFF1a1a1a),  // Dark shadow
                    radius = moonRadius * 0.85f,
                    center = Offset(centerX + moonRadius * 0.4f, moonCenterY - moonRadius * 0.15f)
                )

                // Bottom cloud layers (stylized like your design)
                // Left bottom cloud
                val leftCloudPath = Path().apply {
                    moveTo(0f, height * 0.75f)

                    // First bump
                    cubicTo(
                        width * 0.05f, height * 0.65f,
                        width * 0.1f, height * 0.6f,
                        width * 0.18f, height * 0.6f
                    )

                    // Second bump
                    cubicTo(
                        width * 0.22f, height * 0.6f,
                        width * 0.25f, height * 0.55f,
                        width * 0.3f, height * 0.55f
                    )

                    // Third bump
                    cubicTo(
                        width * 0.35f, height * 0.55f,
                        width * 0.38f, height * 0.58f,
                        width * 0.42f, height * 0.62f
                    )

                    lineTo(width * 0.42f, height)
                    lineTo(0f, height)
                    close()
                }

                drawPath(
                    path = leftCloudPath,
                    color = Color(0xFFCCCCCC)  // Light gray cloud
                )

                // Right bottom cloud
                val rightCloudPath = Path().apply {
                    moveTo(width * 0.58f, height * 0.7f)

                    // First bump
                    cubicTo(
                        width * 0.62f, height * 0.65f,
                        width * 0.66f, height * 0.62f,
                        width * 0.72f, height * 0.62f
                    )

                    // Second bump
                    cubicTo(
                        width * 0.76f, height * 0.62f,
                        width * 0.8f, height * 0.58f,
                        width * 0.85f, height * 0.58f
                    )

                    // Third bump
                    cubicTo(
                        width * 0.9f, height * 0.58f,
                        width * 0.95f, height * 0.65f,
                        width, height * 0.72f
                    )

                    lineTo(width, height)
                    lineTo(width * 0.58f, height)
                    close()
                }

                drawPath(
                    path = rightCloudPath,
                    color = Color(0xFFB8B8B8)  // Medium gray cloud
                )

                // Front center cloud (overlapping, darker)
                val centerCloudPath = Path().apply {
                    moveTo(width * 0.3f, height * 0.8f)

                    // Large center bumps
                    cubicTo(
                        width * 0.35f, height * 0.7f,
                        width * 0.42f, height * 0.65f,
                        width * 0.5f, height * 0.65f
                    )

                    cubicTo(
                        width * 0.58f, height * 0.65f,
                        width * 0.65f, height * 0.7f,
                        width * 0.7f, height * 0.8f
                    )

                    lineTo(width * 0.7f, height)
                    lineTo(width * 0.3f, height)
                    close()
                }

                drawPath(
                    path = centerCloudPath,
                    color = Color(0xFFA0A0A0)  // Darker gray front cloud
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Instruction text at bottom
            Text(
                text = "Connect charger to dismiss",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(bottom = 48.dp)
            )
        }
    }
}

