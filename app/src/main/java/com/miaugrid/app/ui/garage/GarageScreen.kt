package com.miaugrid.app.ui.garage

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miaugrid.app.ui.components.CarIcon
import com.miaugrid.app.ui.components.DrivingRoad
import com.miaugrid.app.ui.components.ScreenScaffold
import com.miaugrid.app.ui.components.drawCar
import com.miaugrid.app.ui.components.pressScale
import com.miaugrid.app.ui.components.toLook
import com.miaugrid.app.ui.theme.Road
import com.miaugrid.core.settings.CarBody
import com.miaugrid.core.settings.CarDecal
import com.miaugrid.core.settings.CarStyle

fun CarBody.label(): String = when (this) {
    CarBody.SEDAN -> "Sedã"
    CarBody.SPORT -> "Esportivo"
    CarBody.BUGGY -> "Buggy"
    CarBody.PICKUP -> "Picape"
    CarBody.VAN -> "Van"
    CarBody.FORMULA -> "Fórmula"
}

fun CarDecal.label(): String = when (this) {
    CarDecal.NONE -> "Liso"
    CarDecal.STRIPES -> "Faixas"
    CarDecal.NUMBER -> "Número"
    CarDecal.CHECKERED -> "Xadrez"
    CarDecal.FLAMES -> "Chamas"
}

private val BodyColors = listOf(
    0xFFE5383B, 0xFFFF7A1A, 0xFFFFC93C, 0xFF2EC4B6, 0xFF1B7F4B, 0xFF1D5FA8,
    0xFF6A4C93, 0xFFF15BB5, 0xFF2B2D42, 0xFFF4F4F6, 0xFF8D99AE, 0xFF8B5E34,
)
private val AccentColors = listOf(0xFFFFFFFF, 0xFF1C1D2B, 0xFFFFC93C, 0xFFFF5A36, 0xFF2EC4B6, 0xFFB0B5C4)

private fun Long.argb() = Color(this)
private fun Color.argbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

@Composable
fun GarageScreen(viewModel: GarageViewModel, onBack: () -> Unit) {
    val style by viewModel.style.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    ScreenScaffold(title = "Garagem", onBack = onBack) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .testTag("garage"),
        ) {
            Turntable(style)
            Spacer(Modifier.height(8.dp))
            DrivingRoad(style.toLook(), height = 56.dp, periodMillis = 2600)

            Section("Modelo")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CarBody.entries.forEach { body ->
                    BodyOption(style.copy(body = body), selected = style.body == body, enabled = ready) {
                        viewModel.edit { it.copy(body = body) }
                    }
                }
            }

            Section("Pintura")
            SwatchRow(BodyColors, style.bodyColor, "paint", ready) { c -> viewModel.edit { it.copy(bodyColor = c) } }
            HueSlider(style.bodyColor, ready) { c -> viewModel.edit { it.copy(bodyColor = c) } }

            Section("Cor dos detalhes")
            SwatchRow(AccentColors, style.accentColor, "accent", ready) { c -> viewModel.edit { it.copy(accentColor = c) } }

            Section("Adesivo")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CarDecal.entries.forEach { decal ->
                    DecalChip(decal, selected = style.decal == decal, enabled = ready) { viewModel.edit { it.copy(decal = decal) } }
                }
            }

            if (style.decal == CarDecal.NUMBER) {
                Section("Número de corrida")
                NumberStepper(style.number, ready) { n -> viewModel.edit { it.copy(number = n) } }
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = viewModel::reset, enabled = ready, modifier = Modifier.align(Alignment.CenterHorizontally).testTag("garage_reset")) {
                Text("Voltar ao carro original")
            }
            Text(
                "As mudanças são salvas automaticamente e aparecem no tabuleiro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp),
            )
        }
    }
}

