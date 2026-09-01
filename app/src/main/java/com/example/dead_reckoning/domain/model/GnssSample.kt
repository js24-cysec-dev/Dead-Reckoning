package com.example.dead_reckoning.domain.model

data class GnssSample(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val speed: Float = 0f,
    val accuracy: Float = 0f,
    val bearing: Float = 0f,
    val timestampNs: Long = 0L
)
