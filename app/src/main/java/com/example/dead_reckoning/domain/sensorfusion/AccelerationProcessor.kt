package com.example.dead_reckoning.domain.sensorfusion

import com.example.dead_reckoning.domain.model.AccelerometerSample
import kotlin.math.sqrt

/**
 * Processed acceleration output containing world-frame total acceleration,
 * isolated gravity vector, linear acceleration vector, vertical acceleration,
 * and magnitude of linear acceleration.
 */
data class ProcessedAcceleration(
    val worldAccel: FloatArray = floatArrayOf(0f, 0f, 0f),
    val gravityWorld: FloatArray = floatArrayOf(0f, 0f, 9.81f),
    val linearAccelWorld: FloatArray = floatArrayOf(0f, 0f, 0f),
    val verticalAccel: Float = 0f,
    val linearAccelMagnitude: Float = 0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ProcessedAcceleration
        return worldAccel.contentEquals(other.worldAccel) &&
                gravityWorld.contentEquals(other.gravityWorld) &&
                linearAccelWorld.contentEquals(other.linearAccelWorld) &&
                verticalAccel == other.verticalAccel &&
                linearAccelMagnitude == other.linearAccelMagnitude
    }

    override fun hashCode(): Int {
        var result = worldAccel.contentHashCode()
        result = 31 * result + gravityWorld.contentHashCode()
        result = 31 * result + linearAccelWorld.contentHashCode()
        result = 31 * result + verticalAccel.hashCode()
        result = 31 * result + linearAccelMagnitude.hashCode()
        return result
    }
}

/**
 * AccelerationProcessor handles low-pass filtering to isolate Gravity from Linear Acceleration
 * in the World (ENU) frame and computes vertical acceleration and linear magnitude.
 */
class AccelerationProcessor(
    private val gravityFilterAlpha: Float = 0.90f
) {
    private val gravityWorld = floatArrayOf(0f, 0f, 9.81f)
    private var isInitialized = false

    fun process(
        accelSample: AccelerometerSample,
        orientationEstimator: OrientationEstimator,
        orientationState: OrientationState
    ): ProcessedAcceleration {
        val deviceAccel = floatArrayOf(accelSample.x, accelSample.y, accelSample.z)
        val worldAccel = orientationEstimator.transformDeviceToWorld(deviceAccel, orientationState.rotationMatrix)

        if (!isInitialized) {
            gravityWorld[0] = worldAccel[0]
            gravityWorld[1] = worldAccel[1]
            gravityWorld[2] = worldAccel[2]
            isInitialized = true
        } else {
            // Low-pass filter to isolate gravity in world frame
            gravityWorld[0] = gravityFilterAlpha * gravityWorld[0] + (1f - gravityFilterAlpha) * worldAccel[0]
            gravityWorld[1] = gravityFilterAlpha * gravityWorld[1] + (1f - gravityFilterAlpha) * worldAccel[1]
            gravityWorld[2] = gravityFilterAlpha * gravityWorld[2] + (1f - gravityFilterAlpha) * worldAccel[2]
        }

        // High-pass filter: Linear Acceleration = World Acceleration - Gravity
        val linX = worldAccel[0] - gravityWorld[0]
        val linY = worldAccel[1] - gravityWorld[1]
        val linZ = worldAccel[2] - gravityWorld[2]

        val linearAccelWorld = floatArrayOf(linX, linY, linZ)
        val verticalAccel = linZ
        val linearAccelMagnitude = sqrt(linX * linX + linY * linY + linZ * linZ)

        return ProcessedAcceleration(
            worldAccel = worldAccel,
            gravityWorld = gravityWorld.clone(),
            linearAccelWorld = linearAccelWorld,
            verticalAccel = verticalAccel,
            linearAccelMagnitude = linearAccelMagnitude
        )
    }

    fun reset() {
        gravityWorld[0] = 0f
        gravityWorld[1] = 0f
        gravityWorld[2] = 9.81f
        isInitialized = false
    }
}
