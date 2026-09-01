package com.example.dead_reckoning.domain.deadreckoning

import com.example.dead_reckoning.domain.activity.ActivityType
import kotlin.math.pow

interface StepLengthModel {
    fun estimateStepLength(
        peakAccel: Float,
        minAccel: Float,
        cadenceSpm: Float,
        activity: ActivityType
    ): Float
}

/**
 * Weinberg step length model based on vertical acceleration range:
 * SL = k * (a_max - a_min)^(1/4)
 */
class WeinbergStepLengthModel(
    private val kFactor: Float = 0.42f
) : StepLengthModel {

    override fun estimateStepLength(
        peakAccel: Float,
        minAccel: Float,
        cadenceSpm: Float,
        activity: ActivityType
    ): Float {
        if (activity == ActivityType.STATIONARY) return 0f

        val deltaAccel = (peakAccel - minAccel).coerceAtLeast(0.1f)
        var stepLength = kFactor * deltaAccel.pow(0.25f)

        // Adjust for activity type
        if (activity == ActivityType.RUNNING) {
            stepLength *= 1.2f
        }

        // Clamp to human walking/running stride bounds [0.35m, 1.60m]
        return stepLength.coerceIn(0.35f, 1.60f)
    }
}

/**
 * Pluggable StepLengthEstimator wrapper supporting mathematical models or ML prediction models.
 */
class StepLengthEstimator(
    var model: StepLengthModel = WeinbergStepLengthModel()
) {
    fun estimate(
        peakAccel: Float,
        minAccel: Float,
        cadenceSpm: Float,
        activity: ActivityType
    ): Float {
        return model.estimateStepLength(peakAccel, minAccel, cadenceSpm, activity)
    }
}
