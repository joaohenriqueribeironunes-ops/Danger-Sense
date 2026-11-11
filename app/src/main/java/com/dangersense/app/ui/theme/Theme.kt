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
    // Amarelo mostarda: paleta total, minimalista (inspiração iOS 26).
    val GraphiteDeep = Color(0xFF2A2108)
    val Graphite = Color(0xFF3A2E0A)
    val GraphiteSurface = Color(0xFF4A3B0E)
    val GraphiteElevated = Color(0xFF5C4A13)
    val GraphiteGreen = Color(0xFF4F4010)
    val GraphiteGreenLine = Color(0xFFB9921A)

    val PureBlack = Color(0xFF1F1906)
    val MatteBlack = Color(0xFF2A2108)

    val Aurora = Color(0xFFE0B000)
    val AuroraDim = Color(0xFFC79A00)
    val AuroraSoft = Color(0xFFFFE08A)
    val Mustard = Color(0xFFD4A017)
    val MustardLight = Color(0xFFF2C94C)
    val MustardDark = Color(0xFF8C6A0A)
    val Alert = Color(0xFFFFC107)
    val TextPrimary = Color(0xFFFFF6DC)
    val TextSecondary = Color(0xFFE6D2A3)
    val RadiusXl = 28
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
