package com.example.alarmly.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.alarmly.MainActivity
import com.example.alarmly.R
import com.example.alarmly.data.local.AlarmDatabase
import com.example.alarmly.data.repository.AlarmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var alarmId: Int = -1
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val chargerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_POWER_CONNECTED) {
                // Charger connected - dismiss alarm
                stopAlarm()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Register charger receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(chargerReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(chargerReceiver, filter)
        }

        // Acquire wake lock - no timeout, will be released when charger is connected
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "Alarmly::AlarmWakeLock"
        )
        wakeLock?.acquire() // Acquire indefinitely until charger is connected
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1

        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Immediately launch the alarm ringing screen
        val alarmIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            action = Intent.ACTION_VIEW
            putExtra("SHOW_ALARM_SCREEN", true)
            putExtra("ALARM_ID", alarmId)
        }
        startActivity(alarmIntent)

        serviceScope.launch {
            val repository = AlarmRepository(AlarmDatabase.getDatabase(applicationContext).alarmDao())
            val alarm = repository.getAlarmById(alarmId)

            alarm?.let {
                playAlarm(it.alarmSound, it.vibration)

                // Only reschedule repeating alarms, don't disable one-time alarms yet
                if (it.repeatDays.isNotEmpty()) {
                    val scheduler = AlarmScheduler(applicationContext)
                    scheduler.scheduleAlarm(it)
                }
            }
        }

        // Return START_STICKY to ensure service is restarted if killed by system
        return START_STICKY
    }

    private fun playAlarm(soundUri: String, shouldVibrate: Boolean) {
        try {
            val alarmUri = if (soundUri == "default") {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            } else {
                soundUri.toUri()
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                isLooping = true
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                prepare()
                start()
            }

            if (shouldVibrate) {
                vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                val pattern = longArrayOf(0, 1000, 1000) // 0ms delay, vibrate 1s, pause 1s
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createWaveform(pattern, 0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        // Disable one-time alarms when dismissed
        serviceScope.launch {
            val repository = AlarmRepository(AlarmDatabase.getDatabase(applicationContext).alarmDao())
            val alarm = repository.getAlarmById(alarmId)

            alarm?.let {
                // Only disable if it's a one-time alarm (no repeat days)
                if (it.repeatDays.isEmpty()) {
                    repository.updateAlarm(it.copy(isEnabled = false))
                }
            }
        }

        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null

        wakeLock?.release()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = Intent.ACTION_VIEW
            putExtra("SHOW_ALARM_SCREEN", true)
            putExtra("ALARM_ID", alarmId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            alarmId,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Full screen intent for immediate display
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            alarmId + 1000,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Alarm Ringing")
            .setContentText("Connect charger to dismiss")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(fullScreenIntent, true) // Show full screen when alarm rings
            .setOngoing(true) // Makes notification persistent
            .setAutoCancel(false) // Prevents dismissing by swiping
            .setPriority(NotificationCompat.PRIORITY_MAX) // Highest priority
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Show on lock screen
            .setOnlyAlertOnce(false) // Keep alerting
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // Show immediately
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for active alarms"
                setSound(null, null)
                enableLights(true)
                enableVibration(false) // Vibration handled separately
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true) // Bypass Do Not Disturb
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(chargerReceiver)
        stopAlarm()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID = 1
    }
}

