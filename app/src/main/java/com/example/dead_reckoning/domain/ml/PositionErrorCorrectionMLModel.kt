package com.example.dead_reckoning.domain.ml

interface PositionErrorCorrectionMLModel {
    val modelStatus: MLModelStatus

    fun predictPositionErrorOffset(
        pdrX: Double,
        pdrY: Double,
        imuSequence: List<Float> = emptyList()
    ): Pair<Double, Double>
}

class DefaultPositionErrorCorrectionMLModel(
    private val tfliteManager: TFLiteModelManager
) : PositionErrorCorrectionMLModel {

    override val modelStatus: MLModelStatus
        get() = tfliteManager.getPositionModelStatus()

    override fun predictPositionErrorOffset(
        pdrX: Double,
        pdrY: Double,
        imuSequence: List<Float>
    ): Pair<Double, Double> {
        if (modelStatus == MLModelStatus.LOADED && imuSequence.isNotEmpty()) {
            return tfliteManager.runPositionInference(imuSequence)
        }
        return Pair(0.0, 0.0)
    }
}
