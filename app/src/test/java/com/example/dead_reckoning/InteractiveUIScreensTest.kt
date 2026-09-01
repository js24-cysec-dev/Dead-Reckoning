package com.example.dead_reckoning

import com.example.dead_reckoning.domain.deadreckoning.WeinbergStepLengthModel
import com.example.dead_reckoning.domain.model.CombinedTelemetry
import com.example.dead_reckoning.domain.model.ReferenceOrigin
import com.example.dead_reckoning.domain.model.TrajectoryPoint
import com.example.dead_reckoning.ui.experiments.ExperimentProtocol
import com.example.dead_reckoning.ui.experiments.ExperimentSessionReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractiveUIScreensTest {

    @Test
    fun experimentProtocols_containsTenProtocols() {
        val protocols = ExperimentProtocol.entries
        assertEquals(10, protocols.size)
        assertTrue(protocols.any { it.title == "Straight Walking" })
        assertTrue(protocols.any { it.title == "Walking with Turns" })
        assertTrue(protocols.any { it.title == "Slow Walking" })
        assertTrue(protocols.any { it.title == "Normal Walking" })
        assertTrue(protocols.any { it.title == "Fast Walking" })
        assertTrue(protocols.any { it.title == "Running" })
        assertTrue(protocols.any { it.title == "Indoor Walking" })
        assertTrue(protocols.any { it.title == "Outdoor Walking" })
        assertTrue(protocols.any { it.title == "GNSS-Denied Simulation" })
        assertTrue(protocols.any { it.title == "Stairs (Barometer)" })
    }

    @Test
    fun trajectoryPointAndOrigin_dataClassesHoldValues() {
        val origin = ReferenceOrigin(latitude = 37.7749, longitude = -122.4194, altitude = 10.0)
        val point = TrajectoryPoint(xMeters = 5.2, yMeters = -3.1, latitude = 37.7750, longitude = -122.4193, timestampNs = 123456789L)

        assertEquals(37.7749, origin.latitude, 0.0001)
        assertEquals(-122.4194, origin.longitude, 0.0001)

        assertEquals(5.2, point.xMeters, 0.01)
        assertEquals(-3.1, point.yMeters, 0.01)
        assertEquals(123456789L, point.timestampNs)
    }

    @Test
    fun experimentSessionReport_capturesMetadataCorrectly() {
        val report = ExperimentSessionReport(
            experimentId = "exp_001",
            protocol = ExperimentProtocol.STAIRS,
            startTimeMs = 1000L,
            endTimeMs = 60000L,
            durationFormatted = "00:00:59",
            subjectHeightCm = 180f,
            notes = "Climbing 3 flights of stairs",
            stepCount = 120L,
            distanceMeters = 85.0,
            driftRmseMeters = 1.2,
            maxErrorMeters = 2.5,
            csvFileName = "sensor_data_exp001.csv"
        )

        assertEquals("exp_001", report.experimentId)
        assertEquals(ExperimentProtocol.STAIRS, report.protocol)
        assertEquals("00:00:59", report.durationFormatted)
        assertEquals(180f, report.subjectHeightCm)
        assertEquals(120L, report.stepCount)
        assertEquals(85.0, report.distanceMeters, 0.01)
        assertEquals(1.2, report.driftRmseMeters, 0.01)
    }

    @Test
    fun stepLengthModel_kFactorAdjustsStepEstimate() {
        val modelDefault = WeinbergStepLengthModel(kFactor = 0.42f)
        val modelCalibrated = WeinbergStepLengthModel(kFactor = 0.50f)

        val slDefault = modelDefault.estimateStepLength(15f, 9f, 110f, com.example.dead_reckoning.domain.activity.ActivityType.WALKING)
        val slCalibrated = modelCalibrated.estimateStepLength(15f, 9f, 110f, com.example.dead_reckoning.domain.activity.ActivityType.WALKING)

        assertTrue(slCalibrated > slDefault)
    }
}
