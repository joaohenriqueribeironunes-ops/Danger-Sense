package com.dangersense.app.ui.hyper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dangersense.app.ui.theme.OneUI

/**
 * Tokens de design HyperOS 1: squircles 24/28dp, acrylic glass,
 * Azul Hyper (foco), Verde Vitalidade (positivo), Laranja/Vermelho (risco).
 */
object HyperOS {
    val TrueBlack = Color(0xFF000000)
    val Graphite = Color(0xFF0A0C10)
    val Surface = Color(0xFF141821)
    val Elevated = Color(0xFF1E2430)
    val HyperBlue = Color(0xFF4C8DFF)
    val HyperBlueDim = Color(0xFF2E6BE6)
    val VitalGreen = Color(0xFF34C759)
    val AlertOrange = Color(0xFFFF7A45)
    val AlertRed = Color(0xFFFF4D4F)
    val TextPrimary = Color(0xFFF2F5FA)
    val TextSecondary = Color(0xFF8E97A6)

    val SquircleLg: RoundedCornerShape = RoundedCornerShape(28.dp)
    val SquircleMd: RoundedCornerShape = RoundedCornerShape(24.dp)
    val Capsule: RoundedCornerShape = RoundedCornerShape(50)
}

fun Modifier.hyperGlass(shape: androidx.compose.ui.graphics.Shape = HyperOS.SquircleLg): Modifier =
    this
        .clip(shape)
        .background(
            Brush.horizontalGradient(
                listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.05f))
            )
        )
        .border(1.dp, Color.White.copy(alpha = 0.14f), shape)

@Composable
fun StatusCapsule(
    label: String,
    value: String,
    accent: Color = HyperOS.HyperBlue,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(HyperOS.Capsule)
            .background(HyperOS.Surface)
            .border(1.dp, accent.copy(alpha = 0.35f), HyperOS.Capsule)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(HyperOS.Capsule)
                    .background(accent)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "$label $value",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = OneUI.TextPrimary
            )
        }
    }
}

@Composable
fun RiskGauge(risk: Float, size: Dp = 132.dp) {
    val animated by animateFloatAsState(targetValue = risk.coerceIn(0f, 1f), label = "gauge")
    val accent = when {
        risk >= 0.75f -> HyperOS.AlertRed
        risk >= 0.5f -> HyperOS.AlertOrange
        else -> HyperOS.VitalGreen
    }
    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = HyperOS.Elevated,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = accent,
                startAngle = 135f,
                sweepAngle = 270f * animated,
                useCenter = false,
                style = stroke
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(animated * 100).toInt()}%",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
            Text("risco", fontSize = 12.sp, color = OneUI.TextSecondary)
        }
    }
}

@Composable
fun HyperCapsule(
    onPausa: () -> Unit,
    onFalsoAlarme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .hyperGlass(HyperOS.Capsule)
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Sentidos Sobrecarregados",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = HyperOS.AlertOrange
                )
                Spacer(Modifier.height(10.dp))
                Row {
                    QuickAction("Preciso de pausa", HyperOS.HyperBlue, onPausa)
                    Spacer(Modifier.size(10.dp))
                    QuickAction("Falso Alarme", HyperOS.VitalGreen, onFalsoAlarme)
                }
            }
        }
    }
}

@Composable
private fun QuickAction(text: String, accent: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(HyperOS.Capsule)
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.5f), HyperOS.Capsule)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = accent)
    }
}
