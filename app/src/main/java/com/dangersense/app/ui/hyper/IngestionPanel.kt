package com.dangersense.app.ui.hyper

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
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
            .background(OneUI.GraphiteSurface)
            .border(1.dp, OneUI.GraphiteGreenLine.copy(alpha = 0.35f), HyperOS.SquircleLg)
            .padding(20.dp)
            .animateContentSize(tween(350))
    ) {
        Text(
            "Diagnóstico alimentar",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = OneUI.TextPrimary
        )
        Text(
            "Análise completa com Gemini",
            fontSize = 12.sp,
            color = OneUI.TextSecondary
        )
        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = nome,
            onValueChange = { nome = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Alimento, medicamento ou suplemento", color = OneUI.TextSecondary) },
            shape = RoundedCornerShape(18.dp),
            colors = fieldColors()
        )

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CATEGORIES.forEach { c ->
                val selected = c == categoria
                Text(
                    c,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) OneUI.GraphiteDeep else OneUI.TextPrimary,
                    modifier = Modifier
                        .clip(HyperOS.Capsule)
                        .background(if (selected) OneUI.Mustard else OneUI.GraphiteElevated)
                        .clickable { categoria = c }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (nome.isNotBlank()) {
                    vm.analisarIngestao(nome.trim(), categoria)
                    nome = ""
                }
            },
            enabled = nome.isNotBlank() && status != "analisando",
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OneUI.PureBlack,
                contentColor = OneUI.TextPrimary,
                disabledContainerColor = OneUI.PureBlack.copy(alpha = 0.45f),
                disabledContentColor = OneUI.TextSecondary
            )
        ) {
            Text(
                if (status == "analisando") "Analisando…" else "Analisar",
                fontWeight = FontWeight.SemiBold
            )
        }

        status.takeIf { it.isNotBlank() && it != "ok" && it != "analisando" }?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 12.sp, color = OneUI.MustardLight)
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Text(
                "Todos os alimentos e efeitos",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = OneUI.MustardLight
            )
            Spacer(Modifier.height(8.dp))
            history.forEach { item -> IngestionRow(item) }
        }
    }
}

@Composable
private fun IngestionRow(item: IngestaoEntity) {
    val fmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    val gatilho = (item.risco_gatilho * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(OneUI.GraphiteElevated.copy(alpha = 0.65f))
            .border(1.dp, OneUI.GraphiteGreenLine.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(14.dp)
            .animateContentSize(tween(300))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                item.nome,
                fontWeight = FontWeight.SemiBold,
                color = OneUI.TextPrimary,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Text(fmt.format(Date(item.timestamp)), fontSize = 11.sp, color = OneUI.TextSecondary)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${item.categoria} · TEA ${fmtScore(item.impacto_tea)} · TDAH ${fmtScore(item.impacto_tdah)} · gatilho $gatilho%",
            fontSize = 12.sp,
            color = OneUI.MustardLight
        )
        Spacer(Modifier.height(8.dp))
        Text(
            item.mecanismo_cientifico,
            fontSize = 13.sp,
            color = OneUI.TextPrimary,
            lineHeight = 19.sp
        )
        if (item.fontes_cientificas.isNotBlank() && item.fontes_cientificas != "[]") {
            Spacer(Modifier.height(8.dp))
            Text(
                "Fontes: ${item.fontes_cientificas}",
                fontSize = 11.sp,
                color = OneUI.TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

private fun fmtScore(v: Float): String = when {
    v > 0.05f -> "+%.1f".format(v)
    v < -0.05f -> "%.1f".format(v)
    else -> "0.0"
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = OneUI.Mustard,
    unfocusedBorderColor = OneUI.GraphiteGreenLine.copy(alpha = 0.4f),
    focusedContainerColor = OneUI.GraphiteDeep,
    unfocusedContainerColor = OneUI.GraphiteDeep,
    focusedTextColor = OneUI.TextPrimary,
    unfocusedTextColor = OneUI.TextPrimary,
    cursorColor = OneUI.Mustard
)
