package com.miaugrid.app.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miaugrid.app.ui.components.CatIcon
import com.miaugrid.app.ui.components.ErrorState
import com.miaugrid.app.ui.components.LoadingState
import com.miaugrid.app.ui.components.formatDuration
import com.miaugrid.core.game.GameState
import com.miaugrid.core.game.GameStatus
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
    onTap: (com.miaugrid.core.model.Position) -> Unit,
    onDoubleTap: (com.miaugrid.core.model.Position) -> Unit,
    onDragMarks: (List<com.miaugrid.core.model.Position>, Boolean) -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onRestart: () -> Unit,
    onPause: () -> Unit,
    onSelectTool: (Tool) -> Unit,
    onHintAction: () -> Unit,
    onDismissHint: () -> Unit,
    onMessageShown: () -> Unit,
    onExit: () -> Unit,
    overlays: @Composable () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            onMessageShown()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        when {
            ui.loading -> LoadingState("Preparando o tabuleiro…")
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
                    ProgressRow(game, ui.puzzle.size)
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
                            enabled = ui.interactive,
                            onTap = onTap,
                            onDoubleTap = onDoubleTap,
                            onDragMarks = onDragMarks,
                            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    StatusArea(ui, onHintAction, onDismissHint)
                    Spacer(Modifier.height(8.dp))
                    if (ui.settings.controlMode == ControlMode.TOOL_SELECT) {
                        ToolSelector(ui.tool, onSelectTool)
                        Spacer(Modifier.height(4.dp))
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
        IconButton(onClick = onPause, modifier = Modifier.testTag("pause")) {
            Icon(Icons.Filled.Pause, contentDescription = "Pausar e configurações")
        }
        Column(Modifier.weight(1f)) {
            Text(ui.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("level_title"))
            Text(ui.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (ui.settings.showTimer) {
            Text(
                formatDuration(game.elapsedMillis),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(end = 12.dp).testTag("timer"),
            )
        }
        Lives(game.lives, ui.config.initialLives)
    }
}

@Composable
private fun Lives(lives: Int, max: Int) {
    val total = maxOf(lives, max)
    Row(
        Modifier
            .testTag("lives")
            .semantics(mergeDescendants = true) { contentDescription = "$lives de $max vidas" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            Icon(
                if (i < lives) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(" $lives", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ProgressRow(game: GameState, size: Int) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Gatos: ${game.cats.size}/$size", style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("cat_count"))
            Text("Erros: ${game.mistakes}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.testTag("mistakes"))
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { game.cats.size.toFloat() / size },
            modifier = Modifier.fillMaxWidth().height(6.dp),
        )
    }
}

@Composable
private fun StatusArea(ui: GameUiState, onHintAction: () -> Unit, onDismissHint: () -> Unit) {
    val hint = ui.hint
    val feedback = ui.feedback
    Box(Modifier.fillMaxWidth().height(92.dp)) {
        when {
            hint != null -> Card(
                Modifier.fillMaxSize().testTag("hint_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(hint.text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        IconButton(onClick = onDismissHint, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Fechar dica")
                        }
                        hint.actionLabel?.let { TextButton(onClick = onHintAction) { Text(it) } }
                    }
                }
            }
            feedback != null -> Text(
                feedback.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().align(Alignment.Center).testTag("feedback"),
            )
            else -> Text(
                if (ui.settings.controlMode == ControlMode.DOUBLE_TAP) "Toque: marcar X · Toque duplo: gato · Arraste: vários X"
                else "Toque aplica a ferramenta · Toque duplo: gato · Arraste: vários X",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun ToolSelector(tool: Tool, onSelectTool: (Tool) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilterChip(
            selected = tool == Tool.MARK,
            onClick = { onSelectTool(Tool.MARK) },
            label = { Text("✕  Marcar") },
            modifier = Modifier.testTag("tool_mark"),
        )
        FilterChip(
            selected = tool == Tool.CAT,
            onClick = { onSelectTool(Tool.CAT) },
            label = { Text("Gato") },
            leadingIcon = { CatIcon(Modifier.size(20.dp)) },
            modifier = Modifier.testTag("tool_cat"),
        )
    }
}

@Composable
private fun Controls(ui: GameUiState, game: GameState, onUndo: () -> Unit, onHint: () -> Unit, onRestart: () -> Unit) {
    val playing = ui.interactive
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        ControlButton("Desfazer", "undo", enabled = playing && game.history.isNotEmpty(), onClick = onUndo) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
        }
        val hintLabel = if (game.freeHintsLeft > 0) "Dica (${game.freeHintsLeft})" else "Dica"
        ControlButton(hintLabel, "hint", enabled = playing, onClick = onHint) {
            Icon(Icons.Filled.Lightbulb, contentDescription = null)
        }
        ControlButton("Reiniciar", "restart", enabled = game.status != GameStatus.WON, onClick = onRestart) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
        }
    }
}

@Composable
private fun ControlButton(label: String, tag: String, enabled: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.testTag(tag)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            icon()
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Fundo escurecido com cartão central, usado pelas telas sobrepostas. */
@Composable
fun OverlayCard(tag: String, content: @Composable () -> Unit) {
    val visibility = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(visibleState = visibility, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                .testTag(tag),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                Modifier.padding(24.dp).widthIn(max = 420.dp).fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) { content() }
            }
        }
    }
}
