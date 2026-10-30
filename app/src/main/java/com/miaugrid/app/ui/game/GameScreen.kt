package com.miaugrid.app.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miaugrid.app.ui.components.CarIcon
import com.miaugrid.app.ui.components.CarLook
import com.miaugrid.app.ui.components.ErrorState
import com.miaugrid.app.ui.components.LoadingState
import com.miaugrid.app.ui.components.LocalCarLook
import com.miaugrid.app.ui.components.RoadSign
import com.miaugrid.app.ui.components.TireLives
import com.miaugrid.app.ui.components.drawCar
import com.miaugrid.app.ui.components.formatDuration
import com.miaugrid.app.ui.components.pressScale
import com.miaugrid.app.ui.theme.LocalBoardColors
import com.miaugrid.app.ui.theme.Road
import com.miaugrid.core.game.GameState
import com.miaugrid.core.game.GameStatus
import com.miaugrid.core.model.Position
import com.miaugrid.core.settings.ControlMode

@Composable
fun GameRoute(
    viewModel: GameViewModel,
    onExit: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLevel: (String) -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val haptics by viewModel.haptics.collectAsStateWithLifecycle()
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(haptics?.first) {
        when (haptics?.second) {
            HapticCue.LIGHT -> hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            HapticCue.ERROR -> hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            null -> Unit
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onAppPaused() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAppResumed() }

    BackHandler(enabled = ui.game != null) {
        when {
            ui.overlay != Overlay.None -> viewModel.onDismissOverlay()
            ui.game?.status == GameStatus.PLAYING -> viewModel.onPause()
            else -> viewModel.onLeave(onExit)
        }
    }

    GameScreen(
        ui = ui,
        onTap = viewModel::onCellTap,
        onDoubleTap = viewModel::onCellDoubleTap,
        onDragMarks = viewModel::onDragMarks,
        onUndo = viewModel::onUndo,
        onHint = viewModel::onHint,
        onRestart = viewModel::onRestartRequested,
        onPause = viewModel::onPause,
        onSelectTool = viewModel::onSelectTool,
        onHintAction = viewModel::onHintAction,
        onDismissHint = viewModel::onDismissHint,
        onMessageShown = viewModel::onMessageShown,
        onDriveFinished = viewModel::onDriveFinished,
        onExit = { viewModel.onLeave(onExit) },
        overlays = {
            GameOverlays(
                ui = ui,
                onResume = viewModel::onDismissOverlay,
                onRestartRequested = viewModel::onRestartRequested,
                onRestartConfirmed = viewModel::onRestartConfirmed,
                onOpenSettings = onOpenSettings,
                onExit = { viewModel.onLeave(onExit) },
                onNextLevel = { viewModel.onNextLevel(onOpenLevel) },
                onRecover = viewModel::onRecoverWithAd,
                onWatchAdForHint = viewModel::onWatchAdForHint,
            )
        },
    )
}

/** Tela da partida, sem estado próprio (facilita testes de interface). */
@Composable
fun GameScreen(
    ui: GameUiState,
    onTap: (Position) -> Unit,
    onDoubleTap: (Position) -> Unit,
    onDragMarks: (List<Position>, Boolean) -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onRestart: () -> Unit,
    onPause: () -> Unit,
    onSelectTool: (Tool) -> Unit,
    onHintAction: () -> Unit,
    onDismissHint: () -> Unit,
    onMessageShown: () -> Unit,
    onExit: () -> Unit,
    onDriveFinished: (Long) -> Unit = {},
    overlays: @Composable () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            onMessageShown()
        }
    }
    val bg = MaterialTheme.colorScheme.background

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bg, MaterialTheme.colorScheme.surfaceVariant)))
            .safeDrawingPadding(),
    ) {
        when {
            ui.loading -> LoadingState("Aquecendo o motor…")
            ui.error != null -> ErrorState(ui.error) { Button(onClick = onExit) { Text("Voltar") } }
            ui.puzzle != null && ui.game != null -> {
                val game = ui.game
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GameHeader(ui, game, onPause)
                    Spacer(Modifier.height(8.dp))
                    RulesStrip()
                    Spacer(Modifier.height(8.dp))
                    RoadProgress(game, ui.puzzle.size, LocalCarLook.current)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        GameBoard(
                            puzzle = ui.puzzle,
                            game = game,
                            blocked = ui.blocked,
                            selected = ui.selected,
                            settings = ui.settings,
                            feedback = ui.feedback,
                            pulse = ui.pulse,
                            hint = ui.hint,
                            drive = ui.drive,
                            driver = LocalCarLook.current,
                            enabled = ui.interactive,
                            onTap = onTap,
                            onDoubleTap = onDoubleTap,
                            onDragMarks = onDragMarks,
                            onDriveFinished = onDriveFinished,
                            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    StatusArea(ui, onHintAction, onDismissHint)
                    Spacer(Modifier.height(8.dp))
                    if (ui.settings.controlMode == ControlMode.TOOL_SELECT) {
                        ToolSelector(ui.tool, LocalCarLook.current, onSelectTool)
                        Spacer(Modifier.height(10.dp))
                    }
                    Controls(ui, game, onUndo, onHint, onRestart)
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp))
        overlays()
    }
}

