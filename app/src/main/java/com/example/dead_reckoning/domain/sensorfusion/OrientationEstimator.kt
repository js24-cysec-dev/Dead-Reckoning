package com.example.dead_reckoning.domain.sensorfusion

import com.example.dead_reckoning.domain.model.AccelerometerSample
import com.example.dead_reckoning.domain.model.GyroscopeSample
import com.example.dead_reckoning.domain.model.MagnetometerSample
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Orientation State representation holding roll, pitch, yaw (in radians and degrees),
 * compass heading, and device-to-world (ENU) 3x3 rotation matrix.
 */
data class OrientationState(
    val rollRad: Float = 0f,
    val pitchRad: Float = 0f,
    val yawRad: Float = 0f,           // Yaw angle in radians (-PI to PI, ENU frame)
    val rollDeg: Float = 0f,
    val pitchDeg: Float = 0f,
    val yawDeg: Float = 0f,
    val headingDeg: Float = 0f,        // Compass heading in degrees [0, 360) clockwise from North
    val rotationMatrix: FloatArray = floatArrayOf(
        1f, 0f, 0f,
        0f, 1f, 0f,
        0f, 0f, 1f
    )
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as OrientationState
        return rollRad == other.rollRad &&
                pitchRad == other.pitchRad &&
                yawRad == other.yawRad &&
                headingDeg == other.headingDeg &&
                rotationMatrix.contentEquals(other.rotationMatrix)
    }

    override fun hashCode(): Int {
        var result = rollRad.hashCode()
        result = 31 * result + pitchRad.hashCode()
        result = 31 * result + yawRad.hashCode()
        result = 31 * result + headingDeg.hashCode()
        result = 31 * result + rotationMatrix.contentHashCode()
        return result
    }
}

/**
 * OrientationEstimator uses a Complementary Filter to fuse Accelerometer, Gyroscope,
 * and Magnetometer measurements into a stable device orientation (Roll, Pitch, Yaw).
 *
 * --- WHY RAW GYRO DRIFTS WITHOUT ACCELEROMETER / MAGNETOMETER CORRECTION ---
 * Standard gyroscopes measure angular rate (radians/sec). Determining orientation via
 * pure gyro integration (θ(t) = θ_0 + ∫ ω(t) dt) suffers from two fundamental limitations:
 * 1. Constant sensor bias / zero-rate offset: Even tiny sensor bias (e.g. 0.01 rad/s) accumulates
 *    linearly over time, leading to unbounded orientation drift.
 * 2. High-frequency noise integration: Noise integrated over time produces random-walk drift.
 *
 * To correct for gyro drift:
 * - Accelerometer acts as a low-frequency reference for Pitch and Roll by measuring the static
 *   gravity vector direction (downward vector).
 * - Magnetometer acts as a low-frequency reference for Yaw / Heading by measuring the Earth's
 *   magnetic field vector (pointing toward Magnetic North).
 *
 * The Complementary Filter combines high-passed integrated gyroscope rates (responsive to quick
 * movements) with low-passed accelerometer/magnetometer tilt-compensated orientation (drift-free
 * over long periods).
 */
