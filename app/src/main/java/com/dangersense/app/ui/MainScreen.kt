package com.dangersense.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangersense.app.domain.MainViewModel
import com.dangersense.app.ui.hyper.DiagnosticoTriggerCard
import com.dangersense.app.ui.hyper.HyperCapsule
import com.dangersense.app.ui.hyper.HyperMindHub
import com.dangersense.app.ui.hyper.HyperOSDiagnosticOverlay
import com.dangersense.app.ui.hyper.IngestionPanel
import com.dangersense.app.ui.theme.OneUI

@Composable
fun MainScreen(vm: MainViewModel) {
    val db by vm.dbLevel.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle(initialValue = emptyList())
    val enabled by vm.controller.enabled.collectAsStateWithLifecycle()
    val alert by vm.alert.collectAsStateWithLifecycle()
    val dimmed by vm.reducedStimuli.collectAsStateWithLifecycle()
    val diagnostico by vm.diagnostico.collectAsStateWithLifecycle()
    val ingestoes by vm.ingestoes.collectAsStateWithLifecycle(initialValue = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(containerColor = OneUI.Graphite) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .statusBarsPadding()
            ) {
                Header(enabled = enabled, onToggle = { vm.controller.toggle() })

                AnimatedVisibility(
                    visible = alert,
                    enter = fadeIn(tween(500)),
                    exit = fadeOut(tween(500))
                ) { CrisisBanner() }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { FadeInItem { HyperMindHub(db = db) } }
                    item { FadeInItem(delay = 60) { DiagnosticoTriggerCard(vm) } }
                    item { FadeInItem(delay = 120) { IngestionPanel(vm) } }
                    item { FadeInItem(delay = 180) { WeeklyOverloadChart(events) } }
                    item { Spacer(Modifier.height(12.dp)) }
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
                HyperCapsule(onPausa = { vm.requestPause() }, onFalsoAlarme = { vm.dismissAlert() })
            }
        }

        if (dimmed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { vm.restoreVision() },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OneUI.PureBlack,
                        contentColor = OneUI.TextPrimary
                    )
                ) { Text("Restaurar visão") }
            }
        }

        HyperOSDiagnosticOverlay(
            diagnostico = diagnostico,
            ingestoes = ingestoes,
            onDismiss = { vm.fecharDiagnostico() },
            modifier = Modifier.fillMaxSize().zIndex(20f)
        )
    }
}

@Composable
private fun FadeInItem(delay: Int = 0, content: @Composable () -> Unit) {
    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(durationMillis = 550, delayMillis = delay))
    ) { content() }
}

@Composable
private fun Header(enabled: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Danger Sense",
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                color = OneUI.TextPrimary
            )
            Text(
                if (enabled) "Ativo" else "Pausado",
                style = MaterialTheme.typography.bodyMedium,
                color = OneUI.TextSecondary
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
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(OneUI.RadiusXl.dp))
            .background(OneUI.Mustard)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Text(
                "Sobrecarga",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = OneUI.GraphiteDeep
            )
            Text(
                "Reduza estímulos e respire.",
                style = MaterialTheme.typography.bodyMedium,
                color = OneUI.GraphiteDeep.copy(alpha = 0.8f)
            )
        }
    }
}
