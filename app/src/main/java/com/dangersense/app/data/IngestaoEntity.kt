package com.dangersense.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingestao")
data class IngestaoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val nome: String,
    val categoria: String,
    val impacto_tea: Float,
    val impacto_tdah: Float,
    val mecanismo_cientifico: String,
    val risco_gatilho: Float,
    val fontes_cientificas: String
)
