package com.miaugrid.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.miaugrid.core.settings.CarBody
import com.miaugrid.core.settings.CarDecal
import com.miaugrid.core.settings.CarStyle

/** Aparência pronta para desenhar. */
data class CarLook(
    val body: CarBody,
    val color: Color,
    val accent: Color,
    val decal: CarDecal = CarDecal.NONE,
    val number: Int = 0,
)

fun CarStyle.toLook() = CarLook(body, Color(bodyColor), Color(accentColor), decal, number)

private val Glass = Color(0xFF1E2A3A)
private val Tire = Color(0xFF16161C)
private val Headlight = Color(0xFFFFF3B0)
private val Taillight = Color(0xFFE53935)

/**
 * Carrinho original visto de cima, desenhado só com vetores. Aponta para cima quando
 * [rotation] = 0. [length] é o comprimento em pixels.
 */
fun DrawScope.drawCar(
    center: Offset,
    length: Float,
    look: CarLook,
    rotation: Float = 0f,
    alpha: Float = 1f,
    textMeasurer: TextMeasurer? = null,
    locked: Boolean = false,
) {
    if (length <= 0f || alpha <= 0f) return
    rotate(rotation, pivot = center) {
        CarPainter(this, center, length, alpha).paint(look, textMeasurer, locked)
    }
}

/** Desenha em coordenadas unitárias: x e y entre -0,5 e 0,5 do comprimento; frente em y = -0,5. */
private class CarPainter(val scope: DrawScope, val c: Offset, val l: Float, val a: Float) {

    fun o(x: Float, y: Float) = Offset(c.x + x * l, c.y + y * l)

    fun rect(x0: Float, y0: Float, x1: Float, y1: Float, color: Color, r: Float = 0f) {
        scope.drawRoundRect(color, o(x0, y0), Size((x1 - x0) * l, (y1 - y0) * l), CornerRadius(r * l), alpha = a)
    }

    fun strokeRect(x0: Float, y0: Float, x1: Float, y1: Float, color: Color, width: Float, r: Float = 0f) {
        scope.drawRoundRect(color, o(x0, y0), Size((x1 - x0) * l, (y1 - y0) * l), CornerRadius(r * l), alpha = a, style = Stroke(width * l))
    }

    fun circle(x: Float, y: Float, r: Float, color: Color) = scope.drawCircle(color, r * l, o(x, y), alpha = a)

