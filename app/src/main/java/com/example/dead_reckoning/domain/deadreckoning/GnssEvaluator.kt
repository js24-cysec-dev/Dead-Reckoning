package com.example.dead_reckoning.domain.deadreckoning

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * GNSS-Denied Manager & Drift Metric Evaluator.
 * Computes Haversine error, instant/mean/median/RMSE/max error, and handles GNSS-denied mode simulation.
 */
class GnssEvaluator(
    private val earthRadiusMeters: Double = 6_378_137.0
) {
    var isGnssDeniedMode: Boolean = false
        private set

    private val errorHistory = mutableListOf<Double>()
    private var totalDriftDistance: Double = 0.0
    private var gnssDeniedDuration: Double = 0.0
    private var totalSamples: Long = 0L

    private var groundTruthLat: Double = 0.0
    private var groundTruthLng: Double = 0.0

    fun setGnssDeniedMode(enabled: Boolean) {
        isGnssDeniedMode = enabled
    }

    /**
     * Calculates Haversine distance in meters between two geographical coordinates.
     */
    fun calculateHaversineDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        if (lat1 == 0.0 && lon1 == 0.0) return 0.0
        if (lat2 == 0.0 && lon2 == 0.0) return 0.0

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2.0).pow(2.0) +
                cos(rLat1) * cos(rLat2) * sin(dLon / 2.0).pow(2.0)
        val c = 2.0 * asin(sqrt(a).coerceAtMost(1.0))
        return earthRadiusMeters * c
    }

    /**
     * Evaluates estimation error against true GNSS position.
     */
    fun evaluate(
        estimatedLat: Double,
        estimatedLng: Double,
        trueLat: Double,
        trueLng: Double,
        stepLengthMeters: Float = 0f,
        dtSec: Double = 0.0
    ): DriftMetrics {
        if (trueLat != 0.0 || trueLng != 0.0) {
            groundTruthLat = trueLat
            groundTruthLng = trueLng
        }

        if (estimatedLat == 0.0 || estimatedLng == 0.0 || groundTruthLat == 0.0 || groundTruthLng == 0.0) {
            return getCurrentMetrics(0.0)
        }

        val instantError = calculateHaversineDistance(estimatedLat, estimatedLng, groundTruthLat, groundTruthLng)
        errorHistory.add(instantError)
        totalSamples++

        if (isGnssDeniedMode) {
            totalDriftDistance += stepLengthMeters.toDouble()
            gnssDeniedDuration += dtSec
        }

        return getCurrentMetrics(instantError)
    }

    /**
     * Calculates accumulated metric drift vector (dx, dy in meters) relative to true GNSS.
     */
    fun calculateRecorrectionVector(
        estimatedLat: Double,
        estimatedLng: Double,
        trueLat: Double,
        trueLng: Double
    ): Pair<Double, Double> {
        val latRad = Math.toRadians(trueLat)
        val dLat = Math.toRadians(trueLat - estimatedLat)
        val dLng = Math.toRadians(trueLng - estimatedLng)

        val dx = earthRadiusMeters * dLng * cos(latRad)
        val dy = earthRadiusMeters * dLat
        return Pair(dx, dy)
    }

    private fun getCurrentMetrics(instantError: Double): DriftMetrics {
        if (errorHistory.isEmpty()) {
            return DriftMetrics(
                instantErrorMeters = instantError,
                meanErrorMeters = instantError,
                medianErrorMeters = instantError,
                rmseMeters = instantError,
                maxErrorMeters = instantError,
                totalDriftDistanceMeters = totalDriftDistance,
                gnssDeniedDurationSec = gnssDeniedDuration,
                totalSamplesCount = totalSamples
            )
        }

        val mean = errorHistory.average()
        val sorted = errorHistory.sorted()
        val median = if (sorted.size % 2 == 1) {
            sorted[sorted.size / 2]
        } else {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
        val rmse = sqrt(errorHistory.sumOf { it * it } / errorHistory.size)
        val maxError = errorHistory.maxOrNull() ?: 0.0

        return DriftMetrics(
            instantErrorMeters = instantError,
            meanErrorMeters = mean,
            medianErrorMeters = median,
            rmseMeters = rmse,
            maxErrorMeters = maxError,
            totalDriftDistanceMeters = totalDriftDistance,
            gnssDeniedDurationSec = gnssDeniedDuration,
            totalSamplesCount = totalSamples
        )
    }

    fun reset() {
        errorHistory.clear()
        totalDriftDistance = 0.0
        gnssDeniedDuration = 0.0
        totalSamples = 0L
        groundTruthLat = 0.0
        groundTruthLng = 0.0
    }
}
