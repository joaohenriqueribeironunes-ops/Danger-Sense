package com.dangersense.app.ai

import com.dangersense.app.BuildConfig
import com.dangersense.app.data.IngestaoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GenerativeModelService {

    private val apiKey = BuildConfig.GEMINI_API_KEY

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analisar(nome: String, categoria: String): Result<IngestaoEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                check(apiKey.isNotBlank()) { "GEMINI_API_KEY ausente (gradle.properties)" }

                val text = generate(buildPrompt(nome, categoria))
                val start = text.indexOf('{')
                val end = text.lastIndexOf('}')
                check(start >= 0 && end > start) { "Resposta sem JSON: $text" }
                val json = JSONObject(text.substring(start, end + 1))

                IngestaoEntity(
                    timestamp = System.currentTimeMillis(),
                    nome = json.optString("nome", nome),
                    categoria = json.optString("categoria", categoria),
                    impacto_tea = json.optDouble("impacto_tea", 0.0).toFloat().coerceIn(-1f, 1f),
                    impacto_tdah = json.optDouble("impacto_tdah", 0.0).toFloat().coerceIn(-1f, 1f),
                    mecanismo_cientifico = json.optString("mecanismo_cientifico", ""),
                    risco_gatilho = json.optDouble("risco_gatilho", 0.0).toFloat().coerceIn(0f, 1f),
                    fontes_cientificas = json.optJSONArray("fontes_cientificas")?.toString() ?: "[]"
                )
            }
        }

    private fun generate(prompt: String): String {
        val body = JSONObject()
            .put("contents", JSONObject().put("parts", JSONObject().put("text", prompt)))
            .toString()
            .toRequestBody(JSON)

        val request = Request.Builder()
            .url("$ENDPOINT/$MODEL:generateContent?key=$apiKey")
            .post(body)
            .build()

        http.newCall(request).execute().use { resp ->
            val payload = resp.body?.string().orEmpty()
            check(resp.isSuccessful) { "Gemini HTTP ${resp.code}: $payload" }

            val candidate = JSONObject(payload)
                .getJSONArray("candidates")
                .getJSONObject(0)

            return candidate.getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .optString("text", "")
        }
    }

    private fun buildPrompt(nome: String, categoria: String) = """
        Você é um pesquisador biomédico sênior especializado em TEA (autismo) e TDAH.
        Analise a substância "$nome" (categoria: $categoria).
        Pesquise em fontes confiáveis (PubMed, Nature, Elsevier, Frontiers, Cochrane)
        os efeitos sobre TEA e TDAH: benefícios, riscos, mecanismos neurobiológicos
        (neuroinflamação, barreira intestinal, dopamina/serotonina, glutamato),
        interações e gatilhos de sobrecarga sensorial.
        Retorne SOMENTE um JSON válido, sem markdown, exatamente neste formato:
        {
          "nome": "$nome",
          "categoria": "$categoria",
          "impacto_tea": <float -1.0 (muito negativo) a 1.0 (muito positivo)>,
          "impacto_tdah": <float -1.0 a 1.0>,
          "mecanismo_cientifico": "<resumo do mecanismo em uma frase em português>",
          "risco_gatilho": <float 0.0 a 1.0>,
          "fontes_cientificas": ["<PMID ou URL>", "<PMID ou URL>"]
        }
    """.trimIndent()

    companion object {
        private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val MODEL = "gemini-3.5-flash"
        private val JSON = "application/json".toMediaType()
    }
}
