package com.dangersense.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dangersense.app.data.CrisisEvent
import com.dangersense.app.ui.theme.OneUI
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun WeeklyOverloadChart(events: List<CrisisEvent>) {
    val locale = Locale("pt", "BR")
    val dias = remember(events) { ultimos7Dias() }
    val porDia = remember(events) {
        events.groupBy { diaKey(it.timestamp) }
    }
    val max = porDia.values.maxOfOrNull { it.size }?.coerceAtLeast(1) ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(OneUI.RadiusXl.dp))
            .background(OneUI.GraphiteSurface)
            .border(1.dp, OneUI.GraphiteGreenLine.copy(alpha = 0.4f), RoundedCornerShape(OneUI.RadiusXl.dp))
            .padding(20.dp)
    ) {
        Text(
            "Sobrecargas · semana",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = OneUI.TextPrimary
        )
        Text(
            "Dia da semana, data e hora de cada ocorrência",
            fontSize = 12.sp,
            color = OneUI.TextSecondary
        )

        Spacer(Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val barSpace = size.width / 7f
            val barW = barSpace * 0.46f
            dias.forEachIndexed { i, d ->
                val key = diaKey(d.timeInMillis)
                val count = porDia[key]?.size ?: 0
                val h = if (count == 0) 6f else (count.toFloat() / max) * (size.height - 18.dp.toPx())
                val x = i * barSpace + (barSpace - barW) / 2f
                val y = size.height - h
                drawRoundRect(
                    color = if (count == 0) OneUI.GraphiteElevated else OneUI.Mustard,
                    topLeft = Offset(x, y),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            dias.forEach { d ->
                val key = diaKey(d.timeInMillis)
                val count = porDia[key]?.size ?: 0
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        d.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, locale)
                            ?.replace(".", "")?.replaceFirstChar { it.uppercase() } ?: "",
                        fontSize = 11.sp,
                        color = OneUI.TextSecondary
                    )
                    Text(
                        SimpleDateFormat("dd/MM", locale).format(d.time),
                        fontSize = 10.sp,
                        color = OneUI.TextSecondary.copy(alpha = 0.8f)
                    )
                    Text(
                        "$count",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (count > 0) OneUI.MustardLight else OneUI.TextSecondary
                    )
                }
            }
        }

        val semana = events.filter { e ->
            dias.any { diaKey(it.timeInMillis) == diaKey(e.timestamp) }
        }.sortedByDescending { it.timestamp }

        if (semana.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            semana.forEach { e -> OcorrenciaRow(e, locale) }
        } else {
            Spacer(Modifier.height(10.dp))
            Text("Nenhuma sobrecarga registrada nesta semana.", fontSize = 13.sp, color = OneUI.TextSecondary)
        }
    }
}

@Composable
private fun OcorrenciaRow(e: CrisisEvent, locale: Locale) {
    val cal = remember(e.timestamp) { Calendar.getInstance().apply { timeInMillis = e.timestamp } }
    val diaSemana = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, locale)
        ?.replaceFirstChar { it.uppercase() } ?: ""
    val data = SimpleDateFormat("dd/MM", locale).format(Date(e.timestamp))
    val hora = SimpleDateFormat("HH:mm", locale).format(Date(e.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(OneUI.GraphiteElevated.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(OneUI.Mustard)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "$diaSemana · $data · $hora",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = OneUI.TextPrimary
            )
            Text(
                (e.crisisType ?: "Sobrecarga") + " · ${"%.0f".format(e.decibels)} dB",
                fontSize = 12.sp,
                color = OneUI.TextSecondary
            )
        }
    }
}

private fun ultimos7Dias(): List<Calendar> {
    val hoje = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    return (6 downTo 0).map { delta ->
        (hoje.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -delta) }
    }
}

private fun diaKey(ts: Long): String =
    SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(ts))
