package com.miaugrid.app.ui.game

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import com.miaugrid.app.ui.components.drawCat
import com.miaugrid.app.ui.theme.BoardColors
import com.miaugrid.app.ui.theme.LocalBoardColors
import com.miaugrid.core.game.GameState
import com.miaugrid.core.model.Position
import com.miaugrid.core.model.Puzzle
import com.miaugrid.core.settings.UserSettings
import com.miaugrid.core.solver.BoardUnit

/**
 * Tabuleiro interativo desenhado num único Canvas (barato mesmo em 10x10).
 *
 * Gestos:
 * - toque simples: marca/desmarca X (ou aplica a ferramenta escolhida);
 * - toque duplo: coloca gato. O detector espera o intervalo de toque duplo do sistema antes
 *   de confirmar um toque simples, então um toque duplo nunca vira "X + gato";
 * - arrastar: marca (ou desmarca) várias células com X numa única jogada. Um arrasto consome
 *   os eventos e cancela o detector de toques, evitando gestos concorrentes.
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
    enabled: Boolean,
    onTap: (Position) -> Unit,
    onDoubleTap: (Position) -> Unit,
    onDragMarks: (List<Position>, Boolean) -> Unit,
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

    var dragCells by remember { mutableStateOf<List<Position>>(emptyList()) }
    var dragMarking by remember { mutableStateOf(true) }

    // Animações discretas: gato "pulando" ao ser colocado, tremida e brilho de erro, pulso da dica.
    val pop = remember { Animatable(1f) }
    LaunchedEffect(pulse?.id) {
        if (pulse != null) {
            pop.snapTo(0.3f)
            pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
        }
    }
    val shake = remember { Animatable(0f) }
    val errorAlpha = remember { Animatable(0f) }
    LaunchedEffect(feedback?.id) {
        if (feedback != null) {
            errorAlpha.snapTo(1f)
            shake.snapTo(0f)
            shake.animateTo(0f, keyframes {
                durationMillis = 360
                -1f at 60; 1f at 120; -0.7f at 180; 0.7f at 240; -0.3f at 300
            })
            errorAlpha.animateTo(0f, tween(durationMillis = 1100, easing = LinearEasing))
        }
    }
    val glow by rememberInfiniteTransition(label = "hint").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "hintGlow",
    )

    fun cellAt(offset: Offset, size: IntSize): Position? {
        if (size.width == 0) return null
        val cell = size.width.toFloat() / n
        val col = (offset.x / cell).toInt()
        val row = (offset.y / cell).toInt()
        return if (row in 0 until n && col in 0 until n) Position(row, col) else null
    }

    val description = "Tabuleiro ${n} por ${n}, ${game.cats.size} de ${n} gatos colocados, ${game.marks.size} marcações."

    Canvas(
        modifier
            .aspectRatio(1f)
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
        val layout = puzzle.layout

        // 1. Regiões (cor + padrão opcional + letra opcional)
        for (r in 0 until n) for (c in 0 until n) {
            val region = layout.regionAt(r, c)
            val topLeft = Offset(c * cell, r * cell)
            drawRect(colors.regions[region % colors.regions.size], topLeft, Size(cell, cell))
            if (settings.colorBlindAssist) drawPattern(region, topLeft, cell, colors)
        }
        if (settings.colorBlindAssist) {
            val style = TextStyle(color = colors.patternInk.copy(alpha = 0.85f), fontSize = (cell * 0.2f).toSp(), fontWeight = FontWeight.Bold)
            for (region in 0 until n) {
                val first = layout.regionCells[region].first()
                drawText(textMeasurer, "${'A' + region}", topLeft = Offset(first.col * cell + cell * 0.06f, first.row * cell + cell * 0.02f), style = style)
            }
        }

        // 2. Destaques de linha/coluna da célula selecionada
        if (selected != null && settings.highlightLines) {
            val tint = colors.selection.copy(alpha = 0.10f)
            drawRect(tint, Offset(0f, selected.row * cell), Size(size.width, cell))
            drawRect(tint, Offset(selected.col * cell, 0f), Size(cell, size.height))
        }

        // 3. Dica: grupos envolvidos e células relevantes
        if (hint != null) {
            for (unit in hint.units) for (p in unitCells(unit, puzzle)) {
                drawRect(colors.hint.copy(alpha = 0.22f * glow), Offset(p.col * cell, p.row * cell), Size(cell, cell))
            }
            val dash = PathEffect.dashPathEffect(floatArrayOf(cell * 0.12f, cell * 0.08f))
            for (p in hint.cells) {
                drawRect(
                    colors.hint.copy(alpha = glow), Offset(p.col * cell + cell * 0.08f, p.row * cell + cell * 0.08f),
                    Size(cell * 0.84f, cell * 0.84f), style = Stroke(width = cell * 0.06f, pathEffect = dash),
                )
            }
            hint.focus?.let { p ->
                drawCircle(colors.hint.copy(alpha = glow), radius = cell * 0.44f, center = center(p, cell), style = Stroke(cell * 0.08f))
            }
        }

        // 4. Grade fina e bordas grossas entre regiões
        val thin = (cell * 0.02f).coerceAtLeast(1f)
        val thick = (cell * 0.07f).coerceAtLeast(3f)
        for (i in 1 until n) {
            drawLine(colors.gridLine, Offset(0f, i * cell), Offset(size.width, i * cell), thin)
            drawLine(colors.gridLine, Offset(i * cell, 0f), Offset(i * cell, size.height), thin)
        }
        for (r in 0 until n) for (c in 0 until n) {
            val region = layout.regionAt(r, c)
            if (c + 1 < n && layout.regionAt(r, c + 1) != region) {
                drawLine(colors.regionBorder, Offset((c + 1) * cell, r * cell), Offset((c + 1) * cell, (r + 1) * cell), thick, StrokeCap.Square)
            }
            if (r + 1 < n && layout.regionAt(r + 1, c) != region) {
                drawLine(colors.regionBorder, Offset(c * cell, (r + 1) * cell), Offset((c + 1) * cell, (r + 1) * cell), thick, StrokeCap.Square)
            }
        }
        drawRect(colors.regionBorder, Offset.Zero, size, style = Stroke(thick * 1.4f))

        // 5. Seleção
        if (selected != null) {
            drawRect(
                colors.selection, Offset(selected.col * cell + thick / 2, selected.row * cell + thick / 2),
                Size(cell - thick, cell - thick), style = Stroke(cell * 0.06f),
            )
        }

        // 6. Bloqueios confirmados pelo motor (pontos) e marcações X do jogador
        val dragSet = dragCells.toSet()
        for (r in 0 until n) for (c in 0 until n) {
            val p = Position(r, c)
            if (p in game.cats) continue
            val marked = if (p in dragSet) dragMarking else p in game.marks
            when {
                marked -> drawX(center(p, cell), cell * 0.22f, colors.markX.copy(alpha = if (p in dragSet) 0.55f else 1f), cell * 0.07f)
                settings.showBlockedCells && p in blocked -> drawCircle(colors.blockedDot, radius = cell * 0.07f, center = center(p, cell))
            }
        }

        // 7. Gatos
        for (p in game.cats) {
            val scale = if (pulse?.position == p) pop.value else 1f
            val fixed = p in game.fixedCats
            drawCat(
                center = center(p, cell),
                size = cell * 0.86f * scale,
                fur = if (fixed) colors.catFurFixed else colors.catFur,
                detail = colors.catDetail,
                innerEar = colors.catInnerEar,
                fixed = fixed,
            )
        }

        // 8. Erro: X vermelho tremendo na célula recusada e anel nos gatos em conflito
        if (feedback != null && errorAlpha.value > 0f) {
            val a = errorAlpha.value
            val c0 = center(feedback.position, cell) + Offset(shake.value * cell * 0.12f, 0f)
            drawCircle(colors.error.copy(alpha = 0.25f * a), radius = cell * 0.42f, center = c0)
            drawX(c0, cell * 0.25f, colors.error.copy(alpha = a), cell * 0.1f)
            for (p in feedback.conflicts) {
                drawCircle(colors.error.copy(alpha = a), radius = cell * 0.46f, center = center(p, cell), style = Stroke(cell * 0.08f))
            }
        }
    }
}

private fun center(p: Position, cell: Float) = Offset(p.col * cell + cell / 2, p.row * cell + cell / 2)

private fun unitCells(unit: BoardUnit, puzzle: Puzzle): List<Position> = when (unit) {
    is BoardUnit.Row -> List(puzzle.size) { Position(unit.index, it) }
    is BoardUnit.Column -> List(puzzle.size) { Position(it, unit.index) }
    is BoardUnit.Region -> puzzle.layout.regionCells[unit.id]
}

private fun DrawScope.drawX(center: Offset, half: Float, color: Color, width: Float) {
    drawLine(color, center + Offset(-half, -half), center + Offset(half, half), width, StrokeCap.Round)
    drawLine(color, center + Offset(-half, half), center + Offset(half, -half), width, StrokeCap.Round)
}

/** Padrões por região para o modo de acessibilidade de cores. */
private fun DrawScope.drawPattern(region: Int, topLeft: Offset, cell: Float, colors: BoardColors) {
    val ink = colors.patternInk
    val w = (cell * 0.025f).coerceAtLeast(1f)
    clipRect(topLeft.x, topLeft.y, topLeft.x + cell, topLeft.y + cell) {
        val step = cell / 4
        when (region % 6) {
            0 -> for (k in -4..4) drawLine(ink, topLeft + Offset(k * step, 0f), topLeft + Offset(k * step + cell, cell), w)
            1 -> for (i in 1..3) for (j in 1..3) drawCircle(ink, radius = w * 1.4f, center = topLeft + Offset(i * step, j * step))
            2 -> for (i in 1..3) drawLine(ink, topLeft + Offset(0f, i * step), topLeft + Offset(cell, i * step), w)
            3 -> for (i in 1..3) drawLine(ink, topLeft + Offset(i * step, 0f), topLeft + Offset(i * step, cell), w)
            4 -> for (k in -4..4) drawLine(ink, topLeft + Offset(k * step + cell, 0f), topLeft + Offset(k * step, cell), w)
            else -> {
                for (i in 1..3) drawLine(ink, topLeft + Offset(0f, i * step), topLeft + Offset(cell, i * step), w)
                for (i in 1..3) drawLine(ink, topLeft + Offset(i * step, 0f), topLeft + Offset(i * step, cell), w)
            }
        }
    }
}
