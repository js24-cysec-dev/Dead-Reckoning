package com.example.dead_reckoning.domain.deadreckoning

import com.example.dead_reckoning.domain.activity.ActivityClassifier
import com.example.dead_reckoning.domain.activity.ActivityType
import com.example.dead_reckoning.domain.model.AccelerometerSample
import com.example.dead_reckoning.domain.model.GyroscopeSample
import com.example.dead_reckoning.domain.model.MagnetometerSample
import com.example.dead_reckoning.domain.sensorfusion.OrientationState
import com.example.dead_reckoning.domain.sensorfusion.ProcessedAcceleration
import com.example.dead_reckoning.domain.sensorfusion.SensorFusionEngine

data class PdrEstimateResult(
    val orientationState: OrientationState = OrientationState(),
    val processedAccel: ProcessedAcceleration = ProcessedAcceleration(),
    val activity: ActivityType = ActivityType.STATIONARY,
    val stepDetectionResult: StepDetectionResult = StepDetectionResult(),
    val stepLengthMeters: Float = 0f,
    val headingState: HeadingState = HeadingState(),
    val positionState: PdrPositionState = PdrPositionState()
)

/**
 * PdrEstimator coordinates sensor fusion, activity classification, step detection,
 * step length estimation, heading estimation, and PDR position update.
 */
class PdrEstimator(
    val sensorFusionEngine: SensorFusionEngine = SensorFusionEngine(),
    val activityClassifier: ActivityClassifier = ActivityClassifier(),
    val stepDetector: StepDetector = StepDetector(),
    val stepLengthEstimator: StepLengthEstimator = StepLengthEstimator(),
    val headingEstimator: HeadingEstimator = HeadingEstimator(),
    val pdrEngine: PdrEngine = PdrEngine()
) {
    fun processSample(
        accel: AccelerometerSample,
        gyro: GyroscopeSample,
        mag: MagnetometerSample,
        timestampNs: Long = accel.timestampNs,
        gnssLat: Double = 0.0,
        gnssLng: Double = 0.0
    ): PdrEstimateResult {
        if (gnssLat != 0.0 || gnssLng != 0.0) {
            pdrEngine.setGnssReference(gnssLat, gnssLng)
        }

        val fusionResult = sensorFusionEngine.process(accel, gyro, mag, timestampNs)
        val stepResult = stepDetector.process(fusionResult.processedAccel.verticalAccel, timestampNs)
        val activity = activityClassifier.classify(fusionResult.processedAccel.linearAccelMagnitude, stepResult.cadenceSpm)

        val stepLength = if (stepResult.stepDetected) {
            stepLengthEstimator.estimate(stepResult.peakAccel, stepResult.minAccel, stepResult.cadenceSpm, activity)
        } else {
            0f
        }

        val headingState = headingEstimator.process(
            orientationState = fusionResult.orientationState,
            gyroZ = gyro.z,
            magX = mag.x,
            magY = mag.y,
            magZ = mag.z,
            timestampNs = timestampNs
        )

        val posState = pdrEngine.update(
            stepDetected = stepResult.stepDetected,
            stepLengthMeters = stepLength,
            fusedHeadingRad = headingState.fusedHeadingRad
        )

        return PdrEstimateResult(
            orientationState = fusionResult.orientationState,
            processedAccel = fusionResult.processedAccel,
            activity = activity,
            stepDetectionResult = stepResult,
            stepLengthMeters = stepLength,
            headingState = headingState,
            positionState = posState
        )
    }

    fun reset() {
        sensorFusionEngine.reset()
        activityClassifier.reset()
        stepDetector.reset()
        headingEstimator.reset()
        pdrEngine.reset()
    }
}
