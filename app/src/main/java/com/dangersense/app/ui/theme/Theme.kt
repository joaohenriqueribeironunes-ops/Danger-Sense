package com.dangersense.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object OneUI {
    // Verde Graphite: fundo e superfícies estruturais.
    val GraphiteDeep = Color(0xFF080D0B)
    val Graphite = Color(0xFF0F1714)
    val GraphiteSurface = Color(0xFF161F1B)
    val GraphiteElevated = Color(0xFF1F2B26)
    val GraphiteGreen = Color(0xFF1B2C26)
    val GraphiteGreenLine = Color(0xFF2E463D)

    // Contraste minimalista dos botões de ação.
    val PureBlack = Color(0xFF000000)
    val MatteBlack = Color(0xFF0A0B0A)

    // Acentos (fora de botões de ação).
    val Aurora = Color(0xFF6EE7C8)
    val AuroraDim = Color(0xFF3DBFA0)
    val AuroraSoft = Color(0xFFA8F2DF)
    val Alert = Color(0xFFFFD426)
    val TextPrimary = Color(0xFFEAF4F0)
    val TextSecondary = Color(0xFF9DB3AC)
    val RadiusXl = 32
}

private val Scheme = darkColorScheme(
    primary = OneUI.Aurora,
    onPrimary = OneUI.PureBlack,
    secondary = OneUI.AuroraDim,
    background = OneUI.Graphite,
    onBackground = OneUI.TextPrimary,
    surface = OneUI.GraphiteSurface,
    onSurface = OneUI.TextPrimary,
    surfaceVariant = OneUI.GraphiteElevated,
    onSurfaceVariant = OneUI.TextSecondary,
    error = OneUI.Alert,
    onError = OneUI.GraphiteDeep
)

private val OneTypography = Typography(
    displayLarge = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.SemiBold, lineHeight = 46.sp),
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 34.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun SenseCalmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = OneTypography,
        content = content
    )
}
