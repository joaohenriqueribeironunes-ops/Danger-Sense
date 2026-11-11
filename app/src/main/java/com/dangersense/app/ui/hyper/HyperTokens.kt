package com.dangersense.app.ui.hyper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dangersense.app.ui.theme.OneUI

/**
 * Tokens de design HyperOS 1: squircles 24/28dp, acrylic glass,
 * Azul Hyper (foco), Verde Vitalidade (positivo), Laranja/Vermelho (risco).
 */
object HyperOS {
    val TrueBlack = Color(0xFF1F1906)
    val PureBlack = Color(0xFF1F1906)
    val MatteBlack = Color(0xFF2A2108)
    val Graphite = Color(0xFF2F2508)
    val Surface = Color(0xFF46380D)
    val Elevated = Color(0xFF5A4812)
    val HyperBlue = Color(0xFFD4A017)
    val HyperBlueDim = Color(0xFFB38600)
    val VitalGreen = Color(0xFFF2C94C)
    val AlertOrange = Color(0xFFFFC107)
    val AlertRed = Color(0xFFD97706)
    val TextPrimary = Color(0xFFFFF6DC)
    val TextSecondary = Color(0xFFE6D2A3)

    val SquircleLg: RoundedCornerShape = RoundedCornerShape(28.dp)
    val SquircleMd: RoundedCornerShape = RoundedCornerShape(22.dp)
    val Capsule: RoundedCornerShape = RoundedCornerShape(50)
}

/**
 * MiSans. Enquanto `res/font/misans.ttf` não for incluído, usa o sans-serif do
 * sistema (métricas equivalentes) com os pesos MiSans. Trocar [Familia] por
 * `FontFamily(Font(R.font.misans))` ativa a fonte original.
 */
object MiSans {
    val Familia: FontFamily = FontFamily.SansSerif
    val Titulo: TextStyle = TextStyle(fontFamily = Familia, fontWeight = FontWeight.Bold)
    val Subtitulo: TextStyle = TextStyle(fontFamily = Familia, fontWeight = FontWeight.SemiBold)
    val Legenda: TextStyle = TextStyle(fontFamily = Familia, fontWeight = FontWeight.Medium)
    val Corpo: TextStyle = TextStyle(fontFamily = Familia, fontWeight = FontWeight.Normal)
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

/**
 * Medidor de decibéis em tempo real: leitura grande do dB atual,
 * faixa fina de nível e rótulo de status. Substitui o antigo gauge de risco.
 */
@Composable
fun DecibelMeter(db: Float, modifier: Modifier = Modifier) {
    val normalizado = ((db - 30f) / 90f).coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = normalizado, label = "db")
    val status = when {
        db <= 0f -> "aguardando microfone"
        db < 40f -> "silêncio"
        db < 60f -> "ambiente calmo"
        db < 75f -> "ruído moderado"
        db < 85f -> "ruído alto"
        else -> "ruído crítico"
    }
    val accent = when {
        db < 60f -> OneUI.MustardLight
        db < 75f -> OneUI.Mustard
        db < 85f -> OneUI.Aurora
        else -> HyperOS.AlertRed
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (db > 0f) "%.0f".format(db) else "—",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    color = OneUI.TextPrimary
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    "dB",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = OneUI.TextSecondary,
                    modifier = Modifier.padding(bottom = 9.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(HyperOS.Capsule)
                    .background(HyperOS.Surface)
                    .border(1.dp, accent.copy(alpha = 0.45f), HyperOS.Capsule)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(status, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = accent)
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(HyperOS.Capsule)
                .background(HyperOS.Elevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(4.dp)
                    .clip(HyperOS.Capsule)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                OneUI.MustardLight,
                                OneUI.Mustard,
                                OneUI.Aurora,
                                HyperOS.AlertRed
                            )
                        )
                    )
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("30", "60", "90", "120").forEach {
                Text(it, fontSize = 10.sp, color = OneUI.TextSecondary.copy(alpha = 0.7f))
            }
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
            .background(HyperOS.PureBlack)
            .border(1.dp, accent.copy(alpha = 0.55f), HyperOS.Capsule)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = OneUI.TextPrimary)
    }
}
