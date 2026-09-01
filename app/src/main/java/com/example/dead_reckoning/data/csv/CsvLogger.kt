package com.example.dead_reckoning.data.csv

import android.content.Context
import android.net.Uri
import com.example.dead_reckoning.domain.model.CombinedTelemetry
import com.example.dead_reckoning.utils.Constants
import com.example.dead_reckoning.utils.FileUtils
import com.example.dead_reckoning.utils.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicLong

class CsvLogger(private val context: Context) {

    private var outputStream: OutputStream? = null
    private var writer: BufferedWriter? = null
    private var currentFileUri: Uri? = null

    private val sampleChannel = Channel<CombinedTelemetry>(capacity = Channel.UNLIMITED)
    private var loggingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _recordedSampleCount = MutableStateFlow(0L)
    val recordedSampleCount: StateFlow<Long> = _recordedSampleCount.asStateFlow()

    private val counter = AtomicLong(0L)

    private var isLoggingActive = false

    @Synchronized
    fun startLogging(): String {
        if (isLoggingActive) return ""

        val timeStamp = TimeUtils.getCurrentTimestampString()
        val fileName = "AI_DR_$timeStamp.csv"

        val (uri, os) = FileUtils.createCsvOutputStream(context, fileName)
        currentFileUri = uri
        outputStream = os

        if (os != null) {
            val bufferedWriter = BufferedWriter(OutputStreamWriter(os))
            bufferedWriter.write(Constants.CSV_HEADER)
            bufferedWriter.newLine()
            bufferedWriter.flush()
            writer = bufferedWriter
        }

        counter.set(0L)
        _recordedSampleCount.value = 0L
        isLoggingActive = true

        startConsumerJob()
        return fileName
    }

    private fun startConsumerJob() {
        loggingJob?.cancel()
        loggingJob = scope.launch {
            for (telemetry in sampleChannel) {
                val currentWriter = writer ?: break
                try {
                    currentWriter.write(telemetry.toCsvRow())
                    currentWriter.newLine()
                    val count = counter.incrementAndGet()
                    if (count % 50L == 0L) {
                        currentWriter.flush()
                        _recordedSampleCount.value = count
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun logSample(telemetry: CombinedTelemetry) {
        if (isLoggingActive) {
            sampleChannel.trySend(telemetry)
        }
    }

    suspend fun stopLogging(): Long = withContext(Dispatchers.IO) {
        if (!isLoggingActive) return@withContext 0L

        isLoggingActive = false

        // Drain channel
        var item = sampleChannel.tryReceive().getOrNull()
        while (item != null) {
            try {
                writer?.write(item.toCsvRow())
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

        loggingJob?.cancel()
        loggingJob = null

        val finalCount = counter.get()
        _recordedSampleCount.value = finalCount
        return@withContext finalCount
    }
}
