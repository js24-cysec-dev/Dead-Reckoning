package com.example.dead_reckoning.domain.activity

import kotlin.math.pow

enum class ActivityType {
    STATIONARY,
    WALKING,
    RUNNING,
    UNKNOWN
}

/**
 * ActivityClassifier performs rule-based baseline activity detection (STATIONARY, WALKING, RUNNING, UNKNOWN)
 * based on linear acceleration energy/variance and step cadence over a rolling time window.
 */
class ActivityClassifier(
    private val windowCapacity: Int = 50
) {
    private val linearAccelBuffer = ArrayDeque<Float>()

    fun classify(
        linearAccelMagnitude: Float,
        cadenceSpm: Float = 0f
    ): ActivityType {
        linearAccelBuffer.addLast(linearAccelMagnitude)
        if (linearAccelBuffer.size > windowCapacity) {
            linearAccelBuffer.removeFirst()
        }

        if (linearAccelBuffer.isEmpty()) {
            return ActivityType.STATIONARY
        }

        val count = linearAccelBuffer.size
        val mean = linearAccelBuffer.sum() / count
        val variance = linearAccelBuffer.fold(0f) { acc, v -> acc + (v - mean).pow(2) } / count
        val energy = linearAccelBuffer.fold(0f) { acc, v -> acc + v.pow(2) } / count

        // Rule-based classification thresholds
        return when {
            variance < 0.20f && energy < 0.40f && cadenceSpm < 20f -> {
                ActivityType.STATIONARY
            }
            (variance in 0.20f..6.5f) || (cadenceSpm in 20f..145f) -> {
                ActivityType.WALKING
            }
            variance > 6.5f || cadenceSpm > 145f -> {
                ActivityType.RUNNING
            }
            else -> {
                ActivityType.UNKNOWN
            }
        }
    }

    fun reset() {
        linearAccelBuffer.clear()
    }
}
