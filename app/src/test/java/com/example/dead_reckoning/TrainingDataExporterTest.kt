package com.example.dead_reckoning

import com.example.dead_reckoning.data.csv.TrainingDataExporter
import com.example.dead_reckoning.data.csv.TrainingSample
import com.example.dead_reckoning.domain.model.CombinedTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingDataExporterTest {

    @Test
    fun csvHeader_containsExpectedColumns() {
        val header = TrainingDataExporter.CSV_HEADER
        assertTrue(header.contains("timestamp_ns"))
        assertTrue(header.contains("accel_variance"))
        assertTrue(header.contains("accel_energy"))
        assertTrue(header.contains("cadence_spm"))
        assertTrue(header.contains("gt_step_length"))
        assertTrue(header.contains("gt_heading_error"))
        assertTrue(header.contains("gt_position_error"))
    }

    @Test
    fun formatRow_generatesValidCsvRow() {
        val telemetry = CombinedTelemetry(
            timestampNs = 1000000000L,
            accelX = 0f,
            accelY = 0f,
            accelZ = 9.81f,
            gyroX = 0.1f,
            gyroY = 0.2f,
            gyroZ = 0.3f,
            magX = 20f,
            magY = 10f,
            magZ = -40f,
            pressure = 1013.25f,
            relativeAltitude = 5.0f,
            pitchRad = 0.1f,
            rollRad = 0.2f,
            yawRad = 0.3f,
            fusedHeadingDeg = 45f
        )

        val sample = TrainingSample(
            telemetry = telemetry,
            cadenceSpm = 110f,
            gtStepLength = 0.75f,
            gtHeadingError = 2.5f,
            gtPositionError = 1.2
        )

        val exporter = TrainingDataExporter(context = null)
        val row = exporter.formatRow(sample)

        val parts = row.split(",")
        assertEquals(22, parts.size)
        assertEquals("1000000000", parts[0])
        assertEquals("0.75", parts[19])
        assertEquals("2.5", parts[20])
        assertEquals("1.2", parts[21])
    }
}
