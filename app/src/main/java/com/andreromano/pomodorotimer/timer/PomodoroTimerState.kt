package com.andreromano.pomodorotimer.timer

import com.andreromano.pomodorotimer.Seconds

data class PomodoroTimerState(
    val status: PomodoroTimerStatus,
    val timeRemainingInState: Seconds,
    val isPaused: Boolean
)