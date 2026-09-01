package com.example.dead_reckoning.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dead_reckoning.data.repository.TelemetryRepository
import com.example.dead_reckoning.domain.model.CombinedTelemetry
import com.example.dead_reckoning.domain.model.SensorStatusInfo
import com.example.dead_reckoning.domain.model.SensorType
import com.example.dead_reckoning.domain.model.ValidationSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DiagnosticsViewModel(private val repository: TelemetryRepository) : ViewModel() {

    val sensorStatusMap: StateFlow<Map<SensorType, SensorStatusInfo>> = repository.sensorStatusMap
    val latestTelemetry: StateFlow<CombinedTelemetry> = repository.latestTelemetry
    val validationSummary: StateFlow<ValidationSummary> = repository.validationSummary

    val dataQualityScore: StateFlow<Int> = repository.validationSummary.map { summary ->
        var score = 100
        if (!summary.isDataValid) score -= 25
        score -= (summary.monotonicityViolations * 10).toInt().coerceAtMost(30)
        score -= (summary.stuckSensorViolations * 15).toInt().coerceAtMost(30)
        score.coerceIn(0, 100)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100)

    class Factory(private val repository: TelemetryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DiagnosticsViewModel(repository) as T
        }
    }
}
