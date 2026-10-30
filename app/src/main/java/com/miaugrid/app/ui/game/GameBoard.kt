package com.miaugrid.app.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import com.miaugrid.app.ui.components.CarLook
import com.miaugrid.app.ui.components.drawCar
import com.miaugrid.app.ui.theme.BoardColors
import com.miaugrid.app.ui.theme.LocalBoardColors
import com.miaugrid.app.ui.theme.Road
import com.miaugrid.core.game.GameState
import com.miaugrid.core.model.Position
import com.miaugrid.core.model.Puzzle
import com.miaugrid.core.settings.CarBody
import com.miaugrid.core.settings.UserSettings
import com.miaugrid.core.solver.BoardUnit
import kotlin.math.atan2
import kotlin.math.floor

/**
 * Tabuleiro como mapa de uma cidade: cada região é um bairro (lote colorido) separado por
 * ruas de asfalto. Desenhado num único Canvas, então continua leve em 10x10.
 *
 * Gestos:
 * - toque simples: marca/desmarca X (ou aplica a ferramenta escolhida);
 * - toque duplo: estaciona um carro. O detector espera o intervalo de toque duplo do sistema
 *   antes de confirmar o toque simples, então um toque duplo nunca vira "X + carro";
 * - arrastar: marca (ou desmarca) várias células com X numa única jogada.
 *
 * Quando a marcação automática está ligada, o carrinho do jogador ([driver]) sai da vaga
 * recém-ocupada e percorre as células bloqueadas; cada X só aparece quando ele passa.
 */
