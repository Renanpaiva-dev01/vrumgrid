package com.miaugrid.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Plum = Color(0xFF4A3B52)
private val Tangerine = Color(0xFFF28C38)
private val Cream = Color(0xFFFFF4E8)
private val Berry = Color(0xFFD94E6A)

private val LightColors = lightColorScheme(
    primary = Color(0xFFB85C1E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF3A1A00),
    secondary = Plum,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEBDCF2),
    onSecondaryContainer = Color(0xFF2B1D33),
    tertiary = Color(0xFF2F7D6D),
    background = Cream,
    onBackground = Color(0xFF2E2433),
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF2E2433),
    surfaceVariant = Color(0xFFF1E4D8),
    onSurfaceVariant = Color(0xFF5A4B5F),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB783),
    onPrimary = Color(0xFF552100),
    primaryContainer = Color(0xFF7A3A0A),
    onPrimaryContainer = Color(0xFFFFDCC2),
    secondary = Color(0xFFD5C0DF),
    onSecondary = Color(0xFF3A2B42),
    secondaryContainer = Color(0xFF524159),
    onSecondaryContainer = Color(0xFFF2DCF9),
    tertiary = Color(0xFF8BD3C1),
    background = Color(0xFF1E1B24),
    onBackground = Color(0xFFECE0EA),
    surface = Color(0xFF26222D),
    onSurface = Color(0xFFECE0EA),
    surfaceVariant = Color(0xFF3A3441),
    onSurfaceVariant = Color(0xFFCFC2D3),
    error = Color(0xFFFFB4AB),
)

/** Cores do tabuleiro, separadas do tema Material para controle fino de contraste. */
@Immutable
data class BoardColors(
    val regions: List<Color>,
    val gridLine: Color,
    val regionBorder: Color,
    val selection: Color,
    val lineHighlight: Color,
    val markX: Color,
    val blockedDot: Color,
    val error: Color,
    val hint: Color,
    val catFur: Color,
    val catFurFixed: Color,
    val catDetail: Color,
    val catInnerEar: Color,
    val patternInk: Color,
)

/** Paleta pastel original (12 regiões). A cor nunca é o único indicador: ver modo daltônico. */
val RegionPalette = listOf(
    Color(0xFFFFB3BA), Color(0xFFFFDFBA), Color(0xFFFFF3B0), Color(0xFFBAFFC9),
    Color(0xFFBAE1FF), Color(0xFFD7BAFF), Color(0xFFFFC8E6), Color(0xFFC9F2E8),
    Color(0xFFE6D3B3), Color(0xFFC4C9FF), Color(0xFFE2F0B5), Color(0xFFF5C7A9),
)

/** Nome da cor de cada região, usado em textos de dica (sempre junto com a letra). */
val RegionColorNames = listOf(
    "rosa", "pêssego", "amarela", "menta", "azul-céu", "lavanda",
    "rosa-chiclete", "água", "areia", "pervinca", "lima", "damasco",
)

private val LightBoard = BoardColors(
    regions = RegionPalette,
    gridLine = Color(0x332E2433),
    regionBorder = Plum,
    selection = Color(0xFF2F7D6D),
    lineHighlight = Color(0x22FFFFFF),
    markX = Color(0xFF5A4B5F),
    blockedDot = Color(0x665A4B5F),
    error = Berry,
    hint = Color(0xFFFFB300),
    catFur = Tangerine,
    catFurFixed = Color(0xFF8C8FA8),
    catDetail = Color(0xFF2E2433),
    catInnerEar = Color(0xFFFFC9D6),
    patternInk = Color(0x402E2433),
)

private val DarkBoard = LightBoard.copy(
    regions = RegionPalette.map { it.copy(alpha = 0.82f) },
    regionBorder = Color(0xFF1A161F),
)

val LocalBoardColors = staticCompositionLocalOf { LightBoard }

private val AppTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    )
}

@Composable
fun MiauGridTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalBoardColors provides if (darkTheme) DarkBoard else LightBoard) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
