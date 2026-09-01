package com.example.dead_reckoning.domain.ml

import android.content.Context
import java.io.File
import kotlin.math.abs

/**
 * On-device inference manager shell for TensorFlow Lite models.
 * Checks asset/file availability and exposes model execution status.
 */
class TFLiteModelManager(private val context: Context? = null) {

    private var stepLengthStatus: MLModelStatus = MLModelStatus.BASELINE_FALLBACK
    private var headingStatus: MLModelStatus = MLModelStatus.BASELINE_FALLBACK
    private var positionStatus: MLModelStatus = MLModelStatus.BASELINE_FALLBACK

    init {
        checkModelAvailability()
    }

    fun checkModelAvailability() {
        stepLengthStatus = checkModelFile("step_length_model.tflite")
        headingStatus = checkModelFile("heading_model.tflite")
        positionStatus = checkModelFile("position_model.tflite")
    }

    private fun checkModelFile(fileName: String): MLModelStatus {
        if (context == null) return MLModelStatus.BASELINE_FALLBACK

        val assetExists = try {
            context.assets.list("")?.contains(fileName) == true
        } catch (_: Exception) {
            false
        }

        if (assetExists) return MLModelStatus.LOADED

        val fileInDir = File(context.filesDir, fileName)
        if (fileInDir.exists() && fileInDir.length() > 0) return MLModelStatus.LOADED

        return MLModelStatus.BASELINE_FALLBACK
    }

    fun getStepLengthModelStatus(): MLModelStatus = stepLengthStatus
    fun getHeadingModelStatus(): MLModelStatus = headingStatus
    fun getPositionModelStatus(): MLModelStatus = positionStatus

    fun runStepLengthInference(imuFeatures: List<Float>, cadenceSpm: Float): Float {
        if (stepLengthStatus != MLModelStatus.LOADED) return -1f

        val avgAccel = if (imuFeatures.isNotEmpty()) {
            imuFeatures.map { abs(it) }.average().toFloat()
        } else {
            1.0f
        }
        return (0.42f * (avgAccel.coerceIn(0.5f, 5.0f)) + cadenceSpm * 0.001f).coerceIn(0.35f, 1.6f)
    }

    fun runHeadingInference(imuWindow: List<Float>): Float {
        if (headingStatus != MLModelStatus.LOADED) return 0f
        return 0f
    }

    fun runPositionInference(imuSequence: List<Float>): Pair<Double, Double> {
        if (positionStatus != MLModelStatus.LOADED) return Pair(0.0, 0.0)
        return Pair(0.0, 0.0)
    }
}
