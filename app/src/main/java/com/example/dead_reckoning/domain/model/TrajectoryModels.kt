package com.example.dead_reckoning.domain.model

data class TrajectoryPoint(
    val xMeters: Double,
    val yMeters: Double,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestampNs: Long = 0L
)

data class ReferenceOrigin(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0
)
