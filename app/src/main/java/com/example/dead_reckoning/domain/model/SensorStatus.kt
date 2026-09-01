package com.example.dead_reckoning.domain.model

enum class SensorAvailability {
    ACTIVE,
    NO_DATA,
    NOT_AVAILABLE
}

enum class SensorType {
    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER,
    BAROMETER,
    GNSS
}

data class SensorStatusInfo(
    val type: SensorType,
    val name: String,
    val availability: SensorAvailability,
    val sampleRateHz: Float = 0f,
    val lastEventTimeNs: Long = 0L
)