@Composable
private fun GameHeader(ui: GameUiState, game: GameState, onPause: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        val interaction = remember { MutableInteractionSource() }
        IconButton(
            onClick = onPause,
            interactionSource = interaction,
            modifier = Modifier
                .testTag("pause")
                .pressScale(interaction)
                .background(Road.Asphalt, CircleShape),
        ) {
            Icon(Icons.Filled.Pause, contentDescription = "Pausar e configurações", tint = Color.White)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            RoadSign(ui.title, ui.subtitle)
        }
        Column(horizontalAlignment = Alignment.End) {
            TireLives(game.lives, ui.config.initialLives)
            if (ui.settings.showTimer) {
                Text(
                    formatDuration(game.elapsedMillis),
                    style = MaterialTheme.typography.labelLarge,
                    color = Road.TaxiYellow,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .background(Road.AsphaltDeep, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .testTag("timer"),
                )
            }
        }
    }
}

/** As três regras, como placas pequenas desenhadas em vetor (sem texto longo). */
@Composable
private fun RulesStrip() {
    val board = LocalBoardColors.current
    val look = LocalCarLook.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RuleCard("1 carro por bairro", Modifier.weight(1f)) {
            val cell = size.minDimension / 3
            val origin = Offset((size.width - cell * 3) / 2, (size.height - cell * 3) / 2)
            val shades = listOf(board.regions[0], board.regions[1], board.regions[3])
            for (r in 0 until 3) for (c in 0 until 3) {
                val region = if (c + r < 2) 0 else if (c >= r) 1 else 2
                drawRoundRect(shades[region], origin + Offset(c * cell + 1, r * cell + 1), Size(cell - 2, cell - 2), CornerRadius(cell * 0.2f))
            }
            drawCar(origin + Offset(cell * 0.5f, cell * 0.5f), cell * 0.9f, look)
        }
        RuleCard("1 por linha e coluna", Modifier.weight(1f)) {
            val cell = size.minDimension / 3
            val origin = Offset((size.width - cell * 3) / 2, (size.height - cell * 3) / 2)
            drawRoundRect(Road.TaxiYellow.copy(alpha = 0.45f), origin + Offset(0f, cell), Size(cell * 3, cell), CornerRadius(cell * 0.3f))
            drawRoundRect(Road.TaxiYellow.copy(alpha = 0.45f), origin + Offset(cell, 0f), Size(cell, cell * 3), CornerRadius(cell * 0.3f))
            drawCar(origin + Offset(cell * 1.5f, cell * 1.5f), cell * 0.9f, look, rotation = 90f)
        }
        RuleCard("Carros não se encostam", Modifier.weight(1f)) {
            val cell = size.minDimension / 3
            val c = Offset(size.width / 2, size.height / 2)
            drawCar(c - Offset(cell * 0.6f, 0f), cell * 1.1f, look)
            drawCar(c + Offset(cell * 0.6f, 0f), cell * 1.1f, look.copy(color = board.fixedCar))
            drawCircle(board.error, cell * 1.35f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(cell * 0.16f))
            drawLine(board.error, c + Offset(-cell, -cell), c + Offset(cell, cell), cell * 0.16f)
        }
    }
}

@Composable
private fun RuleCard(label: String, modifier: Modifier, icon: DrawScope.() -> Unit) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(6.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Regra: $label" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(34.dp), onDraw = icon)
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** Progresso como uma estradinha: o carro anda até a bandeira quadriculada. */
@Composable
private fun RoadProgress(game: GameState, boardSize: Int, look: CarLook) {
    val fraction by animateFloatAsState(game.cats.size.toFloat() / boardSize, spring(dampingRatio = 0.7f, stiffness = 120f), label = "progress")
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Carros: ${game.cats.size}/$boardSize", style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("cat_count"))
            Text("Erros: ${game.mistakes}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("mistakes"))
        }
        Spacer(Modifier.height(4.dp))
        Canvas(Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(11.dp))) {
            val h = size.height
            drawRect(Road.Asphalt)
            val flag = h * 0.9f
            drawLine(
                Road.LanePaint.copy(alpha = 0.7f), Offset(h, h / 2), Offset(size.width - flag, h / 2), h * 0.08f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(h * 0.5f, h * 0.4f)),
            )
            val sq = h / 4
            for (r in 0 until 4) for (c in 0 until 2) {
                drawRect(if ((r + c) % 2 == 0) Color.White else Color.Black, Offset(size.width - flag + c * sq, r * sq), Size(sq, sq))
            }
            val travel = size.width - flag - h * 1.2f
            drawCar(Offset(h * 0.6f + travel * fraction, h / 2), h * 1.1f, look, rotation = 90f)
        }
    }
}

