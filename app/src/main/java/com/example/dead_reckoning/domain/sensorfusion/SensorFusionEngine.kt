package com.example.dead_reckoning.domain.sensorfusion

import com.example.dead_reckoning.domain.model.AccelerometerSample
import com.example.dead_reckoning.domain.model.GyroscopeSample
import com.example.dead_reckoning.domain.model.MagnetometerSample

data class SensorFusionResult(
    val orientationState: OrientationState = OrientationState(),
    val processedAccel: ProcessedAcceleration = ProcessedAcceleration()
)

/**
 * High-level SensorFusionEngine that combines OrientationEstimator and AccelerationProcessor.
 */
class SensorFusionEngine(
    val orientationEstimator: OrientationEstimator = OrientationEstimator(),
    val accelerationProcessor: AccelerationProcessor = AccelerationProcessor()
) {
    fun process(
        accel: AccelerometerSample,
        gyro: GyroscopeSample,
        mag: MagnetometerSample,
        timestampNs: Long = accel.timestampNs
    ): SensorFusionResult {
        val orientation = orientationEstimator.process(accel, gyro, mag, timestampNs)
        val accelResult = accelerationProcessor.process(accel, orientationEstimator, orientation)
        return SensorFusionResult(
            orientationState = orientation,
            processedAccel = accelResult
        )
    }

    fun reset() {
        orientationEstimator.reset()
        accelerationProcessor.reset()
    }
}
