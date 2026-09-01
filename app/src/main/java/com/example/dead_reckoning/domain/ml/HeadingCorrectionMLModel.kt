package com.example.dead_reckoning.domain.ml

interface HeadingCorrectionMLModel {
    val modelStatus: MLModelStatus

    fun predictHeadingCorrection(
        fusedHeadingDeg: Float,
        imuWindow: List<Float> = emptyList()
    ): Float
}

class DefaultHeadingCorrectionMLModel(
    private val tfliteManager: TFLiteModelManager
) : HeadingCorrectionMLModel {

    override val modelStatus: MLModelStatus
        get() = tfliteManager.getHeadingModelStatus()

    override fun predictHeadingCorrection(
        fusedHeadingDeg: Float,
        imuWindow: List<Float>
    ): Float {
        if (modelStatus == MLModelStatus.LOADED && imuWindow.isNotEmpty()) {
            val correction = tfliteManager.runHeadingInference(imuWindow)
            var heading = (fusedHeadingDeg + correction) % 360f
            if (heading < 0f) heading += 360f
            return heading
        }
        return fusedHeadingDeg
    }
}
