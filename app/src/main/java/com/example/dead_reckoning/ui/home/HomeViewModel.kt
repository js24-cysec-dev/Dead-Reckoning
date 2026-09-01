package com.example.dead_reckoning.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dead_reckoning.data.repository.TelemetryRepository
import com.example.dead_reckoning.domain.model.CombinedTelemetry
import com.example.dead_reckoning.domain.model.SensorStatusInfo
import com.example.dead_reckoning.domain.model.ValidationSummary
import com.example.dead_reckoning.service.TelemetryService
import com.example.dead_reckoning.utils.TimeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: TelemetryRepository) : ViewModel() {

    val isRecording: StateFlow<Boolean> = repository.isRecording

    val recordingDuration: StateFlow<String> = repository.recordingDurationMs
        .map { TimeUtils.formatDuration(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "00:00:00")

    val recordedSamplesCount: StateFlow<Long> = repository.recordedSamplesCount

    val recordedFilesCount: StateFlow<Int> = repository.recordedFilesCount

    val currentFileName: StateFlow<String> = repository.currentFileName

    val latestTelemetry: StateFlow<CombinedTelemetry> = repository.latestTelemetry

    val validationSummary: StateFlow<ValidationSummary> = repository.validationSummary

    val sensorStatusList: StateFlow<List<SensorStatusInfo>> = repository.sensorStatusMap
        .map { map -> map.values.toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        repository.startCollection()
        repository.refreshRecordedFilesCount()
    }

    fun toggleRecording(context: Context) {
        viewModelScope.launch {
            if (isRecording.value) {
                TelemetryService.stopService(context)
            } else {
                TelemetryService.startService(context)
            }
        }
    }

    fun refreshFileCount() {
        repository.refreshRecordedFilesCount()
    }

    class Factory(private val repository: TelemetryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
