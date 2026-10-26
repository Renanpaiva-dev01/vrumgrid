package com.miaugrid.core

import com.miaugrid.core.Fixtures.p
import com.miaugrid.core.game.GameAction
import com.miaugrid.core.game.GameModeConfig
import com.miaugrid.core.game.GameModeId
import com.miaugrid.core.game.GameSession
import com.miaugrid.core.game.GameStatus
import com.miaugrid.core.game.ValidationPolicy
import com.miaugrid.core.hints.Hint
import com.miaugrid.core.hints.HintEngine
import com.miaugrid.core.levels.LevelRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HintEngineTest {

    @Test fun `X numa celula da solucao vira dica de correcao`() {
        val session = GameSession(Fixtures.puzzle5, GameModeConfig.CLASSIC)
        val state = session.reduce(session.newGame("h"), GameAction.ToggleMark(p(1, 2))).state
        assertEquals(Hint.MarkedSolutionCell(setOf(p(1, 2))), HintEngine.findHint(Fixtures.puzzle5, state))
    }

    @Test fun `gato fora da solucao no modo somente regras vira dica`() {
        val session = GameSession(Fixtures.puzzle5, GameModeConfig(GameModeId.CLASSIC, validation = ValidationPolicy.RULES_ONLY))
        val state = session.reduce(session.newGame("h"), GameAction.PlaceCat(p(0, 0))).state
        assertEquals(Hint.WrongCats(setOf(p(0, 0))), HintEngine.findHint(Fixtures.puzzle5, state))
    }

    /**
     * Seguir apenas as dicas resolve todas as fases sem nenhum erro: as dicas nunca sugerem
     * colocações incompatíveis com a solução e nunca marcam células da solução.
     */
    @Test fun `seguir as dicas resolve todas as fases sem erros`() {
        val repo = LevelRepository.loadCampaign()
        for (level in repo.levels) {
            val puzzle = repo.puzzle(level.id)!!
            val session = GameSession(puzzle, GameModeConfig.CLASSIC)
            var state = session.newGame(level.id)
            var guard = 0
            while (state.status == GameStatus.PLAYING && guard++ < 500) {
                val hint = HintEngine.findHint(puzzle, state)
                assertNotNull(level.id, hint)
                val action = when (hint) {
                    is Hint.ForcedPlacement -> GameAction.PlaceCat(hint.position)
                    is Hint.CorrectPosition -> GameAction.PlaceCat(hint.position)
                    is Hint.Elimination -> {
                        assertTrue(level.id, hint.cells.none { it in puzzle.solution })
                        GameAction.SetMarks(hint.cells.filter { it !in state.cats }, true)
                    }
                    else -> error("dica inesperada $hint em ${level.id}")
                }
                state = session.reduce(state, action).state
            }
            assertEquals(level.id, GameStatus.WON, state.status)
            assertEquals(level.id, 0, state.mistakes)
        }
    }

    @Test fun `alvo de revelacao pertence a solucao`() {
        val repo = LevelRepository.loadCampaign()
        val puzzle = repo.puzzle(repo.levels[20].id)!!
        val session = GameSession(puzzle, GameModeConfig.CLASSIC)
        val target = HintEngine.revealTarget(puzzle, session.newGame("r"))!!
        assertTrue(target in puzzle.solution)
        assertFalse(session.reduce(session.newGame("r"), GameAction.RevealCat(target)).state.cats.isEmpty())
    }
}
