package com.example.dead_reckoning

import android.app.Application
import com.example.dead_reckoning.data.repository.TelemetryRepository

class DeadReckoningApp : Application() {
    lateinit var repository: TelemetryRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = TelemetryRepository.getInstance(this)
    }
}
