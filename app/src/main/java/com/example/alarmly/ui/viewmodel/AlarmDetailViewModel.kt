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

data class AlarmDetailState(
    val hour: Int = 4,
    val minute: Int = 0,
    val repeatDays: Set<Int> = emptySet(),
    val alarmSound: String = "default",
    val vibration: Boolean = true,
    val snoozeMinutes: Int = 5,
    val label: String = ""
)

class AlarmDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AlarmRepository
    private val alarmScheduler: AlarmScheduler

    private val _state = MutableStateFlow(AlarmDetailState())
    val state: StateFlow<AlarmDetailState> = _state.asStateFlow()

    init {
        val alarmDao = AlarmDatabase.getDatabase(application).alarmDao()
        repository = AlarmRepository(alarmDao)
        alarmScheduler = AlarmScheduler(application)
    }

    fun loadAlarm(alarmId: Int) {
        viewModelScope.launch {
            val alarm = repository.getAlarmById(alarmId)
            alarm?.let {
                _state.value = AlarmDetailState(
                    hour = it.hour,
                    minute = it.minute,
                    repeatDays = it.repeatDays.toSet(),
                    alarmSound = it.alarmSound,
                    vibration = it.vibration,
                    snoozeMinutes = it.snoozeMinutes,
                    label = it.label
                )
            }
        }
    }

    fun updateTime(hour: Int, minute: Int) {
        _state.value = _state.value.copy(hour = hour, minute = minute)
    }

    fun toggleRepeatDay(day: Int) {
        val currentDays = _state.value.repeatDays.toMutableSet()
        if (currentDays.contains(day)) {
            currentDays.remove(day)
        } else {
            currentDays.add(day)
        }
        _state.value = _state.value.copy(repeatDays = currentDays)
    }

    fun updateAlarmSound(sound: String) {
        _state.value = _state.value.copy(alarmSound = sound)
    }

    fun updateVibration(enabled: Boolean) {
        _state.value = _state.value.copy(vibration = enabled)
    }

    fun updateSnooze(minutes: Int) {
        _state.value = _state.value.copy(snoozeMinutes = minutes)
    }

    fun updateLabel(label: String) {
        _state.value = _state.value.copy(label = label)
    }

    fun saveAlarm(alarmId: Int? = null, onSaved: () -> Unit) {
        viewModelScope.launch {
            val alarm = AlarmEntity(
                id = alarmId ?: 0,
                hour = _state.value.hour,
                minute = _state.value.minute,
                repeatDays = _state.value.repeatDays.toList(),
                isEnabled = true,
                alarmSound = _state.value.alarmSound,
                vibration = _state.value.vibration,
                snoozeMinutes = _state.value.snoozeMinutes,
                label = _state.value.label
            )

            val savedId = if (alarmId == null) {
                repository.insertAlarm(alarm)
            } else {
                repository.updateAlarm(alarm)
                alarmId.toLong()
            }

            // Schedule the alarm
            val savedAlarm = alarm.copy(id = savedId.toInt())
            alarmScheduler.scheduleAlarm(savedAlarm)

            onSaved()
        }
    }
}

