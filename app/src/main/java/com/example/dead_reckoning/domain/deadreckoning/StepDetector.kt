package com.example.dead_reckoning.domain.deadreckoning

import kotlin.math.pow
import kotlin.math.sqrt

data class StepDetectionResult(
    val stepDetected: Boolean = false,
    val totalStepCount: Long = 0L,
    val cadenceSpm: Float = 0f,
    val peakAccel: Float = 0f,
    val minAccel: Float = 0f
)

/**
 * StepDetector performs peak detection on vertical linear acceleration or linear magnitude
 * with dynamic adaptive thresholding and refractory window (~280ms) for robustness.
 */
class StepDetector(
    private val minStepIntervalNs: Long = 280_000_000L, // 280ms refractory period
    private val baseThreshold: Float = 1.2f,             // Minimum m/s² acceleration peak
    private val windowCapacity: Int = 20
) {
    private val signalBuffer = ArrayDeque<Float>()
    private val recentStepTimestampsNs = ArrayDeque<Long>()

    private var lastStepTimestampNs: Long = 0L
    private var totalStepCount: Long = 0L
    private var currentCadenceSpm: Float = 0f

    private var recentPeak: Float = 0f
    private var recentMin: Float = 0f
    private var trackingTrough: Float = Float.MAX_VALUE

    fun process(
        accelSignal: Float,
        timestampNs: Long
    ): StepDetectionResult {
        signalBuffer.addLast(accelSignal)
        if (accelSignal < trackingTrough) {
            trackingTrough = accelSignal
        }

        if (signalBuffer.size > windowCapacity) {
            signalBuffer.removeFirst()
        }

        var isStep = false

        if (signalBuffer.size >= 3) {
            val prev2 = signalBuffer[signalBuffer.size - 3]
            val prev1 = signalBuffer[signalBuffer.size - 2]
            val current = signalBuffer[signalBuffer.size - 1]

            // Dynamic Adaptive Thresholding
            val mean = signalBuffer.sum() / signalBuffer.size
            val stdDev = sqrt(signalBuffer.fold(0f) { acc, v -> acc + (v - mean).pow(2) } / signalBuffer.size)
            val adaptiveThreshold = (mean + 0.6f * stdDev).coerceAtLeast(baseThreshold)

            // Local maximum check on prev1
            val isPeak = (prev1 > prev2) && (prev1 > current) && (prev1 >= adaptiveThreshold)
            val timeSinceLastStep = timestampNs - lastStepTimestampNs

            if (isPeak && (timeSinceLastStep >= minStepIntervalNs || lastStepTimestampNs == 0L)) {
                isStep = true
                totalStepCount++
                lastStepTimestampNs = timestampNs

                recentPeak = prev1
                recentMin = if (trackingTrough != Float.MAX_VALUE) trackingTrough else 0f
                trackingTrough = Float.MAX_VALUE

                // Update cadence calculation
                recentStepTimestampsNs.addLast(timestampNs)
                if (recentStepTimestampsNs.size > 8) {
                    recentStepTimestampsNs.removeFirst()
                }

                if (recentStepTimestampsNs.size >= 2) {
                    val durationSec = (recentStepTimestampsNs.last() - recentStepTimestampsNs.first()) / 1_000_000_000f
                    val stepsCountInWindow = recentStepTimestampsNs.size - 1
                    if (durationSec > 0.1f) {
                        currentCadenceSpm = (stepsCountInWindow / durationSec) * 60f
                    }
                }
            }
        }

        // Decay cadence if no step detected for > 2 seconds
        if (lastStepTimestampNs > 0L && (timestampNs - lastStepTimestampNs) > 2_000_000_000L) {
            currentCadenceSpm = (currentCadenceSpm * 0.9f).coerceAtLeast(0f)
        }

        return StepDetectionResult(
            stepDetected = isStep,
            totalStepCount = totalStepCount,
            cadenceSpm = currentCadenceSpm,
            peakAccel = recentPeak,
            minAccel = recentMin
        )
    }

    fun reset() {
        signalBuffer.clear()
        recentStepTimestampsNs.clear()
        lastStepTimestampNs = 0L
        totalStepCount = 0L
        currentCadenceSpm = 0f
        recentPeak = 0f
        recentMin = 0f
        trackingTrough = Float.MAX_VALUE
    }
}