class OrientationEstimator(
    private val alpha: Float = 0.96f
) {
    private var lastTimestampNs: Long = 0L
    private var currentRoll: Float = 0f
    private var currentPitch: Float = 0f
    private var currentYaw: Float = 0f
    private var isInitialized: Boolean = false

    fun process(
        accel: AccelerometerSample,
        gyro: GyroscopeSample,
        mag: MagnetometerSample,
        timestampNs: Long = accel.timestampNs
    ): OrientationState {
        // Accelerometer-based Roll and Pitch
        val ax = accel.x
        val ay = accel.y
        val az = accel.z

        val accelNorm = sqrt(ax * ax + ay * ay + az * az)
        val rollAccel = if (accelNorm > 0.1f) atan2(ay, az) else 0f
        val pitchAccel = if (accelNorm > 0.1f) atan2(-ax, sqrt(ay * ay + az * az)) else 0f

        // Tilt-compensated Magnetometer Yaw
        val mx = mag.x
        val my = mag.y
        val mz = mag.z

        val cosRoll = cos(rollAccel.toDouble()).toFloat()
        val sinRoll = sin(rollAccel.toDouble()).toFloat()
        val cosPitch = cos(pitchAccel.toDouble()).toFloat()
        val sinPitch = sin(pitchAccel.toDouble()).toFloat()

        val mxComp = mx * cosPitch + mz * sinPitch
        val myComp = mx * sinRoll * sinPitch + my * cosRoll - mz * sinRoll * cosPitch
        val magYaw = atan2(-mxComp, myComp)

        if (!isInitialized || lastTimestampNs == 0L) {
            currentRoll = rollAccel
            currentPitch = pitchAccel
            currentYaw = magYaw
            lastTimestampNs = timestampNs
            isInitialized = true
        } else {
            val dt = (timestampNs - lastTimestampNs) / 1_000_000_000f
            lastTimestampNs = timestampNs

            if (dt in 0.0001f..1.0f) {
                // Gyro integration
                val gyroRoll = currentRoll + gyro.x * dt
                val gyroPitch = currentPitch + gyro.y * dt
                val gyroYaw = currentYaw + gyro.z * dt

                // Complementary filter update
                currentRoll = alpha * gyroRoll + (1f - alpha) * rollAccel
                currentPitch = alpha * gyroPitch + (1f - alpha) * pitchAccel
                currentYaw = blendAngles(gyroYaw, magYaw, alpha)
            }
        }

        // Compute 3x3 Rotation Matrix (Device to World/ENU frame)
        val rotMatrix = computeRotationMatrix(currentRoll, currentPitch, currentYaw)

        // Convert Yaw to Compass Heading (0..360 degrees clockwise from North)
        val headingDeg = normalizeHeadingDeg((-currentYaw * 180f / PI.toFloat()))

        return OrientationState(
            rollRad = currentRoll,
            pitchRad = currentPitch,
            yawRad = currentYaw,
            rollDeg = currentRoll * 180f / PI.toFloat(),
            pitchDeg = currentPitch * 180f / PI.toFloat(),
            yawDeg = currentYaw * 180f / PI.toFloat(),
            headingDeg = headingDeg,
            rotationMatrix = rotMatrix
        )
    }

    /**
     * Transforms a 3D vector from Device Frame (Xd, Yd, Zd) to World Frame (East, North, Up - ENU).
     * V_world = R * V_device
     */
    fun transformDeviceToWorld(deviceVector: FloatArray, rotationMatrix: FloatArray): FloatArray {
        if (deviceVector.size < 3 || rotationMatrix.size < 9) return deviceVector
        val x = deviceVector[0]
        val y = deviceVector[1]
        val z = deviceVector[2]

        val wx = rotationMatrix[0] * x + rotationMatrix[1] * y + rotationMatrix[2] * z
        val wy = rotationMatrix[3] * x + rotationMatrix[4] * y + rotationMatrix[5] * z
        val wz = rotationMatrix[6] * x + rotationMatrix[7] * y + rotationMatrix[8] * z

        return floatArrayOf(wx, wy, wz)
    }

    /**
     * Computes the 3x3 Device-to-World (ENU) rotation matrix from Roll, Pitch, Yaw angles.
     */
    private fun computeRotationMatrix(roll: Float, pitch: Float, yaw: Float): FloatArray {
        val cRoll = cos(roll.toDouble()).toFloat()
        val sRoll = sin(roll.toDouble()).toFloat()
        val cPitch = cos(pitch.toDouble()).toFloat()
        val sPitch = sin(pitch.toDouble()).toFloat()
        val cYaw = cos(yaw.toDouble()).toFloat()
        val sYaw = sin(yaw.toDouble()).toFloat()

        // R = Rz(yaw) * Rx(pitch) * Ry(roll)
        return floatArrayOf(
            cYaw * cRoll - sYaw * sPitch * sRoll, -sYaw * cPitch, cYaw * sRoll + sYaw * sPitch * cRoll,
            sYaw * cRoll + cYaw * sPitch * sRoll,  cYaw * cPitch, sYaw * sRoll - cYaw * sPitch * cRoll,
            -cPitch * sRoll,                       sPitch,       cPitch * cRoll
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
        lastTimestampNs = 0L
        currentRoll = 0f
        currentPitch = 0f
        currentYaw = 0f
        isInitialized = false
    }
}
