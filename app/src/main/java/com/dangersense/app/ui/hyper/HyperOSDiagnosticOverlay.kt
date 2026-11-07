package com.dangersense.app.ui.hyper

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dangersense.app.ai.DiagnosticoAdaptativo
import com.dangersense.app.domain.MainViewModel
import com.dangersense.app.ui.theme.OneUI

private const val BLUR_RADIUS = 30f

private enum class CategoriaInsight(
    val rotulo: String,
    val acento: Color,
    val chaves: List<String>
) {
    SONO(
        "Sono", HyperOS.HyperBlue,
        listOf("sono", "dorme", "dormir", "noite", "repouso", "insônia", "acordou")
    ),
    ALIMENTACAO(
        "Alimentação", HyperOS.VitalGreen,
        listOf("aliment", "cafeína", "cafein", "glúten", "gluten", "comida",
            "refei", "açúcar", "suplement", "ômega", "omega")
    ),
    FOCO(
        "Foco", HyperOS.AlertOrange,
        listOf("foco", "atenç", "atenca", "tarefa", "procrast", "estímulo",
            "estimulo", "concentr", "sensorial", "sobrecarga")
    )
}

/** Catálogo de cores/gradientes compartilhado por todos os tokens. */
private fun acentoDe(d: DiagnosticoAdaptativo): Color = when {
    d.nivelVulnerabilidade >= 75 -> HyperOS.AlertRed
    d.nivelVulnerabilidade >= 50 -> HyperOS.AlertOrange
    else -> HyperOS.VitalGreen
}

private fun categoriasAtivas(texto: String): Set<CategoriaInsight> =
    CategoriaInsight.entries.filter { c -> c.chaves.any { texto.contains(it, ignoreCase = true) } }
        .toSet()

/**
 * Cartão de diagnóstico sobreposto da HyperOS 1.
 * Squircle 28dp + camada acrylic com desfoque gaussiano, tipografia MiSans,
 * barra ultrafina de vulnerabilidade, chips de ícone e botão-pílula com spring.
 */
@Composable
fun HyperOSDiagnosticOverlay(
    diagnostico: DiagnosticoAdaptativo?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val d = diagnostico
    val springSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    AnimatedVisibility(
        visible = d != null,
        modifier = modifier,
        enter = fadeIn(springSpec) + scaleIn(initialScale = 0.92f, animationSpec = springSpec),
        exit = fadeOut(springSpec) + scaleOut(targetScale = 0.9f, animationSpec = springSpec)
    ) {
        if (d != null) {
            OverlayCard(d = d, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun OverlayCard(d: DiagnosticoAdaptativo, onDismiss: () -> Unit) {
    val texto = d.insightPrincipal + " " + d.correlacaoDetectada + " " + d.recomendacaoClinica
    val ativas = remember(d) { categoriasAtivas(texto) }
    val shape = HyperOS.SquircleLg

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 26.dp)
        ) {
            // Vidro fosco: camada acrylic desfocada atrás do cartão.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= 31) {
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(
                                    BLUR_RADIUS, BLUR_RADIUS,
                                    android.graphics.Shader.TileMode.CLAMP
                                )
                                .asComposeRenderEffect()
                        }
                        scaleX = 1.05f
                        scaleY = 1.05f
                    }
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                acentoDe(d).copy(alpha = 0.40f),
                                Color.White.copy(alpha = 0.12f),
                                acentoDe(d).copy(alpha = 0.22f)
                            )
                        )
                    )
            )

            Card(
                shape = shape,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                colors = CardDefaults.cardColors(
                    containerColor = HyperOS.Surface.copy(alpha = 0.80f)
                )
            ) {
                CardContent(d = d, ativas = ativas, onDismiss = onDismiss)
            }
        }
    }
}

