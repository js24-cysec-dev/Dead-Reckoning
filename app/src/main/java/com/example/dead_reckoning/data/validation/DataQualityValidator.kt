package com.example.dead_reckoning.data.validation

import com.example.dead_reckoning.domain.model.CombinedTelemetry
import com.example.dead_reckoning.domain.model.ValidationSummary
import java.util.LinkedList

class DataQualityValidator {

    private var totalSamples = 0L
    private var monotonicityViolations = 0L
    private var stuckSensorViolations = 0L

    private var previousElapsedRealtimeNs: Long = 0L

    private val windowSize = 50
    private val accelHistory = LinkedList<Triple<Float, Float, Float>>()
    private val gyroHistory = LinkedList<Triple<Float, Float, Float>>()

    private var firstSampleTimeNs: Long = 0L
    private var lastSampleTimeNs: Long = 0L

    fun reset() {
        totalSamples = 0L
        monotonicityViolations = 0L
        stuckSensorViolations = 0L
        previousElapsedRealtimeNs = 0L
        accelHistory.clear()
        gyroHistory.clear()
        firstSampleTimeNs = 0L
        lastSampleTimeNs = 0L
    }

    fun validateSample(sample: CombinedTelemetry): ValidationSummary {
        totalSamples++

        if (firstSampleTimeNs == 0L) {
            firstSampleTimeNs = sample.elapsedRealtimeNs
        }
        lastSampleTimeNs = sample.elapsedRealtimeNs

        // 1. Monotonicity check
        if (previousElapsedRealtimeNs != 0L && sample.elapsedRealtimeNs <= previousElapsedRealtimeNs) {
            monotonicityViolations++
        }
        previousElapsedRealtimeNs = sample.elapsedRealtimeNs

        // 2. Stuck sensor check
        accelHistory.addLast(Triple(sample.accelX, sample.accelY, sample.accelZ))
        if (accelHistory.size > windowSize) accelHistory.removeFirst()

        gyroHistory.addLast(Triple(sample.gyroX, sample.gyroY, sample.gyroZ))
        if (gyroHistory.size > windowSize) gyroHistory.removeFirst()

        if (accelHistory.size == windowSize && isStuck(accelHistory)) {
            stuckSensorViolations++
        }

        val durationSec = (lastSampleTimeNs - firstSampleTimeNs) / 1_000_000_000f
        val avgHz = if (durationSec > 0) totalSamples / durationSec else 0f

        val issues = mutableListOf<String>()
        if (monotonicityViolations > 0) {
            issues.add("Monotonicity violations: $monotonicityViolations")
        }
        if (stuckSensorViolations > 0) {
            issues.add("Stuck sensor detected: $stuckSensorViolations times")
        }

        val isValid = monotonicityViolations == 0L && stuckSensorViolations < 10L

        return ValidationSummary(
            totalSamples = totalSamples,
            monotonicityViolations = monotonicityViolations,
            stuckSensorViolations = stuckSensorViolations,
            averageSamplingRateHz = avgHz,
            isDataValid = isValid,
            issues = issues
        )
    }

    private fun isStuck(history: List<Triple<Float, Float, Float>>): Boolean {
        if (history.isEmpty()) return false
        val first = history[0]
        return history.all { it.first == first.first && it.second == first.second && it.third == first.third }
    }
}
