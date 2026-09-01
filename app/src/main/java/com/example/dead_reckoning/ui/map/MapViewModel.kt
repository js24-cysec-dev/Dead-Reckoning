package com.example.dead_reckoning.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dead_reckoning.data.repository.TelemetryRepository
import com.example.dead_reckoning.domain.deadreckoning.DriftMetrics
import com.example.dead_reckoning.domain.model.CombinedTelemetry
import com.example.dead_reckoning.domain.model.ReferenceOrigin
import com.example.dead_reckoning.domain.model.TrajectoryPoint
import com.example.dead_reckoning.domain.sensorfusion.FusedPosition
import kotlinx.coroutines.flow.StateFlow

class MapViewModel(private val repository: TelemetryRepository) : ViewModel() {

    val gnssTrajectory: StateFlow<List<TrajectoryPoint>> = repository.gnssTrajectory
    val pdrTrajectory: StateFlow<List<TrajectoryPoint>> = repository.pdrTrajectory
    val fusedTrajectory: StateFlow<List<TrajectoryPoint>> = repository.fusedTrajectory
    val referenceOrigin: StateFlow<ReferenceOrigin?> = repository.referenceOrigin
    val driftMetrics: StateFlow<DriftMetrics> = repository.driftMetrics
    val isGnssDeniedMode: StateFlow<Boolean> = repository.isGnssDeniedMode
    val latestTelemetry: StateFlow<CombinedTelemetry> = repository.latestTelemetry
    val fusedPosition: StateFlow<FusedPosition> = repository.fusedPosition

    fun toggleGnssDeniedMode() {
        repository.setGnssDeniedMode(!isGnssDeniedMode.value)
    }

    fun resetTrajectories() {
        repository.resetTrajectories()
    }

    class Factory(private val repository: TelemetryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MapViewModel(repository) as T
        }
    }
}
