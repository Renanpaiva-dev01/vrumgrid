package com.miaugrid.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.miaugrid.app.ui.theme.LocalBoardColors

/**
 * Ilustração original de gato desenhada com vetores. [center] e [size] em pixels.
 * Gatos fixos usam pelagem acinzentada e coleira com sininho (diferença também de forma,
 * não só de cor).
 */
fun DrawScope.drawCat(
    center: Offset,
    size: Float,
    fur: Color,
    detail: Color,
    innerEar: Color,
    fixed: Boolean = false,
    collar: Color = Color(0xFF2F7D6D),
    blink: Boolean = false,
) {
    val r = size / 2f
    val headW = r * 0.82f
    val headH = r * 0.70f
    val headCenter = Offset(center.x, center.y + r * 0.10f)

    // Orelhas
    fun ear(sign: Float, color: Color, scale: Float) {
        val path = Path().apply {
            moveTo(headCenter.x + sign * headW * 0.92f * scale, headCenter.y - headH * 0.15f)
            lineTo(headCenter.x + sign * headW * 0.70f * scale, headCenter.y - headH * 1.30f * scale)
            lineTo(headCenter.x + sign * headW * 0.18f * scale, headCenter.y - headH * 0.72f)
            close()
        }
        drawPath(path, color)
    }
    ear(-1f, fur, 1f); ear(1f, fur, 1f)
    ear(-1f, innerEar, 0.78f); ear(1f, innerEar, 0.78f)

    // Cabeça
    drawOval(fur, topLeft = Offset(headCenter.x - headW, headCenter.y - headH), size = Size(headW * 2, headH * 2))

    // Olhos
    val eyeY = headCenter.y - headH * 0.05f
    val eyeDx = headW * 0.38f
    if (blink) {
        for (s in listOf(-1f, 1f)) {
            drawLine(detail, Offset(headCenter.x + s * eyeDx - r * 0.11f, eyeY), Offset(headCenter.x + s * eyeDx + r * 0.11f, eyeY), strokeWidth = r * 0.07f, cap = StrokeCap.Round)
        }
    } else {
        for (s in listOf(-1f, 1f)) {
            val eye = Offset(headCenter.x + s * eyeDx, eyeY)
            drawOval(detail, topLeft = Offset(eye.x - r * 0.10f, eye.y - r * 0.13f), size = Size(r * 0.20f, r * 0.26f))
            drawCircle(Color.White, radius = r * 0.04f, center = Offset(eye.x + r * 0.03f, eye.y - r * 0.05f))
        }
    }

    // Nariz e boca
    val noseY = headCenter.y + headH * 0.28f
    val nose = Path().apply {
        moveTo(headCenter.x - r * 0.08f, noseY)
        lineTo(headCenter.x + r * 0.08f, noseY)
        lineTo(headCenter.x, noseY + r * 0.08f)
        close()
    }
    drawPath(nose, Color(0xFFE85D75))
    val mouthStroke = Stroke(width = r * 0.045f, cap = StrokeCap.Round)
    val mouth = Path().apply {
        moveTo(headCenter.x - r * 0.15f, noseY + r * 0.12f)
        quadraticTo(headCenter.x - r * 0.07f, noseY + r * 0.20f, headCenter.x, noseY + r * 0.09f)
        quadraticTo(headCenter.x + r * 0.07f, noseY + r * 0.20f, headCenter.x + r * 0.15f, noseY + r * 0.12f)
    }
    drawPath(mouth, detail, style = mouthStroke)

    // Bigodes
    for (s in listOf(-1f, 1f)) for (k in listOf(-1f, 1f)) {
        drawLine(
            detail.copy(alpha = 0.7f),
            start = Offset(headCenter.x + s * headW * 0.35f, noseY + k * r * 0.04f + r * 0.05f),
            end = Offset(headCenter.x + s * headW * 1.08f, noseY + k * r * 0.12f + r * 0.02f),
            strokeWidth = r * 0.03f,
            cap = StrokeCap.Round,
        )
    }

    if (fixed) {
        // Coleira e sininho: identifica o gato fixo sem depender da cor.
        val collarY = headCenter.y + headH * 0.88f
        drawLine(collar, Offset(headCenter.x - headW * 0.55f, collarY), Offset(headCenter.x + headW * 0.55f, collarY), strokeWidth = r * 0.12f, cap = StrokeCap.Round)
        drawCircle(Color(0xFFFFC94A), radius = r * 0.11f, center = Offset(headCenter.x, collarY + r * 0.10f))
        drawCircle(detail, radius = r * 0.025f, center = Offset(headCenter.x, collarY + r * 0.14f))
    }
}

/** Gato isolado, para telas e botões. */
@Composable
fun CatIcon(modifier: Modifier = Modifier, fixed: Boolean = false, tilt: Float = 0f, description: String? = null) {
    val colors = LocalBoardColors.current
    val semantic = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
    Canvas(modifier.then(semantic)) {
        rotate(tilt) {
            drawCat(
                center = Offset(size.width / 2, size.height / 2),
                size = minOf(size.width, size.height),
                fur = if (fixed) colors.catFurFixed else colors.catFur,
                detail = colors.catDetail,
                innerEar = colors.catInnerEar,
                fixed = fixed,
            )
        }
    }
}
