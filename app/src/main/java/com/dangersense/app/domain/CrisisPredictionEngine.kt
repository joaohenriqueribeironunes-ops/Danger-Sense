package com.dangersense.app.domain

import kotlin.math.abs

data class DiagnosticInput(
    val sleepMinutes: Int,
    val dietText: String,
    val activitiesText: String,
    val peakDb: Float,
    val temperature: Float,
    val historicalCrisisRate: Float
)

object CrisisPredictionEngine {

    fun probability(i: DiagnosticInput): Float {
        val deficit = ((480 - i.sleepMinutes) / 360f).coerceIn(0f, 1f)
        val sleepRisk = deficit * deficit

        val noiseRisk = when {
            i.peakDb <= 0f -> 0.35f
            i.peakDb < 80f -> ((i.peakDb - 60f) / 20f).coerceIn(0f, 1f) * 0.4f
            i.peakDb <= 85f -> (i.peakDb - 80f) / 5f
            else -> 1f
        }

        val dietScore = DietAudioAnalysisEngine.analyze(i.dietText, i.peakDb)
        val dietRisk = (1f - dietScore) / 2f

        val tempRisk = if (i.temperature <= 0f) 0.25f
        else ((abs(i.temperature - 23f) - 8f) / 12f).coerceIn(0f, 1f)

        val actRisk = DietAudioAnalysisEngine.activityRisk(i.activitiesText)

        val p = 0.35f * sleepRisk +
                0.25f * noiseRisk +
                0.25f * dietRisk +
                0.05f * tempRisk +
                0.05f * actRisk +
                0.05f * i.historicalCrisisRate

        return p.coerceIn(0f, 1f)
    }
}
