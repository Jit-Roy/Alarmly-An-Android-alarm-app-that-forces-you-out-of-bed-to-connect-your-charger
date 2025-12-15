package com.example.alarmly.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val repeatDays: List<Int>, // 0 = Sunday, 1 = Monday, etc.
    val isEnabled: Boolean = true,
    val alarmSound: String = "default", // URI or "default"
    val vibration: Boolean = true,
    val snoozeMinutes: Int = 5,
    val label: String = ""
)

