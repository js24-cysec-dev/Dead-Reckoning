package com.example.dead_reckoning

import com.example.dead_reckoning.domain.activity.ActivityClassifier
import com.example.dead_reckoning.domain.activity.ActivityType
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ActivityClassifierTest {

    private lateinit var classifier: ActivityClassifier

    @Before
    fun setUp() {
        classifier = ActivityClassifier()
    }

    @Test
    fun classify_lowVariance_returnsStationary() {
        var activity = ActivityType.UNKNOWN
        // Feed 10 stationary samples with small noise
        for (i in 0..10) {
            val noise = (i % 2) * 0.02f
            activity = classifier.classify(linearAccelMagnitude = noise, cadenceSpm = 0f)
        }
        assertEquals(ActivityType.STATIONARY, activity)
    }

    @Test
    fun classify_moderateVarianceAndCadence_returnsWalking() {
        var activity = ActivityType.UNKNOWN
        // Feed walking-like signals
        for (i in 0..20) {
            val signal = if (i % 2 == 0) 2.5f else 0.2f
            activity = classifier.classify(linearAccelMagnitude = signal, cadenceSpm = 100f)
        }
        assertEquals(ActivityType.WALKING, activity)
    }

    @Test
    fun classify_highVarianceAndCadence_returnsRunning() {
        var activity = ActivityType.UNKNOWN
        // Feed running-like signals
        for (i in 0..20) {
            val signal = if (i % 2 == 0) 8.5f else 0.1f
            activity = classifier.classify(linearAccelMagnitude = signal, cadenceSpm = 160f)
        }
        assertEquals(ActivityType.RUNNING, activity)
    }
}
