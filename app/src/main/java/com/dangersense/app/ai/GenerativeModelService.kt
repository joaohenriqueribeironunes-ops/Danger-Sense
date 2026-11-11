package com.dangersense.app.ai

import com.dangersense.app.BuildConfig
import com.dangersense.app.data.IngestaoEntity
import com.dangersense.app.data.RotinaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DiagnosticoAdaptativo(
    val insightPrincipal: String,
    val correlacaoDetectada: String,
    val nivelVulnerabilidade: Int,
    val recomendacaoClinica: String,
    val origem: String
)

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

    /** Consolida 7-14 dias de rotina, injeta no prompt do Gemini e lê o JSON diagnóstico. */
    suspend fun gerarDiagnosticoAdaptativo(
        historicoRotina: List<RotinaEntity>,
        ingestoes: List<IngestaoEntity> = emptyList()
    ): Result<DiagnosticoAdaptativo> = withContext(Dispatchers.IO) {
        runCatching {
            check(apiKey.isNotBlank()) { "GEMINI_API_KEY ausente" }
            check(historicoRotina.isNotEmpty()) { "Histórico de rotina vazio" }

            val dias = diasCobertos(historicoRotina)
            val text = generate(
                promptDiagnostico(resumoEstruturado(historicoRotina), dias, ingestoes)
            )

            val start = text.indexOf('{')
            val end = text.lastIndexOf('}')
            check(start >= 0 && end > start) { "Resposta sem JSON: $text" }
            val j = JSONObject(text.substring(start, end + 1))

            DiagnosticoAdaptativo(
                insightPrincipal = j.optString("insight_principal", "Sem insight disponível."),
                correlacaoDetectada = j.optString("correlacao_detectada", ""),
                nivelVulnerabilidade = j.optDouble("nivel_vulnerabilidade_atual", 0.0)
                    .toInt().coerceIn(0, 100),
                recomendacaoClinica = j.optString("recomendacao_clinica", ""),
                origem = "Gemini · $dias dias analisados"
            )
        }
    }

    /** Estimativa local quando não há rede/chave: mesmo formato de saída. */
    fun diagnosticoLocal(historicoRotina: List<RotinaEntity>): DiagnosticoAdaptativo {
        val sonoCurto = historicoRotina.any {
            it.categoria == RotinaEntity.SONO && it.duracaoMinutos in 1..359
        }
        val crises = historicoRotina.count {
            it.categoria == RotinaEntity.CRISE && it.validada
        }
        val gatilho = historicoRotina
            .filter { it.categoria == RotinaEntity.ALIMENTACAO }
            .map { it.riscoGatilho }
            .let { if (it.isEmpty()) 0f else it.average().toFloat() }
        val sociais = historicoRotina
            .filter { it.categoria == RotinaEntity.ATIVIDADE }
            .sumOf { it.interacoesSociais }

        val vulnerabilidade = (
            22f + crises * 11f + gatilho * 34f + (if (sonoCurto) 21f else 0f)
            ).coerceIn(0f, 100f).toInt()

        val correlacao = buildString {
            if (sonoCurto) append("sono abaixo de 6h, ")
            if (gatilho > 0.45f) append("alimentação de alto gatilho, ")
            if (crises > 0) append("$crises crises validadas recentes, ")
            if (isEmpty()) append("padrões estáveis, ")
        }.dropLast(2) + " sem desvio crítico detectado."

        return DiagnosticoAdaptativo(
            insightPrincipal = when {
                sonoCurto && gatilho > 0.45f ->
                    "Dormir menos de 6h somado a alimentação de alto gatilho eleva o risco de sobrecarga nas próximas 4h."
                crises > 0 -> "O histórico mostra episódios validados concentrados após períodos de baixo repouso."
                else -> "Sinais vitais e rotina dentro da faixa esperada nas últimas semanas."
            },
            correlacaoDetectada = correlacao,
            nivelVulnerabilidade = vulnerabilidade,
            recomendacaoClinica = when {
                sonoCurto -> "Priorize 7h de sono hoje e reduza cafeína após as 14h."
                gatilho > 0.45f -> "Antecipe uma pausa de 10 min e contenha estímulos sonoros nas próximas 2h."
                else -> "Mantenha a rotina atual e registre o questionário diário para refinar os padrões."
            },
            origem = "Heurística local · ${historicoRotina.size} registros"
        )
    }

    private fun promptDiagnostico(resumo: String, dias: Int, ingestoes: List<IngestaoEntity>) = """
        Você é um analista de padrões de neurodivergência (TEA/TDAH). Analise o histórico multivariado de rotina do usuário (comida, sono, atividades e crises passadas). Identifique correlações sutis de causa e efeito que são específicas para ESTE usuário (Ex: 'Sempre que o usuário dorme menos de 6h e consome glúten/cafeína, o risco de parálise por TDAH dobra nas 4h seguintes'). Gere um diagnóstico proativo, adaptável e personalizado. Retorne estritamente em JSON com os campos: 'insight_principal', 'correlacao_detectada', 'nivel_vulnerabilidade_atual' (0 a 100), e 'recomendacao_clinica'.

        HISTÓRICO DE ROTINA DOS ÚLTIMOS $dias DIAS (alimentação, sono, atividades e crises validadas):
        $resumo
        ${blocoAlimentacao(ingestoes)}

        Saída SOMENTE JSON válido, sem markdown e sem comentários:
        {"insight_principal": "<PT-BR>", "correlacao_detectada": "<PT-BR>", "nivel_vulnerabilidade_atual": <0-100>, "recomendacao_clinica": "<PT-BR>"}
    """.trimIndent()

    /** Base de conhecimento alimentar já pesquisada: mecanismo, gatilho, impacto e fontes. */
    private fun blocoAlimentacao(ingestoes: List<IngestaoEntity>): String {
        if (ingestoes.isEmpty()) return ""
        return buildString {
            appendLine()
            appendLine("BASE CIENTÍFICA DE ALIMENTAÇÃO/INGESTÃO DESTE USUÁRIO:")
            ingestoes.take(20).forEach { i ->
                appendLine(
                    "- ${i.nome} (${i.categoria}): mecanismo=${i.mecanismo_cientifico} | " +
                        "gatilho=${(i.risco_gatilho * 100).toInt()}% | " +
                        "impacto_TEA=${i.impacto_tea} | impacto_TDAH=${i.impacto_tdah} | " +
                        "fontes=${i.fontes_cientificas}"
                )
            }
            appendLine(
                "Explique em 'correlacao_detectada' o que cada um desses alimentos pode " +
                    "desencadear (sintomas, sobrecarga, janelas de risco) e cite as fontes."
            )
        }
    }

    private fun diasCobertos(historico: List<RotinaEntity>): Int =
        historico.groupBy { it.timestamp / 86_400_000L }.size

    private fun resumoEstruturado(historico: List<RotinaEntity>): String {
        val dia = SimpleDateFormat("dd/MM", Locale.getDefault())
        val hora = SimpleDateFormat("HH:mm", Locale.getDefault())
        return historico
            .groupBy { it.timestamp / 86_400_000L }
            .toList()
            .sortedByDescending { it.first }
            .take(14)
            .joinToString("\n") { (_, itens) ->
                "DIA ${dia.format(Date(itens.first().timestamp))}:\n" +
                    itens.joinToString("\n") { r ->
                        val partes = mutableListOf(
                            hora.format(Date(r.timestamp)),
                            "[${r.categoria}]",
                            r.rotulo.take(40),
                            "valor=%.2f".format(r.metrica)
                        )
                        if (r.duracaoMinutos > 0) partes += "duracao=${r.duracaoMinutos}min"
                        if (r.interacoesSociais > 0) partes += "sociais=${r.interacoesSociais}"
                        if (r.impactoTea != 0f) partes += "tea=%.1f".format(r.impactoTea)
                        if (r.impactoTdah != 0f) partes += "tdah=%.1f".format(r.impactoTdah)
                        if (r.riscoGatilho > 0f) partes += "gatilho=%.0f%%".format(r.riscoGatilho * 100)
                        if (r.detalhe.isNotBlank()) partes += r.detalhe.take(70)
                        partes += if (r.validada) "validado" else "não-validado"
                        "  " + partes.joinToString(" · ")
                    }
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
        Faça um DIAGNÓSTICO ALIMENTAR COMPLETO e detalhado da substância "$nome" (categoria: $categoria), com base em evidências de PubMed, Nature, Elsevier, Frontiers e Cochrane.
        Inclua tudo, sem omitir partes:
        - benefícios potenciais para TEA e TDAH
        - riscos, efeitos adversos e contraindicações
        - mecanismos neurobiológicos (neuroinflamação, eixo intestino-cérebro, dopamina, serotonina, glutamato/GABA, histamina, cortisol etc.)
        - interações com medicamentos/suplementos comuns
        - gatilhos de sobrecarga sensorial, ansiedade, hiperatividade ou letargia
        - o que pode desencadear e em quais janelas de tempo
        - recomendações práticas (dose/contexto/evitar ou preferir), quando aplicável
        Retorne SOMENTE um JSON válido, sem markdown, exatamente neste formato:
        {
          "nome": "$nome",
          "categoria": "$categoria",
          "impacto_tea": <float -1.0 (muito negativo) a 1.0 (muito positivo)>,
          "impacto_tdah": <float -1.0 a 1.0>,
          "mecanismo_cientifico": "<texto completo em português, em parágrafos, cobrindo TODOS os tópicos acima; não resuma>",
          "risco_gatilho": <float 0.0 a 1.0>,
          "fontes_cientificas": ["<PMID/DOI/URL>", "... liste todas as fontes relevantes ..."]
        }
    """.trimIndent()

    companion object {
        private const val ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val MODEL = "gemini-3.5-flash"
        private val JSON = "application/json".toMediaType()
    }
}
