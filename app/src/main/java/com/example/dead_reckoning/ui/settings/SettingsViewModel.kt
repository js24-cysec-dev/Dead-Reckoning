package com.example.dead_reckoning.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dead_reckoning.data.repository.TelemetryRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(private val repository: TelemetryRepository) : ViewModel() {

    val samplingRateHz: StateFlow<Int> = repository.samplingRateHz
    val stepFactorK: StateFlow<Float> = repository.stepFactorK
    val isGnssFusionEnabled: StateFlow<Boolean> = repository.isGnssFusionEnabled
    val isMlModelEnabled: StateFlow<Boolean> = repository.isMlModelEnabled
    val mapPathHistoryLimit: StateFlow<Int> = repository.mapPathHistoryLimit
    val recordedFilesCount: StateFlow<Int> = repository.recordedFilesCount

    fun setSamplingRateHz(rateHz: Int) {
        repository.setSamplingRateHz(rateHz)
    }

    fun setStepFactorK(k: Float) {
        repository.setStepFactorK(k)
    }

    fun setGnssFusionEnabled(enabled: Boolean) {
        repository.setGnssFusionEnabled(enabled)
    }

    fun setMlModelEnabled(enabled: Boolean) {
        repository.setMlModelEnabled(enabled)
    }

    fun setMapPathHistoryLimit(limit: Int) {
        repository.setMapPathHistoryLimit(limit)
    }

    fun refreshFileCount() {
        repository.refreshRecordedFilesCount()
    }

    class Factory(private val repository: TelemetryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repository) as T
        }
    }
}
