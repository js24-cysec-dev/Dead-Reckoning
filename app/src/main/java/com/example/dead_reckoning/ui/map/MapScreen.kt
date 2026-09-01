package com.example.dead_reckoning.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dead_reckoning.domain.model.TrajectoryPoint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    val gnssTrajectory by viewModel.gnssTrajectory.collectAsStateWithLifecycle()
    val pdrTrajectory by viewModel.pdrTrajectory.collectAsStateWithLifecycle()
    val fusedTrajectory by viewModel.fusedTrajectory.collectAsStateWithLifecycle()
    val referenceOrigin by viewModel.referenceOrigin.collectAsStateWithLifecycle()
    val driftMetrics by viewModel.driftMetrics.collectAsStateWithLifecycle()
    val isGnssDenied by viewModel.isGnssDeniedMode.collectAsStateWithLifecycle()
    val telemetry by viewModel.latestTelemetry.collectAsStateWithLifecycle()
    val fusedPos by viewModel.fusedPosition.collectAsStateWithLifecycle()

    var zoomScale by remember { mutableFloatStateOf(12.0f) } // pixels per meter
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Interactive Canvas Map View
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(2.0f, 150.0f)
                        panOffset += pan
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val center = Offset(canvasWidth / 2f + panOffset.x, canvasHeight / 2f + panOffset.y)

            fun toCanvas(xMeters: Double, yMeters: Double): Offset {
                return Offset(
                    center.x + (xMeters * zoomScale).toFloat(),
                    center.y - (yMeters * zoomScale).toFloat()
                )
            }

            // 1. Draw Grid Lines (every 5 meters or 10 meters depending on scale)
            val gridStepMeters = if (zoomScale > 15f) 5.0 else if (zoomScale > 5f) 10.0 else 25.0
            val gridStepPx = (gridStepMeters * zoomScale).toFloat()

            val strokeWidthPx = 1.dp.toPx()
            val gridColor = Color.Gray.copy(alpha = 0.25f)
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GRAY
                textSize = 28f
            }

            // Vertical Grid Lines
            var currX = center.x % gridStepPx
            while (currX < canvasWidth) {
                drawLine(
                    color = gridColor,
                    start = Offset(currX, 0f),
                    end = Offset(currX, canvasHeight),
                    strokeWidth = strokeWidthPx
                )
                currX += gridStepPx
            }

            // Horizontal Grid Lines
            var currY = center.y % gridStepPx
            while (currY < canvasHeight) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, currY),
                    end = Offset(canvasWidth, currY),
                    strokeWidth = strokeWidthPx
                )
                currY += gridStepPx
            }

            // Axis Lines (0,0 Origin Crosshair Axes)
            drawLine(
                color = Color.Gray.copy(alpha = 0.4f),
                start = Offset(center.x, 0f),
                end = Offset(center.x, canvasHeight),
                strokeWidth = 2.dp.toPx()
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.4f),
                start = Offset(0f, center.y),
                end = Offset(canvasWidth, center.y),
                strokeWidth = 2.dp.toPx()
            )

            // Scale Bar Text
            drawContext.canvas.nativeCanvas.drawText(
                "Grid: ${gridStepMeters.toInt()}m | Scale: ${"%.1f".format(zoomScale)} px/m",
                20f,
                canvasHeight - 20f,
                textPaint
            )

            // 2. Draw Reference Origin Marker (0,0)
            val originPt = Offset(center.x, center.y)
            drawCircle(
                color = Color(0xFFFFC107),
                radius = 8.dp.toPx(),
                center = originPt
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = originPt
            )

            // 3. Draw Blue Path: GNSS Ground Truth Trajectory
            if (gnssTrajectory.size > 1) {
                val gnssPath = Path()
                gnssTrajectory.forEachIndexed { idx, pt ->
                    val cPt = toCanvas(pt.xMeters, pt.yMeters)
                    if (idx == 0) gnssPath.moveTo(cPt.x, cPt.y) else gnssPath.lineTo(cPt.x, cPt.y)
                }
                drawPath(
                    path = gnssPath,
                    color = Color(0xFF2196F3),
                    style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f))
                )
            }

            // 4. Draw Orange/Red Path: Pedestrian Dead Reckoning (DR) Trajectory
            if (pdrTrajectory.size > 1) {
                val pdrPath = Path()
                pdrTrajectory.forEachIndexed { idx, pt ->
                    val cPt = toCanvas(pt.xMeters, pt.yMeters)
                    if (idx == 0) pdrPath.moveTo(cPt.x, cPt.y) else pdrPath.lineTo(cPt.x, cPt.y)
                }
                drawPath(
                    path = pdrPath,
                    color = Color(0xFFFF5722),
                    style = Stroke(width = 3.5.dp.toPx())
                )
            }

            // 5. Draw Green Path: EKF Fused Position Trajectory
            if (fusedTrajectory.size > 1) {
                val fusedPath = Path()
                fusedTrajectory.forEachIndexed { idx, pt ->
                    val cPt = toCanvas(pt.xMeters, pt.yMeters)
                    if (idx == 0) fusedPath.moveTo(cPt.x, cPt.y) else fusedPath.lineTo(cPt.x, cPt.y)
                }
                drawPath(
                    path = fusedPath,
                    color = Color(0xFF4CAF50),
                    style = Stroke(width = 4.dp.toPx())
                )
            }

            // 6. Draw Current GNSS Marker & Accuracy Halo
            if (gnssTrajectory.isNotEmpty()) {
                val lastGnss = gnssTrajectory.last()
                val cGnss = toCanvas(lastGnss.xMeters, lastGnss.yMeters)
                if (!isGnssDenied && telemetry.gpsAccuracy > 0f) {
                    val haloRadius = (telemetry.gpsAccuracy * zoomScale).coerceAtLeast(8f)
                    drawCircle(
                        color = Color(0x332196F3),
                        radius = haloRadius,
                        center = cGnss
                    )
                }
                drawCircle(
                    color = Color(0xFF1976D2),
                    radius = 6.dp.toPx(),
                    center = cGnss
                )
            }

            // 7. Draw Current PDR Marker
            if (pdrTrajectory.isNotEmpty()) {
                val lastPdr = pdrTrajectory.last()
                val cPdr = toCanvas(lastPdr.xMeters, lastPdr.yMeters)
                drawCircle(
                    color = Color(0xFFE64A19),
                    radius = 7.dp.toPx(),
                    center = cPdr
                )
            }

            // 8. Draw Current Fused Marker + Uncertainty Halo + Heading Pointer
            val cFused = toCanvas(fusedPos.xMeters, fusedPos.yMeters)
            val uncertaintyRadius = (fusedPos.positionUncertaintyMeters.toFloat() * zoomScale).coerceAtLeast(10f)
            drawCircle(
                color = Color(0x334CAF50),
                radius = uncertaintyRadius,
                center = cFused
            )
            drawCircle(
                color = Color(0xFF388E3C),
                radius = 8.dp.toPx(),
                center = cFused
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = cFused
            )

            // Heading pointer line
            val headingRad = (fusedPos.headingDeg * PI / 180.0)
            val arrowLength = 20.dp.toPx()
            val arrowEnd = Offset(
                cFused.x + (arrowLength * sin(headingRad)).toFloat(),
                cFused.y - (arrowLength * cos(headingRad)).toFloat()
            )
            drawLine(
                color = Color(0xFF1B5E20),
                start = cFused,
                end = arrowEnd,
                strokeWidth = 3.5.dp.toPx()
            )
        }

        // Top Control Bar Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            // Active Mode Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { },
                            label = { Text(if (isGnssDenied) "GNSS: DENIED" else "GNSS: ACTIVE", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isGnssDenied) Color(0xFFFFEBEE) else Color(0xFFE3F2FD),
                                labelColor = if (isGnssDenied) Color(0xFFC62828) else Color(0xFF1565C0)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AssistChip(
                            onClick = { },
                            label = { Text("DR: ACTIVE", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFFBE9E7),
                                labelColor = Color(0xFFD84315)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AssistChip(
                            onClick = { },
                            label = { Text("FUSED: ACTIVE", fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFE8F5E9),
                                labelColor = Color(0xFF2E7D32)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            // GNSS-Denied Simulation Mode Alert Banner
            if (isGnssDenied) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GNSS-Denied Simulation Active",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Live GNSS feed disabled. App is navigating using PDR + EKF sensor fusion.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        // Map Legend Overlay (Top Right / Bottom Right)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                LegendItem(color = Color(0xFF2196F3), label = "GNSS Truth")
                Spacer(modifier = Modifier.height(4.dp))
                LegendItem(color = Color(0xFFFF5722), label = "PDR Trajectory")
                Spacer(modifier = Modifier.height(4.dp))
                LegendItem(color = Color(0xFF4CAF50), label = "EKF Fused")
                Spacer(modifier = Modifier.height(4.dp))
                LegendItem(color = Color(0xFFFFC107), label = "Origin Reference")
            }
        }

        // Floating Control Buttons (Right Side)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(
                onClick = {
                    panOffset = Offset(
                        - (fusedPos.xMeters * zoomScale).toFloat(),
                        (fusedPos.yMeters * zoomScale).toFloat()
                    )
                },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Re-center")
            }

            IconButton(
                onClick = { zoomScale = (zoomScale * 1.3f).coerceAtMost(150f) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In")
            }

            IconButton(
                onClick = { zoomScale = (zoomScale / 1.3f).coerceAtLeast(2f) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
            }

            IconButton(
                onClick = { viewModel.resetTrajectories() },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Trajectory")
            }
        }

        // Bottom Drift Metrics Panel & Toggle Actions Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            // Live Drift Metrics Panel
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Live Drift Metrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricItem("Instant Error", "%.2f m".format(driftMetrics.instantErrorMeters))
                        MetricItem("RMSE", "%.2f m".format(driftMetrics.rmseMeters))
                        MetricItem("Max Error", "%.2f m".format(driftMetrics.maxErrorMeters))
                        MetricItem("Total Distance", "%.1f m".format(driftMetrics.totalDriftDistanceMeters))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Toggle GNSS Denied Simulation
            OutlinedButton(
                onClick = { viewModel.toggleGnssDeniedMode() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isGnssDenied) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isGnssDenied) "Restore GNSS Signal" else "Simulate GNSS-Denied Environment"
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}