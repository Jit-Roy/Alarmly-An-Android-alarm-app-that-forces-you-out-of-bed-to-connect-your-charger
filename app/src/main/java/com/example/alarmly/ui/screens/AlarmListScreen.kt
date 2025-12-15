package com.example.alarmly.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alarmly.R
import com.example.alarmly.data.local.AlarmEntity
import com.example.alarmly.ui.viewmodel.AlarmListViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlarmListScreen(
    onNavigateToDetail: (Int?) -> Unit,
    viewModel: AlarmListViewModel = viewModel()
) {
    val alarms by viewModel.alarms.collectAsState()
    var nextAlarmTime by remember { mutableStateOf(viewModel.getNextAlarmCountdown()) }

    // Update countdown every second
    LaunchedEffect(alarms) {
        while (true) {
            nextAlarmTime = viewModel.getNextAlarmCountdown()
            kotlinx.coroutines.delay(1000) // Update every second
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Always show content in a LazyColumn (banner + list or empty state)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 32.dp,  // Top padding from screen edge
                bottom = 16.dp
            )
        ) {
            // Header banner - always visible
            item {
                AlarmHeaderBanner(nextAlarmTime = nextAlarmTime)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // "Your Schedules" title with + button - ALWAYS VISIBLE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Schedules",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )

                    IconButton(
                        onClick = { onNavigateToDetail(null) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Alarm",
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (alarms.isEmpty()) {
                // Empty state message
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No alarms set\nTap + to create one",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 18.sp,
                                lineHeight = 28.sp
                            ),
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Alarm items
                items(alarms) { alarm ->
                    AlarmItem(
                        alarm = alarm,
                        timeUntil = viewModel.getTimeUntilAlarm(alarm),
                        onToggle = { viewModel.toggleAlarm(alarm) },
                        onEdit = { onNavigateToDetail(alarm.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun AlarmHeaderBanner(nextAlarmTime: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A))  // Fallback color
        ) {
            // Your Figma background design as PNG
            // Export from Figma as PNG and save to: res/drawable/alarm_background.png
            Image(
                painter = painterResource(id = R.drawable.alarm_background),
                contentDescription = "Alarm background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.9f
            )

            // Content Layer (Text + Illustration)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Alarm In",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = nextAlarmTime,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Character illustration is already part of the alarm_background image
                // No separate overlay needed
            }
        }
    }
}

@Composable
fun AlarmItem(
    alarm: AlarmEntity,
    timeUntil: String,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Time and countdown
            Column(modifier = Modifier.weight(1f)) {
                // Time display with am/pm
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val displayHour = when {
                        alarm.hour == 0 -> 12  // Midnight
                        alarm.hour > 12 -> alarm.hour - 12  // PM hours
                        else -> alarm.hour  // AM hours
                    }
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", displayHour, alarm.minute),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (alarm.hour >= 12) "pm" else "am",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Time until alarm
                if (alarm.isEnabled) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeUntil,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            // Middle: Date
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Text(
                    text = getDateString(alarm),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            // Right side: Toggle switch
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF4CAF50),
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.Gray
                )
            )
        }
    }
}

private fun getDateString(alarm: AlarmEntity): String {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)

    // If alarm time has already passed today, show tomorrow's date
    if (alarm.hour < currentHour || (alarm.hour == currentHour && alarm.minute <= currentMinute)) {
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }
    // Otherwise show today's date

    val dateFormat = SimpleDateFormat("EEE, dd MMM", Locale.ENGLISH)
    return dateFormat.format(calendar.time)
}


