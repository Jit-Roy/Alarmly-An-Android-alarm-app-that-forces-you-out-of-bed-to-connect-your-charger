package com.example.alarmly.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarmly.alarm.AlarmScheduler
import com.example.alarmly.data.local.AlarmDatabase
import com.example.alarmly.data.local.AlarmEntity
import com.example.alarmly.data.repository.AlarmRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository
    private val alarmScheduler: AlarmScheduler

    val alarms: StateFlow<List<AlarmEntity>>

    init {
        val alarmDao = AlarmDatabase.getDatabase(application).alarmDao()
        repository = AlarmRepository(alarmDao)
        alarmScheduler = AlarmScheduler(application)

        val _alarms = MutableStateFlow<List<AlarmEntity>>(emptyList())
        alarms = _alarms.asStateFlow()

        viewModelScope.launch {
            repository.getAllAlarms().collect { alarmList ->
                _alarms.value = alarmList
            }
        }
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val updatedAlarm = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.updateAlarm(updatedAlarm)

            if (updatedAlarm.isEnabled) {
                alarmScheduler.scheduleAlarm(updatedAlarm)
            } else {
                alarmScheduler.cancelAlarm(updatedAlarm.id)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            alarmScheduler.cancelAlarm(alarm.id)
            repository.deleteAlarm(alarm)
        }
    }

    fun getTimeUntilAlarm(alarm: AlarmEntity): String {
        if (!alarm.isEnabled) return "Disabled"

        val nextAlarmTime = alarmScheduler.getNextAlarmTime(alarm)
        val currentTime = System.currentTimeMillis()
        val diffMillis = nextAlarmTime - currentTime

        val hours = (diffMillis / (1000 * 60 * 60)) % 24
        val minutes = (diffMillis / (1000 * 60)) % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m from now"
            minutes > 0 -> "${minutes}m from now"
            else -> "Less than a minute"
        }
    }

    fun getNextAlarmCountdown(): String {
        val alarmsList = alarms.value
        if (alarmsList.isEmpty()) return "No alarms set"

        val enabledAlarms = alarmsList.filter { it.isEnabled }
        if (enabledAlarms.isEmpty()) return "No alarms set"

        return try {
            // Find the nearest alarm
            val nextAlarm = enabledAlarms.minByOrNull { alarm ->
                alarmScheduler.getNextAlarmTime(alarm)
            } ?: return "No alarms set"

            val nextAlarmTime = alarmScheduler.getNextAlarmTime(nextAlarm)
            val currentTime = System.currentTimeMillis()
            val diffMillis = nextAlarmTime - currentTime

            if (diffMillis < 0) return "No alarms set"

            val hours = (diffMillis / (1000 * 60 * 60))
            val minutes = (diffMillis / (1000 * 60)) % 60
            val seconds = (diffMillis / 1000) % 60

            when {
                hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
                minutes > 0 -> "${minutes}m ${seconds}s"
                seconds > 0 -> "${seconds}s"
                else -> "No alarms set"
            }
        } catch (e: Exception) {
            "No alarms set"
        }
    }
}