@Composable
fun GameBoard(
    puzzle: Puzzle,
    game: GameState,
    blocked: Set<Position>,
    selected: Position?,
    settings: UserSettings,
    feedback: Feedback?,
    pulse: PlacementPulse?,
    hint: HintDisplay?,
    drive: DriveAnimation?,
    driver: CarLook,
    enabled: Boolean,
    onTap: (Position) -> Unit,
    onDoubleTap: (Position) -> Unit,
    onDragMarks: (List<Position>, Boolean) -> Unit,
    onDriveFinished: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalBoardColors.current
    val n = puzzle.size
    val textMeasurer = rememberTextMeasurer()

    val currentGame by rememberUpdatedState(game)
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnDrag by rememberUpdatedState(onDragMarks)
    val currentOnDriveFinished by rememberUpdatedState(onDriveFinished)

    var dragCells by remember { mutableStateOf<List<Position>>(emptyList()) }
    var dragMarking by remember { mutableStateOf(true) }

    // Entrada do tabuleiro: cresce e aparece suavemente.
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(puzzle.id) { entrance.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 220f)) }

    // Carro estacionando: cai na vaga girando de leve.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(pulse?.id) {
        if (pulse != null) {
            pop.snapTo(0f)
            pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 420f))
        }
    }

    // Erro: cone tremendo e brilho que some.
    val shake = remember { Animatable(0f) }
    val errorAlpha = remember { Animatable(0f) }
    LaunchedEffect(feedback?.id) {
        if (feedback != null) {
            errorAlpha.snapTo(1f)
            shake.snapTo(0f)
            shake.animateTo(0f, keyframes {
                durationMillis = 380
                -1f at 60; 1f at 120; -0.7f at 190; 0.7f at 260; -0.3f at 320
            })
            errorAlpha.animateTo(0f, tween(durationMillis = 1200, easing = LinearEasing))
        }
    }

    // Carrinho marcando X.
    val driveProgress = remember { Animatable(0f) }
    val driveAlpha = remember { Animatable(0f) }
    LaunchedEffect(drive?.id) {
        if (drive == null) {
            driveAlpha.snapTo(0f)
            return@LaunchedEffect
        }
        driveProgress.snapTo(0f)
        driveAlpha.snapTo(1f)
        driveProgress.animateTo(drive.route.size.toFloat(), tween(drive.route.size * drive.millisPerCell, easing = LinearEasing))
        driveAlpha.animateTo(0f, tween(260))
        currentOnDriveFinished(drive.id)
    }

    val pulseGlow by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "hintGlow",
    )

    fun cellAt(offset: Offset, size: IntSize): Position? {
        if (size.width == 0) return null
        val cell = size.width.toFloat() / n
        val col = floor(offset.x / cell).toInt()
        val row = floor(offset.y / cell).toInt()
        return if (row in 0 until n && col in 0 until n) Position(row, col) else null
    }

    val description = "Tabuleiro $n por $n, ${game.cats.size} de $n carros estacionados, ${game.marks.size} marcações."

    Canvas(
        modifier
            .aspectRatio(1f)
            .graphicsLayer {
                val s = 0.9f + 0.1f * entrance.value
                scaleX = s
                scaleY = s
                alpha = entrance.value.coerceIn(0f, 1f)
            }
            .testTag("board")
            .semantics { contentDescription = description }
            .pointerInput(n) {
                detectTapGestures(
                    onTap = { o -> if (currentEnabled) cellAt(o, size)?.let(currentOnTap) },
                    onDoubleTap = { o -> if (currentEnabled) cellAt(o, size)?.let(currentOnDoubleTap) },
                )
            }
            .pointerInput(n) {
                detectDragGestures(
                    onDragStart = { o ->
                        val start = cellAt(o, size)
                        if (!currentEnabled || start == null) {
                            dragCells = emptyList()
                        } else {
                            dragMarking = start !in currentGame.marks
                            dragCells = if (start in currentGame.cats) emptyList() else listOf(start)
                        }
                    },
                    onDrag = { change, _ ->
                        if (currentEnabled) {
                            change.consume()
                            val cell = cellAt(change.position, size)
                            if (cell != null && cell !in dragCells && cell !in currentGame.cats) dragCells = dragCells + cell
                        }
                    },
                    onDragEnd = {
                        val cells = dragCells
                        dragCells = emptyList()
                        if (currentEnabled && cells.isNotEmpty()) currentOnDrag(cells, dragMarking)
                    },
                    onDragCancel = { dragCells = emptyList() },
                )
            },
    ) {
        val cell = size.width / n
        val gap = cell * 0.09f
        val radius = cell * 0.2f
        val layout = puzzle.layout

        // 1. Ruas (fundo de asfalto)
        drawRoundRect(colors.street, cornerRadius = CornerRadius(cell * 0.3f))

        // 2. Bairros: lotes arredondados, emendados quando vizinhos são do mesmo bairro
        for (r in 0 until n) for (c in 0 until n) {
            val region = layout.regionAt(r, c)
            val color = colors.regions[region % colors.regions.size]
            val x0 = c * cell + gap / 2
            val y0 = r * cell + gap / 2
            drawRoundRect(color, Offset(x0, y0), Size(cell - gap, cell - gap), CornerRadius(radius))
            if (c + 1 < n && layout.regionAt(r, c + 1) == region) {
                drawRect(color, Offset((c + 1) * cell - gap / 2 - radius, y0), Size(gap + 2 * radius, cell - gap))
            }
            if (r + 1 < n && layout.regionAt(r + 1, c) == region) {
                drawRect(color, Offset(x0, (r + 1) * cell - gap / 2 - radius), Size(cell - gap, gap + 2 * radius))
            }
            if (c + 1 < n && r + 1 < n && layout.regionAt(r, c + 1) == region &&
                layout.regionAt(r + 1, c) == region && layout.regionAt(r + 1, c + 1) == region
            ) {
                drawRect(color, Offset((c + 1) * cell - gap / 2 - radius, (r + 1) * cell - gap / 2 - radius), Size(gap + 2 * radius, gap + 2 * radius))
            }
            if (settings.colorBlindAssist) drawPattern(region, Offset(x0, y0), cell - gap, colors)
        }

        // 3. Faixa tracejada no meio das ruas entre bairros diferentes
        val lane = PathEffect.dashPathEffect(floatArrayOf(cell * 0.12f, cell * 0.1f))
        for (r in 0 until n) for (c in 0 until n) {
            val region = layout.regionAt(r, c)
            if (c + 1 < n && layout.regionAt(r, c + 1) != region) {
                drawLine(colors.streetPaint, Offset((c + 1) * cell, r * cell + gap), Offset((c + 1) * cell, (r + 1) * cell - gap), gap * 0.18f, pathEffect = lane)
            }
            if (r + 1 < n && layout.regionAt(r + 1, c) != region) {
                drawLine(colors.streetPaint, Offset(c * cell + gap, (r + 1) * cell), Offset((c + 1) * cell - gap, (r + 1) * cell), gap * 0.18f, pathEffect = lane)
            }
        }

        if (settings.colorBlindAssist) {
            val style = TextStyle(color = colors.patternInk.copy(alpha = 0.9f), fontSize = (cell * 0.2f).toSp(), fontWeight = FontWeight.Black)
            for (region in 0 until n) {
                val first = layout.regionCells[region].first()
                drawText(textMeasurer, "${'A' + region}", topLeft = Offset(first.col * cell + gap + cell * 0.04f, first.row * cell + gap), style = style)
            }
        }

        // 4. Destaque de linha e coluna selecionadas
        if (selected != null && settings.highlightLines) {
            val tint = Color.White.copy(alpha = 0.22f)
            drawRect(tint, Offset(0f, selected.row * cell), Size(size.width, cell))
            drawRect(tint, Offset(selected.col * cell, 0f), Size(cell, size.height))
        }

        // 5. Dica
        if (hint != null) {
            for (unit in hint.units) for (p in unitCells(unit, puzzle)) {
                drawRoundRect(colors.hint.copy(alpha = 0.3f * pulseGlow), Offset(p.col * cell + gap / 2, p.row * cell + gap / 2), Size(cell - gap, cell - gap), CornerRadius(radius))
            }
            val dash = PathEffect.dashPathEffect(floatArrayOf(cell * 0.12f, cell * 0.08f))
            for (p in hint.cells) {
                drawRoundRect(
                    colors.hint.copy(alpha = pulseGlow), Offset(p.col * cell + cell * 0.1f, p.row * cell + cell * 0.1f),
                    Size(cell * 0.8f, cell * 0.8f), CornerRadius(radius), style = Stroke(width = cell * 0.07f, pathEffect = dash),
                )
            }
            hint.focus?.let { p ->
                drawCircle(colors.hint.copy(alpha = pulseGlow), radius = cell * 0.46f, center = center(p, cell), style = Stroke(cell * 0.08f))
            }
        }

        // 6. Seleção
        if (selected != null) {
            drawRoundRect(
                colors.selection, Offset(selected.col * cell + gap / 2, selected.row * cell + gap / 2),
                Size(cell - gap, cell - gap), CornerRadius(radius), style = Stroke(cell * 0.07f),
            )
        }

        // 7. Trajeto do carrinho (marcas de pneu)
        val points = drive?.let { listOf(it.start) + it.route }.orEmpty()
        val t = driveProgress.value
        val driving = drive != null && driveAlpha.value > 0f
        if (driving && points.size > 1) {
            val reached = floor(t).toInt().coerceIn(0, points.size - 1)
            val trail = Path().apply {
                val first = center(points[0], cell)
                moveTo(first.x, first.y)
                for (k in 1..reached) center(points[k], cell).let { lineTo(it.x, it.y) }
                carPosition(points, t, cell).let { lineTo(it.x, it.y) }
            }
            drawPath(trail, colors.trail, alpha = driveAlpha.value, style = Stroke(cell * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // 8. Pontos de bloqueio confirmados pelo motor e X (do jogador ou do carrinho)
        val dragSet = dragCells.toSet()
        val routeIndex: Map<Position, Int> = if (driving) drive!!.route.withIndex().associate { (i, p) -> p to i } else emptyMap()
        for (r in 0 until n) for (c in 0 until n) {
            val p = Position(r, c)
            if (p in game.cats) continue
            val marked = if (p in dragSet) dragMarking else p in game.marks
            val k = routeIndex[p]
            // Célula do trajeto: o X só aparece quando o carrinho chega (k + 1 é a chegada).
            val revealAt = if (k != null) k + 0.85f else null
            when {
                marked && (revealAt == null || t >= revealAt) -> {
                    val grow = if (revealAt != null) ((t - revealAt) / 1.2f).coerceIn(0f, 1f) else 1f
                    val s = 1.45f - 0.45f * grow
                    drawX(center(p, cell), cell * 0.2f * s, colors.markX.copy(alpha = if (p in dragSet) 0.55f else 0.9f), cell * 0.08f)
                }
                marked -> Unit
                settings.showBlockedCells && p in blocked -> drawCircle(colors.blockedDot, radius = cell * 0.06f, center = center(p, cell))
            }
        }

        // 9. Carros estacionados
        for (p in game.cats) {
            val fixed = p in game.fixedCats
            val region = layout.regionAt(p)
            // Carros do jogador usam o modelo e o adesivo da garagem, na cor do bairro (a cor
            // continua dizendo de qual bairro é o carro). Fixos: van cinza com cadeado.
            val look = if (fixed) {
                CarLook(body = CarBody.VAN, color = colors.fixedCar, accent = Color.White)
            } else {
                driver.copy(color = colors.parkedCars[region % colors.parkedCars.size])
            }
            val anim = if (pulse?.position == p) pop.value else 1f
            scale(1f + 0.5f * (1f - anim), center(p, cell)) {
                drawCar(center(p, cell), cell * 0.82f, look, rotation = -24f * (1f - anim), alpha = anim.coerceIn(0.2f, 1f), textMeasurer = textMeasurer, locked = fixed)
            }
        }

        // 10. Carrinho do jogador dirigindo
        if (driving && points.size > 1) {
            val pos = carPosition(points, t, cell)
            val heading = carHeading(points, t, cell)
            drawCar(pos, cell * 0.78f, driver, rotation = heading, alpha = driveAlpha.value, textMeasurer = textMeasurer)
        }

        // 11. Erro: cone tremendo na célula recusada e anel nos carros em conflito
        if (feedback != null && errorAlpha.value > 0f) {
            val a = errorAlpha.value
            val c0 = center(feedback.position, cell) + Offset(shake.value * cell * 0.12f, 0f)
            drawCircle(colors.error.copy(alpha = 0.25f * a), radius = cell * 0.42f, center = c0)
            drawCone(c0, cell * 0.62f, a)
            for (p in feedback.conflicts) {
                drawCircle(
                    colors.error.copy(alpha = a), radius = cell * 0.47f, center = center(p, cell),
                    style = Stroke(cell * 0.08f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(cell * 0.15f, cell * 0.08f))),
                )
            }
        }
    }
}

private fun center(p: Position, cell: Float) = Offset(p.col * cell + cell / 2, p.row * cell + cell / 2)

private fun smooth(x: Float) = x * x * (3 - 2 * x)

/** Posição do carrinho no instante [t] (0 = vaga de saída; k = k-ésima célula do trajeto). */
private fun carPosition(points: List<Position>, t: Float, cell: Float): Offset {
    val i = floor(t).toInt().coerceIn(0, points.size - 2)
    val f = smooth((t - i).coerceIn(0f, 1f))
    val a = center(points[i], cell)
    val b = center(points[i + 1], cell)
    return a + (b - a) * f
}

/** Direção em graus (0 = para cima), suavizada perto das curvas. */
private fun carHeading(points: List<Position>, t: Float, cell: Float): Float {
    val i = floor(t).toInt().coerceIn(0, points.size - 2)
    fun angle(k: Int): Float {
        val a = center(points[k], cell)
        val b = center(points[k + 1], cell)
        return Math.toDegrees(atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble())).toFloat() + 90f
    }
    val current = angle(i)
    val f = (t - i).coerceIn(0f, 1f)
    if (f < 0.8f || i + 1 > points.size - 2) return current
    var next = angle(i + 1)
    while (next - current > 180f) next -= 360f
    while (next - current < -180f) next += 360f
    return current + (next - current) * ((f - 0.8f) / 0.2f)
}

