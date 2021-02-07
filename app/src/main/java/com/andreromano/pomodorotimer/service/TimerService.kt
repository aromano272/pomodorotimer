package com.andreromano.pomodorotimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.andreromano.pomodorotimer.MainActivity
import com.andreromano.pomodorotimer.R
import com.andreromano.pomodorotimer.Seconds
import com.andreromano.pomodorotimer.timer.PomodoroTimer
import com.andreromano.pomodorotimer.timer.PomodoroTimerState
import com.andreromano.pomodorotimer.timer.PomodoroTimerStatus
import kotlinx.coroutines.flow.collect
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class TimerService : LifecycleService() {

    companion object {
        val EXTRA_WORK_TIME_SECONDS = "EXTRA_WORK_TIME_SECONDS"
        val EXTRA_REST_TIME_SECONDS = "EXTRA_REST_TIME_SECONDS"

        val NOTIFICATION_ACTION_STOP = "NOTIFICATION_ACTION_STOP"
        val NOTIFICATION_ACTION_PAUSE = "NOTIFICATION_ACTION_PAUSE"
        val NOTIFICATION_ACTION_RESUME = "NOTIFICATION_ACTION_RESUME"

        private val NOTIFICATION_ID = 1
        private val NOTIFICATION_CHANNEL_ID = "Pomodoro Timer"
        private val NOTIFICATION_CHANNEL_NAME = "Pomodoro Timer"
        private val NOTIFICATION_CHANNEL_IMPORTANCE = NotificationManagerCompat.IMPORTANCE_HIGH
    }

    private var isForegroundServiceStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val workTime: Seconds? = intent?.getIntExtra(EXTRA_WORK_TIME_SECONDS, 0)
        val restTime: Seconds? = intent?.getIntExtra(EXTRA_REST_TIME_SECONDS, 0)
        if (
            workTime == null ||
            restTime == null ||
            workTime == 0 ||
            restTime == 0
        ) return super.onStartCommand(intent, flags, startId)

        PomodoroTimer.stop()

        lifecycleScope.launchWhenStarted {
            PomodoroTimer.asFlow()
                .collect {
                    val notification = buildNotification(it)

                    if (isForegroundServiceStarted) showNotification(notification).also { isForegroundServiceStarted = true }
                    else startForegroundService(notification)
                }
        }

        PomodoroTimer.start(workTime, restTime)

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService(notification: Notification) {
        createNotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NOTIFICATION_CHANNEL_IMPORTANCE)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(state: PomodoroTimerState): Notification {
        val pauseActionIntent = Intent(applicationContext, PomodoroTimerActionReceiver::class.java).apply {
            action = if (state.isPaused) NOTIFICATION_ACTION_RESUME else NOTIFICATION_ACTION_PAUSE
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pausePendingIntent: PendingIntent = PendingIntent.getBroadcast(applicationContext, 0, pauseActionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val stopActionIntent = Intent(applicationContext, PomodoroTimerActionReceiver::class.java).apply {
            action = NOTIFICATION_ACTION_STOP
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val stopPendingIntent: PendingIntent = PendingIntent.getBroadcast(applicationContext, 0, stopActionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val timeFormatter = DateTimeFormat.forPattern("mm:ss")
        val timeDateTime = DateTime().withMillis(state.timeRemainingInState * 1000L)
        val timeStr = timeFormatter.print(timeDateTime)

        val titleStr = when (state.status) {
            PomodoroTimerStatus.WORKING -> "WORK NOW!!!"
            PomodoroTimerStatus.RESTING -> "TIME TO REST zzz"
        }
        val contentTextStr =
            if (state.isPaused) "$timeStr - Paused"
            else timeStr
        val pauseButtonStr = if (state.isPaused) "RESUME" else "PAUSE"

        return createNotificationBuilderWithDefaults(stopPendingIntent)
            .setContentTitle(titleStr)
            .setContentText(contentTextStr)
            .addAction(0, pauseButtonStr, pausePendingIntent)
            .addAction(0, "STOP", stopPendingIntent)
            .build()
    }

    private fun buildWorkNotification(time: Seconds, isPaused: Boolean): Notification {
        val pauseActionIntent = Intent(applicationContext, PomodoroTimerActionReceiver::class.java).apply {
            action = if (isPaused) NOTIFICATION_ACTION_RESUME else NOTIFICATION_ACTION_PAUSE
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pausePendingIntent: PendingIntent = PendingIntent.getBroadcast(applicationContext, 0, pauseActionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val stopActionIntent = Intent(applicationContext, PomodoroTimerActionReceiver::class.java).apply {
            action = NOTIFICATION_ACTION_STOP
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val stopPendingIntent: PendingIntent = PendingIntent.getBroadcast(applicationContext, 0, stopActionIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val timeFormatter = DateTimeFormat.forPattern("mm:ss")
        val timeStr = DateTime().withMillis(time * 1000L)

        val pauseStr = if (isPaused) "RESUME" else "PAUSE"

        return createNotificationBuilderWithDefaults(stopPendingIntent)
            .setContentTitle("WORK NOW!!!")
            .setContentText(timeFormatter.print(timeStr))
            .addAction(0, pauseStr, pausePendingIntent)
            .addAction(0, "STOP", stopPendingIntent)
            .build()
    }

    private fun buildRestNotification(time: Seconds, isPaused: Boolean): Notification {
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
        PomodoroTimer.stop()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        PomodoroTimer.stop()
        stopForeground(true)
        stopSelf()
    }
}