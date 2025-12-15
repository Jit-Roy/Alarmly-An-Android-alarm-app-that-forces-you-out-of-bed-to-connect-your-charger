package com.example.alarmly.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alarmly.ui.viewmodel.AlarmDetailViewModel
import java.util.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmDetailScreen(
    alarmId: Int?,
    onNavigateBack: () -> Unit,
    viewModel: AlarmDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(alarmId) {
        alarmId?.let { viewModel.loadAlarm(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (alarmId == null) "New Alarm" else "Edit Alarm") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.saveAlarm(alarmId) {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Analog Clock
            AnalogClock(
                hour = state.hour,
                minute = state.minute,
                onTimeChange = { hour, minute ->
                    viewModel.updateTime(hour, minute)
                },
                modifier = Modifier
                    .size(280.dp)
                    .padding(32.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Digital time display with AM/PM toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format(Locale.US, "%02d:%02d",
                        if (state.hour == 0) 12
                        else if (state.hour > 12) state.hour - 12
                        else state.hour,
                        state.minute
                    ),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.width(16.dp))

                // AM/PM Toggle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = {
                            val newHour = if (state.hour >= 12) state.hour - 12 else state.hour
                            viewModel.updateTime(newHour, state.minute)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (state.hour < 12)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Gray
                        )
                    ) {
                        Text("AM", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            val newHour = if (state.hour < 12) state.hour + 12 else state.hour
                            viewModel.updateTime(newHour, state.minute)
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (state.hour >= 12)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Gray
                        )
                    ) {
                        Text("PM", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Repeat days selector
            Text(
                text = "Repeat",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            RepeatDaysSelector(
                selectedDays = state.repeatDays,
                onDayToggle = { day -> viewModel.toggleRepeatDay(day) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Alarm Sound
            SettingItem(
                title = "Alarm Sound",
                value = if (state.alarmSound == "default") "Big Phone" else "Custom",
                onClick = { /* TODO: Open sound picker */ }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Vibration
            SettingItem(
                title = "Vibration",
                value = if (state.vibration) "Basic call" else "Off",
                onClick = { viewModel.updateVibration(!state.vibration) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Snooze
            SettingItem(
                title = "Snooze",
                value = "${state.snoozeMinutes} Minutes",
                onClick = { /* TODO: Open snooze picker */ }
            )
        }
    }
}

@Composable
fun AnalogClock(
    hour: Int,
    minute: Int,
    onTimeChange: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Track which hand is being dragged
    var draggingHour by remember { mutableStateOf(false) }

    // Use rememberUpdatedState to always get the latest values in gesture handlers
    val currentHour by rememberUpdatedState(hour)
    val currentMinute by rememberUpdatedState(minute)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val touchDistance = kotlin.math.sqrt(dx * dx + dy * dy)
                            val minDim = kotlin.math.min(size.width, size.height)
                            val radius = minDim / 2f

                            // Calculate touch angle
                            val touchAngleRad = atan2(dy.toDouble(), dx.toDouble())
                            val touchAngleDeg = Math.toDegrees(touchAngleRad) + 90.0

                            // Calculate hand angles - use current values
                            val hourAngleDeg = (((currentHour % 24) % 12) * 30.0) - 90.0
                            val minuteAngleDeg = (currentMinute * 6.0) - 90.0

                            // Hand lengths
                            val hourHandLength = radius * 0.45f
                            val minuteHandLength = radius * 0.75f

                            // Calculate angular difference (shortest path on circle)
                            fun angleDiff(a1: Double, a2: Double): Double {
                                var diff = (a1 - a2) % 360.0
                                if (diff > 180.0) diff -= 360.0
                                if (diff < -180.0) diff += 360.0
                                return kotlin.math.abs(diff)
                            }

                            val hourDiff = angleDiff(touchAngleDeg, hourAngleDeg)
                            val minuteDiff = angleDiff(touchAngleDeg, minuteAngleDeg)

                            // Calculate perpendicular distance from touch point to each hand line
                            // This is more accurate than just angular difference
                            fun distanceToHandLine(handAngleDeg: Double, handLength: Float): Double {
                                val handAngleRad = Math.toRadians(handAngleDeg)
                                val handEndX = cos(handAngleRad) * handLength
                                val handEndY = sin(handAngleRad) * handLength

                                // Distance from touch point to the line segment (center to hand end)
                                // Project touch point onto hand line
                                val dotProduct = (dx * handEndX + dy * handEndY) / (handLength * handLength)
                                val projectionRatio = dotProduct.coerceIn(0.0, 1.0)

                                val projX = handEndX * projectionRatio
                                val projY = handEndY * projectionRatio

                                val distX = dx - projX
                                val distY = dy - projY

                                return kotlin.math.sqrt(distX * distX + distY * distY)
                            }

                            val distToHourHand = distanceToHandLine(hourAngleDeg, hourHandLength)
                            val distToMinuteHand = distanceToHandLine(minuteAngleDeg, minuteHandLength)

                            // Decide which hand to drag based on closest distance to hand line
                            // Add a small tolerance zone around each hand
                            val hourTolerance = radius * 0.15f  // 15% of radius
                            val minuteTolerance = radius * 0.15f

                            draggingHour = when {
                                // If close to hour hand line and within its length range
                                distToHourHand < hourTolerance && touchDistance <= hourHandLength * 1.3f -> {
                                    // Check if also close to minute hand - choose the closer one
                                    if (distToMinuteHand < minuteTolerance) {
                                        distToHourHand < distToMinuteHand
                                    } else {
                                        true
                                    }
                                }
                                // If close to minute hand line
                                distToMinuteHand < minuteTolerance && touchDistance <= minuteHandLength * 1.1f -> {
                                    false
                                }
                                // Fallback: use angular proximity if not close to either line
                                else -> {
                                    if (touchDistance < hourHandLength * 1.1f) {
                                        hourDiff < minuteDiff
                                    } else {
                                        minuteDiff > 30.0 && hourDiff < 20.0  // Prefer minute unless very close to hour
                                    }
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = (change.position.x - center.x).toDouble()
                            val dy = (change.position.y - center.y).toDouble()

                            // Angle: 0° at 12 o'clock, clockwise positive
                            val angleRad: Double = atan2(dy, dx)
                            var degrees: Double = Math.toDegrees(angleRad) + 90.0
                            if (degrees < 0.0) degrees += 360.0

                            if (draggingHour) {
                                // Dragging hour hand - only change hour, keep minute completely fixed
                                // Map 0..360 degrees to 0..11 hour positions
                                val hourIndex = (((degrees / 30.0).roundToInt() % 12) + 12) % 12

                                // Preserve AM/PM: if current hour >= 12, keep it in PM
                                val newHour = if (currentHour >= 12) {
                                    if (hourIndex == 0) 12 else hourIndex + 12
                                } else {
                                    if (hourIndex == 0) 0 else hourIndex
                                }

                                // Keep minute unchanged - use currentMinute to get latest value!
                                onTimeChange(newHour, currentMinute)
                            } else {
                                // Dragging minute hand - only change minute, keep hour completely fixed
                                val newMinute = (((degrees / 6.0).roundToInt() % 60) + 60) % 60

                                // Keep hour unchanged - use currentHour to get latest value!
                                onTimeChange(currentHour, newMinute)
                            }
                        }
                    )
                }
        ) {
            val minDim = kotlin.math.min(size.width, size.height)
            val radius = minDim / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            // Clock circle
            drawCircle(
                color = Color.White,
                radius = radius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )

            // Minute dots
            for (i in 0 until 60) {
                val angleDeg: Double = (i * 6 - 90).toDouble()
                val angle: Double = angleDeg * PI / 180.0
                val markerRadius = if (i % 5 == 0) radius * 0.9f else radius * 0.95f
                val dotRadius = if (i % 5 == 0) 5f else 2.5f

                drawCircle(
                    color = Color.White,
                    radius = dotRadius,
                    center = Offset(
                        center.x + (cos(angle) * markerRadius.toDouble()).toFloat(),
                        center.y + (sin(angle) * markerRadius.toDouble()).toFloat()
                    )
                )
            }

            // Hour hand angle: only based on hour (independent of minutes)
            val hourAngleDeg: Double = (((hour % 24) % 12) * 30.0) - 90.0
            val hourAngle: Double = hourAngleDeg * PI / 180.0
            val hourHandLength = radius * 0.45f
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(
                    center.x + (cos(hourAngle) * hourHandLength.toDouble()).toFloat(),
                    center.y + (sin(hourAngle) * hourHandLength.toDouble()).toFloat()
                ),
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Minute hand
            val minuteAngleDeg: Double = (minute * 6.0) - 90.0
            val minuteAngle: Double = minuteAngleDeg * PI / 180.0
            val minuteHandLength = radius * 0.75f
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(
                    center.x + (cos(minuteAngle) * minuteHandLength.toDouble()).toFloat(),
                    center.y + (sin(minuteAngle) * minuteHandLength.toDouble()).toFloat()
                ),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center knob
            drawCircle(
                color = Color.White,
                radius = 8.dp.toPx(),
                center = center
            )
        }
    }
}

@Composable
fun RepeatDaysSelector(
    selectedDays: Set<Int>,
    onDayToggle: (Int) -> Unit
) {
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        dayLabels.forEachIndexed { index, label ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (selectedDays.contains(index))
                            MaterialTheme.colorScheme.primary
                        else
                            Color.Gray.copy(alpha = 0.2f)
                    )
                    .clickable { onDayToggle(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selectedDays.contains(index))
                        Color.White
                    else
                        Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SettingItem(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
