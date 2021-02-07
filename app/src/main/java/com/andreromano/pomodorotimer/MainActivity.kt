package com.andreromano.pomodorotimer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.andreromano.pomodorotimer.service.TimerService
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            val intent = Intent(this, TimerService::class.java)
                .putExtra(TimerService.EXTRA_WORK_TIME_SECONDS, 10)
                .putExtra(TimerService.EXTRA_REST_TIME_SECONDS, 5)
            startService(intent)
        }

    }
}