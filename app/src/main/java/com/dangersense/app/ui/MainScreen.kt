package com.dangersense.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangersense.app.data.CrisisEvent
import com.dangersense.app.domain.MainViewModel
import com.dangersense.app.ui.hyper.DiagnosticoTriggerCard
import com.dangersense.app.ui.hyper.HyperCapsule
import com.dangersense.app.ui.hyper.HyperMindHub
import com.dangersense.app.ui.hyper.HyperOSDiagnosticOverlay
import com.dangersense.app.ui.hyper.IngestionPanel
import com.dangersense.app.ui.theme.OneUI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(vm: MainViewModel) {
    val motion by vm.motion.collectAsStateWithLifecycle()
    val db by vm.dbLevel.collectAsStateWithLifecycle()
    val dbPeak by vm.dbPeak.collectAsStateWithLifecycle()
    val dbAmbient by vm.dbAmbient.collectAsStateWithLifecycle()
    val sampleRate by vm.sampleRate.collectAsStateWithLifecycle()
    val prediction by vm.prediction.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle(initialValue = emptyList())
    val foreground by vm.tracker.foreground.collectAsStateWithLifecycle()
    val enabled by vm.controller.enabled.collectAsStateWithLifecycle()
    val alert by vm.alert.collectAsStateWithLifecycle()
    val messages by vm.messages.collectAsStateWithLifecycle()
    val dimmed by vm.reducedStimuli.collectAsStateWithLifecycle()
    val bpm by vm.bpm.collectAsStateWithLifecycle()
    val screenMinutes by vm.screenMinutes.collectAsStateWithLifecycle()
    val local by vm.localPrediction.collectAsStateWithLifecycle()
    val diagnostico by vm.diagnostico.collectAsStateWithLifecycle()

    var draft by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        containerColor = OneUI.Graphite,
        bottomBar = {
            MessageBar(
                value = draft,
                onValueChange = { draft = it },
                onSend = {
                    vm.onMessage(draft)
                    draft = ""
                },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) {
            Header(
                enabled = enabled,
                onToggle = { vm.controller.toggle() }
            )

            AnimatedVisibility(
                visible = alert,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                CrisisBanner()
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    HyperMindHub(
                        risk = prediction?.risk ?: local?.risco ?: 0f,
                        db = db,
                        bpm = bpm,
                        screenMinutes = screenMinutes,
                        localSource = local?.origem ?: "aguardando anomalia",
                        localAction = local?.acaoPreventiva ?: ""
                    )
                }
                item {
                    TelemetryCard(
                        motion = motion,
                        db = db,
                        dbPeak = dbPeak,
                        dbAmbient = dbAmbient,
                        sampleRate = sampleRate,
                        foreground = foreground
                    )
                }
                if (messages.isNotEmpty()) {
                    item { MessagesCard(messages) }
                }
                item {
                    PredictionCard(
                        risk = prediction?.risk ?: 0f,
                        minutes = prediction?.windowMinutes ?: 0,
                        pkg = prediction?.topPackage ?: "",
                        detected = prediction?.detected == true
                    )
                }
                item { DiagnosticoTriggerCard(vm) }
                item { DiagnosticSection(vm) }
                item { IngestionPanel(vm) }
                if (events.isNotEmpty()) {
                    item {
                        Text(
                            "Registro de sobrecargas",
                            style = MaterialTheme.typography.titleMedium,
                            color = OneUI.TextSecondary,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                        )
                    }
                    items(events) { e -> EventRow(e) }
                }
            }
        }
        }

        if (alert && !dimmed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp)
                    .zIndex(10f)
            ) {
                HyperCapsule(
                    onPausa = { vm.requestPause() },
                    onFalsoAlarme = { vm.dismissAlert() }
                )
            }
        }

        if (dimmed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.96f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Estímulos reduzidos",
                        color = OneUI.TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { vm.restoreVision() },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = OneUI.GraphiteElevated,
                            contentColor = OneUI.Aurora
                        )
                    ) {
                        Text("Restaurar visão")
                    }
                }
            }
        }

        HyperOSDiagnosticOverlay(
            diagnostico = diagnostico,
            onDismiss = { vm.fecharDiagnostico() },
            modifier = Modifier.fillMaxSize().zIndex(20f)
        )
    }
}

@Composable
private fun Header(enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Danger Sense",
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                color = OneUI.TextPrimary
            )
            Text(
                if (enabled) "Monitoramento ativo" else "Monitoramento pausado",
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) OneUI.Aurora else OneUI.TextSecondary
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedTrackColor = OneUI.Aurora,
                checkedThumbColor = OneUI.GraphiteDeep,
                uncheckedTrackColor = OneUI.GraphiteElevated,
                uncheckedThumbColor = OneUI.TextSecondary
            )
        )
    }
}

