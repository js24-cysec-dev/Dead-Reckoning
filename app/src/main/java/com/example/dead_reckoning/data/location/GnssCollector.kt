package com.example.dead_reckoning.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.dead_reckoning.domain.model.GnssSample
import com.example.dead_reckoning.domain.model.SensorAvailability
import com.example.dead_reckoning.domain.model.SensorStatusInfo
import com.example.dead_reckoning.domain.model.SensorType
import com.example.dead_reckoning.utils.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GnssCollector(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _gnssSample = MutableStateFlow<GnssSample?>(null)
    val gnssSample: StateFlow<GnssSample?> = _gnssSample.asStateFlow()

    private val _gnssStatus = MutableStateFlow(
        SensorStatusInfo(
            type = SensorType.GNSS,
            name = "GNSS / GPS",
            availability = SensorAvailability.NO_DATA
        )
    )
    val gnssStatus: StateFlow<SensorStatusInfo> = _gnssStatus.asStateFlow()

    private var lastFixTimeNs: Long = 0L

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation ?: return
            val nowNs = System.nanoTime()
            lastFixTimeNs = nowNs

            _gnssSample.value = GnssSample(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                speed = location.speed,
                accuracy = location.accuracy,
                bearing = location.bearing,
                timestampNs = location.elapsedRealtimeNanos
            )

            _gnssStatus.value = SensorStatusInfo(
                type = SensorType.GNSS,
                name = "GNSS / GPS",
                availability = SensorAvailability.ACTIVE,
                sampleRateHz = 1.0f,
                lastEventTimeNs = nowNs
            )
        }
    }

    fun startListening() {
        if (!hasLocationPermission()) {
            _gnssStatus.value = SensorStatusInfo(
                type = SensorType.GNSS,
                name = "GNSS / GPS",
                availability = SensorAvailability.NOT_AVAILABLE
            )
            return
        }

        try {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1000L
            ).setMinUpdateIntervalMillis(500L).build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            _gnssStatus.value = SensorStatusInfo(
                type = SensorType.GNSS,
                name = "GNSS / GPS",
                availability = SensorAvailability.NOT_AVAILABLE
            )
        }
    }

    fun stopListening() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {
            // Ignore
        }
    }

    fun checkStatus() {
        if (!hasLocationPermission()) {
            _gnssStatus.value = SensorStatusInfo(
                type = SensorType.GNSS,
                name = "GNSS / GPS",
                availability = SensorAvailability.NOT_AVAILABLE
            )
            return
        }

        val nowNs = System.nanoTime()
        val elapsedMs = (nowNs - lastFixTimeNs) / 1_000_000L
        if (lastFixTimeNs > 0 && elapsedMs < Constants.SENSOR_TIMEOUT_MS * 2) {
            _gnssStatus.value = SensorStatusInfo(
                type = SensorType.GNSS,
                name = "GNSS / GPS",
                availability = SensorAvailability.ACTIVE,
                sampleRateHz = 1.0f,
                lastEventTimeNs = lastFixTimeNs
            )
        } else {
            _gnssStatus.value = SensorStatusInfo(
                type = SensorType.GNSS,
                name = "GNSS / GPS",
                availability = SensorAvailability.NO_DATA
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineGranted || coarseGranted
    }
}
