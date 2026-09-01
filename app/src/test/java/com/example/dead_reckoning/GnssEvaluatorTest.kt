package com.example.dead_reckoning

import com.example.dead_reckoning.domain.deadreckoning.GnssEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GnssEvaluatorTest {

    private lateinit var evaluator: GnssEvaluator

    @Before
    fun setUp() {
        evaluator = GnssEvaluator()
    }

    @Test
    fun calculateHaversineDistance_samePoint_returnsZero() {
        val distance = evaluator.calculateHaversineDistance(
            37.4220, -122.0841,
            37.4220, -122.0841
        )
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun calculateHaversineDistance_knownPoints_returnsAccurateDistance() {
        // Distance between Googleplex (37.4220, -122.0841) and nearby point ~111 meters North (0.001 deg lat)
        val distance = evaluator.calculateHaversineDistance(
            37.4220, -122.0841,
            37.4230, -122.0841
        )
        // 0.001 deg lat is approximately 111.2 meters
        assertEquals(111.2, distance, 1.0)
    }

    @Test
    fun evaluate_calculatesMeanMedianRmseMaxError() {
        val lat = 37.4220
        val lng = -122.0841

        // Sample 1: 0m error
        evaluator.evaluate(lat, lng, lat, lng)
        // Sample 2: ~111.2m error
        evaluator.evaluate(lat + 0.001, lng, lat, lng)

        val metrics = evaluator.evaluate(lat + 0.001, lng, lat, lng)

        assertEquals(3L, metrics.totalSamplesCount)
        assertTrue(metrics.meanErrorMeters > 0.0)
        assertTrue(metrics.rmseMeters >= metrics.meanErrorMeters)
        assertEquals(111.2, metrics.maxErrorMeters, 2.0)
    }

    @Test
    fun setGnssDeniedMode_accumulatesDriftDistanceAndDuration() {
        assertFalse(evaluator.isGnssDeniedMode)

        evaluator.setGnssDeniedMode(true)
        assertTrue(evaluator.isGnssDeniedMode)

        val metrics = evaluator.evaluate(
            estimatedLat = 37.4220,
            estimatedLng = -122.0841,
            trueLat = 37.4220,
            trueLng = -122.0841,
            stepLengthMeters = 0.8f,
            dtSec = 0.5
        )

        assertEquals(0.8, metrics.totalDriftDistanceMeters, 0.01)
        assertEquals(0.5, metrics.gnssDeniedDurationSec, 0.01)
    }

    @Test
    fun calculateRecorrectionVector_returnsExpectedDeltas() {
        // Estimated is south of True position by 0.001 deg lat
        val (dx, dy) = evaluator.calculateRecorrectionVector(
            estimatedLat = 37.4210,
            estimatedLng = -122.0841,
            trueLat = 37.4220,
            trueLng = -122.0841
        )

        assertEquals(0.0, dx, 0.1)
        assertEquals(111.2, dy, 1.0) // ~111.2 meters North correction needed
    }
}
