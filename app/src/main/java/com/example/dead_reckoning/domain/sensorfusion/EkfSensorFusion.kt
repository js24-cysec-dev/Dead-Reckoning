package com.example.dead_reckoning.domain.sensorfusion

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class FusedPosition(
    val xMeters: Double = 0.0,
    val yMeters: Double = 0.0,
    val zMeters: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val velocityX: Double = 0.0,
    val velocityY: Double = 0.0,
    val headingRad: Float = 0f,
    val headingDeg: Float = 0f,
    val gyroBiasRad: Float = 0f,
    val positionUncertaintyMeters: Double = 0.0,
    val covariance: Array<DoubleArray> = MatrixUtils.identity(7)
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FusedPosition) return false
        return xMeters == other.xMeters &&
                yMeters == other.yMeters &&
                zMeters == other.zMeters &&
                latitude == other.latitude &&
                longitude == other.longitude &&
                altitude == other.altitude &&
                velocityX == other.velocityX &&
                velocityY == other.velocityY &&
                headingRad == other.headingRad &&
                headingDeg == other.headingDeg &&
                gyroBiasRad == other.gyroBiasRad &&
                positionUncertaintyMeters == other.positionUncertaintyMeters
    }

    override fun hashCode(): Int {
        var result = xMeters.hashCode()
        result = 31 * result + yMeters.hashCode()
        result = 31 * result + zMeters.hashCode()
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + altitude.hashCode()
        result = 31 * result + velocityX.hashCode()
        result = 31 * result + velocityY.hashCode()
        result = 31 * result + headingRad.hashCode()
        result = 31 * result + headingDeg.hashCode()
        result = 31 * result + gyroBiasRad.hashCode()
        result = 31 * result + positionUncertaintyMeters.hashCode()
        return result
    }
}

/**
 * Extended Kalman Filter (EKF) Sensor Fusion for dead reckoning and GNSS/Sensor integration.
 * State vector: [x, y, z, v_x, v_y, psi, b_psi]^T (7x1)
 */
