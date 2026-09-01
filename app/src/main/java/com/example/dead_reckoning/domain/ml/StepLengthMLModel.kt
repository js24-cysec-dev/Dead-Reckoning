package com.example.dead_reckoning.domain.ml

import com.example.dead_reckoning.domain.activity.ActivityType
import com.example.dead_reckoning.domain.deadreckoning.StepLengthModel
import com.example.dead_reckoning.domain.deadreckoning.WeinbergStepLengthModel

interface StepLengthMLModel : StepLengthModel {
    val modelStatus: MLModelStatus

    fun predictStepLength(
        peakAccel: Float,
        minAccel: Float,
        cadenceSpm: Float,
        activity: ActivityType,
        imuFeatures: List<Float> = emptyList()
    ): Float
}

class DefaultStepLengthMLModel(
    private val tfliteManager: TFLiteModelManager,
    private val fallbackModel: StepLengthModel = WeinbergStepLengthModel()
) : StepLengthMLModel {

    override val modelStatus: MLModelStatus
        get() = tfliteManager.getStepLengthModelStatus()

    override fun estimateStepLength(
        peakAccel: Float,
        minAccel: Float,
        cadenceSpm: Float,
        activity: ActivityType
    ): Float {
        return predictStepLength(peakAccel, minAccel, cadenceSpm, activity)
    }

    override fun predictStepLength(
        peakAccel: Float,
        minAccel: Float,
        cadenceSpm: Float,
        activity: ActivityType,
        imuFeatures: List<Float>
    ): Float {
        if (modelStatus == MLModelStatus.LOADED && imuFeatures.isNotEmpty()) {
            val mlPrediction = tfliteManager.runStepLengthInference(imuFeatures, cadenceSpm)
            if (mlPrediction > 0f) {
                return mlPrediction.coerceIn(0.35f, 1.6f)
            }
        }
        return fallbackModel.estimateStepLength(peakAccel, minAccel, cadenceSpm, activity)
    }
}
