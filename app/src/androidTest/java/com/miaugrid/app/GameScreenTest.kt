package com.miaugrid.app

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.miaugrid.app.ui.game.GameOverlays
import com.miaugrid.app.ui.game.GameScreen
import com.miaugrid.app.ui.game.GameUiState
import com.miaugrid.app.ui.game.Overlay
import com.miaugrid.app.ui.game.Tool
import com.miaugrid.app.ui.theme.MiauGridTheme
import com.miaugrid.core.game.GameAction
import com.miaugrid.core.game.GameModeConfig
import com.miaugrid.core.game.GameSession
import com.miaugrid.core.settings.ControlMode
import com.miaugrid.core.settings.UserSettings
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tela da partida isolada, com estado controlado pelo teste. */
@RunWith(AndroidJUnit4::class)
class GameScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val puzzle = Levels.puzzle(1)
    private val session = GameSession(puzzle, GameModeConfig.CLASSIC)

    private fun render(ui: GameUiState, onUndo: () -> Unit = {}, onRecover: () -> Unit = {}) {
        compose.setContent {
            MiauGridTheme {
                GameScreen(
                    ui = ui, onTap = {}, onDoubleTap = {}, onDragMarks = { _, _ -> }, onUndo = onUndo, onHint = {},
                    onRestart = {}, onPause = {}, onSelectTool = {}, onHintAction = {}, onDismissHint = {},
                    onMessageShown = {}, onExit = {},
                    overlays = {
                        GameOverlays(
                            ui = ui, onResume = {}, onRestartRequested = {}, onRestartConfirmed = {}, onOpenSettings = {},
                            onExit = {}, onNextLevel = {}, onRecover = onRecover, onWatchAdForHint = {},
                        )
                    },
                )
            }
        }
    }

    @Test
    fun desfazerDesabilitadoSemHistorico() {
        render(GameUiState(loading = false, puzzle = puzzle, game = session.newGame("t"), title = "Fase 1"))
        compose.onNodeWithTag("undo").assertIsNotEnabled()
    }

    @Test
    fun desfazerChamaOViewModel() {
        var calls = 0
        val state = session.reduce(session.newGame("t"), GameAction.ToggleMark(Levels.wrongCells(puzzle).first())).state
        render(GameUiState(loading = false, puzzle = puzzle, game = state), onUndo = { calls++ })
        compose.onNodeWithTag("undo").performClick()
        assertEquals(1, calls)
    }

    @Test
    fun ferramentaCarroEhSelecionavel() {
        var chosen: Tool? = null
        compose.setContent {
            MiauGridTheme {
                GameScreen(
                    ui = GameUiState(
                        loading = false, puzzle = puzzle, game = session.newGame("t"),
                        settings = UserSettings(controlMode = ControlMode.TOOL_SELECT),
                    ),
                    onTap = {}, onDoubleTap = {}, onDragMarks = { _, _ -> }, onUndo = {}, onHint = {},
                    onRestart = {}, onPause = {}, onSelectTool = { chosen = it }, onHintAction = {}, onDismissHint = {},
                    onMessageShown = {}, onExit = {}, overlays = {},
                )
            }
        }
        compose.onNodeWithTag("tool_car").performClick()
        assertEquals(Tool.CAR, chosen)
    }

    @Test
    fun derrotaSemAnunciosNaoMostraBotaoDeRecuperacao() {
        val lost = Levels.wrongCells(puzzle).take(3).fold(session.newGame("t")) { s, p -> session.reduce(s, GameAction.PlaceCat(p)).state }
        render(GameUiState(loading = false, puzzle = puzzle, game = lost, overlay = Overlay.Defeat(), canRecover = true, adsEnabled = false))
        compose.onNodeWithTag("defeat").assertExists()
        compose.onNodeWithTag("recover").assertDoesNotExist()
    }

    @Test
    fun derrotaComAnunciosOfereceRecuperacao() {
        var asked = 0
        val lost = Levels.wrongCells(puzzle).take(3).fold(session.newGame("t")) { s, p -> session.reduce(s, GameAction.PlaceCat(p)).state }
        render(
            GameUiState(loading = false, puzzle = puzzle, game = lost, overlay = Overlay.Defeat(), canRecover = true, adsEnabled = true),
            onRecover = { asked++ },
        )
        compose.onNodeWithTag("recover").performClick()
        assertEquals(1, asked)
    }
}
