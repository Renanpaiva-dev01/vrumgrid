package com.miaugrid.app

import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.miaugrid.core.game.GameModeConfig
import com.miaugrid.core.game.GameSession
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Fluxos principais em um aparelho/emulador: navegação, estacionar por toque duplo,
 * marcação por toque, marcação automática pelo carrinho, desfazer, pausa, vidas, derrota, reinício, vitória e persistência.
 */
@RunWith(AndroidJUnit4::class)
class GameFlowTest {

    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(ResetPlayerDataRule()).around(compose)

    private val puzzle = Levels.puzzle(1)
    private val n = puzzle.size

    private fun waitFor(tag: String) = compose.waitUntil(5_000) {
        compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun openLevel1() {
        waitFor("home_title")
        compose.onNodeWithTag("open_levels").performClick()
        waitFor("level_1")
        compose.onNodeWithTag("level_1").performClick()
        waitFor("board")
        compose.onNodeWithTag("level_title").assertTextContains("Fase 1")
    }

    private fun board() = compose.onNodeWithTag("board")

    @Test
    fun iniciaPartidaPelaTelaInicial() {
        waitFor("play")
        compose.onNodeWithTag("play").performClick()
        waitFor("board")
        board().assertIsDisplayed()
        compose.onNodeWithTag("lives").assertContentDescriptionContains("3 de 3 vidas")
    }

    @Test
    fun toqueMarcaXeToqueDuploEstacionaCarro() {
        openLevel1()
        val wrong = Levels.wrongCells(puzzle).first()
        board().tapCell(wrong, n)
        compose.waitForIdle()
        board().assertContentDescriptionContains("1 marcações")

        val right = puzzle.solution.positions().first()
        board().doubleTapCell(right, n)
        compose.waitForIdle()
        compose.onNodeWithTag("cat_count").assertTextContains("Carros: 1/$n")
        compose.onNodeWithTag("lives").assertContentDescriptionContains("3 de 3 vidas")

        compose.onNodeWithTag("undo").performClick()
        compose.onNodeWithTag("cat_count").assertTextContains("Carros: 0/$n")
    }

    @Test
    fun carrinhoMarcaXAutomaticamenteEDesfazerRemoveTudo() {
        openLevel1()
        val start = GameSession(puzzle, GameModeConfig.CLASSIC).newGame("t")
        val right = puzzle.solution.positions().first { it !in start.cats }
        val expected = GameSession(puzzle, GameModeConfig.CLASSIC).autoMarksFor(right, start.cats, start.marks).size
        board().doubleTapCell(right, n)
        compose.waitForIdle()
        board().assertContentDescriptionContains("$expected marcações")
        compose.onNodeWithTag("undo").performClick()
        compose.waitForIdle()
        board().assertContentDescriptionContains("0 marcações")
    }

    @Test
    fun erroConsomeVidaEDerrotaPermiteReiniciar() {
        openLevel1()
        val wrong = Levels.wrongCells(puzzle)
        board().doubleTapCell(wrong[0], n)
        compose.waitForIdle()
        compose.onNodeWithTag("lives").assertContentDescriptionContains("2 de 3 vidas")
        compose.onNodeWithTag("feedback").assertIsDisplayed()

        board().doubleTapCell(wrong[1], n)
        board().doubleTapCell(wrong[2], n)
        waitFor("defeat")
        compose.onNodeWithTag("defeat_restart").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("lives").assertContentDescriptionContains("3 de 3 vidas")
    }

    @Test
    fun resolverAFaseMostraVitoriaELiberaAProxima() {
        openLevel1()
        puzzle.solution.positions().forEach { board().doubleTapCell(it, n) }
        waitFor("victory")
        compose.onNodeWithTag("next_level").performClick()
        waitFor("board")
        compose.onNodeWithTag("level_title").assertTextContains("Fase 2")
    }

    @Test
    fun pausaEContinuar() {
        openLevel1()
        compose.onNodeWithTag("pause").performClick()
        waitFor("pause_menu")
        compose.onNodeWithTag("resume").performClick()
        compose.waitForIdle()
        board().assertIsDisplayed()
    }

    @Test
    fun partidaSobreviveARecriacaoDaActivity() {
        openLevel1()
        board().doubleTapCell(puzzle.solution.positions().first(), n)
        compose.waitForIdle()
        compose.activityRule.scenario.recreate()
        waitFor("board")
        compose.onNodeWithTag("cat_count").assertTextContains("Carros: 1/$n")
    }
}
