package com.miaugrid.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Identidade "VrumGrid": asfalto, tinta de pista, placas de estrada e laranja de corrida. */
object Road {
    val Asphalt = Color(0xFF2B2D42)
    val AsphaltDeep = Color(0xFF1C1D2B)
    val LanePaint = Color(0xFFF7F3E3)
    val TaxiYellow = Color(0xFFFFC93C)
    val RacingOrange = Color(0xFFFF5A36)
    val SignGreen = Color(0xFF1B7F4B)
    val SignBlue = Color(0xFF1D5FA8)
    val Cone = Color(0xFFFF7A1A)
    val Sand = Color(0xFFF4EFE6)
}

private val LightColors = lightColorScheme(
    primary = Color(0xFFE0461F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD0),
    onPrimaryContainer = Color(0xFF3B0A00),
    secondary = Road.Asphalt,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E2EE),
    onSecondaryContainer = Road.AsphaltDeep,
    tertiary = Road.SignGreen,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEFC2),
    onTertiaryContainer = Color(0xFF3A2C00),
    background = Road.Sand,
    onBackground = Road.AsphaltDeep,
    surface = Color(0xFFFFFCF7),
    onSurface = Road.AsphaltDeep,
    surfaceVariant = Color(0xFFEAE4D9),
    onSurfaceVariant = Color(0xFF55576A),
    error = Color(0xFFC62828),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8A65),
    onPrimary = Color(0xFF4A1300),
    primaryContainer = Color(0xFF6B2410),
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = Color(0xFFC5C6DA),
    onSecondary = Road.AsphaltDeep,
    secondaryContainer = Color(0xFF3A3C55),
    onSecondaryContainer = Color(0xFFE2E2EE),
    tertiary = Color(0xFF7FD6A4),
    onTertiary = Color(0xFF00391E),
    tertiaryContainer = Color(0xFF4A3D10),
    onTertiaryContainer = Color(0xFFFFEFC2),
    background = Color(0xFF14151E),
    onBackground = Color(0xFFE8E6F0),
    surface = Color(0xFF1E2030),
    onSurface = Color(0xFFE8E6F0),
    surfaceVariant = Color(0xFF2E3044),
    onSurfaceVariant = Color(0xFFC3C4D6),
    error = Color(0xFFFF8A80),
)

/** Cores do tabuleiro (mapa da cidade), separadas do tema Material. */
@Immutable
data class BoardColors(
    val regions: List<Color>,
    val street: Color,
    val streetPaint: Color,
    val selection: Color,
    val markX: Color,
    val blockedDot: Color,
    val error: Color,
    val hint: Color,
    val parkedCars: List<Color>,
    val fixedCar: Color,
    val patternInk: Color,
    val trail: Color,
)

/** Cores dos "bairros" (12 regiões). A cor nunca é o único indicador: ver modo de cores acessível. */
val RegionPalette = listOf(
    Color(0xFF7CC6FE), Color(0xFFFFB4C8), Color(0xFFB79CED), Color(0xFFA8E6A1),
    Color(0xFFFFD670), Color(0xFFFFA372), Color(0xFF8EE3E0), Color(0xFFD9B48F),
    Color(0xFFF49AC1), Color(0xFFB8C4D6), Color(0xFFD4E79E), Color(0xFFE6A8FF),
)

/** Nome de cada bairro, usado nos textos de dica (sempre junto da letra). */
val RegionColorNames = listOf(
    "azul", "rosa", "lilás", "verde", "amarelo", "laranja",
    "turquesa", "caramelo", "pink", "cinza", "lima", "violeta",
)

/** Carros estacionados: tom mais escuro e saturado do bairro, para contrastar com o lote. */
private val ParkedCars = listOf(
    Color(0xFF1565C0), Color(0xFFD81B60), Color(0xFF5E35B1), Color(0xFF2E7D32),
    Color(0xFFF9A825), Color(0xFFE64A19), Color(0xFF00897B), Color(0xFF6D4C41),
    Color(0xFFC2185B), Color(0xFF455A64), Color(0xFF7CB342), Color(0xFF8E24AA),
)

private val LightBoard = BoardColors(
    regions = RegionPalette,
    street = Road.Asphalt,
    streetPaint = Road.LanePaint.copy(alpha = 0.55f),
    selection = Road.TaxiYellow,
    markX = Color(0xFF2B2D42),
    blockedDot = Color(0x802B2D42),
    error = Color(0xFFE53935),
    hint = Road.TaxiYellow,
    parkedCars = ParkedCars,
    fixedCar = Color(0xFF78909C),
    patternInk = Color(0x552B2D42),
    trail = Color(0x332B2D42),
)

private val DarkBoard = LightBoard.copy(
    regions = RegionPalette.map { it.copy(alpha = 0.85f) },
    street = Road.AsphaltDeep,
)

val LocalBoardColors = staticCompositionLocalOf { LightBoard }

private val AppTypography = Typography().let { base ->
    base.copy(
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = (-1).sp),
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
    )
}

@Composable
fun MiauGridTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBoardColors provides if (darkTheme) DarkBoard else LightBoard) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}
