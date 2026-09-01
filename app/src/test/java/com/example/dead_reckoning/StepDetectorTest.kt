package com.example.dead_reckoning

import com.example.dead_reckoning.domain.deadreckoning.StepDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StepDetectorTest {

    private lateinit var stepDetector: StepDetector

    @Before
    fun setUp() {
        stepDetector = StepDetector(minStepIntervalNs = 280_000_000L, baseThreshold = 1.0f)
    }

    @Test
    fun process_singlePeakAboveThreshold_detectsStep() {
        var stepDetectedCount = 0

        // Simulate baseline
        stepDetector.process(0.1f, 1_000_000_000L)
        stepDetector.process(0.2f, 1_020_000_000L)

        // Peak signal
        val result1 = stepDetector.process(2.5f, 1_040_000_000L)
        if (result1.stepDetected) stepDetectedCount++

        val result2 = stepDetector.process(0.2f, 1_060_000_000L)
        if (result2.stepDetected) stepDetectedCount++

        val result3 = stepDetector.process(0.1f, 1_080_000_000L)
        if (result3.stepDetected) stepDetectedCount++

        assertEquals(1, stepDetectedCount)
        assertEquals(1L, result3.totalStepCount)
    }

    @Test
    fun process_refractoryPeriod_suppressesRapidDoublePeak() {
        // Step 1
        stepDetector.process(0.1f, 1_000_000_000L)
        stepDetector.process(2.5f, 1_020_000_000L)
        val res1 = stepDetector.process(0.1f, 1_040_000_000L)

        // False peak only 100ms later (< 280ms refractory period)
        stepDetector.process(0.1f, 1_100_000_000L)
        stepDetector.process(3.0f, 1_120_000_000L)
        val res2 = stepDetector.process(0.1f, 1_140_000_000L)

        assertTrue(res1.stepDetected)
        assertFalse(res2.stepDetected)
        assertEquals(1L, res2.totalStepCount)
    }

    @Test
    fun reset_clearsStepCounter() {
        stepDetector.process(0.1f, 1_000_000_000L)
        stepDetector.process(2.5f, 1_020_000_000L)
        stepDetector.process(0.1f, 1_040_000_000L)

        stepDetector.reset()

        val res = stepDetector.process(0.1f, 5_000_000_000L)
        assertEquals(0L, res.totalStepCount)
    }
}
