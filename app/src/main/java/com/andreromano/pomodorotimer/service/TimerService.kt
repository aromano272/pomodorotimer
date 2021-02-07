package com.andreromano.pomodorotimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.andreromano.pomodorotimer.MainActivity
import com.andreromano.pomodorotimer.R
import com.andreromano.pomodorotimer.Seconds
import com.andreromano.pomodorotimer.timer.PomodoroTimer
import com.andreromano.pomodorotimer.timer.PomodoroTimerStatus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class TimerService : LifecycleService() {

    companion object {
        val EXTRA_WORK_TIME_SECONDS = "EXTRA_WORK_TIME_SECONDS"
        val EXTRA_REST_TIME_SECONDS = "EXTRA_REST_TIME_SECONDS"

        val NOTIFICATION_ACTION_STOP = "NOTIFICATION_ACTION_STOP"

        private val NOTIFICATION_ID = 1
        private val NOTIFICATION_CHANNEL_ID = "Pomodoro Timer"
        private val NOTIFICATION_CHANNEL_NAME = "Pomodoro Timer"
        private val NOTIFICATION_CHANNEL_IMPORTANCE = NotificationManagerCompat.IMPORTANCE_HIGH
    }

    private lateinit var pomodoroTimer: PomodoroTimer

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val workTime: Seconds? = intent?.getIntExtra(EXTRA_WORK_TIME_SECONDS, 0)
        val restTime: Seconds? = intent?.getIntExtra(EXTRA_REST_TIME_SECONDS, 0)
        if (
            workTime == null ||
            restTime == null ||
            workTime == 0 ||
            restTime == 0
        ) return super.onStartCommand(intent, flags, startId)

        startForegroundService()

        pomodoroTimer = PomodoroTimer(workTime, restTime)

        lifecycleScope.launchWhenStarted {
            pomodoroTimer.asFlow()
                .collect {
                    val notification = when (it.status) {
                        PomodoroTimerStatus.WORKING -> buildWorkNotification(it.timeRemainingInState)
                        PomodoroTimerStatus.RESTING -> buildRestNotification(it.timeRemainingInState)
                    }

                    showNotification(notification)
                }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService() {
        createNotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NOTIFICATION_CHANNEL_IMPORTANCE)
        startForeground(NOTIFICATION_ID, createNotificationBuilder().build())
    }

    private fun buildWorkNotification(time: Seconds): Notification {
        val actionIntent = Intent(applicationContext, PomodoroTimerActionReceiver::class.java).apply {
            action = NOTIFICATION_ACTION_STOP
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(applicationContext, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val timeFormatter = DateTimeFormat.forPattern("mm:ss")
        val timeStr = DateTime().withMillis(time * 1000L)

        return createNotificationBuilderWithDefaults(pendingIntent)
            .setContentTitle("WORK NOW!!!")
            .setContentText(timeFormatter.print(timeStr))
            .addAction(0, "STOP", pendingIntent)
            .build()
    }

    private fun buildRestNotification(time: Seconds): Notification {
        val actionIntent = Intent(applicationContext, PomodoroTimerActionReceiver::class.java).apply {
            action = NOTIFICATION_ACTION_STOP
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent: PendingIntent = PendingIntent.getBroadcast(applicationContext, 0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val timeFormatter = DateTimeFormat.forPattern("mm:ss")
        val timeStr = DateTime().withMillis(time * 1000L)

        return createNotificationBuilderWithDefaults(pendingIntent)
            .setContentTitle("TIME TO REST zzz")
            .setContentText(timeFormatter.print(timeStr))
            .addAction(0, "STOP", pendingIntent)
            .build()
    }

    // TODO: Clean up
    private fun createNotificationBuilder(): NotificationCompat.Builder {
        val pendingIntent: PendingIntent = createPendingIntentWithDefaults(NOTIFICATION_ID)

        val notification = createNotificationBuilderWithDefaults(pendingIntent)
            .setContentTitle("title")
            .setContentText("content")

        return notification
    }

    private fun createPendingIntentWithDefaults(notificationId: Int, extras: Bundle? = null): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (extras != null) putExtras(extras)
        }

        return PendingIntent.getActivity(applicationContext, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createNotificationBuilderWithDefaults(contentIntent: PendingIntent) =
        NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(ContextCompat.getColor(applicationContext, R.color.purple_200))

    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(this.applicationContext)
    }

    private fun showNotification(notification: Notification) {
        createNotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NOTIFICATION_CHANNEL_IMPORTANCE)

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(channelId: String, channelName: String, importance: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                importance
            )
            notificationManager.createNotificationChannel(channel)
        }
    }


    override fun onDestroy() {
        if (::pomodoroTimer.isInitialized) pomodoroTimer.stop()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        if (::pomodoroTimer.isInitialized) pomodoroTimer.stop()
        stopForeground(true)
        stopSelf()
    }
}