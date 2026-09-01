package com.example.dead_reckoning.ui.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dead_reckoning.domain.model.SensorAvailability
import com.example.dead_reckoning.domain.model.SensorStatusInfo
import com.example.dead_reckoning.domain.model.SensorType

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    modifier: Modifier = Modifier
) {
    val sensorStatusMap by viewModel.sensorStatusMap.collectAsStateWithLifecycle()
    val telemetry by viewModel.latestTelemetry.collectAsStateWithLifecycle()
    val validationSummary by viewModel.validationSummary.collectAsStateWithLifecycle()
    val qualityScore by viewModel.dataQualityScore.collectAsStateWithLifecycle()

    val accelStatus = sensorStatusMap[SensorType.ACCELEROMETER]
    val gyroStatus = sensorStatusMap[SensorType.GYROSCOPE]
    val magStatus = sensorStatusMap[SensorType.MAGNETOMETER]
    val baroStatus = sensorStatusMap[SensorType.BAROMETER]
    val gnssStatus = sensorStatusMap[SensorType.GNSS]

    val scrollState = rememberScrollState()

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dashboard Title Header
            Text(
                text = "Diagnostics Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // 1. Data Quality & Monotonicity Validation Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quality Score Gauge
                    val scoreColor = when {
                        qualityScore >= 90 -> Color(0xFF4CAF50)
                        qualityScore >= 70 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(80.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { qualityScore / 100f },
                            modifier = Modifier.fillMaxSize(),
                            color = scoreColor,
                            strokeWidth = 8.dp,
                            trackColor = scoreColor.copy(alpha = 0.2f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$qualityScore%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor
                            )
                            Text(
                                text = "Score",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.HealthAndSafety,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Data Health Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (validationSummary.isDataValid) "Data Stream Valid & Monotonic" else "Data Quality Issues Detected",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (validationSummary.isDataValid) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Monotonic Violations: ${validationSummary.monotonicityViolations}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Stuck Sensors: ${validationSummary.stuckSensorViolations}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Avg Frequency: %.1f Hz".format(validationSummary.averageSamplingRateHz),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Individual Sensor Telemetry Cards Section
            Text(
                text = "Live Sensor Telemetry",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // Accelerometer Card
            SensorTelemetryCard(
                title = "Accelerometer",
                sensorType = "TYPE_ACCELEROMETER",
                status = accelStatus,
                values = listOf(
                    "X" to "%.3f m/s²".format(telemetry.accelX),
                    "Y" to "%.3f m/s²".format(telemetry.accelY),
                    "Z" to "%.3f m/s²".format(telemetry.accelZ)
                ),
                accuracy = telemetry.sensorAccuracy,
                lastEventNs = accelStatus?.lastEventTimeNs ?: 0L,
                isStuck = validationSummary.stuckSensorViolations > 0
            )

            // Gyroscope Card
            SensorTelemetryCard(
                title = "Gyroscope",
                sensorType = "TYPE_GYROSCOPE",
                status = gyroStatus,
                values = listOf(
                    "X" to "%.3f rad/s".format(telemetry.gyroX),
                    "Y" to "%.3f rad/s".format(telemetry.gyroY),
                    "Z" to "%.3f rad/s".format(telemetry.gyroZ)
                ),
                accuracy = telemetry.sensorAccuracy,
                lastEventNs = gyroStatus?.lastEventTimeNs ?: 0L
            )

            // Magnetometer Card
            SensorTelemetryCard(
                title = "Magnetometer",
                sensorType = "TYPE_MAGNETIC_FIELD",
                status = magStatus,
                values = listOf(
                    "X" to "%.1f µT".format(telemetry.magX),
                    "Y" to "%.1f µT".format(telemetry.magY),
                    "Z" to "%.1f µT".format(telemetry.magZ)
                ),
                accuracy = telemetry.sensorAccuracy,
                lastEventNs = magStatus?.lastEventTimeNs ?: 0L,
                anomalyWarning = telemetry.magneticAnomaly
            )

            // Barometer Card
            SensorTelemetryCard(
                title = "Barometer",
                sensorType = "TYPE_PRESSURE",
                status = baroStatus,
                values = listOf(
                    "Pressure" to "%.2f hPa".format(telemetry.pressure),
                    "Rel Alt" to "%.2f m".format(telemetry.relativeAltitude)
                ),
                accuracy = telemetry.sensorAccuracy,
                lastEventNs = baroStatus?.lastEventTimeNs ?: 0L
            )

            // 3. GNSS Telemetry Status Card
            GnssTelemetryCard(
                status = gnssStatus,
                telemetry = telemetry
            )
        }
    }
}

@Composable
private fun SensorTelemetryCard(
    title: String,
    sensorType: String,
    status: SensorStatusInfo?,
    values: List<Pair<String, String>>,
    accuracy: Int,
    lastEventNs: Long,
    isStuck: Boolean = false,
    anomalyWarning: Boolean = false
) {
    val availability = status?.availability ?: SensorAvailability.NOT_AVAILABLE
    val (availText, availColor) = when (availability) {
        SensorAvailability.ACTIVE -> "ACTIVE" to Color(0xFF4CAF50)
        SensorAvailability.NO_DATA -> "NO DATA" to Color(0xFFFF9800)
        SensorAvailability.NOT_AVAILABLE -> "NOT AVAILABLE" to Color(0xFFF44336)
    }

    val accuracyText = when (accuracy) {
        3 -> "HIGH"
        2 -> "MEDIUM"
        1 -> "LOW"
        else -> "UNRELIABLE"
    }

    val nowNs = System.nanoTime()
    val delayMs = if (lastEventNs > 0) ((nowNs - lastEventNs) / 1_000_000L).coerceAtLeast(0L) else 0L

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = sensorType,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = availColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = availText,
                        color = availColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Values Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                values.forEach { (label, valStr) ->
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = valStr,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-metrics: Hz, Latency delay, Accuracy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rate: %.1f Hz".format(status?.sampleRateHz ?: 0f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Delay: ${delayMs}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Accuracy: $accuracyText",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (accuracy >= 2) Color(0xFF2E7D32) else Color(0xFFD84315)
                )
            }

            // Warning Badges if stuck or magnetic anomaly
            if (isStuck || anomalyWarning) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFD84315),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isStuck) "Warning: Stuck Sensor Readings Detected" else "Warning: Magnetic Anomaly Detected",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD84315),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun GnssTelemetryCard(
    status: SensorStatusInfo?,
    telemetry: com.example.dead_reckoning.domain.model.CombinedTelemetry
) {
    val isFixActive = (telemetry.latitude != 0.0 || telemetry.longitude != 0.0) && status?.availability == SensorAvailability.ACTIVE

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "GNSS Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GPS / GLONASS / Galileo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isFixActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Text(
                        text = if (isFixActive) "3D FIX ACTIVE" else "NO FIX / DENIED",
                        color = if (isFixActive) Color(0xFF2E7D32) else Color(0xFFC62828),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Coordinates Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Latitude", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "%.6f".format(telemetry.latitude), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = "Longitude", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "%.6f".format(telemetry.longitude), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = "Altitude", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "%.1f m".format(telemetry.altitude), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Speed, Bearing, Accuracy
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "%.2f m/s".format(telemetry.gpsSpeed), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = "Bearing", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "%.1f°".format(telemetry.gpsBearing), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text(text = "Accuracy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "±%.1f m".format(telemetry.gpsAccuracy), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
