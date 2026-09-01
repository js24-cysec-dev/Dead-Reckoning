package com.example.dead_reckoning.domain.model

data class CombinedTelemetry(
    val timestampNs: Long = 0L,
    val elapsedRealtimeNs: Long = 0L,
    val wallTimeMs: Long = 0L,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val pressure: Float = 0f,
    val relativeAltitude: Float = 0f,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val gpsSpeed: Float = 0f,
    val gpsAccuracy: Float = 0f,
    val gpsBearing: Float = 0f,
    val sensorAccuracy: Int = 0,
    val activity: String = "STATIONARY",
    val stepDetected: Boolean = false,
    val rollRad: Float = 0f,
    val pitchRad: Float = 0f,
    val yawRad: Float = 0f,
    val fusedHeadingDeg: Float = 0f,
    val stepCount: Long = 0L,
    val stepLengthMeters: Float = 0f,
    val pdrXMeters: Double = 0.0,
    val pdrYMeters: Double = 0.0,
    val pdrLatitude: Double = 0.0,
    val pdrLongitude: Double = 0.0,
    val magneticAnomaly: Boolean = false
) {
    /**
     * CSV row representation matching Constants.CSV_HEADER
     */
    fun toCsvRow(): String {
        return "$timestampNs,$elapsedRealtimeNs,$wallTimeMs," +
                "$accelX,$accelY,$accelZ," +
                "$gyroX,$gyroY,$gyroZ," +
                "$magX,$magY,$magZ," +
                "$pressure,$relativeAltitude," +
                "$latitude,$longitude,$altitude," +
                "$gpsSpeed,$gpsAccuracy,$gpsBearing," +
                "$sensorAccuracy,$activity,$stepDetected," +
                "$rollRad,$pitchRad,$yawRad,$fusedHeadingDeg," +
                "$stepCount,$stepLengthMeters,$pdrXMeters,$pdrYMeters," +
                "$pdrLatitude,$pdrLongitude,$magneticAnomaly"
    }
}
