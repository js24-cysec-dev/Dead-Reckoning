package com.example.dead_reckoning.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.dead_reckoning.DeadReckoningApp
import com.example.dead_reckoning.MainActivity
import com.example.dead_reckoning.R
import com.example.dead_reckoning.data.repository.TelemetryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class TelemetryService : Service() {

    private lateinit var repository: TelemetryRepository
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var observerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as DeadReckoningApp
        repository = app.repository
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundServiceInternal()
            }
            ACTION_STOP -> {
                stopForegroundServiceInternal()
            }
        }
        return START_STICKY
    }

    private fun startForegroundServiceInternal() {
        val notification = createNotification("Sensor Recording Active", "0 samples recorded")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        repository.startCollection()
        repository.startRecording()

        observerJob?.cancel()
        observerJob = serviceScope.launch {
            repository.recordedSamplesCount.collect { count ->
                val updatedNotification = createNotification(
                    "Sensor Recording Active",
                    "$count samples logged to CSV"
                )
                val notificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, updatedNotification)
            }
        }
    }

    private fun stopForegroundServiceInternal() {
        observerJob?.cancel()
        observerJob = null
        serviceScope.launch {
            repository.stopRecording()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Telemetry Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String, contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "TelemetryServiceChannel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        fun startService(context: Context) {
            val intent = Intent(context, TelemetryService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, TelemetryService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
