package com.example.alarmly.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alarmly.data.local.AlarmDatabase
import com.example.alarmly.data.repository.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all enabled alarms
            CoroutineScope(Dispatchers.IO).launch {
                val repository = AlarmRepository(
                    AlarmDatabase.getDatabase(context).alarmDao()
                )
                val enabledAlarms = repository.getEnabledAlarms()
                val scheduler = AlarmScheduler(context)

                enabledAlarms.forEach { alarm ->
                    scheduler.scheduleAlarm(alarm)
                }
            }
        }
    }
}

