package com.example.dead_reckoning

import com.example.dead_reckoning.domain.activity.ActivityType
import com.example.dead_reckoning.domain.ml.DefaultHeadingCorrectionMLModel
import com.example.dead_reckoning.domain.ml.DefaultPositionErrorCorrectionMLModel
import com.example.dead_reckoning.domain.ml.DefaultStepLengthMLModel
import com.example.dead_reckoning.domain.ml.MLModelStatus
import com.example.dead_reckoning.domain.ml.TFLiteModelManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MLModelTest {

    private lateinit var tfliteManager: TFLiteModelManager
    private lateinit var stepLengthModel: DefaultStepLengthMLModel
    private lateinit var headingModel: DefaultHeadingCorrectionMLModel
    private lateinit var positionModel: DefaultPositionErrorCorrectionMLModel

    @Before
    fun setUp() {
        tfliteManager = TFLiteModelManager(context = null)
        stepLengthModel = DefaultStepLengthMLModel(tfliteManager)
        headingModel = DefaultHeadingCorrectionMLModel(tfliteManager)
        positionModel = DefaultPositionErrorCorrectionMLModel(tfliteManager)
    }

    @Test
    fun tfliteManager_withoutContext_defaultsToBaselineFallback() {
        assertEquals(MLModelStatus.BASELINE_FALLBACK, tfliteManager.getStepLengthModelStatus())
        assertEquals(MLModelStatus.BASELINE_FALLBACK, tfliteManager.getHeadingModelStatus())
        assertEquals(MLModelStatus.BASELINE_FALLBACK, tfliteManager.getPositionModelStatus())
    }

    @Test
    fun stepLengthMLModel_fallback_usesWeinbergModel() {
        val stepLength = stepLengthModel.predictStepLength(
            peakAccel = 12.0f,
            minAccel = 8.0f,
            cadenceSpm = 110f,
            activity = ActivityType.WALKING
        )

        // Weinberg calculation for delta = 4.0 m/s^2: 0.42 * 4^0.25 = ~0.59m
        assertTrue(stepLength in 0.35f..1.60f)
        assertEquals(0.594f, stepLength, 0.05f)
    }

    @Test
    fun headingCorrectionMLModel_fallback_returnsBaselineHeading() {
        val heading = headingModel.predictHeadingCorrection(
            fusedHeadingDeg = 180f,
            imuWindow = listOf(1f, 2f, 3f)
        )

        assertEquals(180f, heading, 0.001f)
    }

    @Test
    fun positionErrorCorrectionMLModel_fallback_returnsZeroOffset() {
        val offset = positionModel.predictPositionErrorOffset(
            pdrX = 10.0,
            pdrY = 20.0,
            imuSequence = listOf(0.1f, 0.2f)
        )

        assertEquals(0.0, offset.first, 0.001)
        assertEquals(0.0, offset.second, 0.001)
    }

    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}
