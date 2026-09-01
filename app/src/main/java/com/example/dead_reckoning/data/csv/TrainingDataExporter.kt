package com.example.dead_reckoning.data.csv

import android.content.Context
import com.example.dead_reckoning.domain.model.CombinedTelemetry
import com.example.dead_reckoning.utils.FileUtils
import com.example.dead_reckoning.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.pow
import kotlin.math.sqrt

data class TrainingSample(
    val telemetry: CombinedTelemetry,
    val cadenceSpm: Float = 0f,
    val gtStepLength: Float = 0f,
    val gtHeadingError: Float = 0f,
    val gtPositionError: Double = 0.0
)

class TrainingDataExporter(private val context: Context? = null) {

    private var outputStream: OutputStream? = null
    private var writer: BufferedWriter? = null

    private val sampleChannel = Channel<TrainingSample>(capacity = Channel.UNLIMITED)
    private var exporterJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val counter = AtomicLong(0L)
    var isExporting: Boolean = false
        private set

    private val accelWindow = mutableListOf<Float>()
    private val windowSize = 50

    companion object {
        const val CSV_HEADER = "timestamp_ns,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z,mag_x,mag_y,mag_z,pressure,relative_altitude,accel_variance,accel_energy,cadence_spm,pitch_rad,roll_rad,yaw_rad,fused_heading_deg,gt_step_length,gt_heading_error,gt_position_error"
    }

    @Synchronized
    fun startExport(): String {
        if (isExporting) return ""
        val ctx = context ?: return ""

        val timeStamp = TimeUtils.getCurrentTimestampString()
        val fileName = "ml_training_$timeStamp.csv"

        val (_, os) = FileUtils.createCsvOutputStream(ctx, fileName)
        outputStream = os

        if (os != null) {
            val bufferedWriter = BufferedWriter(OutputStreamWriter(os))
            bufferedWriter.write(CSV_HEADER)
            bufferedWriter.newLine()
            bufferedWriter.flush()
            writer = bufferedWriter
        }

        counter.set(0L)
        synchronized(accelWindow) {
            accelWindow.clear()
        }
        isExporting = true

        startConsumerJob()
        return fileName
    }

    private fun startConsumerJob() {
        exporterJob?.cancel()
        exporterJob = scope.launch {
            for (sample in sampleChannel) {
                val currentWriter = writer ?: break
                try {
                    val row = formatRow(sample)
                    currentWriter.write(row)
                    currentWriter.newLine()
                    val count = counter.incrementAndGet()
                    if (count % 50L == 0L) {
                        currentWriter.flush()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun logSample(
        telemetry: CombinedTelemetry,
        cadenceSpm: Float = 0f,
        gtStepLength: Float = 0f,
        gtHeadingError: Float = 0f,
        gtPositionError: Double = 0.0
    ) {
        if (isExporting) {
            sampleChannel.trySend(
                TrainingSample(telemetry, cadenceSpm, gtStepLength, gtHeadingError, gtPositionError)
            )
        }
    }

    fun formatRow(sample: TrainingSample): String {
        val t = sample.telemetry
        val accelMag = sqrt((t.accelX * t.accelX + t.accelY * t.accelY + t.accelZ * t.accelZ).toDouble()).toFloat()

        synchronized(accelWindow) {
            accelWindow.add(accelMag)
            if (accelWindow.size > windowSize) {
                accelWindow.removeAt(0)
            }
        }

        val (variance, energy) = synchronized(accelWindow) {
            if (accelWindow.isEmpty()) Pair(0f, 0f)
            else {
                val mean = accelWindow.average().toFloat()
                val varSum = accelWindow.sumOf { (it - mean).toDouble().pow(2) }.toFloat() / accelWindow.size
                val energySum = accelWindow.sumOf { (it * it).toDouble() }.toFloat() / accelWindow.size
                Pair(varSum, energySum)
            }
        }

        return "${t.timestampNs}," +
                "${t.accelX},${t.accelY},${t.accelZ}," +
                "${t.gyroX},${t.gyroY},${t.gyroZ}," +
                "${t.magX},${t.magY},${t.magZ}," +
                "${t.pressure},${t.relativeAltitude}," +
                "$variance,$energy,${sample.cadenceSpm}," +
                "${t.pitchRad},${t.rollRad},${t.yawRad},${t.fusedHeadingDeg}," +
                "${sample.gtStepLength},${sample.gtHeadingError},${sample.gtPositionError}"
    }

    suspend fun stopExport(): Long = withContext(Dispatchers.IO) {
        if (!isExporting) return@withContext 0L

        isExporting = false

        var item = sampleChannel.tryReceive().getOrNull()
        while (item != null) {
            try {
                writer?.write(formatRow(item))
                writer?.newLine()
                counter.incrementAndGet()
            } catch (_: Exception) {}
            item = sampleChannel.tryReceive().getOrNull()
        }

        try {
            writer?.flush()
            writer?.close()
            outputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            writer = null
            outputStream = null
        }

        exporterJob?.cancel()
        exporterJob = null

        return@withContext counter.get()
    }
}
