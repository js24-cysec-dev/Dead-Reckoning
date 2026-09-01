package com.example.dead_reckoning

import com.example.dead_reckoning.domain.activity.ActivityType
import com.example.dead_reckoning.domain.deadreckoning.StepLengthEstimator
import com.example.dead_reckoning.domain.deadreckoning.WeinbergStepLengthModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StepLengthEstimatorTest {

    private lateinit var estimator: StepLengthEstimator

    @Before
    fun setUp() {
        estimator = StepLengthEstimator(WeinbergStepLengthModel(kFactor = 0.42f))
    }

    @Test
    fun estimate_stationary_returnsZero() {
        val sl = estimator.estimate(
            peakAccel = 3.0f,
            minAccel = -1.0f,
            cadenceSpm = 0f,
            activity = ActivityType.STATIONARY
        )
        assertEquals(0f, sl, 0.001f)
    }

    @Test
    fun estimate_walkingWeinberg_calculatesExpectedStride() {
        // peak = 3.0, min = -1.0 => deltaAccel = 4.0
        // Weinberg: 0.42 * (4.0)^0.25 = 0.42 * 1.4142 = 0.594 meters
        val sl = estimator.estimate(
            peakAccel = 3.0f,
            minAccel = -1.0f,
            cadenceSpm = 110f,
            activity = ActivityType.WALKING
        )
        assertEquals(0.594f, sl, 0.02f)
    }

    @Test
    fun estimate_running_scalesStepLengthUp() {
        val slWalking = estimator.estimate(
            peakAccel = 4.0f,
            minAccel = -1.5f,
            cadenceSpm = 120f,
            activity = ActivityType.WALKING
        )

        val slRunning = estimator.estimate(
            peakAccel = 4.0f,
            minAccel = -1.5f,
            cadenceSpm = 160f,
            activity = ActivityType.RUNNING
        )

        assertTrue(slRunning > slWalking)
    }
}
