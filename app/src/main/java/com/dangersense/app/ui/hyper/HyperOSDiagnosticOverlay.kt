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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.dangersense.app.data.IngestaoEntity
import com.dangersense.app.domain.MainViewModel
import com.dangersense.app.ui.theme.OneUI

private const val BLUR_RADIUS = 30f

/** Verde Graphite: cor estrutural e de texto do diagnóstico adaptativo. */
private val VerdeGrafite = OneUI.GraphiteGreen
private val VerdeGrafiteLinha = OneUI.GraphiteGreenLine
private val VerdeGrafiteTexto = OneUI.AuroraSoft
private val VerdeGrafiteRotulo = OneUI.AuroraDim

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

private fun acentoDe(d: DiagnosticoAdaptativo): Color = when {
    d.nivelVulnerabilidade >= 75 -> HyperOS.AlertRed
    d.nivelVulnerabilidade >= 50 -> HyperOS.AlertOrange
    else -> HyperOS.VitalGreen
}

private fun categoriasAtivas(texto: String): Set<CategoriaInsight> =
    CategoriaInsight.entries.filter { c -> c.chaves.any { texto.contains(it, ignoreCase = true) } }
        .toSet()

/**
 * Overlay minimalista de diagnóstico integrado à tela principal.
 * Squircle 28dp + camada acrylic desfocada, tema Verde Graphite,
 * conteúdo integral com rolagem e gatilhos alimentares detalhados.
 */
@Composable
fun HyperOSDiagnosticOverlay(
    diagnostico: DiagnosticoAdaptativo?,
    ingestoes: List<IngestaoEntity>,
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
            OverlayCard(d = d, ingestoes = ingestoes, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun OverlayCard(
    d: DiagnosticoAdaptativo,
    ingestoes: List<IngestaoEntity>,
    onDismiss: () -> Unit
) {
    val scroll = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .verticalScroll(scroll),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AcrylicLayer(modifier = Modifier.matchParentSize())

                Card(
                    shape = HyperOS.SquircleLg,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, VerdeGrafiteLinha),
                    colors = CardDefaults.cardColors(
                        containerColor = OneUI.GraphiteSurface.copy(alpha = 0.94f)
                    )
                ) {
                    CardContent(
                        d = d,
                        ativas = categoriasAtivas(
                            d.insightPrincipal + " " + d.correlacaoDetectada + " " +
                                    d.recomendacaoClinica
                        ),
                        ingestoes = ingestoes
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = HyperOS.Capsule,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HyperOS.PureBlack,
                    contentColor = OneUI.TextPrimary
                ),
                border = BorderStroke(1.dp, VerdeGrafiteLinha)
            ) {
                Text(text = "Entendido", style = MiSans.Subtitulo, fontSize = 15.sp)
            }
        }
    }
}

/** Camada acrylic: desfoque gaussiano atrás do cartão gerando o halo de vidro fosco. */
@Composable
private fun AcrylicLayer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= 31) {
                    renderEffect = android.graphics.RenderEffect
                        .createBlurEffect(
                            BLUR_RADIUS, BLUR_RADIUS,
                            android.graphics.Shader.TileMode.CLAMP
                        )
                        .asComposeRenderEffect()
                }
                scaleX = 1.03f
                scaleY = 1.03f
            }
            .clip(HyperOS.SquircleLg)
            .background(
                Brush.linearGradient(
                    listOf(
                        VerdeGrafite.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.10f),
                        VerdeGrafite.copy(alpha = 0.35f)
                    )
                )
            )
    )
}

@Composable
private fun CardContent(
    d: DiagnosticoAdaptativo,
    ativas: Set<CategoriaInsight>,
    ingestoes: List<IngestaoEntity>
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
                color = VerdeGrafiteTexto,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = d.origem,
                style = MiSans.Legenda,
                fontSize = 10.sp,
                color = OneUI.TextSecondary
            )
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
                            listOf(
                                HyperOS.VitalGreen,
                                HyperOS.AlertOrange,
                                HyperOS.AlertRed
                            )
                        )
                    )
            )
        }

        Spacer(Modifier.height(16.dp))

        BlocoDiagnostico(rotulo = "Insight", texto = d.insightPrincipal, destaque = true)

        if (d.correlacaoDetectada.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            BlocoDiagnostico(
                rotulo = "Correlação detectada",
                texto = d.correlacaoDetectada,
                destaque = false
            )
        }

        if (d.recomendacaoClinica.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            BlocoDiagnostico(
                rotulo = "Recomendação clínica",
                texto = d.recomendacaoClinica,
                destaque = false
            )
        }

        Spacer(Modifier.height(16.dp))

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

        if (ingestoes.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            GatilhosAlimentares(ingestoes)
        }
    }
}

