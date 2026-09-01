package com.example.dead_reckoning

import com.example.dead_reckoning.domain.sensorfusion.EkfSensorFusion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EkfSensorFusionTest {

    private lateinit var ekf: EkfSensorFusion

    @Before
    fun setUp() {
        ekf = EkfSensorFusion()
    }

    @Test
    fun predict_withStepDetected_updatesPositionAndVelocity() {
        // Heading 0 rad = North (dy = step_length = 1.0m)
        ekf.predict(
            dtSec = 0.5,
            gyroZ = 0f,
            stepDetected = true,
            stepLengthMeters = 1.0f,
            pdrHeadingRad = 0f
        )

        val pos = ekf.getFusedPosition()
        assertEquals(0.0, pos.xMeters, 0.01)
        assertEquals(1.0, pos.yMeters, 0.01)
        assertEquals(2.0, pos.velocityY, 0.1) // 1.0m / 0.5s = 2.0m/s
    }

    @Test
    fun updateGnss_setsReferenceAndUpdatesState() {
        // Call updateGnss to pass measurement
        ekf.updateGnss(
            lat = 37.4220,
            lng = -122.0841,
            alt = 10.0,
            speed = 1.5f,
            accuracy = 3.0f,
            bearingDeg = 90f // East
        )

        assertTrue(ekf.isReferenceSet)
        val pos = ekf.getFusedPosition()
        assertEquals(37.4220, pos.latitude, 0.0001)
        assertEquals(-122.0841, pos.longitude, 0.0001)
        assertTrue(pos.velocityX > 0.0) // EKF kalman update pulls velocity positive towards measurement 1.5
    }

    @Test
    fun updateBarometer_updatesAltitudeState() {
        ekf.updateBarometer(altMeters = 15.5, accuracyMeters = 0.5)
        val pos = ekf.getFusedPosition()
        // EKF moves altitude state towards measurement 15.5 weighted by covariance (~14.76m)
        assertEquals(15.5, pos.zMeters, 1.0)
    }

    @Test
    fun updateZupt_reducesVelocityToZero() {
        // Step forward to induce velocity
        ekf.predict(dtSec = 1.0, stepDetected = true, stepLengthMeters = 1.0f, pdrHeadingRad = 0f)
        val beforeZupt = ekf.getFusedPosition()
        assertTrue(beforeZupt.velocityY > 0.0)

        // Apply zero velocity update (stationary)
        ekf.updateZupt()
        val afterZupt = ekf.getFusedPosition()
        assertEquals(0.0, afterZupt.velocityY, 0.05)
        assertEquals(0.0, afterZupt.velocityX, 0.05)
    }

    @Test
    fun recorrectPosition_alignsPositionWithTrueGnss() {
        ekf.updateGnss(lat = 37.4220, lng = -122.0841, alt = 0.0, speed = 0f, accuracy = 5f, bearingDeg = 0f)
        // Simulate drift
        ekf.predict(dtSec = 1.0, stepDetected = true, stepLengthMeters = 5.0f, pdrHeadingRad = 0f)

        // Re-correct back to origin
        ekf.recorrectPosition(trueLat = 37.4220, trueLng = -122.0841)
        val pos = ekf.getFusedPosition()
        assertEquals(37.4220, pos.latitude, 0.0001)
        assertEquals(-122.0841, pos.longitude, 0.0001)
        assertEquals(0.0, pos.xMeters, 0.01)
        assertEquals(0.0, pos.yMeters, 0.01)
    }

    @Test
    fun reset_clearsState() {
        ekf.updateGnss(lat = 37.4220, lng = -122.0841, alt = 10.0, speed = 0f, accuracy = 5f, bearingDeg = 0f)
        ekf.reset()

        assertFalse(ekf.isReferenceSet)
        val pos = ekf.getFusedPosition()
        assertEquals(0.0, pos.xMeters, 0.001)
        assertEquals(0.0, pos.yMeters, 0.001)
    }
}