class EkfSensorFusion(
    private val earthRadiusMeters: Double = 6_378_137.0
) {
    // State vector [x, y, z, v_x, v_y, psi, b_psi]
    private val state = DoubleArray(7)

    // Covariance matrix P (7x7)
    private var P = MatrixUtils.identity(7)

    var isReferenceSet: Boolean = false
        private set
    private var refLat: Double = 0.0
    private var refLng: Double = 0.0
    private var refAlt: Double = 0.0

    init {
        resetP()
    }

    private fun resetP() {
        P = MatrixUtils.identity(7)
        P[0][0] = 10.0 // x var
        P[1][1] = 10.0 // y var
        P[2][2] = 5.0  // z var
        P[3][3] = 1.0  // vx var
        P[4][4] = 1.0  // vy var
        P[5][5] = 0.1  // heading var
        P[6][6] = 0.01 // bias var
    }

    fun setReferenceOrigin(lat: Double, lng: Double, alt: Double = 0.0) {
        if (lat != 0.0 || lng != 0.0) {
            refLat = lat
            refLng = lng
            refAlt = alt
            isReferenceSet = true
        }
    }

    /**
     * Prediction step driven by dt, gyro rate, and/or PDR step propagation.
     */
    fun predict(
        dtSec: Double,
        gyroZ: Float = 0f,
        stepDetected: Boolean = false,
        stepLengthMeters: Float = 0f,
        pdrHeadingRad: Float = 0f
    ) {
        val dt = dtSec.coerceAtLeast(0.001)

        // Correction on heading by gyro bias
        val currentBias = state[6]
        val effectiveHeading = if (pdrHeadingRad != 0f) {
            pdrHeadingRad - currentBias.toFloat()
        } else {
            (state[5] + (gyroZ - currentBias) * dt).toFloat()
        }

        // Normalize heading to [-PI, PI]
        state[5] = normalizeAngleRad(effectiveHeading.toDouble())

        val F = MatrixUtils.identity(7)
        F[5][6] = -dt // psi wrt b_psi

        if (stepDetected && stepLengthMeters > 0f) {
            val sl = stepLengthMeters.toDouble()
            val dx = sl * sin(state[5])
            val dy = sl * cos(state[5])

            state[0] += dx
            state[1] += dy
            state[3] = dx / dt
            state[4] = dy / dt

            // Jacobian terms for step propagation
            F[0][5] = sl * cos(state[5])
            F[0][6] = -sl * cos(state[5])
            F[1][5] = -sl * sin(state[5])
            F[1][6] = sl * sin(state[5])
        } else {
            state[0] += state[3] * dt
            state[1] += state[4] * dt

            F[0][3] = dt
            F[1][4] = dt
        }

        // Process noise Q
        val Q = MatrixUtils.create(7, 7)
        Q[0][0] = 0.01 * dt
        Q[1][1] = 0.01 * dt
        Q[2][2] = 0.01 * dt
        Q[3][3] = 0.1 * dt
        Q[4][4] = 0.1 * dt
        Q[5][5] = 0.001 * dt
        Q[6][6] = 0.00001 * dt

        val FP = MatrixUtils.multiply(F, P)
        val FPFt = MatrixUtils.multiply(FP, MatrixUtils.transpose(F))
        P = MatrixUtils.add(FPFt, Q)
    }

    /**
     * GNSS measurement update for position and velocity.
     */
    fun updateGnss(
        lat: Double,
        lng: Double,
        alt: Double,
        speed: Float,
        accuracy: Float,
        bearingDeg: Float
    ) {
        if (!isReferenceSet) {
            setReferenceOrigin(lat, lng, alt)
        }

        val latRad = refLat * PI / 180.0
        val dLat = (lat - refLat) * PI / 180.0
        val dLng = (lng - refLng) * PI / 180.0

        val gnssX = earthRadiusMeters * dLng * cos(latRad)
        val gnssY = earthRadiusMeters * dLat
        val gnssZ = alt - refAlt

        val bearingRad = bearingDeg.toDouble() * PI / 180.0
        val gnssVx = speed.toDouble() * sin(bearingRad)
        val gnssVy = speed.toDouble() * cos(bearingRad)

        // Measurement vector z (5x1)
        val z = arrayOf(
            doubleArrayOf(gnssX),
            doubleArrayOf(gnssY),
            doubleArrayOf(gnssZ),
            doubleArrayOf(gnssVx),
            doubleArrayOf(gnssVy)
        )

        // H (5x7)
        val H = MatrixUtils.create(5, 7)
        H[0][0] = 1.0
        H[1][1] = 1.0
        H[2][2] = 1.0
        H[3][3] = 1.0
        H[4][4] = 1.0

        // Observation noise R (5x5)
        val varPos = accuracy.toDouble().coerceAtLeast(1.0).let { it * it }
        val R = MatrixUtils.create(5, 5)
        R[0][0] = varPos
        R[1][1] = varPos
        R[2][2] = varPos * 2.0
        R[3][3] = 1.0
        R[4][4] = 1.0

        performKalmanUpdate(z, H, R)
    }

    /**
     * Barometer measurement update for altitude.
     */
    fun updateBarometer(altMeters: Double, accuracyMeters: Double = 0.5) {
        val z = arrayOf(doubleArrayOf(altMeters))
        val H = MatrixUtils.create(1, 7)
        H[0][2] = 1.0

        val R = arrayOf(doubleArrayOf(accuracyMeters * accuracyMeters))
        performKalmanUpdate(z, H, R)
    }

    /**
     * ZUPT (Zero Velocity Update) applied when stationary.
     */
    fun updateZupt() {
        val z = arrayOf(
            doubleArrayOf(0.0),
            doubleArrayOf(0.0)
        )
        val H = MatrixUtils.create(2, 7)
        H[0][3] = 1.0
        H[1][4] = 1.0

        val R = MatrixUtils.create(2, 2)
        R[0][0] = 0.0001 // 0.01^2
        R[1][1] = 0.0001

        performKalmanUpdate(z, H, R)
    }

    /**
     * Trajectory re-correction reset when GNSS is restored.
     */
    fun recorrectPosition(trueLat: Double, trueLng: Double) {
        if (!isReferenceSet) {
            setReferenceOrigin(trueLat, trueLng)
            return
        }

        val latRad = refLat * PI / 180.0
        val dLat = (trueLat - refLat) * PI / 180.0
        val dLng = (trueLng - refLng) * PI / 180.0

        val targetX = earthRadiusMeters * dLng * cos(latRad)
        val targetY = earthRadiusMeters * dLat

        state[0] = targetX
        state[1] = targetY
        state[3] = 0.0
        state[4] = 0.0

        // Reset position covariance variance
        P[0][0] = 2.0
        P[1][1] = 2.0
    }

    private fun performKalmanUpdate(
        z: Array<DoubleArray>,
        H: Array<DoubleArray>,
        R: Array<DoubleArray>
    ) {
        val m = z.size // number of measurement dimensions
        val Hx = MatrixUtils.create(m, 1)

        for (i in 0 until m) {
            var sum = 0.0
            for (j in 0 until 7) {
                sum += H[i][j] * state[j]
            }
            Hx[i][0] = sum
        }

        // Residual y = z - Hx
        val y = MatrixUtils.subtract(z, Hx)

        // S = H * P * H^T + R
        val HP = MatrixUtils.multiply(H, P)
        val HPHt = MatrixUtils.multiply(HP, MatrixUtils.transpose(H))
        val S = MatrixUtils.add(HPHt, R)

        // S_inv
        val SInv = MatrixUtils.invert(S)

        // K = P * H^T * S_inv (7xm)
        val PHt = MatrixUtils.multiply(P, MatrixUtils.transpose(H))
        val K = MatrixUtils.multiply(PHt, SInv)

        // State update: x = x + K * y
        val Ky = MatrixUtils.multiply(K, y)
        for (i in 0 until 7) {
            state[i] += Ky[i][0]
        }

        // P = (I - K * H) * P
        val KH = MatrixUtils.multiply(K, H)
        val IKH = MatrixUtils.subtract(MatrixUtils.identity(7), KH)
        P = MatrixUtils.multiply(IKH, P)
    }

    fun getFusedPosition(): FusedPosition {
        val curLat: Double
        val curLng: Double

        if (isReferenceSet) {
            val deltaLatDeg = (state[1] / earthRadiusMeters) * (180.0 / PI)
            val latRad = refLat * PI / 180.0
            val deltaLngDeg = (state[0] / (earthRadiusMeters * cos(latRad))) * (180.0 / PI)

            curLat = refLat + deltaLatDeg
            curLng = refLng + deltaLngDeg
        } else {
            curLat = 0.0
            curLng = 0.0
        }

        val headingRad = state[5].toFloat()
        var headingDeg = (headingRad * 180.0 / PI).toFloat() % 360f
        if (headingDeg < 0f) headingDeg += 360f

        val posUncertainty = sqrt(P[0][0] + P[1][1])

        return FusedPosition(
            xMeters = state[0],
            yMeters = state[1],
            zMeters = state[2],
            latitude = curLat,
            longitude = curLng,
            altitude = refAlt + state[2],
            velocityX = state[3],
            velocityY = state[4],
            headingRad = headingRad,
            headingDeg = headingDeg,
            gyroBiasRad = state[6].toFloat(),
            positionUncertaintyMeters = posUncertainty,
            covariance = P
        )
    }

    fun reset() {
        for (i in 0 until 7) state[i] = 0.0
        resetP()
        isReferenceSet = false
        refLat = 0.0
        refLng = 0.0
        refAlt = 0.0
    }

    private fun normalizeAngleRad(angleRad: Double): Double {
        var a = angleRad % (2 * PI)
        if (a > PI) a -= 2 * PI
        if (a < -PI) a += 2 * PI
        return a
    }
}
