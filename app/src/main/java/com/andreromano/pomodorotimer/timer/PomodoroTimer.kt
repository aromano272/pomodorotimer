package com.andreromano.pomodorotimer.timer

import com.andreromano.pomodorotimer.Seconds
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

object PomodoroTimer {

    private var workTime: Seconds by Delegates.notNull()
    private var restTime: Seconds by Delegates.notNull()

    private lateinit var tickerChannel: ReceiveChannel<Unit>
    private var currentTickCount: Int = 0

    private val moduloTickCount: Int
        get() = currentTickCount % (workTime + restTime)

    private val currentStatus: PomodoroTimerStatus
        get() = if (moduloTickCount / workTime == 0) PomodoroTimerStatus.WORKING else PomodoroTimerStatus.RESTING

    private val timeRemainingInState: Seconds
        get() = when (currentStatus) {
            PomodoroTimerStatus.WORKING -> workTime - moduloTickCount
            PomodoroTimerStatus.RESTING -> workTime - moduloTickCount + restTime
        }

    private val currentState: PomodoroTimerState
        get() = PomodoroTimerState(currentStatus, timeRemainingInState, isPaused)

    private var isPaused = false

    private fun startTicker() {
        if (::tickerChannel.isInitialized) tickerChannel.cancel()
        tickerChannel = ticker(1000L)

        GlobalScope.launch {
            mutableFlow.value = currentState

            for (tick in tickerChannel) {
                currentTickCount++

                mutableFlow.value = currentState
            }
        }
    }

    private val mutableFlow = MutableStateFlow<PomodoroTimerState?>(null)
    fun asFlow(): Flow<PomodoroTimerState> = mutableFlow.filterNotNull()

    private fun stopTicker() {
        if (::tickerChannel.isInitialized) tickerChannel.cancel()
    }

    fun start(workTime: Seconds, restTime: Seconds) {
        this.workTime = workTime
        this.restTime = restTime
        startTicker()
    }

    fun resume() {
        isPaused = false
        mutableFlow.value = currentState
        startTicker()
    }

    fun pause() {
        stopTicker()
        isPaused = true
        mutableFlow.value = currentState
    }

    fun stop() {
        stopTicker()
    }

}