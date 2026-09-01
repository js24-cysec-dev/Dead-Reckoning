package com.example.dead_reckoning.utils

import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtils {
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    fun getCurrentTimestampString(): String {
        return dateFormat.format(Date())
    }

    fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        val hours = (durationMs / (1000 * 60 * 60))
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun elapsedRealtimeNanosToWallTimeMs(elapsedNs: Long): Long {
        val currentWallMs = System.currentTimeMillis()
        val currentElapsedNs = SystemClock.elapsedRealtimeNanos()
        val deltaMs = (elapsedNs - currentElapsedNs) / 1_000_000L
        return currentWallMs + deltaMs
    }

    fun elapsedRealtimeNanosToTimestampNs(elapsedNs: Long): Long {
        return elapsedRealtimeNanosToWallTimeMs(elapsedNs) * 1_000_000L
    }
}
