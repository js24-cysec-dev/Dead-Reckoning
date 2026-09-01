package com.example.dead_reckoning.domain.model

data class AccelerometerSample(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val timestampNs: Long = 0L,
    val accuracy: Int = 0
)

data class GyroscopeSample(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val timestampNs: Long = 0L,
    val accuracy: Int = 0
)

data class MagnetometerSample(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val timestampNs: Long = 0L,
    val accuracy: Int = 0
)

data class BarometerSample(
    val pressureHpa: Float = 0f,
    val relativeAltitudeMeters: Float = 0f,
    val timestampNs: Long = 0L,
    val accuracy: Int = 0
)
