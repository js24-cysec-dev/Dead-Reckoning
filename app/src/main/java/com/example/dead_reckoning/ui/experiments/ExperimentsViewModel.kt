package com.example.dead_reckoning.ui.experiments

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dead_reckoning.data.repository.TelemetryRepository
import com.example.dead_reckoning.domain.deadreckoning.DriftMetrics
import com.example.dead_reckoning.domain.model.CombinedTelemetry
import com.example.dead_reckoning.utils.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class ExperimentProtocol(
    val title: String,
    val description: String
) {
    STRAIGHT_WALKING("Straight Walking", "Walk in a straight line for 50 meters"),
    WALKING_WITH_TURNS("Walking with Turns", "Walk with 90° and 180° turns"),
    SLOW_WALKING("Slow Walking", "Walk at a slow pace (~0.8 m/s)"),
    NORMAL_WALKING("Normal Walking", "Standard comfortable walking pace (~1.2 m/s)"),
    FAST_WALKING("Fast Walking", "Brisk walking pace (~1.6 m/s)"),
    RUNNING("Running", "Jogging or running protocol (~2.5 m/s)"),
    INDOOR_WALKING("Indoor Walking", "Corridor or indoor room walking"),
    OUTDOOR_WALKING("Outdoor Walking", "Open outdoor sky walking with GNSS"),
    GNSS_DENIED_SIMULATION("GNSS-Denied Simulation", "Simulated loss of GNSS signal protocol"),
    STAIRS("Stairs (Barometer)", "Ascend/descend stairs tracking elevation")
}

data class ExperimentSessionReport(
    val experimentId: String,
    val protocol: ExperimentProtocol,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationFormatted: String,
    val subjectHeightCm: Float,
    val notes: String,
    val stepCount: Long,
    val distanceMeters: Double,
    val driftRmseMeters: Double,
    val maxErrorMeters: Double,
    val csvFileName: String
)

class ExperimentsViewModel(private val repository: TelemetryRepository) : ViewModel() {

    val isRecording: StateFlow<Boolean> = repository.isRecording
    val latestTelemetry: StateFlow<CombinedTelemetry> = repository.latestTelemetry
    val driftMetrics: StateFlow<DriftMetrics> = repository.driftMetrics

    val recordingDurationFormatted: StateFlow<String> = repository.recordingDurationMs
        .map { TimeUtils.formatDuration(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "00:00:00")

    private val _selectedProtocol = MutableStateFlow(ExperimentProtocol.NORMAL_WALKING)
    val selectedProtocol: StateFlow<ExperimentProtocol> = _selectedProtocol.asStateFlow()

    private val _subjectHeightCm = MutableStateFlow("175")
    val subjectHeightCm: StateFlow<String> = _subjectHeightCm.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _experimentHistory = MutableStateFlow<List<ExperimentSessionReport>>(emptyList())
    val experimentHistory: StateFlow<List<ExperimentSessionReport>> = _experimentHistory.asStateFlow()

    private val _latestReport = MutableStateFlow<ExperimentSessionReport?>(null)
    val latestReport: StateFlow<ExperimentSessionReport?> = _latestReport.asStateFlow()

    private val _exportStatusMessage = MutableStateFlow<String?>(null)
    val exportStatusMessage: StateFlow<String?> = _exportStatusMessage.asStateFlow()

    private var currentStartTimeMs: Long = 0L

    fun selectProtocol(protocol: ExperimentProtocol) {
        _selectedProtocol.value = protocol
    }

    fun updateSubjectHeight(height: String) {
        _subjectHeightCm.value = height
    }

    fun updateNotes(newNotes: String) {
        _notes.value = newNotes
    }

    fun clearExportStatus() {
        _exportStatusMessage.value = null
    }

    fun toggleExperiment(context: Context) {
        viewModelScope.launch {
            if (isRecording.value) {
                stopExperiment(context)
            } else {
                startExperiment(context)
            }
        }
    }

    private fun startExperiment(context: Context) {
        currentStartTimeMs = System.currentTimeMillis()
        if (_selectedProtocol.value == ExperimentProtocol.GNSS_DENIED_SIMULATION) {
            repository.setGnssDeniedMode(true)
        }
        repository.startRecording()
    }

    private suspend fun stopExperiment(context: Context) {
        val endTimeMs = System.currentTimeMillis()
        val durationMs = endTimeMs - currentStartTimeMs
        val fileName = repository.currentFileName.value
        val telemetry = latestTelemetry.value
        val metrics = driftMetrics.value

        repository.stopRecording()

        if (_selectedProtocol.value == ExperimentProtocol.GNSS_DENIED_SIMULATION) {
            repository.setGnssDeniedMode(false)
        }

        val heightVal = _subjectHeightCm.value.toFloatOrNull() ?: 175f

        val report = ExperimentSessionReport(
            experimentId = UUID.randomUUID().toString().take(8),
            protocol = _selectedProtocol.value,
            startTimeMs = currentStartTimeMs,
            endTimeMs = endTimeMs,
            durationFormatted = TimeUtils.formatDuration(durationMs),
            subjectHeightCm = heightVal,
            notes = _notes.value,
            stepCount = telemetry.stepCount,
            distanceMeters = metrics.totalDriftDistanceMeters,
            driftRmseMeters = metrics.rmseMeters,
            maxErrorMeters = metrics.maxErrorMeters,
            csvFileName = fileName
        )

        _latestReport.value = report
        _experimentHistory.value = listOf(report) + _experimentHistory.value
    }

    fun exportDataset() {
        viewModelScope.launch {
            val count = repository.recordedFilesCount.value
            _exportStatusMessage.value = "Dataset ready in Downloads/AI-DR/ ($count experiment files recorded)."
        }
    }

    class Factory(private val repository: TelemetryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExperimentsViewModel(repository) as T
        }
    }
}
