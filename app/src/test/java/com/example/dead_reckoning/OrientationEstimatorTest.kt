package com.example.dead_reckoning

import com.example.dead_reckoning.domain.model.AccelerometerSample
import com.example.dead_reckoning.domain.model.GyroscopeSample
import com.example.dead_reckoning.domain.model.MagnetometerSample
import com.example.dead_reckoning.domain.sensorfusion.OrientationEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OrientationEstimatorTest {

    private lateinit var estimator: OrientationEstimator

    @Before
    fun setUp() {
        estimator = OrientationEstimator()
    }

    @Test
    fun process_deviceFlatFaceUp_returnsZeroRollAndPitch() {
        val accel = AccelerometerSample(x = 0f, y = 0f, z = 9.81f, timestampNs = 1_000_000_000L)
        val gyro = GyroscopeSample(x = 0f, y = 0f, z = 0f, timestampNs = 1_000_000_000L)
        val mag = MagnetometerSample(x = 0f, y = 25f, z = -40f, timestampNs = 1_000_000_000L)

        val state = estimator.process(accel, gyro, mag)

        assertEquals(0f, state.rollRad, 0.01f)
        assertEquals(0f, state.pitchRad, 0.01f)
        assertEquals(0f, state.headingDeg, 1.0f) // Facing magnetic north
    }

    @Test
    fun transformDeviceToWorld_identityRotation_preservesVector() {
        val identityMatrix = floatArrayOf(
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f
        )
        val deviceVec = floatArrayOf(1.0f, 2.0f, 3.0f)

        val worldVec = estimator.transformDeviceToWorld(deviceVec, identityMatrix)

        assertEquals(1.0f, worldVec[0], 0.001f)
        assertEquals(2.0f, worldVec[1], 0.001f)
        assertEquals(3.0f, worldVec[2], 0.001f)
    }

    @Test
    fun process_gyroIntegration_updatesOrientation() {
        val accel = AccelerometerSample(x = 0f, y = 0f, z = 9.81f, timestampNs = 1_000_000_000L)
        val gyroZero = GyroscopeSample(x = 0f, y = 0f, z = 0f, timestampNs = 1_000_000_000L)
        val mag = MagnetometerSample(x = 0f, y = 25f, z = -40f, timestampNs = 1_000_000_000L)

        // Initialize at t = 1.0s
        estimator.process(accel, gyroZero, mag, timestampNs = 1_000_000_000L)

        // Rotate around Z axis (yaw rate = 0.5 rad/s for 1.0s)
        val gyroSpin = GyroscopeSample(x = 0f, y = 0f, z = 0.5f, timestampNs = 2_000_000_000L)
        val state = estimator.process(accel, gyroSpin, mag, timestampNs = 2_000_000_000L)

        // Should shift yaw due to gyro integration
        assertTrue(state.yawRad != 0f)
    }

    @Test
    fun reset_clearsInternalState() {
        val accel = AccelerometerSample(x = 0f, y = 0f, z = 9.81f, timestampNs = 1_000_000_000L)
        val gyro = GyroscopeSample(x = 0f, y = 0f, z = 0f, timestampNs = 1_000_000_000L)
        val mag = MagnetometerSample(x = 0f, y = 25f, z = -40f, timestampNs = 1_000_000_000L)

        estimator.process(accel, gyro, mag)
        estimator.reset()

        val freshState = estimator.process(accel, gyro, mag, timestampNs = 5_000_000_000L)
        assertEquals(0f, freshState.rollRad, 0.01f)
        assertEquals(0f, freshState.pitchRad, 0.01f)
    }
}
