package com.example.dead_reckoning.domain.deadreckoning

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class PdrPositionState(
    val xMeters: Double = 0.0,
    val yMeters: Double = 0.0,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val totalDistanceMeters: Double = 0.0,
    val stepCount: Long = 0L,
    val isGnssRefSet: Boolean = false,
    val gnssRefLat: Double = 0.0,
    val gnssRefLng: Double = 0.0
)

/**
 * Pedestrian Dead Reckoning (PDR) Engine.
 * Maintains local metric coordinate position (x, y in meters) and WGS84 (Lat, Lng) estimation.
 */
class PdrEngine(
    private val earthRadiusMeters: Double = 6_378_137.0
) {
    private var xMeters: Double = 0.0
    private var yMeters: Double = 0.0
    private var totalDistance: Double = 0.0
    private var stepCount: Long = 0L

    private var gnssRefLat: Double = 0.0
    private var gnssRefLng: Double = 0.0
    private var isGnssRefSet: Boolean = false

    fun setGnssReference(lat: Double, lng: Double) {
        if (lat != 0.0 || lng != 0.0) {
            gnssRefLat = lat
            gnssRefLng = lng
            isGnssRefSet = true
        }
    }

    fun update(
        stepDetected: Boolean,
        stepLengthMeters: Float,
        fusedHeadingRad: Float
    ): PdrPositionState {
        if (stepDetected && stepLengthMeters > 0f) {
            val sl = stepLengthMeters.toDouble()

            // Heading angle theta in radians:
            // dx = step_length * sin(heading) (East displacement)
            // dy = step_length * cos(heading) (North displacement)
            val dx = sl * sin(fusedHeadingRad.toDouble())
            val dy = sl * cos(fusedHeadingRad.toDouble())

            xMeters += dx
            yMeters += dy
            totalDistance += sl
            stepCount++
        }

        // WGS84 Lat/Lng estimation from ENU displacement
        val currentLat: Double
        val currentLng: Double

        if (isGnssRefSet) {
            val deltaLatDeg = (yMeters / earthRadiusMeters) * (180.0 / PI)
            val latRad = gnssRefLat * PI / 180.0
            val deltaLngDeg = (xMeters / (earthRadiusMeters * cos(latRad))) * (180.0 / PI)

            currentLat = gnssRefLat + deltaLatDeg
            currentLng = gnssRefLng + deltaLngDeg
        } else {
            currentLat = 0.0
            currentLng = 0.0
        }

        return PdrPositionState(
            xMeters = xMeters,
            yMeters = yMeters,
            latitude = currentLat,
            longitude = currentLng,
            totalDistanceMeters = totalDistance,
            stepCount = stepCount,
            isGnssRefSet = isGnssRefSet,
            gnssRefLat = gnssRefLat,
            gnssRefLng = gnssRefLng
        )
    }

    fun reset() {
        xMeters = 0.0
        yMeters = 0.0
        totalDistance = 0.0
        stepCount = 0L
        isGnssRefSet = false
        gnssRefLat = 0.0
        gnssRefLng = 0.0
    }
}
