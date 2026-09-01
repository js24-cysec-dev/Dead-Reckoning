package com.example.dead_reckoning.domain.deadreckoning

data class DriftMetrics(
    val instantErrorMeters: Double = 0.0,
    val meanErrorMeters: Double = 0.0,
    val medianErrorMeters: Double = 0.0,
    val rmseMeters: Double = 0.0,
    val maxErrorMeters: Double = 0.0,
    val totalDriftDistanceMeters: Double = 0.0,
    val gnssDeniedDurationSec: Double = 0.0,
    val totalSamplesCount: Long = 0L
)