/** Plataforma giratória com o carro em destaque; "quica" a cada mudança. */
@Composable
private fun Turntable(style: CarStyle) {
    val spin = rememberInfiniteTransition(label = "turntable")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "angle")
    val bump = remember(style) { androidx.compose.animation.core.Animatable(0.85f) }
    androidx.compose.runtime.LaunchedEffect(style) { bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 400f)) }
    val measurer = rememberTextMeasurer()
    val look = style.toLook()
    Box(
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .semantics { contentDescription = "Prévia: ${style.body.label()}, adesivo ${style.decal.label()}" }
            .testTag("garage_preview"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(210.dp).scale(bump.value)) {
            val r = size.minDimension / 2
            drawCircle(Brush.radialGradient(listOf(Road.Asphalt, Road.AsphaltDeep), center, r), r)
            drawCircle(Road.TaxiYellow, r * 0.96f, style = Stroke(r * 0.03f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.12f, r * 0.08f))))
            rotate(angle) {
                for (k in 0 until 12) {
                    val a = Math.toRadians(k * 30.0)
                    val dir = Offset(kotlin.math.cos(a).toFloat(), kotlin.math.sin(a).toFloat())
                    drawLine(Color.White.copy(alpha = 0.08f), center + dir * (r * 0.35f), center + dir * (r * 0.85f), r * 0.02f)
                }
            }
            drawCar(center, r * 1.45f, look, rotation = angle, textMeasurer = measurer)
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
private fun BodyOption(style: CarStyle, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val border by animateFloatAsState(if (selected) 3f else 1f, label = "border")
    Column(
        Modifier
            .width(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .border(border.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onClick)
            .pressScale(interaction)
            .semantics { this.selected = selected }
            .padding(8.dp)
            .testTag("body_${style.body.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CarIcon(style.toLook(), Modifier.size(56.dp))
        Text(style.body.label(), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SwatchRow(colors: List<Long>, current: Long, tagPrefix: String, enabled: Boolean, onPick: (Long) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEachIndexed { i, c ->
            val selected = c == current
            val scale by animateFloatAsState(if (selected) 1.15f else 1f, spring(dampingRatio = 0.45f), label = "swatch")
            Box(
                Modifier
                    .padding(4.dp)
                    .size(38.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(c.argb())
                    .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(enabled = enabled, role = Role.RadioButton) { onPick(c) }
                    .semantics { this.selected = selected; contentDescription = "Cor ${i + 1}" }
                    .testTag("${tagPrefix}_$i"),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = if (c.argb().luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/** Qualquer tom: matiz livre com saturação e brilho fixos (sempre uma cor viva e legível). */
@Composable
private fun HueSlider(current: Long, enabled: Boolean, onPick: (Long) -> Unit) {
    val hsv = FloatArray(3)
    val color = current.argb()
    rgbToHsv(color, hsv)
    val rainbow = remember { Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 0.8f, 0.92f) }) }
    Column {
        Box(Modifier.fillMaxWidth().height(10.dp).padding(horizontal = 10.dp).clip(RoundedCornerShape(5.dp)).background(rainbow))
        Slider(
            value = hsv[0],
            onValueChange = { h -> onPick(Color.hsv(h.coerceIn(0f, 359.9f), 0.8f, 0.92f).argbLong()) },
            valueRange = 0f..360f,
            enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent),
            modifier = Modifier.testTag("paint_hue").semantics { contentDescription = "Tom personalizado" },
        )
    }
}

private fun rgbToHsv(color: Color, out: FloatArray) {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6f)
        max == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }
    out[0] = (h + 360f) % 360f
    out[1] = if (max == 0f) 0f else d / max
    out[2] = max
}

@Composable
private fun DecalChip(decal: CarDecal, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Text(
        decal.label(),
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Road.AsphaltDeep else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Road.TaxiYellow else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) Road.TaxiYellow else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.RadioButton, onClick = onClick)
            .pressScale(interaction)
            .semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("decal_${decal.name.lowercase()}"),
    )
}

@Composable
private fun NumberStepper(number: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        FilledTonalIconButton(onClick = { onChange((number + 99) % 100) }, enabled = enabled, modifier = Modifier.testTag("number_minus")) {
            Icon(Icons.Filled.Remove, contentDescription = "Diminuir número")
        }
        Text(
            number.toString().padStart(2, '0'),
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.testTag("car_number"),
        )
        FilledTonalIconButton(onClick = { onChange((number + 1) % 100) }, enabled = enabled, modifier = Modifier.testTag("number_plus")) {
            Icon(Icons.Filled.Add, contentDescription = "Aumentar número")
        }
    }
}
