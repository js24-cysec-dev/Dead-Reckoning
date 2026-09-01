package com.example.dead_reckoning

import com.example.dead_reckoning.domain.deadreckoning.PdrEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI

class PdrEngineTest {

    private lateinit var pdrEngine: PdrEngine

    @Before
    fun setUp() {
        pdrEngine = PdrEngine()
    }

    @Test
    fun update_walkingNorth_incrementsYAxisPosition() {
        // Heading 0 rad = North (dx = 0, dy = step_length)
        val state = pdrEngine.update(
            stepDetected = true,
            stepLengthMeters = 1.0f,
            fusedHeadingRad = 0f
        )

        assertEquals(0.0, state.xMeters, 0.001)
        assertEquals(1.0, state.yMeters, 0.001)
        assertEquals(1.0, state.totalDistanceMeters, 0.001)
        assertEquals(1L, state.stepCount)
    }

    @Test
    fun update_walkingEast_incrementsXAxisPosition() {
        // Heading PI/2 rad = East (dx = step_length, dy = 0)
        val state = pdrEngine.update(
            stepDetected = true,
            stepLengthMeters = 0.8f,
            fusedHeadingRad = (PI / 2).toFloat()
        )

        assertEquals(0.8, state.xMeters, 0.001)
        assertEquals(0.0, state.yMeters, 0.001)
        assertEquals(0.8, state.totalDistanceMeters, 0.001)
    }

    @Test
    fun update_withGnssReference_convertsEnuToWgs84() {
        // Set reference origin at Googleplex (37.4220, -122.0841)
        pdrEngine.setGnssReference(37.4220, -122.0841)

        // Walk 100 meters North
        var state = pdrEngine.update(stepDetected = false, stepLengthMeters = 0f, fusedHeadingRad = 0f)
        repeat(100) {
            state = pdrEngine.update(stepDetected = true, stepLengthMeters = 1.0f, fusedHeadingRad = 0f)
        }

        assertEquals(0.0, state.xMeters, 0.01)
        assertEquals(100.0, state.yMeters, 0.01)
        assertTrue(state.latitude > 37.4220) // Moved North => Latitude increases
        assertEquals(-122.0841, state.longitude, 0.0001) // No East movement => Longitude stays same
    }

    @Test
    fun reset_clearsPositionAndReference() {
        pdrEngine.setGnssReference(37.4220, -122.0841)
        pdrEngine.update(stepDetected = true, stepLengthMeters = 1.0f, fusedHeadingRad = 0f)

        pdrEngine.reset()

        val state = pdrEngine.update(stepDetected = false, stepLengthMeters = 0f, fusedHeadingRad = 0f)
        assertEquals(0.0, state.xMeters, 0.001)
        assertEquals(0.0, state.yMeters, 0.001)
        assertEquals(0L, state.stepCount)
        assertFalse(state.isGnssRefSet)
    }
}
