package com.example.dead_reckoning

import com.example.dead_reckoning.domain.model.AccelerometerSample
import com.example.dead_reckoning.domain.model.GyroscopeSample
import com.example.dead_reckoning.domain.model.MagnetometerSample
import com.example.dead_reckoning.domain.sensorfusion.AccelerationProcessor
import com.example.dead_reckoning.domain.sensorfusion.OrientationEstimator
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AccelerationProcessorTest {

    private lateinit var orientationEstimator: OrientationEstimator
    private lateinit var accelerationProcessor: AccelerationProcessor

    @Before
    fun setUp() {
        orientationEstimator = OrientationEstimator()
        accelerationProcessor = AccelerationProcessor()
    }

    @Test
    fun process_staticGravity_isolatesVerticalAcceleration() {
        val accel = AccelerometerSample(x = 0f, y = 0f, z = 9.81f, timestampNs = 1_000_000_000L)
        val gyro = GyroscopeSample(x = 0f, y = 0f, z = 0f, timestampNs = 1_000_000_000L)
        val mag = MagnetometerSample(x = 0f, y = 25f, z = -40f, timestampNs = 1_000_000_000L)

        val orientation = orientationEstimator.process(accel, gyro, mag)
        val processed = accelerationProcessor.process(accel, orientationEstimator, orientation)

        // For static gravity, linear acceleration should be near 0 m/s²
        assertEquals(0f, processed.verticalAccel, 0.05f)
        assertEquals(0f, processed.linearAccelMagnitude, 0.05f)
        assertEquals(9.81f, processed.gravityWorld[2], 0.1f)
    }

    @Test
    fun process_verticalImpact_detectsVerticalAccelerationPeak() {
        val accelStatic = AccelerometerSample(x = 0f, y = 0f, z = 9.81f, timestampNs = 1_000_000_000L)
        val gyro = GyroscopeSample(x = 0f, y = 0f, z = 0f)
        val mag = MagnetometerSample(x = 0f, y = 25f, z = -40f)

        val orientation = orientationEstimator.process(accelStatic, gyro, mag)
        accelerationProcessor.process(accelStatic, orientationEstimator, orientation)

        // Simulate upward step impact force
        val accelImpact = AccelerometerSample(x = 0f, y = 0f, z = 14.81f, timestampNs = 1_020_000_000L)
        val processedImpact = accelerationProcessor.process(accelImpact, orientationEstimator, orientation)

        // Vertical acceleration should rise by ~4.5 m/s² (accounting for low-pass gravity tracking)
        assertEquals(4.5f, processedImpact.verticalAccel, 0.6f)
    }
}
