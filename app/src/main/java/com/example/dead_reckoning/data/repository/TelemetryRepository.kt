package com.example.dead_reckoning.data.repository

import android.content.Context
import com.example.dead_reckoning.data.csv.CsvLogger
import com.example.dead_reckoning.data.csv.TrainingDataExporter
import com.example.dead_reckoning.data.location.GnssCollector
import com.example.dead_reckoning.data.sensor.SensorCollector
import com.example.dead_reckoning.data.validation.DataQualityValidator
import com.example.dead_reckoning.domain.activity.ActivityType
import com.example.dead_reckoning.domain.deadreckoning.DriftMetrics
import com.example.dead_reckoning.domain.deadreckoning.GnssEvaluator
import com.example.dead_reckoning.domain.deadreckoning.PdrEstimator
import com.example.dead_reckoning.domain.ml.DefaultHeadingCorrectionMLModel
import com.example.dead_reckoning.domain.ml.DefaultPositionErrorCorrectionMLModel
import com.example.dead_reckoning.domain.ml.DefaultStepLengthMLModel
import com.example.dead_reckoning.domain.ml.MLModelStatus
import com.example.dead_reckoning.domain.ml.TFLiteModelManager
import com.example.dead_reckoning.domain.deadreckoning.WeinbergStepLengthModel
import com.example.dead_reckoning.domain.model.CombinedTelemetry
import com.example.dead_reckoning.domain.model.ReferenceOrigin
import com.example.dead_reckoning.domain.model.SensorStatusInfo
import com.example.dead_reckoning.domain.model.SensorType
import com.example.dead_reckoning.domain.model.TrajectoryPoint
import com.example.dead_reckoning.domain.model.ValidationSummary
import com.example.dead_reckoning.domain.sensorfusion.EkfSensorFusion
import com.example.dead_reckoning.domain.sensorfusion.FusedPosition
import com.example.dead_reckoning.utils.FileUtils
import com.example.dead_reckoning.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TelemetryRepository private constructor(private val context: Context) {

    private val sensorCollector = SensorCollector(context)
    private val gnssCollector = GnssCollector(context)
    private val csvLogger = CsvLogger(context)
    private val trainingExporter = TrainingDataExporter(context)
    private val validator = DataQualityValidator()
    private val pdrEstimator = PdrEstimator()

    val ekfSensorFusion = EkfSensorFusion()
    val gnssEvaluator = GnssEvaluator()

    val tfliteManager = TFLiteModelManager(context)
    val stepLengthMlModel = DefaultStepLengthMLModel(tfliteManager)
    val headingMlModel = DefaultHeadingCorrectionMLModel(tfliteManager)
    val positionMlModel = DefaultPositionErrorCorrectionMLModel(tfliteManager)

    private val repositoryScope = CoroutineScope(Dispatchers.Default)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isTrainingExporting = MutableStateFlow(false)
    val isTrainingExporting: StateFlow<Boolean> = _isTrainingExporting.asStateFlow()

    private val _isGnssDeniedMode = MutableStateFlow(false)
    val isGnssDeniedMode: StateFlow<Boolean> = _isGnssDeniedMode.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _recordedSamplesCount = MutableStateFlow(0L)
    val recordedSamplesCount: StateFlow<Long> = _recordedSamplesCount.asStateFlow()

    private val _sensorStatusMap = MutableStateFlow<Map<SensorType, SensorStatusInfo>>(emptyMap())
    val sensorStatusMap: StateFlow<Map<SensorType, SensorStatusInfo>> = _sensorStatusMap.asStateFlow()

    private val _latestTelemetry = MutableStateFlow(CombinedTelemetry())
    val latestTelemetry: StateFlow<CombinedTelemetry> = _latestTelemetry.asStateFlow()

    private val _fusedPosition = MutableStateFlow(FusedPosition())
    val fusedPosition: StateFlow<FusedPosition> = _fusedPosition.asStateFlow()

    private val _driftMetrics = MutableStateFlow(DriftMetrics())
    val driftMetrics: StateFlow<DriftMetrics> = _driftMetrics.asStateFlow()

    private val _mlModelStatus = MutableStateFlow(MLModelStatus.BASELINE_FALLBACK)
    val mlModelStatus: StateFlow<MLModelStatus> = _mlModelStatus.asStateFlow()

    private val _recordedFilesCount = MutableStateFlow(0)
    val recordedFilesCount: StateFlow<Int> = _recordedFilesCount.asStateFlow()

    private val _validationSummary = MutableStateFlow(ValidationSummary())
    val validationSummary: StateFlow<ValidationSummary> = _validationSummary.asStateFlow()

    private val _currentFileName = MutableStateFlow("")
    val currentFileName: StateFlow<String> = _currentFileName.asStateFlow()

    // Settings StateFlows
    private val _samplingRateHz = MutableStateFlow(50)
    val samplingRateHz: StateFlow<Int> = _samplingRateHz.asStateFlow()

    private val _stepFactorK = MutableStateFlow(0.42f)
    val stepFactorK: StateFlow<Float> = _stepFactorK.asStateFlow()

    private val _isGnssFusionEnabled = MutableStateFlow(true)
    val isGnssFusionEnabled: StateFlow<Boolean> = _isGnssFusionEnabled.asStateFlow()

    private val _isMlModelEnabled = MutableStateFlow(false)
    val isMlModelEnabled: StateFlow<Boolean> = _isMlModelEnabled.asStateFlow()

    private val _mapPathHistoryLimit = MutableStateFlow(1000)
    val mapPathHistoryLimit: StateFlow<Int> = _mapPathHistoryLimit.asStateFlow()

    // Trajectory History StateFlows
    private val _gnssTrajectory = MutableStateFlow<List<TrajectoryPoint>>(emptyList())
    val gnssTrajectory: StateFlow<List<TrajectoryPoint>> = _gnssTrajectory.asStateFlow()

    private val _pdrTrajectory = MutableStateFlow<List<TrajectoryPoint>>(emptyList())
    val pdrTrajectory: StateFlow<List<TrajectoryPoint>> = _pdrTrajectory.asStateFlow()

    private val _fusedTrajectory = MutableStateFlow<List<TrajectoryPoint>>(emptyList())
    val fusedTrajectory: StateFlow<List<TrajectoryPoint>> = _fusedTrajectory.asStateFlow()

    private val _referenceOrigin = MutableStateFlow<ReferenceOrigin?>(null)
    val referenceOrigin: StateFlow<ReferenceOrigin?> = _referenceOrigin.asStateFlow()

    private var recordingTimerJob: Job? = null
    private var telemetryCombineJob: Job? = null
    private var lastTimestampNs: Long = 0L

    init {
        refreshRecordedFilesCount()
        _mlModelStatus.value = tfliteManager.getStepLengthModelStatus()
        startTelemetryCombining()
        startStatusMonitoring()
    }

    fun setGnssDeniedMode(enabled: Boolean) {
        val wasEnabled = _isGnssDeniedMode.value
        _isGnssDeniedMode.value = enabled
        gnssEvaluator.setGnssDeniedMode(enabled)

        if (wasEnabled && !enabled) {
            // Restored GNSS -> Trajectory Re-correction
            val currentGnss = gnssCollector.gnssSample.value
            if (currentGnss != null && (currentGnss.latitude != 0.0 || currentGnss.longitude != 0.0)) {
                ekfSensorFusion.recorrectPosition(currentGnss.latitude, currentGnss.longitude)
            }
        }
    }

    fun setSamplingRateHz(rateHz: Int) {
        _samplingRateHz.value = rateHz
        sensorCollector.setSamplingRateHz(rateHz)
    }

    fun setStepFactorK(k: Float) {
        _stepFactorK.value = k
        if (!_isMlModelEnabled.value) {
            pdrEstimator.stepLengthEstimator.model = WeinbergStepLengthModel(k)
        }
    }

    fun setGnssFusionEnabled(enabled: Boolean) {
        _isGnssFusionEnabled.value = enabled
    }

    fun setMlModelEnabled(enabled: Boolean) {
        _isMlModelEnabled.value = enabled
        if (enabled) {
            pdrEstimator.stepLengthEstimator.model = stepLengthMlModel
        } else {
            pdrEstimator.stepLengthEstimator.model = WeinbergStepLengthModel(_stepFactorK.value)
        }
    }

    fun setMapPathHistoryLimit(limit: Int) {
        _mapPathHistoryLimit.value = limit
        trimTrajectoryBuffers(limit)
    }

    fun resetTrajectories() {
        _gnssTrajectory.value = emptyList()
        _pdrTrajectory.value = emptyList()
        _fusedTrajectory.value = emptyList()
    }

    private fun trimTrajectoryBuffers(limit: Int) {
        if (_gnssTrajectory.value.size > limit) {
            _gnssTrajectory.value = _gnssTrajectory.value.takeLast(limit)
        }
        if (_pdrTrajectory.value.size > limit) {
            _pdrTrajectory.value = _pdrTrajectory.value.takeLast(limit)
        }
        if (_fusedTrajectory.value.size > limit) {
            _fusedTrajectory.value = _fusedTrajectory.value.takeLast(limit)
        }
    }

    private fun appendTrajectoryPoint(
        flow: MutableStateFlow<List<TrajectoryPoint>>,
        point: TrajectoryPoint
    ) {
        val limit = _mapPathHistoryLimit.value
        val list = flow.value.toMutableList()
        list.add(point)
        if (list.size > limit) {
            flow.value = list.takeLast(limit)
        } else {
            flow.value = list
        }
    }

    fun startCollection() {
        sensorCollector.startListening()
        gnssCollector.startListening()
    }

    fun stopCollection() {
        sensorCollector.stopListening()
        gnssCollector.stopListening()
    }

    fun refreshRecordedFilesCount() {
        _recordedFilesCount.value = FileUtils.countCsvFiles(context)
    }

    private fun startStatusMonitoring() {
        repositoryScope.launch {
            combine(
                sensorCollector.sensorStatuses,
                gnssCollector.gnssStatus
            ) { sensorMap, gnssStatus ->
                val combinedMap = sensorMap.toMutableMap()
                combinedMap[SensorType.GNSS] = gnssStatus
                combinedMap
            }.collect { combinedStatusMap ->
                _sensorStatusMap.value = combinedStatusMap
            }
        }

        repositoryScope.launch {
            while (isActive) {
                delay(1000L)
                gnssCollector.checkStatus()
            }
        }
    }

    private fun startTelemetryCombining() {
        telemetryCombineJob = repositoryScope.launch {
            combine(
                sensorCollector.accelSample,
                sensorCollector.gyroSample,
                sensorCollector.magSample,
                sensorCollector.baroSample
            ) { accel, gyro, mag, baro ->
                val gnss = gnssCollector.gnssSample.value
                val wallMs = System.currentTimeMillis()
                val elapsedRealtimeNs = android.os.SystemClock.elapsedRealtimeNanos()
                val timestampNs = TimeUtils.elapsedRealtimeNanosToTimestampNs(elapsedRealtimeNs)

                val dtSec = if (lastTimestampNs > 0L) {
                    (timestampNs - lastTimestampNs) / 1_000_000_000.0
                } else {
                    0.02
                }
                lastTimestampNs = timestampNs

                val rawGnssLat = gnss?.latitude ?: 0.0
                val rawGnssLng = gnss?.longitude ?: 0.0
                val rawGnssAlt = gnss?.altitude ?: 0.0
                val rawGnssSpeed = gnss?.speed ?: 0f
                val rawGnssAccuracy = gnss?.accuracy ?: 0f
                val rawGnssBearing = gnss?.bearing ?: 0f

                // In GNSS-Denied mode, simulate loss of GNSS to state estimators
                val feedGnssLat = if (_isGnssDeniedMode.value) 0.0 else rawGnssLat
                val feedGnssLng = if (_isGnssDeniedMode.value) 0.0 else rawGnssLng

                if (feedGnssLat != 0.0 || feedGnssLng != 0.0) {
                    if (_referenceOrigin.value == null) {
                        _referenceOrigin.value = ReferenceOrigin(feedGnssLat, feedGnssLng, rawGnssAlt)
                    }
                    ekfSensorFusion.setReferenceOrigin(feedGnssLat, feedGnssLng, rawGnssAlt)
                    if (_isGnssFusionEnabled.value) {
                        ekfSensorFusion.updateGnss(
                            lat = feedGnssLat,
                            lng = feedGnssLng,
                            alt = rawGnssAlt,
                            speed = rawGnssSpeed,
                            accuracy = rawGnssAccuracy,
                            bearingDeg = rawGnssBearing
                        )
                    }

                    val ref = _referenceOrigin.value
                    if (ref != null) {
                        val earthR = 6378137.0
                        val latRad = Math.toRadians(ref.latitude)
                        val dLatRad = Math.toRadians(rawGnssLat - ref.latitude)
                        val dLngRad = Math.toRadians(rawGnssLng - ref.longitude)
                        val gnssX = earthR * dLngRad * Math.cos(latRad)
                        val gnssY = earthR * dLatRad
                        appendTrajectoryPoint(_gnssTrajectory, TrajectoryPoint(gnssX, gnssY, rawGnssLat, rawGnssLng, timestampNs))
                    }
                }

                // Process PDR estimation
                val pdrResult = pdrEstimator.processSample(
                    accel = accel,
                    gyro = gyro,
                    mag = mag,
                    timestampNs = timestampNs,
                    gnssLat = feedGnssLat,
                    gnssLng = feedGnssLng
                )
                appendTrajectoryPoint(
                    _pdrTrajectory,
                    TrajectoryPoint(
                        pdrResult.positionState.xMeters,
                        pdrResult.positionState.yMeters,
                        pdrResult.positionState.latitude,
                        pdrResult.positionState.longitude,
                        timestampNs
                    )
                )

                // Run EKF Prediction
                ekfSensorFusion.predict(
                    dtSec = dtSec,
                    gyroZ = gyro.z,
                    stepDetected = pdrResult.stepDetectionResult.stepDetected,
                    stepLengthMeters = pdrResult.stepLengthMeters,
                    pdrHeadingRad = pdrResult.headingState.fusedHeadingRad
                )

                // Barometer update
                if (baro.pressureHpa > 0f) {
                    ekfSensorFusion.updateBarometer(baro.relativeAltitudeMeters.toDouble())
                }

                // ZUPT update when stationary
                if (pdrResult.activity == ActivityType.STATIONARY) {
                    ekfSensorFusion.updateZupt()
                }

                val currentFusedPos = ekfSensorFusion.getFusedPosition()
                _fusedPosition.value = currentFusedPos
                appendTrajectoryPoint(
                    _fusedTrajectory,
                    TrajectoryPoint(
                        currentFusedPos.xMeters,
                        currentFusedPos.yMeters,
                        currentFusedPos.latitude,
                        currentFusedPos.longitude,
                        timestampNs
                    )
                )

                // Evaluate drift metrics against ground truth
                val metrics = gnssEvaluator.evaluate(
                    estimatedLat = currentFusedPos.latitude,
                    estimatedLng = currentFusedPos.longitude,
                    trueLat = rawGnssLat,
                    trueLng = rawGnssLng,
                    stepLengthMeters = pdrResult.stepLengthMeters,
                    dtSec = dtSec
                )
                _driftMetrics.value = metrics

                val telemetry = CombinedTelemetry(
                    timestampNs = timestampNs,
                    elapsedRealtimeNs = elapsedRealtimeNs,
                    wallTimeMs = wallMs,
                    accelX = accel.x,
                    accelY = accel.y,
                    accelZ = accel.z,
                    gyroX = gyro.x,
                    gyroY = gyro.y,
                    gyroZ = gyro.z,
                    magX = mag.x,
                    magY = mag.y,
                    magZ = mag.z,
                    pressure = baro.pressureHpa,
                    relativeAltitude = baro.relativeAltitudeMeters,
                    latitude = feedGnssLat,
                    longitude = feedGnssLng,
                    altitude = rawGnssAlt,
                    gpsSpeed = rawGnssSpeed,
                    gpsAccuracy = rawGnssAccuracy,
                    gpsBearing = rawGnssBearing,
                    sensorAccuracy = accel.accuracy,
                    activity = pdrResult.activity.name,
                    stepDetected = pdrResult.stepDetectionResult.stepDetected,
                    rollRad = pdrResult.orientationState.rollRad,
                    pitchRad = pdrResult.orientationState.pitchRad,
                    yawRad = pdrResult.orientationState.yawRad,
                    fusedHeadingDeg = pdrResult.headingState.fusedHeadingDeg,
                    stepCount = pdrResult.positionState.stepCount,
                    stepLengthMeters = pdrResult.stepLengthMeters,
                    pdrXMeters = pdrResult.positionState.xMeters,
                    pdrYMeters = pdrResult.positionState.yMeters,
                    pdrLatitude = pdrResult.positionState.latitude,
                    pdrLongitude = pdrResult.positionState.longitude,
                    magneticAnomaly = pdrResult.headingState.magneticAnomaly
                )

                if (trainingExporter.isExporting) {
                    val headingError = (pdrResult.headingState.fusedHeadingDeg - rawGnssBearing)
                    trainingExporter.logSample(
                        telemetry = telemetry,
                        cadenceSpm = pdrResult.stepDetectionResult.cadenceSpm,
                        gtStepLength = pdrResult.stepLengthMeters,
                        gtHeadingError = headingError,
                        gtPositionError = metrics.instantErrorMeters
                    )
                }

                telemetry
            }.collect { telemetry ->
                _latestTelemetry.value = telemetry

                if (_isRecording.value) {
                    csvLogger.logSample(telemetry)
                    val summary = validator.validateSample(telemetry)
                    _validationSummary.value = summary
                    _recordedSamplesCount.value = summary.totalSamples
                }
            }
        }
    }

    fun startRecording(): String {
        if (_isRecording.value) return _currentFileName.value

        startCollection()
        pdrEstimator.reset()
        ekfSensorFusion.reset()
        gnssEvaluator.reset()
        validator.reset()

        val fileName = csvLogger.startLogging()
        _currentFileName.value = fileName
        _isRecording.value = true

        val startTime = System.currentTimeMillis()
        recordingTimerJob?.cancel()
        recordingTimerJob = repositoryScope.launch {
            while (isActive && _isRecording.value) {
                _recordingDurationMs.value = System.currentTimeMillis() - startTime
                delay(200L)
            }
        }

        return fileName
    }

    suspend fun stopRecording(): Long {
        if (!_isRecording.value) return 0L

        _isRecording.value = false
        recordingTimerJob?.cancel()
        recordingTimerJob = null

        val totalSamples = csvLogger.stopLogging()
        refreshRecordedFilesCount()

        return totalSamples
    }

    fun startTrainingExport(): String {
        startCollection()
        val name = trainingExporter.startExport()
        _isTrainingExporting.value = true
        return name
    }

    suspend fun stopTrainingExport(): Long {
        val samples = trainingExporter.stopExport()
        _isTrainingExporting.value = false
        return samples
    }

    companion object {
        @Volatile
        private var INSTANCE: TelemetryRepository? = null

        fun getInstance(context: Context): TelemetryRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TelemetryRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