@Composable
private fun StatusArea(ui: GameUiState, onHintAction: () -> Unit, onDismissHint: () -> Unit) {
    val hint = ui.hint
    val feedback = ui.feedback
    val key = when {
        hint != null -> "hint"
        feedback != null -> "feedback${feedback.id}"
        else -> "help"
    }
    AnimatedContent(
        targetState = key,
        transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) },
        modifier = Modifier.fillMaxWidth().height(92.dp),
        label = "status",
    ) { current ->
        Box(Modifier.fillMaxSize()) {
            when {
                current == "hint" && hint != null -> Card(
                    Modifier.fillMaxSize().testTag("hint_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = Road.RacingOrange)
                        Text(hint.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            IconButton(onClick = onDismissHint, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = "Fechar dica")
                            }
                            hint.actionLabel?.let { TextButton(onClick = onHintAction) { Text(it) } }
                        }
                    }
                }
                current.startsWith("feedback") && feedback != null -> Text(
                    feedback.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center).testTag("feedback"),
                )
                else -> Text(
                    if (ui.settings.controlMode == ControlMode.DOUBLE_TAP) "Toque: marcar X · Toque duplo: estacionar · Arraste: vários X"
                    else "Toque aplica a ferramenta · Toque duplo: estacionar · Arraste: vários X",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                )
            }
        }
    }
}

/** Seletor de ferramenta em duas metades, com a "pílula" deslizando para a escolhida. */
@Composable
private fun ToolSelector(tool: Tool, look: CarLook, onSelectTool: (Tool) -> Unit) {
    val offset by animateFloatAsState(if (tool == Tool.MARK) 0f else 1f, spring(dampingRatio = 0.75f, stiffness = 500f), label = "tool")
    val width = 280.dp
    Box(
        Modifier
            .width(width)
            .height(48.dp)
            .background(Road.Asphalt, RoundedCornerShape(24.dp))
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .padding(start = (width - 8.dp) / 2 * offset)
                .width((width - 8.dp) / 2)
                .fillMaxHeight()
                .background(Road.TaxiYellow, RoundedCornerShape(20.dp)),
        )
        Row(Modifier.fillMaxSize()) {
            ToolHalf("Marcar X", tool == Tool.MARK, "tool_mark", Modifier.weight(1f), { onSelectTool(Tool.MARK) }) {
                Text("✕", style = MaterialTheme.typography.titleMedium, color = if (tool == Tool.MARK) Road.AsphaltDeep else Color.White)
            }
            ToolHalf("Estacionar", tool == Tool.CAR, "tool_car", Modifier.weight(1f), { onSelectTool(Tool.CAR) }) {
                CarIcon(look, Modifier.size(24.dp), rotation = 90f)
            }
        }
    }
}

@Composable
private fun ToolHalf(label: String, selected: Boolean, tag: String, modifier: Modifier, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .pressScale(interaction)
            .semantics { contentDescription = if (selected) "$label, selecionado" else label }
            .testTag(tag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) Road.AsphaltDeep else Color.White)
    }
}

@Composable
private fun Controls(ui: GameUiState, game: GameState, onUndo: () -> Unit, onHint: () -> Unit, onRestart: () -> Unit) {
    val playing = ui.interactive
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        ControlButton("Desfazer", "undo", Road.SignBlue, enabled = playing && game.history.isNotEmpty(), onClick = onUndo) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null, tint = Color.White)
        }
        val hintLabel = if (game.freeHintsLeft > 0) "Dica (${game.freeHintsLeft})" else "Dica"
        ControlButton(hintLabel, "hint", Road.RacingOrange, enabled = playing, onClick = onHint) {
            Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = Color.White)
        }
        ControlButton("Reiniciar", "restart", Road.SignGreen, enabled = game.status != GameStatus.WON, onClick = onRestart) {
            Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White)
        }
    }
}

/** Botão redondo, com sombra de "pneu" e escala ao pressionar. */
@Composable
private fun ControlButton(label: String, tag: String, color: Color, enabled: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .pressScale(interaction)
            .padding(4.dp)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .background(Road.AsphaltDeep, CircleShape)
                .padding(4.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center,
        ) { icon() }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

/** Fundo escurecido com cartão central que "estaciona" com mola, usado pelas telas sobrepostas. */
@Composable
fun OverlayCard(tag: String, background: @Composable () -> Unit = {}, content: @Composable () -> Unit) {
    val visibility = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibility,
        enter = fadeIn(tween(180)) + scaleIn(spring(dampingRatio = 0.6f, stiffness = 380f), initialScale = 0.85f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                .testTag(tag),
            contentAlignment = Alignment.Center,
        ) {
            background()
            Card(
                Modifier.padding(24.dp).widthIn(max = 420.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Box(Modifier.fillMaxWidth().height(10.dp).background(CheckeredBrush))
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) { content() }
            }
        }
    }
}

/** Faixa quadriculada no topo dos cartões (bandeira de chegada). */
private val CheckeredBrush = Brush.horizontalGradient(
    *(0 until 40).flatMap { i ->
        val c = if (i % 2 == 0) Color.Black else Color.White
        listOf(i / 40f to c, (i + 1) / 40f - 0.0001f to c)
    }.toTypedArray(),
)
