package com.miaugrid.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.miaugrid.app.ui.theme.Road
import com.miaugrid.core.settings.CarStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Carrinho do jogador, disponível em todas as telas. */
val LocalCarLook = compositionLocalOf { CarStyle().toLook() }

/** Faixa de asfalto animada com o carrinho atravessando em loop. */
@Composable
fun DrivingRoad(look: CarLook, modifier: Modifier = Modifier, height: Dp = 72.dp, periodMillis: Int = 3200) {
    val transition = rememberInfiniteTransition(label = "road")
    val progress by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(periodMillis, easing = LinearEasing)), label = "carX")
    val bounce by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(180), RepeatMode.Reverse), label = "bounce")
    val measurer = rememberTextMeasurer()
    Canvas(modifier.fillMaxWidth().height(height)) {
        val h = size.height
        drawRoundRect(Road.Asphalt, cornerRadius = androidx.compose.ui.geometry.CornerRadius(h * 0.3f))
        val dash = h * 0.35f
        val phase = progress * dash * 2 * 6
        drawLine(
            Road.LanePaint, Offset(0f, h / 2), Offset(size.width, h / 2), strokeWidth = h * 0.05f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash), phase = -phase),
        )
        val carLength = h * 0.95f
        val x = -carLength + (size.width + 2 * carLength) * progress
        drawCar(Offset(x, h / 2 + bounce * h * 0.015f), carLength, look, rotation = 90f, textMeasurer = measurer)
    }
}

/** Escala ao pressionar: dá resposta tátil visual a botões. */
@Composable
fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "press")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Placa de estrada verde, usada para o nome da fase. */
@Composable
fun RoadSign(title: String, subtitle: String, modifier: Modifier = Modifier, color: Color = Road.SignGreen) {
    Column(
        modifier
            .background(color, RoundedCornerShape(10.dp))
            .border(2.dp, Color.White, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("level_title"))
        if (subtitle.isNotEmpty()) Text(subtitle, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
    }
}

/** Vidas como pneus: o pneu perdido murcha e fica tracejado (forma, não só cor). */
@Composable
fun TireLives(lives: Int, max: Int, modifier: Modifier = Modifier) {
    val total = maxOf(lives, max)
    Row(
        modifier
            .testTag("lives")
            .semantics(mergeDescendants = true) { contentDescription = "$lives de $max vidas" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i -> Tire(alive = i < lives) }
    }
}

@Composable
private fun Tire(alive: Boolean) {
    val scale by animateFloatAsState(if (alive) 1f else 0.72f, spring(dampingRatio = 0.35f, stiffness = 300f), label = "tire")
    val ink = MaterialTheme.colorScheme.onBackground
    Canvas(Modifier.size(28.dp).padding(2.dp).scale(scale)) {
        val r = size.minDimension / 2
        if (alive) {
            drawCircle(Color(0xFF1C1D2B), r)
            drawCircle(Color(0xFF9EA3B5), r * 0.48f)
            drawCircle(Road.RacingOrange, r * 0.2f)
            for (k in 0 until 8) {
                val ang = k * PI.toFloat() / 4
                drawLine(Color(0xFF3A3C55), center + Offset(cos(ang), sin(ang)) * (r * 0.62f), center + Offset(cos(ang), sin(ang)) * (r * 0.92f), r * 0.09f)
            }
        } else {
            drawCircle(ink.copy(alpha = 0.45f), r * 0.9f, style = Stroke(r * 0.14f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.3f, r * 0.25f))))
        }
    }
}

/** Explosão de confete com bandeirinhas quadriculadas, tocada uma vez a cada [key]. */
@Composable
fun Confetti(key: Any, modifier: Modifier = Modifier, count: Int = 90) {
    val time = remember { Animatable(0f) }
    LaunchedEffect(key) {
        time.snapTo(0f)
        time.animateTo(1f, tween(2200, easing = FastOutSlowInEasing))
    }
    val pieces = remember(key) {
        val rnd = Random(key.hashCode())
        val colors = listOf(Road.RacingOrange, Road.TaxiYellow, Road.SignGreen, Road.SignBlue, Color.White, Color(0xFFE91E63))
        List(count) {
            ConfettiPiece(
                angle = rnd.nextFloat() * 2 * PI.toFloat(),
                speed = 0.35f + rnd.nextFloat() * 0.75f,
                spin = rnd.nextFloat() * 720f - 360f,
                color = colors[rnd.nextInt(colors.size)],
                checker = rnd.nextInt(6) == 0,
            )
        }
    }
    Canvas(modifier) {
        val t = time.value
        if (t <= 0f || t >= 1f) return@Canvas
        val origin = Offset(size.width / 2, size.height * 0.35f)
        val reach = size.minDimension * 0.7f
        for (p in pieces) {
            val d = reach * p.speed * t
            val pos = origin + Offset(cos(p.angle) * d, sin(p.angle) * d + size.height * 0.6f * t * t)
            val s = size.minDimension * 0.018f
            rotate(p.spin * t, pos) {
                if (p.checker) {
                    drawRect(Color.Black, pos - Offset(s, s), androidx.compose.ui.geometry.Size(s, s), alpha = 1 - t)
                    drawRect(Color.White, pos, androidx.compose.ui.geometry.Size(s, s), alpha = 1 - t)
                } else {
                    drawRect(p.color, pos - Offset(s / 2, s), androidx.compose.ui.geometry.Size(s, s * 2), alpha = 1 - t * 0.8f)
                }
            }
        }
    }
}

private data class ConfettiPiece(val angle: Float, val speed: Float, val spin: Float, val color: Color, val checker: Boolean)
