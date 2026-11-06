package com.dangersense.app.ai

import android.content.Context
import com.dangersense.app.data.IngestaoEntity
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import org.json.JSONObject
import java.io.File
import kotlin.math.min

data class LocalPrediction(
    val risco: Float,
    val tipoCriseProvavel: String,
    val acaoPreventiva: String,
    val origem: String
)

class LocalPredictionEngine(private val context: Context) {

    private var llm: LlmInference? = null

    fun init() {
        val model = File(context.getExternalFilesDir(null), MODEL_FILE)
        if (!model.exists()) return
        runCatching {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(model.absolutePath)
                .setMaxTokens(256)
                .setTopK(40)
                .setTemperature(0.3f)
                .build()
            llm = LlmInference.createFromOptions(context, options)
        }
    }

    fun prever(
        db: Float,
        bpm: Int,
        screenMinutes: Int,
        ingestoes4h: List<IngestaoEntity>
    ): LocalPrediction {
        val ingestoes = ingestoes4h.take(8).joinToString("; ") {
            "${it.nome} (${it.categoria}, risco ${it.risco_gatilho})"
        }.ifBlank { "nenhuma nas últimas 4 horas" }

        val prompt = """
            Você é um preditor local de crises de TEA/TDAH. Cruze os dados e responda
            SOMENTE JSON: {"risco": <0.0-1.0>, "tipo_crise_provavel":
            "<Sobrecarga Sensorial|Meltdown|Shutdown>", "acao_preventiva": "<ação curta em PT-BR>"}
            Ruído atual: $db dB. Batimentos: $bpm bpm. Tempo de tela hoje: $screenMinutes min.
            Ingestas últimas 4h: $ingestoes.
        """.trimIndent()

        val raw = runCatching { llm?.generateResponse(prompt) }.getOrNull()
        return parse(raw) ?: heuristic(db, bpm, screenMinutes, ingestoes4h)
    }

    private fun parse(raw: String?): LocalPrediction? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val start = raw.indexOf('{')
            val end = raw.lastIndexOf('}')
            if (start < 0 || end <= start) return null
            val j = JSONObject(raw.substring(start, end + 1))
            LocalPrediction(
                risco = j.optDouble("risco", 0.0).toFloat().coerceIn(0f, 1f),
                tipoCriseProvavel = j.optString("tipo_crise_provavel", "Sobrecarga Sensorial"),
                acaoPreventiva = j.optString("acao_preventiva", "Reduza estímulos e respire"),
                origem = "Gemma 2B (local)"
            )
        }.getOrNull()
    }

    private fun heuristic(
        db: Float,
        bpm: Int,
        screenMinutes: Int,
        ingestoes: List<IngestaoEntity>
    ): LocalPrediction {
        val noise = min(1f, (db - 60f) / 30f).coerceAtLeast(0f)
        val bpmRisk = if (bpm > 0) ((bpm - 75f) / 40f).coerceIn(0f, 1f) else 0.3f
        val screenRisk = (screenMinutes / 360f).coerceIn(0f, 1f)
        val dietRisk = ingestoes.map { it.risco_gatilho }.take(5)
            .let { if (it.isEmpty()) 0.3f else it.average().toFloat() }

        val risco = (0.30f * noise + 0.20f * bpmRisk + 0.15f * screenRisk + 0.35f * dietRisk)
            .coerceIn(0f, 1f)

        val tipo = when {
            risco >= 0.75f && noise > 0.6f -> "Meltdown"
            dietRisk > 0.6f -> "Shutdown"
            else -> "Sobrecarga Sensorial"
        }
        val acao = when (tipo) {
            "Meltdown" -> "Saia do ambiente ruidoso e use estímulo motor seguro"
            "Shutdown" -> "Reduza brilho, som e faça pausa de 10 minutos"
            else -> "Respiração quadrada 4-4-4-4 e isolamento visual"
        }
        return LocalPrediction(risco, tipo, acao, "Heurística local")
    }

    fun close() {
        runCatching { llm?.close() }
        llm = null
    }

    companion object {
        private const val MODEL_FILE = "gemma-2b-it-gpu-int4.bin"
    }
}
