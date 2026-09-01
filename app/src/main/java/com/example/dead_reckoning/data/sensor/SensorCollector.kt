package com.example.dead_reckoning.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.dead_reckoning.domain.model.AccelerometerSample
import com.example.dead_reckoning.domain.model.BarometerSample
import com.example.dead_reckoning.domain.model.GyroscopeSample
import com.example.dead_reckoning.domain.model.MagnetometerSample
import com.example.dead_reckoning.domain.model.SensorAvailability
import com.example.dead_reckoning.domain.model.SensorStatusInfo
import com.example.dead_reckoning.domain.model.SensorType
import com.example.dead_reckoning.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

class SensorCollector(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val baroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    private val _accelSample = MutableStateFlow(AccelerometerSample())
    val accelSample: StateFlow<AccelerometerSample> = _accelSample.asStateFlow()

    private val _gyroSample = MutableStateFlow(GyroscopeSample())
    val gyroSample: StateFlow<GyroscopeSample> = _gyroSample.asStateFlow()

    private val _magSample = MutableStateFlow(MagnetometerSample())
    val magSample: StateFlow<MagnetometerSample> = _magSample.asStateFlow()

    private val _baroSample = MutableStateFlow(BarometerSample())
    val baroSample: StateFlow<BarometerSample> = _baroSample.asStateFlow()

    private val _sensorStatuses = MutableStateFlow<Map<SensorType, SensorStatusInfo>>(emptyMap())
    val sensorStatuses: StateFlow<Map<SensorType, SensorStatusInfo>> = _sensorStatuses.asStateFlow()

    private var initialPressureHpa: Float? = null

    // Timestamp tracking for availability detection
    private var lastAccelNs: Long = 0L
    private var lastGyroNs: Long = 0L
    private var lastMagNs: Long = 0L
    private var lastBaroNs: Long = 0L

    private var accelCount = 0
    private var gyroCount = 0
    private var magCount = 0
    private var baroCount = 0

    private var monitorJob: Job? = null
    private val collectorScope = CoroutineScope(Dispatchers.Default)

    private var currentDelayUs: Int = Constants.SENSOR_DELAY_US
    private var isListening = false

    fun setSamplingRateHz(rateHz: Int) {
        val delayUs = when (rateHz) {
            100 -> 10_000
            20 -> 50_000
            else -> 20_000
        }
        currentDelayUs = delayUs
        if (isListening) {
            stopListening()
            startListening()
        }
    }

    fun startListening() {
        isListening = true
        accelSensor?.let { sensorManager.registerListener(this, it, currentDelayUs) }
        gyroSensor?.let { sensorManager.registerListener(this, it, currentDelayUs) }
        magSensor?.let { sensorManager.registerListener(this, it, currentDelayUs) }
        baroSensor?.let { sensorManager.registerListener(this, it, currentDelayUs) }

        startAvailabilityMonitor()
    }

    fun stopListening() {
        isListening = false
        sensorManager.unregisterListener(this)
        monitorJob?.cancel()
        monitorJob = null
    }

    private fun startAvailabilityMonitor() {
        monitorJob?.cancel()
        monitorJob = collectorScope.launch {
            var lastCheckTime = System.currentTimeMillis()

            while (isActive) {
                delay(1000L)
                val nowNs = System.nanoTime()
                val nowMs = System.currentTimeMillis()
                val deltaSec = (nowMs - lastCheckTime) / 1000f
                lastCheckTime = nowMs

                val accelRate = if (deltaSec > 0) accelCount / deltaSec else 0f
                val gyroRate = if (deltaSec > 0) gyroCount / deltaSec else 0f
                val magRate = if (deltaSec > 0) magCount / deltaSec else 0f
                val baroRate = if (deltaSec > 0) baroCount / deltaSec else 0f

                accelCount = 0
                gyroCount = 0
                magCount = 0
                baroCount = 0

                val statusMap = mutableMapOf<SensorType, SensorStatusInfo>()

                statusMap[SensorType.ACCELEROMETER] = createStatus(
                    type = SensorType.ACCELEROMETER,
                    name = accelSensor?.name ?: "Accelerometer",
                    hasHardware = accelSensor != null,
                    lastEventNs = lastAccelNs,
                    nowNs = nowNs,
                    sampleRateHz = accelRate
                )

                statusMap[SensorType.GYROSCOPE] = createStatus(
                    type = SensorType.GYROSCOPE,
                    name = gyroSensor?.name ?: "Gyroscope",
                    hasHardware = gyroSensor != null,
                    lastEventNs = lastGyroNs,
                    nowNs = nowNs,
                    sampleRateHz = gyroRate
                )

                statusMap[SensorType.MAGNETOMETER] = createStatus(
                    type = SensorType.MAGNETOMETER,
                    name = magSensor?.name ?: "Magnetometer",
                    hasHardware = magSensor != null,
                    lastEventNs = lastMagNs,
                    nowNs = nowNs,
                    sampleRateHz = magRate
                )

                statusMap[SensorType.BAROMETER] = createStatus(
                    type = SensorType.BAROMETER,
                    name = baroSensor?.name ?: "Barometer",
                    hasHardware = baroSensor != null,
                    lastEventNs = lastBaroNs,
                    nowNs = nowNs,
                    sampleRateHz = baroRate
                )

                _sensorStatuses.value = statusMap
            }
        }
    }

    private fun createStatus(
        type: SensorType,
        name: String,
        hasHardware: Boolean,
        lastEventNs: Long,
        nowNs: Long,
        sampleRateHz: Float
    ): SensorStatusInfo {
        if (!hasHardware) {
            return SensorStatusInfo(type, name, SensorAvailability.NOT_AVAILABLE)
        }
        val elapsedMs = (nowNs - lastEventNs) / 1_000_000L
        val availability = if (lastEventNs > 0 && elapsedMs < Constants.SENSOR_TIMEOUT_MS) {
            SensorAvailability.ACTIVE
        } else {
            SensorAvailability.NO_DATA
        }
        return SensorStatusInfo(type, name, availability, sampleRateHz, lastEventNs)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val nowNs = System.nanoTime()
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastAccelNs = nowNs
                accelCount++
                _accelSample.value = AccelerometerSample(
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                    timestampNs = event.timestamp,
                    accuracy = event.accuracy
                )
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastGyroNs = nowNs
                gyroCount++
                _gyroSample.value = GyroscopeSample(
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                    timestampNs = event.timestamp,
                    accuracy = event.accuracy
                )
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                lastMagNs = nowNs
                magCount++
                _magSample.value = MagnetometerSample(
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                    timestampNs = event.timestamp,
                    accuracy = event.accuracy
                )
            }
            Sensor.TYPE_PRESSURE -> {
                lastBaroNs = nowNs
                baroCount++
                val pressureHpa = event.values[0]
                if (initialPressureHpa == null && pressureHpa > 0) {
                    initialPressureHpa = pressureHpa
                }
                val baseline = initialPressureHpa ?: 1013.25f
                val relativeAltitude = 44330.0f * (1.0f - (pressureHpa / baseline).pow(1.0f / 5.255f))
                _baroSample.value = BarometerSample(
                    pressureHpa = pressureHpa,
                    relativeAltitudeMeters = relativeAltitude,
                    timestampNs = event.timestamp,
                    accuracy = event.accuracy
                )
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handled if needed
    }
}