    fun poly(color: Color, vararg pts: Pair<Float, Float>) {
        val path = Path()
        pts.forEachIndexed { i, (x, y) -> val p = o(x, y); if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
        path.close()
        scope.drawPath(path, color, alpha = a)
    }

    fun wheels(hw: Float, front: ClosedFloatingPointRange<Float>, rear: ClosedFloatingPointRange<Float>, out: Float = 0.035f, depth: Float = 0.09f) {
        for (s in listOf(-1f, 1f)) {
            for (range in listOf(front, rear)) {
                val x0 = if (s < 0) -hw - out else hw - depth + out
                rect(x0, range.start, x0 + depth, range.endInclusive, Tire, 0.025f)
            }
        }
    }

    fun shadow(x0: Float, y0: Float, x1: Float, y1: Float, r: Float) =
        rect(x0 + 0.03f, y0 + 0.04f, x1 + 0.03f, y1 + 0.04f, Color.Black.copy(alpha = 0.22f), r)

    fun paint(look: CarLook, measurer: TextMeasurer?, locked: Boolean) {
        val body = look.color
        val roof = lerp(body, Color.White, 0.18f)
        val shade = lerp(body, Color.Black, 0.22f)
        val roofCenter: Float
        when (look.body) {
            CarBody.SEDAN -> {
                shadow(-0.24f, -0.5f, 0.24f, 0.5f, 0.12f)
                wheels(0.24f, -0.37f..-0.2f, 0.2f..0.37f)
                rect(-0.24f, -0.5f, 0.24f, 0.5f, body, 0.12f)
                decalBelowGlass(look)
                poly(Glass, -0.19f to -0.2f, 0.19f to -0.2f, 0.16f to -0.06f, -0.16f to -0.06f)
                rect(-0.17f, -0.06f, 0.17f, 0.2f, roof, 0.05f)
                poly(Glass, -0.16f to 0.2f, 0.16f to 0.2f, 0.18f to 0.3f, -0.18f to 0.3f)
                lights(0.15f, -0.46f, 0.49f)
                roofCenter = 0.07f
            }
            CarBody.SPORT -> {
                shadow(-0.23f, -0.48f, 0.23f, 0.5f, 0.14f)
                wheels(0.23f, -0.36f..-0.2f, 0.2f..0.38f)
                rect(-0.23f, -0.42f, 0.23f, 0.5f, body, 0.14f)
                poly(body, -0.21f to -0.4f, -0.1f to -0.52f, 0.1f to -0.52f, 0.21f to -0.4f)
                decalBelowGlass(look)
                rect(-0.25f, 0.42f, -0.19f, 0.5f, look.accent, 0.01f)
                rect(0.19f, 0.42f, 0.25f, 0.5f, look.accent, 0.01f)
                poly(Glass, -0.17f to -0.16f, 0.17f to -0.16f, 0.13f to -0.02f, -0.13f to -0.02f)
                rect(-0.13f, -0.02f, 0.13f, 0.14f, roof, 0.05f)
                poly(Glass, -0.13f to 0.14f, 0.13f to 0.14f, 0.15f to 0.22f, -0.15f to 0.22f)
                rect(-0.26f, 0.44f, 0.26f, 0.5f, shade, 0.02f)
                lights(0.12f, -0.45f, 0.47f)
                roofCenter = 0.06f
            }
            CarBody.BUGGY -> {
                shadow(-0.2f, -0.4f, 0.2f, 0.42f, 0.1f)
                wheels(0.2f, -0.43f..-0.17f, 0.14f..0.43f, out = 0.1f, depth = 0.13f)
                rect(-0.2f, -0.4f, 0.2f, 0.42f, body, 0.1f)
                decalBelowGlass(look)
                circle(0f, 0.08f, 0.08f, Tire)
                strokeRect(-0.16f, -0.08f, 0.16f, 0.26f, look.accent, 0.03f, 0.04f)
                rect(-0.14f, -0.36f, 0.14f, -0.3f, shade, 0.02f)
                lights(0.12f, -0.37f, 0.41f)
                roofCenter = -0.18f
            }
            CarBody.PICKUP -> {
                shadow(-0.24f, -0.5f, 0.24f, 0.5f, 0.1f)
                wheels(0.24f, -0.38f..-0.21f, 0.22f..0.39f)
                rect(-0.24f, -0.5f, 0.24f, 0.5f, body, 0.1f)
                decalBelowGlass(look)
                poly(Glass, -0.19f to -0.27f, 0.19f to -0.27f, 0.17f to -0.15f, -0.17f to -0.15f)
                rect(-0.18f, -0.15f, 0.18f, 0.05f, roof, 0.04f)
                rect(-0.2f, 0.1f, 0.2f, 0.47f, shade, 0.03f)
                strokeRect(-0.2f, 0.1f, 0.2f, 0.47f, lerp(body, Color.Black, 0.4f), 0.015f, 0.03f)
                lights(0.15f, -0.46f, 0.49f)
                roofCenter = -0.05f
            }
            CarBody.VAN -> {
                shadow(-0.25f, -0.5f, 0.25f, 0.5f, 0.08f)
                wheels(0.25f, -0.38f..-0.22f, 0.24f..0.4f)
                rect(-0.25f, -0.5f, 0.25f, 0.5f, body, 0.08f)
                decalBelowGlass(look)
                poly(Glass, -0.2f to -0.42f, 0.2f to -0.42f, 0.19f to -0.31f, -0.19f to -0.31f)
                rect(-0.21f, -0.3f, 0.21f, 0.46f, roof, 0.04f)
                for (y in listOf(-0.15f, 0.05f, 0.25f)) rect(-0.18f, y, 0.18f, y + 0.02f, shade)
                lights(0.17f, -0.47f, 0.49f)
                roofCenter = 0.15f
            }
            CarBody.FORMULA -> {
                shadow(-0.28f, -0.5f, 0.28f, 0.5f, 0.05f)
                wheels(0.3f, -0.4f..-0.22f, 0.18f..0.4f, out = 0f, depth = 0.12f)
                rect(-0.28f, -0.5f, 0.28f, -0.45f, look.accent, 0.015f)
                poly(body, 0f to -0.49f, 0.08f to -0.2f, 0.14f to 0.1f, 0.1f to 0.42f, -0.1f to 0.42f, -0.14f to 0.1f, -0.08f to -0.2f)
                decalBelowGlass(look)
                rect(-0.25f, 0.41f, 0.25f, 0.5f, look.accent, 0.015f)
                circle(0f, 0f, 0.075f, Glass)
                circle(0f, 0.01f, 0.05f, look.accent)
                roofCenter = 0.22f
            }
        }
        decalOnRoof(look, roofCenter, measurer)
        if (locked) {
            circle(0.17f, 0.36f, 0.11f, Color.White)
            rect(0.12f, 0.35f, 0.22f, 0.43f, Color(0xFF37474F), 0.015f)
            scope.drawArc(Color(0xFF37474F), 180f, 180f, false, o(0.135f, 0.29f), Size(0.07f * l, 0.1f * l), alpha = a, style = Stroke(0.018f * l))
        }
    }

    fun lights(x: Float, front: Float, rear: Float) {
        circle(-x, front, 0.035f, Headlight)
        circle(x, front, 0.035f, Headlight)
        rect(-x - 0.05f, rear - 0.025f, -x + 0.03f, rear, Taillight, 0.01f)
        rect(x - 0.03f, rear - 0.025f, x + 0.05f, rear, Taillight, 0.01f)
    }

    fun decalBelowGlass(look: CarLook) {
        when (look.decal) {
            CarDecal.STRIPES -> {
                rect(-0.075f, -0.5f, -0.03f, 0.5f, look.accent)
                rect(0.03f, -0.5f, 0.075f, 0.5f, look.accent)
            }
            CarDecal.FLAMES -> {
                val flame = Color(0xFFFFB300)
                for (s in listOf(-1f, 1f)) {
                    poly(look.accent, s * 0.2f to -0.46f, s * 0.05f to -0.47f, s * 0.12f to -0.38f, s * 0.04f to -0.36f, s * 0.13f to -0.28f, s * 0.2f to -0.3f)
                    poly(flame, s * 0.18f to -0.44f, s * 0.09f to -0.44f, s * 0.13f to -0.39f, s * 0.09f to -0.37f, s * 0.15f to -0.32f, s * 0.18f to -0.33f)
                }
            }
            else -> Unit
        }
    }

    fun decalOnRoof(look: CarLook, y: Float, measurer: TextMeasurer?) {
        when (look.decal) {
            CarDecal.NUMBER -> {
                circle(0f, y, 0.1f, look.accent)
                if (measurer != null && l > 24f) {
                    val text = look.number.toString()
                    val style = TextStyle(color = Color(0xFF1C1D2B), fontWeight = FontWeight.Black, fontSize = with(scope) { (l * 0.12f).toSp() })
                    val layout = measurer.measure(text, style)
                    scope.drawText(layout, topLeft = o(0f, y) - Offset(layout.size.width / 2f, layout.size.height / 2f), alpha = a)
                }
            }
            CarDecal.CHECKERED -> {
                val s = 0.05f
                for (i in 0 until 4) for (j in 0 until 3) {
                    val color = if ((i + j) % 2 == 0) Color.White else Color(0xFF1C1D2B)
                    rect(-0.1f + i * s, y - 0.075f + j * s, -0.1f + (i + 1) * s, y - 0.075f + (j + 1) * s, color)
                }
            }
            else -> Unit
        }
    }
}

/** Carrinho isolado, para telas e botões. */
@Composable
fun CarIcon(
    look: CarLook,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    description: String? = null,
) {
    val measurer = rememberTextMeasurer()
    val semantic = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
    Canvas(modifier.then(semantic)) {
        drawCar(Offset(size.width / 2, size.height / 2), minOf(size.width, size.height) * 0.92f, look, rotation, textMeasurer = measurer)
    }
}
