package com.example.dead_reckoning

import com.example.dead_reckoning.data.validation.DataQualityValidator
import com.example.dead_reckoning.domain.model.CombinedTelemetry
import com.example.dead_reckoning.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryUnitTest {

    @Test
    fun csvRowFormat_matchesStandardHeaderStructure() {
        val telemetry = CombinedTelemetry(
            timestampNs = 1000000000L,
            elapsedRealtimeNs = 2000000000L,
            wallTimeMs = 1725180000000L,
            accelX = 0.1f, accelY = -0.2f, accelZ = 9.81f,
            gyroX = 0.01f, gyroY = 0.02f, gyroZ = -0.01f,
            magX = 20.5f, magY = -15.2f, magZ = 45.0f,
            pressure = 1012.5f, relativeAltitude = 2.5f,
            latitude = 37.7749, longitude = -122.4194, altitude = 15.0,
            gpsSpeed = 1.2f, gpsAccuracy = 3.5f, gpsBearing = 180.0f,
            sensorAccuracy = 3,
            activity = "WALKING",
            stepDetected = true
        )

        val csvRow = telemetry.toCsvRow()
        val headerCols = Constants.CSV_HEADER.split(",")
        val rowCols = csvRow.split(",")

        assertEquals(headerCols.size, rowCols.size)
        assertEquals("1000000000", rowCols[0])
        assertEquals("2000000000", rowCols[1])
        assertEquals("1725180000000", rowCols[2])
        assertEquals("0.1", rowCols[3])
        assertEquals("-0.2", rowCols[4])
        assertEquals("9.81", rowCols[5])
        assertEquals("WALKING", rowCols[21])
        assertEquals("true", rowCols[22])
    }

    @Test
    fun validator_detectsMonotonicityViolation() {
        val validator = DataQualityValidator()

        val sample1 = CombinedTelemetry(elapsedRealtimeNs = 1000000000L)
        val sample2 = CombinedTelemetry(elapsedRealtimeNs = 900000000L) // Out of order timestamp

        val summary1 = validator.validateSample(sample1)
        assertEquals(0L, summary1.monotonicityViolations)
        assertTrue(summary1.isDataValid)

        val summary2 = validator.validateSample(sample2)
        assertEquals(1L, summary2.monotonicityViolations)
        assertFalse(summary2.isDataValid)
    }
}
