package com.example.dead_reckoning.domain.model

data class ValidationSummary(
    val totalSamples: Long = 0L,
    val monotonicityViolations: Long = 0L,
    val stuckSensorViolations: Long = 0L,
    val averageSamplingRateHz: Float = 0f,
    val isDataValid: Boolean = true,
    val issues: List<String> = emptyList()
)
