package com.dangersense.app.ui.hyper

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangersense.app.data.IngestaoEntity
import com.dangersense.app.domain.MainViewModel
import com.dangersense.app.ui.theme.OneUI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CATEGORIES = listOf("Alimento", "Medicamento", "Suplemento")

@Composable
fun IngestionPanel(vm: MainViewModel) {
    val history by vm.ingestoes.collectAsStateWithLifecycle(initialValue = emptyList())
    val status by vm.analiseStatus.collectAsStateWithLifecycle()

    var nome by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(CATEGORIES[0]) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HyperOS.SquircleLg)
            .background(HyperOS.Surface)
            .padding(20.dp)
    ) {
        Text(
            "Análise Científica · Gemini",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = OneUI.TextPrimary
        )
        Text(
            "PubMed/Nature sobre TEA e TDAH",
            fontSize = 12.sp,
            color = OneUI.TextSecondary
        )
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ex.: ômega-3, ritalina, leite", color = OneUI.TextSecondary) },
            colors = hyperFieldColors()
        )

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CATEGORIES.forEach { c ->
                val selected = c == categoria
                Text(
                    c,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) HyperOS.Graphite else OneUI.TextPrimary,
                    modifier = Modifier
                        .clip(HyperOS.Capsule)
                        .background(
                            if (selected) HyperOS.HyperBlue else HyperOS.Elevated
                        )
                        .clickable { categoria = c }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { if (nome.isNotBlank()) vm.analisarIngestao(nome.trim(), categoria) },
            enabled = nome.isNotBlank() && status != "analisando",
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HyperOS.HyperBlue,
                contentColor = HyperOS.TrueBlack
            )
        ) {
            Text(
                if (status == "analisando") "Pesquisando artigos…" else "Analisar com Gemini",
                fontWeight = FontWeight.SemiBold
            )
        }

        status.takeIf { it != "ok" && it.isNotBlank() }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = HyperOS.AlertOrange)
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            history.take(6).forEach { item -> IngestionRow(item) }
        }
    }
}

@Composable
private fun IngestionRow(item: IngestaoEntity) {
    val fmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val teaColor = if (item.impacto_tea >= 0) HyperOS.VitalGreen else HyperOS.AlertOrange
    val tdahColor = if (item.impacto_tdah >= 0) HyperOS.VitalGreen else HyperOS.AlertOrange

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HyperOS.Elevated)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                item.nome,
                fontWeight = FontWeight.SemiBold,
                color = OneUI.TextPrimary,
                fontSize = 15.sp
            )
            Text(
                fmt.format(Date(item.timestamp)),
                fontSize = 11.sp,
                color = OneUI.TextSecondary
            )
        }
        Text(
            "TEA ${fmtScore(item.impacto_tea)}  ·  TDAH ${fmtScore(item.impacto_tdah)}  ·  " +
                    "gatilho ${(item.risco_gatilho * 100).toInt()}%",
            fontSize = 12.sp,
            color = OneUI.TextSecondary
        )
        Text(
            item.mecanismo_cientifico,
            fontSize = 12.sp,
            color = teaColor.copy(alpha = 0.9f),
            maxLines = 2
        )
        Text(
            "Fontes: ${item.fontes_cientificas}",
            fontSize = 10.sp,
            color = tdahColor.copy(alpha = 0.6f),
            maxLines = 1
        )
    }
}

private fun fmtScore(v: Float): String = when {
    v > 0.3f -> "+%.1f".format(v)
    v < -0.3f -> "%.1f".format(v)
    else -> "0.0"
}

@Composable
private fun hyperFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = HyperOS.HyperBlue,
    unfocusedBorderColor = HyperOS.Elevated,
    focusedContainerColor = HyperOS.Graphite,
    unfocusedContainerColor = HyperOS.Graphite,
    focusedTextColor = OneUI.TextPrimary,
    unfocusedTextColor = OneUI.TextPrimary,
    cursorColor = HyperOS.HyperBlue
)
