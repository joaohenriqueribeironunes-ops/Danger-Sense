package com.dangersense.app.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangersense.app.domain.MainViewModel
import com.dangersense.app.ui.theme.OneUI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticSection(vm: MainViewModel) {
    val peak by vm.soundPeak.collectAsStateWithLifecycle()
    val peakAt by vm.soundPeakAt.collectAsStateWithLifecycle()
    val temp by vm.temperature.collectAsStateWithLifecycle()
    val result by vm.diagnosticResult.collectAsStateWithLifecycle()

    var sleep by remember { mutableStateOf("7") }
    var diet by remember { mutableStateOf("") }
    var activities by remember { mutableStateOf("") }
    var scheduleHour by remember { mutableStateOf("9") }

    Card(
        shape = RoundedCornerShape(OneUI.RadiusXl.dp),
        colors = CardDefaults.cardColors(containerColor = OneUI.GraphiteSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Diagnóstico rotineiro",
                style = MaterialTheme.typography.titleMedium,
                color = OneUI.TextSecondary
            )
            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCol(
                    "Maior som do dia",
                    if (peakAt == 0L) "—" else "%.0f dB · %s".format(
                        peak,
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(peakAt))
                    ),
                    Modifier.weight(1f)
                )
                MetricCol(
                    "Temperatura",
                    if (temp.isNaN()) "n/d" else "%.1f °C".format(temp),
                    Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(14.dp))

            Field(sleep, { sleep = it }, "Horas de sono (ex.: 7,5)")
            Field(diet, { diet = it }, "O que você comeu hoje")
            Field(activities, { activities = it }, "O que você fez durante o dia")
            Field(scheduleHour, { scheduleHour = it }, "Hora do diagnóstico agendado (0-23)")

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    vm.runDiagnostic(
                        sleepHoursText = sleep,
                        diet = diet,
                        activities = activities,
                        scheduleHour = scheduleHour.toIntOrNull()
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OneUI.Aurora,
                    contentColor = OneUI.GraphiteDeep
                )
            ) {
                Text("Calcular chance de crise hoje", fontWeight = FontWeight.SemiBold)
            }

            result?.let { p ->
                Spacer(Modifier.height(14.dp))
                val percent = (p * 100).toInt()
                val alert = percent >= 60
                Text(
                    "Chance de crise hoje: $percent%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (alert) OneUI.Alert else OneUI.Aurora
                )
                Text(
                    if (alert) "Protocolo preventivo recomendado: ruído, sono e dieta críticos."
                    else "Fatores dentro da faixa tolerável hoje.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OneUI.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun MetricCol(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OneUI.TextSecondary)
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = OneUI.TextPrimary)
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        label = { Text(label, color = OneUI.TextSecondary) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OneUI.Aurora,
            unfocusedBorderColor = OneUI.GraphiteElevated,
            focusedContainerColor = OneUI.Graphite,
            unfocusedContainerColor = OneUI.Graphite,
            focusedTextColor = OneUI.TextPrimary,
            unfocusedTextColor = OneUI.TextPrimary,
            cursorColor = OneUI.Aurora
        ),
        maxLines = if (label.contains("horas") || label.contains("Hora")) 1 else 3
    )
}
