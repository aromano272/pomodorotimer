package com.andreromano.pomodorotimer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.andreromano.pomodorotimer.timer.PomodoroTimer

class PomodoroTimerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TimerService.NOTIFICATION_ACTION_STOP -> context.stopService(Intent(context, TimerService::class.java))
            TimerService.NOTIFICATION_ACTION_PAUSE -> PomodoroTimer.pause()
            TimerService.NOTIFICATION_ACTION_RESUME -> PomodoroTimer.resume()
        }
    }

}