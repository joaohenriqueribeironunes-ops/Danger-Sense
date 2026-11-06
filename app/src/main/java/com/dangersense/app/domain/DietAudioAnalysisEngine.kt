package com.dangersense.app.domain

object DietAudioAnalysisEngine {

    private val negative = listOf(
        "ultraprocessad", "macarrão instantâneo", "macarrao instantaneo", "refrigerante",
        "açúcar", "acucar", "doce", "bolo", "biscoito", "pão branco", "glúten", "gluten",
        "caseína", "caseina", "leite", "queijo", "nuggets", "frita", "salgadinho",
        "embutido", "presunto", "salsicha", "corante", "adoçante", "adocante",
        "achocolatado", "hambúrguer", "hamburguer", "pizza", "cereal", "chocolate",
        "farinha", "estralinha", "pirulito", "suco de caixinha"
    )

    private val positive = listOf(
        "salmão", "salmao", "sardinha", "atum", "peixe", "linhaça", "lunaca", "chia",
        "nozes", "castanha", "azeitona", "abacate", "espinafre", "brócolis", "brocolis",
        "cenoura", "ovo", "frango", "feijão", "feijao", "lentilha", "grão-de-bico",
        "aveia", "banana", "morango", "fruta", "salada", "verdura", "legume",
        "integral", "probiótico", "probiotico", "fibra", "ômega", "omega", "triptofano",
        "camomila", "sopa", "purê", "purre", "arroz", "mandioca", "batata-doce", "água", "agua"
    )

    private val activityStress = listOf(
        "escola", "trabalho", "prova", "exame", "festa", "shopping", "multidão",
        "trânsito", "transito", "reunião", "reuniao", "consulta", "dentista", "viagem"
    )

    private val activityProtective = listOf(
        "casa", "parque", "trilha", "leitura", "música", "musica", "banho", "pintura",
        "quebra-cabeça", "quebra cabeca", "livro", "descanso", "planejamento"
    )

    fun neuroImpact(diet: String): Float {
        val t = diet.lowercase()
        val neg = negative.count { t.contains(it) }
        val pos = positive.count { t.contains(it) }
        if (neg + pos == 0) return 0f
        return ((pos - neg).toFloat() / (pos + neg)).coerceIn(-1f, 1f)
    }

    fun activityRisk(activities: String): Float {
        val t = activities.lowercase()
        if (t.isBlank()) return 0.5f
        val neg = activityStress.count { t.contains(it) }
        val pos = activityProtective.count { t.contains(it) }
        return (0.5f + 0.15f * (neg - pos)).coerceIn(0f, 1f)
    }

    fun analyze(diet: String, peakDb: Float): Float {
        var score = neuroImpact(diet)
        if (peakDb > 85f) score -= 0.15f
        return score.coerceIn(-1f, 1f)
    }
}
