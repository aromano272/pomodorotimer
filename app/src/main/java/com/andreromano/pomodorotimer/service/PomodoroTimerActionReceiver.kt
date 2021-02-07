package com.andreromano.pomodorotimer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PomodoroTimerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            TimerService.NOTIFICATION_ACTION_STOP -> {
                context.stopService(Intent(context, TimerService::class.java))
            }
        }
    }

}