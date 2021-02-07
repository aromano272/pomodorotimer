package com.andreromano.pomodorotimer.timer

import com.andreromano.pomodorotimer.Seconds
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class PomodoroTimer(
    private val workTime: Seconds,
    private val restTime: Seconds
) {

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

    private var isPaused = false

    private fun startTicker() {
        if (::tickerChannel.isInitialized) tickerChannel.cancel()
        tickerChannel = ticker(1000L)

        GlobalScope.launch {
            for (tick in tickerChannel) {
                currentTickCount++

                val currentState = PomodoroTimerState(currentStatus, timeRemainingInState, isPaused)
                mutableFlow.value = currentState
            }
        }
    }

    private val mutableFlow = MutableStateFlow<PomodoroTimerState?>(null)
    fun asFlow(): Flow<PomodoroTimerState> = mutableFlow.filterNotNull()

    private fun stopTicker() {
        if (::tickerChannel.isInitialized) tickerChannel.cancel()
    }

    fun start() {
        startTicker()
    }

    fun resume() {
        isPaused = false
        val currentState = PomodoroTimerState(currentStatus, timeRemainingInState, isPaused)
        mutableFlow.value = currentState
        startTicker()
    }

    fun pause() {
        stopTicker()
        isPaused = true
        val currentState = PomodoroTimerState(currentStatus, timeRemainingInState, isPaused)
        mutableFlow.value = currentState
    }

    fun stop() {
        stopTicker()
    }

}