private fun unitCells(unit: BoardUnit, puzzle: Puzzle): List<Position> = when (unit) {
    is BoardUnit.Row -> List(puzzle.size) { Position(unit.index, it) }
    is BoardUnit.Column -> List(puzzle.size) { Position(it, unit.index) }
    is BoardUnit.Region -> puzzle.layout.regionCells[unit.id]
}

private fun DrawScope.drawX(center: Offset, half: Float, color: Color, width: Float) {
    drawLine(color, center + Offset(-half, -half), center + Offset(half, half), width, StrokeCap.Round)
    drawLine(color, center + Offset(-half, half), center + Offset(half, -half), width, StrokeCap.Round)
}

/** Cone de trânsito: indica a vaga recusada. */
private fun DrawScope.drawCone(center: Offset, height: Float, alpha: Float) {
    val h = height
    val base = Path().apply {
        moveTo(center.x - h * 0.42f, center.y + h * 0.42f)
        lineTo(center.x + h * 0.42f, center.y + h * 0.42f)
        lineTo(center.x + h * 0.34f, center.y + h * 0.32f)
        lineTo(center.x - h * 0.34f, center.y + h * 0.32f)
        close()
    }
    val body = Path().apply {
        moveTo(center.x - h * 0.28f, center.y + h * 0.34f)
        lineTo(center.x + h * 0.28f, center.y + h * 0.34f)
        lineTo(center.x + h * 0.06f, center.y - h * 0.48f)
        lineTo(center.x - h * 0.06f, center.y - h * 0.48f)
        close()
    }
    drawPath(base, Road.Cone, alpha = alpha)
    drawPath(body, Road.Cone, alpha = alpha)
    drawLine(Color.White, Offset(center.x - h * 0.18f, center.y), Offset(center.x + h * 0.18f, center.y), h * 0.1f, alpha = alpha)
    drawLine(Color.White, Offset(center.x - h * 0.11f, center.y - h * 0.24f), Offset(center.x + h * 0.11f, center.y - h * 0.24f), h * 0.08f, alpha = alpha)
}

