package com.dangersense.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Modelo unificado de memória de rotina: consolida alimentação/suplementação,
 * sono, atividades e ocorrências de crise numa linha temporal única.
 */
@Entity(tableName = "rotina")
data class RotinaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val categoria: String,
    val rotulo: String,
    /** Horas de sono / gatilho nutricional / esforço 0-100 / dB da crise. */
    val metrica: Float = 0f,
    val duracaoMinutos: Int = 0,
    val interacoesSociais: Int = 0,
    val impactoTea: Float = 0f,
    val impactoTdah: Float = 0f,
    val riscoGatilho: Float = 0f,
    val detalhe: String = "",
    val validada: Boolean = false
) {
    companion object {
        const val SONO = "SONO"
        const val ALIMENTACAO = "ALIMENTACAO"
        const val ATIVIDADE = "ATIVIDADE"
        const val CRISE = "CRISE"
    }
}