/** Bloco de texto integral do diagnóstico, sem truncamento, em Verde Graphite. */
@Composable
private fun BlocoDiagnostico(rotulo: String, texto: String, destaque: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (destaque) VerdeGrafite else HyperOS.Graphite)
            .border(
                1.dp,
                VerdeGrafiteLinha.copy(alpha = if (destaque) 0.9f else 0.5f),
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Text(
            text = rotulo,
            style = MiSans.Legenda,
            fontSize = 11.sp,
            color = VerdeGrafiteRotulo
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = texto,
            style = if (destaque) MiSans.Subtitulo else MiSans.Corpo,
            fontSize = if (destaque) 15.sp else 13.sp,
            color = if (destaque) VerdeGrafiteTexto else OneUI.TextPrimary,
            lineHeight = 20.sp
        )
    }
}

/** Tudo o que a alimentação registrada pode desencadear, com fontes completas. */
@Composable
private fun GatilhosAlimentares(ingestoes: List<IngestaoEntity>) {
    Text(
        text = "O que a alimentação pode desencadear",
        style = MiSans.Titulo,
        fontSize = 15.sp,
        color = VerdeGrafiteTexto
    )
    Spacer(Modifier.height(10.dp))

    ingestoes.take(4).forEach { item ->
        val gatilho = (item.risco_gatilho * 100).toInt()
        val corGatilho = when {
            gatilho >= 70 -> HyperOS.AlertRed
            gatilho >= 40 -> HyperOS.AlertOrange
            else -> HyperOS.VitalGreen
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(VerdeGrafite)
                .border(1.dp, VerdeGrafiteLinha.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.nome,
                    style = MiSans.Subtitulo,
                    fontSize = 15.sp,
                    color = OneUI.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "gatilho $gatilho%",
                    style = MiSans.Legenda,
                    fontSize = 12.sp,
                    color = corGatilho
                )
            }
            Text(
                text = item.categoria,
                style = MiSans.Legenda,
                fontSize = 11.sp,
                color = OneUI.TextSecondary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Mecanismo: ${item.mecanismo_cientifico}",
                style = MiSans.Corpo,
                fontSize = 13.sp,
                color = OneUI.TextPrimary,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "TEA ${fmtScore(item.impacto_tea)}",
                    style = MiSans.Legenda,
                    fontSize = 12.sp,
                    color = if (item.impacto_tea >= 0) HyperOS.VitalGreen else HyperOS.AlertOrange
                )
                Text(
                    text = "TDAH ${fmtScore(item.impacto_tdah)}",
                    style = MiSans.Legenda,
                    fontSize = 12.sp,
                    color = if (item.impacto_tdah >= 0) HyperOS.VitalGreen else HyperOS.AlertOrange
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Fontes: ${item.fontes_cientificas}",
                style = MiSans.Corpo,
                fontSize = 10.sp,
                color = OneUI.TextSecondary.copy(alpha = 0.85f),
                lineHeight = 14.sp
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
                VerdeGrafiteLinha.copy(alpha = if (ativo) 0.9f else 0.3f),
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
                    drawCircle(color = categoria.acento, radius = s / 2f)
                    drawCircle(
                        color = HyperOS.Surface,
                        radius = s / 2f,
                        center = Offset(s * 0.80f, s * 0.26f)
                    )
                }
                CategoriaInsight.ALIMENTACAO -> {
                    drawCircle(
                        color = categoria.acento,
                        radius = (s - traco) / 2f,
                        style = Stroke(traco)
                    )
                    drawCircle(color = categoria.acento, radius = s * 0.16f)
                }
                CategoriaInsight.FOCO -> {
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
            .background(OneUI.GraphiteSurface)
            .border(1.dp, VerdeGrafiteLinha, HyperOS.SquircleLg)
            .padding(20.dp)
    ) {
        Text(
            text = "Memória adaptável de rotina",
            style = MiSans.Titulo,
            fontSize = 17.sp,
            color = VerdeGrafiteTexto
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
                containerColor = HyperOS.PureBlack,
                contentColor = OneUI.TextPrimary
            ),
            border = BorderStroke(1.dp, VerdeGrafiteLinha)
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