/** Padrões por bairro para o modo de cores acessível. */
private fun DrawScope.drawPattern(region: Int, topLeft: Offset, side: Float, colors: BoardColors) {
    val ink = colors.patternInk
    val w = (side * 0.03f).coerceAtLeast(1f)
    clipRect(topLeft.x, topLeft.y, topLeft.x + side, topLeft.y + side) {
        val step = side / 4
        when (region % 6) {
            0 -> for (k in -4..4) drawLine(ink, topLeft + Offset(k * step, 0f), topLeft + Offset(k * step + side, side), w)
            1 -> for (i in 1..3) for (j in 1..3) drawCircle(ink, radius = w * 1.4f, center = topLeft + Offset(i * step, j * step))
            2 -> for (i in 1..3) drawLine(ink, topLeft + Offset(0f, i * step), topLeft + Offset(side, i * step), w)
            3 -> for (i in 1..3) drawLine(ink, topLeft + Offset(i * step, 0f), topLeft + Offset(i * step, side), w)
            4 -> for (k in -4..4) drawLine(ink, topLeft + Offset(k * step + side, 0f), topLeft + Offset(k * step, side), w)
            else -> {
                for (i in 1..3) drawLine(ink, topLeft + Offset(0f, i * step), topLeft + Offset(side, i * step), w)
                for (i in 1..3) drawLine(ink, topLeft + Offset(i * step, 0f), topLeft + Offset(i * step, side), w)
            }
        }
    }
}
