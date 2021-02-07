package com.andreromano.pomodorotimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.andreromano.pomodorotimer.MainActivity
import com.andreromano.pomodorotimer.Millis
import com.andreromano.pomodorotimer.R
import com.andreromano.pomodorotimer.Seconds
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import org.joda.time.DateTime

class TimerService : Service() {

    companion object {
        val EXTRA_WORK_TIME_SECONDS = "EXTRA_WORK_TIME_SECONDS"
        val EXTRA_REST_TIME_SECONDS = "EXTRA_REST_TIME_SECONDS"

        private val NOTIFICATION_ID = 1
        private val NOTIFICATION_CHANNEL_ID = "Pomodoro Timer"
        private val NOTIFICATION_CHANNEL_NAME = "Pomodoro Timer"
        private val NOTIFICATION_CHANNEL_IMPORTANCE = NotificationManagerCompat.IMPORTANCE_HIGH
    }

    override fun onBind(p0: Intent?): IBinder? = null

    lateinit var tickerChannel: ReceiveChannel<Unit>

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (::tickerChannel.isInitialized) tickerChannel.cancel()
        val workTime: Seconds? = intent?.getIntExtra(EXTRA_WORK_TIME_SECONDS, 0)
        val restTime: Seconds? = intent?.getIntExtra(EXTRA_REST_TIME_SECONDS, 0)
        if (
            workTime == null ||
            restTime == null ||
            workTime == 0 ||
            restTime == 0
        ) return super.onStartCommand(intent, flags, startId)

        val totalTime = workTime + restTime

        // TODO: This might prove unrealiable if ticks are skipped somehow, we might need to save a datetime instead
        var tickCount = 0

        tickerChannel = ticker(1000L)

        startForegroundService()

        GlobalScope.launch {
            for (tick in tickerChannel) {
                tickCount++

                val moduloTickCount = tickCount % totalTime
                val isWorking = moduloTickCount / workTime == 0
                val currentTicks =
                    if (isWorking) workTime - moduloTickCount
                    else workTime - moduloTickCount + restTime

                val title = if (isWorking) "WORK NOW!!!" else "TIME TO REST zzz"

                val notification = createNotificationBuilder()
                    .setContentTitle(title)
                    .setContentText("$currentTicks")
                    .build()
                showNotification(notification)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService() {
        createNotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NOTIFICATION_CHANNEL_IMPORTANCE)
        startForeground(NOTIFICATION_ID, createNotificationBuilder().build())
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
        if (::tickerChannel.isInitialized) tickerChannel.cancel()
        super.onDestroy()
    }
}