@Composable
private fun CardContent(
    d: DiagnosticoAdaptativo,
    ativas: Set<CategoriaInsight>,
    onDismiss: () -> Unit
) {
    val accent = acentoDe(d)
    val alvo = d.nivelVulnerabilidade.coerceIn(0, 100) / 100f
    val animado by animateFloatAsState(
        targetValue = alvo,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "vulnerabilidade"
    )

    Column(modifier = Modifier.padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Diagnóstico Adaptativo",
                style = MiSans.Titulo,
                fontSize = 20.sp,
                color = OneUI.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(text = d.origem, style = MiSans.Legenda, fontSize = 10.sp, color = OneUI.TextSecondary)
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Nível de vulnerabilidade",
                style = MiSans.Legenda,
                fontSize = 12.sp,
                color = OneUI.TextSecondary
            )
            Text(
                text = "${d.nivelVulnerabilidade}",
                style = MiSans.Titulo,
                fontSize = 26.sp,
                color = accent
            )
        }

        Spacer(Modifier.height(8.dp))

        // Barra horizontal ultrafina (3dp) com gradiente sutil.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(HyperOS.Capsule)
                .background(HyperOS.Elevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animado)
                    .height(3.dp)
                    .clip(HyperOS.Capsule)
                    .background(
                        Brush.horizontalGradient(
                            listOf(HyperOS.VitalGreen, HyperOS.AlertOrange, HyperOS.AlertRed)
                        )
                    )
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = d.insightPrincipal,
            style = MiSans.Subtitulo,
            fontSize = 15.sp,
            color = OneUI.TextPrimary,
            lineHeight = 21.sp
        )

        if (d.correlacaoDetectada.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Correlação detectada",
                style = MiSans.Legenda,
                fontSize = 11.sp,
                color = accent
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = d.correlacaoDetectada,
                style = MiSans.Corpo,
                fontSize = 13.sp,
                color = OneUI.TextSecondary,
                lineHeight = 18.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoriaInsight.entries.forEach { c ->
                InsightChip(
                    categoria = c,
                    ativo = c in ativas,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (d.recomendacaoClinica.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(
                text = d.recomendacaoClinica,
                style = MiSans.Corpo,
                fontSize = 13.sp,
                color = HyperOS.HyperBlue,
                lineHeight = 18.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(HyperOS.HyperBlue.copy(alpha = 0.10f))
                    .padding(14.dp)
            )
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = HyperOS.HyperBlue,
                contentColor = HyperOS.TrueBlack
            )
        ) {
            Text(text = "Entendido", style = MiSans.Subtitulo, fontSize = 15.sp)
        }
    }
}

@Composable
private fun InsightChip(
    categoria: CategoriaInsight,
    ativo: Boolean,
    modifier: Modifier = Modifier
) {
    val alpha = if (ativo) 1f else 0.35f

    Row(
        modifier = modifier
            .clip(HyperOS.Capsule)
            .background(HyperOS.Elevated)
            .border(
                1.dp,
                categoria.acento.copy(alpha = if (ativo) 0.55f else 0.15f),
                HyperOS.Capsule
            )
            .padding(horizontal = 7.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InsightIcon(categoria = categoria, modifier = Modifier.size(22.dp))
        Spacer(Modifier.size(5.dp))
        Text(
            text = categoria.rotulo,
            style = MiSans.Legenda,
            fontSize = 11.sp,
            color = OneUI.TextPrimary.copy(alpha = alpha),
            maxLines = 1
        )
    }
}

/** Ícone minimalista: cantos arredondados, glifo geométrico simples. */
@Composable
private fun InsightIcon(categoria: CategoriaInsight, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(HyperOS.Surface)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val s = size.minDimension
            val traco = s * 0.16f
            when (categoria) {
                CategoriaInsight.SONO -> {
                    // Lua crescente.
                    drawCircle(color = categoria.acento, radius = s / 2f)
                    drawCircle(
                        color = HyperOS.Surface,
                        radius = s / 2f,
                        center = Offset(s * 0.80f, s * 0.26f)
                    )
                }
                CategoriaInsight.ALIMENTACAO -> {
                    // Prato: anel + centro.
                    drawCircle(
                        color = categoria.acento,
                        radius = (s - traco) / 2f,
                        style = Stroke(traco)
                    )
                    drawCircle(color = categoria.acento, radius = s * 0.16f)
                }
                CategoriaInsight.FOCO -> {
                    // Alvo de foco.
                    drawCircle(
                        color = categoria.acento,
                        radius = (s - traco) / 2f,
                        style = Stroke(traco)
                    )
                    drawCircle(
                        color = categoria.acento,
                        radius = s * 0.24f,
                        style = Stroke(traco)
                    )
                    drawCircle(color = categoria.acento, radius = s * 0.07f)
                }
            }
        }
    }
}

/** Cartão que dispara a geração do diagnóstico adaptativo. */
@Composable
fun DiagnosticoTriggerCard(vm: MainViewModel) {
    val status by vm.diagStatus.collectAsStateWithLifecycle()
    val rolando = status == "analisando"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(HyperOS.SquircleLg)
            .background(HyperOS.Surface)
            .border(1.dp, HyperOS.HyperBlue.copy(alpha = 0.22f), HyperOS.SquircleLg)
            .padding(20.dp)
    ) {
        Text(
            text = "Memória adaptável de rotina",
            style = MiSans.Titulo,
            fontSize = 17.sp,
            color = OneUI.TextPrimary
        )
        Text(
            text = "Sono, alimentação, atividades e crises · últimos 7-14 dias",
            style = MiSans.Corpo,
            fontSize = 12.sp,
            color = OneUI.TextSecondary
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { vm.gerarDiagnostico() },
            enabled = !rolando,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HyperOS.HyperBlue,
                contentColor = HyperOS.TrueBlack
            )
        ) {
            Text(
                text = if (rolando) "Analisando padrões…" else "Gerar diagnóstico adaptativo",
                style = MiSans.Subtitulo,
                fontSize = 15.sp
            )
        }

        if (status.isNotBlank() && !rolando) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = status,
                style = MiSans.Legenda,
                fontSize = 12.sp,
                color = if (status == "ok") HyperOS.VitalGreen else HyperOS.AlertOrange
            )
        }
    }
}