@Composable
private fun CrisisBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(OneUI.RadiusXl.dp))
            .background(
                Brush.horizontalGradient(listOf(OneUI.Alert, OneUI.Alert.copy(alpha = 0.82f)))
            )
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Column {
            Text(
                "Sentidos Sobrecarregados",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OneUI.GraphiteDeep
            )
            Text(
                "Respire. Reduza estímulos por alguns minutos.",
                style = MaterialTheme.typography.bodyMedium,
                color = OneUI.GraphiteDeep.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun TelemetryCard(
    motion: Float,
    db: Float,
    dbPeak: Float,
    dbAmbient: Float,
    sampleRate: Int,
    foreground: String
) {
    Card(
        shape = RoundedCornerShape(OneUI.RadiusXl.dp),
        colors = CardDefaults.cardColors(containerColor = OneUI.GraphiteSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Telemetria em tempo real", style = MaterialTheme.typography.titleMedium,
                color = OneUI.TextSecondary)
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Metric("Movimento", "%.2f g".format(motion), Modifier.weight(1f))
                Metric("Ruído", "%.0f dB".format(db), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Metric("Pico (1,5 s)", "%.0f dB".format(dbPeak), Modifier.weight(1f))
                Metric("Ambiente", "%.0f dB".format(dbAmbient), Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Microfone: ${sampleRate} Hz · sem processamento" ,
                style = MaterialTheme.typography.bodyMedium,
                color = OneUI.Aurora
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "App em primeiro plano: ${foreground.ifBlank { "—" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = OneUI.Aurora
            )
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OneUI.TextSecondary)
        Text(value, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = OneUI.TextPrimary)
    }
}

@Composable
private fun MessagesCard(messages: List<String>) {
    Card(
        shape = RoundedCornerShape(OneUI.RadiusXl.dp),
        colors = CardDefaults.cardColors(containerColor = OneUI.GraphiteSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Descarga cognitiva", style = MaterialTheme.typography.titleMedium,
                color = OneUI.Aurora)
            Spacer(Modifier.height(10.dp))
            messages.takeLast(5).forEach {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OneUI.TextPrimary,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun PredictionCard(risk: Float, minutes: Int, pkg: String, detected: Boolean) {
    val animated by animateFloatAsState(targetValue = risk.coerceIn(0f, 1f), label = "risk")
    val accent = if (detected || risk >= 0.7f) OneUI.Alert else OneUI.Aurora

    Card(
        shape = RoundedCornerShape(OneUI.RadiusXl.dp),
        colors = CardDefaults.cardColors(containerColor = OneUI.GraphiteSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(10.dp))
                Text("Previsão da IA local", style = MaterialTheme.typography.titleMedium,
                    color = OneUI.TextSecondary)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (detected) "Crise em curso" else "Risco de crise em ~$minutes min",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(OneUI.GraphiteElevated)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animated)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(accent)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Probabilidade ${(risk * 100).toInt()}%" + if (pkg.isNotBlank()) " · $pkg" else "",
                style = MaterialTheme.typography.bodyMedium,
                color = OneUI.TextSecondary
            )
        }
    }
}

@Composable
private fun EventRow(e: CrisisEvent) {
    val fmt = remember { SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()) }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = OneUI.GraphiteSurface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(fmt.format(Date(e.timestamp)), style = MaterialTheme.typography.titleMedium,
                    color = OneUI.TextPrimary)
                Text(
                    e.crisisType?.let { "$it · " }.orEmpty() +
                            e.foregroundPackage.ifBlank { "App desconhecido" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (e.crisisType != null) OneUI.Alert else OneUI.TextSecondary,
                    maxLines = 1
                )
            }
            Text(
                "%.1f g · %.0f dB".format(e.motionPeak, e.decibels),
                style = MaterialTheme.typography.bodyMedium,
                color = OneUI.Alert
            )
        }
    }
}

@Composable
private fun MessageBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(OneUI.Graphite)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(OneUI.RadiusXl.dp))
                .background(OneUI.GraphiteElevated)
                .padding(start = 22.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Descreva como está se sentindo…", color = OneUI.TextSecondary)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedTextColor = OneUI.TextPrimary,
                    unfocusedTextColor = OneUI.TextPrimary,
                    cursorColor = OneUI.Aurora
                ),
                maxLines = 3
            )
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(OneUI.Aurora)
                    .clickable(onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Text("➤", fontSize = 18.sp, color = OneUI.GraphiteDeep, fontWeight = FontWeight.Bold)
            }
        }
    }
}
