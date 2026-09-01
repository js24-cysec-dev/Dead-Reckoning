package com.example.dead_reckoning

import com.example.dead_reckoning.domain.deadreckoning.HeadingEstimator
import com.example.dead_reckoning.domain.sensorfusion.OrientationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HeadingEstimatorTest {

    private lateinit var headingEstimator: HeadingEstimator

    @Before
    fun setUp() {
        headingEstimator = HeadingEstimator()
    }

    @Test
    fun process_normalEarthMagneticField_detectsNoAnomaly() {
        val orientation = OrientationState(yawRad = 0f)
        // Earth magnetic field norm ~ 45 uT
        val state = headingEstimator.process(
            orientationState = orientation,
            gyroZ = 0f,
            magX = 20f,
            magY = 20f,
            magZ = -35f,
            timestampNs = 1_000_000_000L
        )

        assertFalse(state.magneticAnomaly)
        assertTrue(state.magneticFieldNorm in 25.0f..65.0f)
    }

    @Test
    fun process_strongMagneticDistortion_flagsAnomaly() {
        val orientation = OrientationState(yawRad = 0f)
        // Anomaly: 150 uT magnetic norm near heavy iron/speaker magnet
        val state = headingEstimator.process(
            orientationState = orientation,
            gyroZ = 0f,
            magX = 100f,
            magY = 100f,
            magZ = 50f,
            timestampNs = 1_000_000_000L
        )

        assertTrue(state.magneticAnomaly)
        assertTrue(state.magneticFieldNorm > 65.0f)
    }

    @Test
    fun process_relativeHeading_tracksAngularChange() {
        val orientation1 = OrientationState(yawRad = 0f)
        headingEstimator.process(orientation1, 0f, 20f, 20f, -35f, 1_000_000_000L)

        // Rotate clockwise by ~0.5 rad
        val orientation2 = OrientationState(yawRad = 0.5f)
        val state2 = headingEstimator.process(orientation2, 0.5f, 20f, 20f, -35f, 2_000_000_000L)

        assertEquals(0.5f, state2.relativeHeadingRad, 0.1f)
    }
}
