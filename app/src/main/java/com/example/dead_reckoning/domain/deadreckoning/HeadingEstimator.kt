package com.example.dead_reckoning.domain.deadreckoning

import com.example.dead_reckoning.domain.sensorfusion.OrientationState
import kotlin.math.PI
import kotlin.math.sqrt

data class HeadingState(
    val magneticHeadingRad: Float = 0f,
    val magneticHeadingDeg: Float = 0f,
    val fusedHeadingRad: Float = 0f,
    val fusedHeadingDeg: Float = 0f,
    val relativeHeadingRad: Float = 0f,
    val magneticAnomaly: Boolean = false,
    val magneticFieldNorm: Float = 0f
)

/**
 * HeadingEstimator provides robust heading estimation by combining tilt-compensated magnetic heading,
 * gyro rate integration, and magnetic anomaly detection (flagging magnetic norms outside 25-65 µT).
 */
class HeadingEstimator(
    private val alphaNormal: Float = 0.95f,
    private val alphaAnomaly: Float = 0.995f // Higher weight on gyro when magnetic anomaly present
) {
    private var initialHeadingRad: Float? = null
    private var lastFusedHeadingRad: Float = 0f
    private var lastTimestampNs: Long = 0L
    private var isInitialized: Boolean = false

    fun process(
        orientationState: OrientationState,
        gyroZ: Float,
        magX: Float,
        magY: Float,
        magZ: Float,
        timestampNs: Long
    ): HeadingState {
        // Magnetic Field Norm
        val magNorm = sqrt(magX * magX + magY * magY + magZ * magZ)

        // Earth magnetic field norm is typically ~25 to 65 uT
        val isAnomaly = magNorm < 25.0f || magNorm > 65.0f

        val magHeadingRad = orientationState.yawRad

        if (!isInitialized) {
            lastFusedHeadingRad = magHeadingRad
            initialHeadingRad = magHeadingRad
            lastTimestampNs = timestampNs
            isInitialized = true
        } else {
            val dt = (timestampNs - lastTimestampNs) / 1_000_000_000f
            lastTimestampNs = timestampNs

            val alpha = if (isAnomaly) alphaAnomaly else alphaNormal

            if (dt in 0.0001f..1.0f) {
                val gyroHeading = lastFusedHeadingRad + gyroZ * dt
                lastFusedHeadingRad = blendAngles(gyroHeading, magHeadingRad, alpha)
            }
        }

        val initial = initialHeadingRad ?: lastFusedHeadingRad
        val relativeHeading = normalizeAngle(lastFusedHeadingRad - initial)

        return HeadingState(
            magneticHeadingRad = magHeadingRad,
            magneticHeadingDeg = normalizeHeadingDeg(magHeadingRad * 180f / PI.toFloat()),
            fusedHeadingRad = lastFusedHeadingRad,
            fusedHeadingDeg = normalizeHeadingDeg(lastFusedHeadingRad * 180f / PI.toFloat()),
            relativeHeadingRad = relativeHeading,
            magneticAnomaly = isAnomaly,
            magneticFieldNorm = magNorm
        )
    }

    private fun blendAngles(angle1: Float, angle2: Float, alpha: Float): Float {
        val diff = normalizeAngle(angle2 - angle1)
        return normalizeAngle(angle1 + (1f - alpha) * diff)
    }

    private fun normalizeAngle(angleRad: Float): Float {
        var a = angleRad % (2f * PI.toFloat())
        if (a > PI.toFloat()) a -= 2f * PI.toFloat()
        if (a < -PI.toFloat()) a += 2f * PI.toFloat()
        return a
    }

    private fun normalizeHeadingDeg(deg: Float): Float {
        var d = deg % 360f
        if (d < 0f) d += 360f
        return d
    }

    fun reset() {
        initialHeadingRad = null
        lastFusedHeadingRad = 0f
        lastTimestampNs = 0L
        isInitialized = false
    }
}
