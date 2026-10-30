package com.miaugrid.core

import com.miaugrid.core.Fixtures.p
import com.miaugrid.core.game.GameAction
import com.miaugrid.core.game.GameEvent
import com.miaugrid.core.game.GameModeConfig
import com.miaugrid.core.game.GameSession
import com.miaugrid.core.game.GameStatus
import com.miaugrid.core.levels.LevelRepository
import com.miaugrid.core.persistence.InMemoryKeyValueStore
import com.miaugrid.core.persistence.Documents
import com.miaugrid.core.persistence.VersionedStorage
import com.miaugrid.core.rules.RulesEngine
import com.miaugrid.core.settings.CarBody
import com.miaugrid.core.settings.CarStyle
import com.miaugrid.core.settings.UserSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Marcação automática feita pelo carrinho ao estacionar uma peça. */
class AutoMarkTest {

    private val session = GameSession(Fixtures.puzzle5, GameModeConfig.CLASSIC)

    @Test fun `marca exatamente as celulas bloqueadas pela nova peca`() {
        val out = session.reduce(session.newGame("a"), GameAction.PlaceCat(p(1, 2), autoMark = true))
        val event = out.events.first() as GameEvent.CatPlaced
        val expected = RulesEngine.blockedCells(Fixtures.layout5, setOf(p(1, 2)))
        assertEquals(expected, event.autoMarked.toSet())
        assertEquals(expected, out.state.marks)
        assertEquals(3, out.state.lives)
    }

    @Test fun `trajeto comeca perto da peca e nao repete celulas`() {
        val event = session.reduce(session.newGame("a"), GameAction.PlaceCat(p(1, 2), autoMark = true)).events.first() as GameEvent.CatPlaced
        val route = event.autoMarked
        assertEquals(route.size, route.toSet().size)
        assertTrue(route.first().touches(p(1, 2)))
    }

    @Test fun `nao remarca X existentes nem celulas com peca`() {
        var state = session.reduce(session.newGame("a"), GameAction.ToggleMark(p(1, 0))).state
        state = session.reduce(state, GameAction.PlaceCat(p(0, 4))).state
        val event = session.reduce(state, GameAction.PlaceCat(p(1, 2), autoMark = true)).events.first() as GameEvent.CatPlaced
        assertFalse(p(1, 0) in event.autoMarked)
        assertFalse(p(0, 4) in event.autoMarked)
    }

    @Test fun `desfazer remove a peca e todos os X automaticos de uma vez`() {
        val manual = session.reduce(session.newGame("a"), GameAction.ToggleMark(p(4, 4))).state
        val placed = session.reduce(manual, GameAction.PlaceCat(p(1, 2), autoMark = true)).state
        val undone = session.reduce(placed, GameAction.Undo).state
        assertEquals(manual.cats, undone.cats)
        assertEquals(manual.marks, undone.marks)
    }

    @Test fun `sem marcacao automatica nada muda`() {
        val out = session.reduce(session.newGame("a"), GameAction.PlaceCat(p(1, 2)))
        assertTrue(out.state.marks.isEmpty())
        assertTrue((out.events.first() as GameEvent.CatPlaced).autoMarked.isEmpty())
    }

    @Test fun `colocacao recusada nao gera marcas`() {
        val out = session.reduce(session.newGame("a"), GameAction.PlaceCat(p(0, 0), autoMark = true))
        assertTrue(out.state.marks.isEmpty())
        assertEquals(2, out.state.lives)
    }

    /** Em todas as fases: o carrinho nunca marca X numa célula da solução. */
    @Test fun `marcacao automatica nunca cobre a solucao em nenhuma fase`() {
        val repo = LevelRepository.loadCampaign()
        for (level in repo.levels) {
            val puzzle = repo.puzzle(level.id)!!
            val s = GameSession(puzzle, GameModeConfig.CLASSIC)
            var state = s.newGame(level.id)
            for (p in puzzle.solution.positions().shuffled(kotlin.random.Random(level.number))) {
                state = s.reduce(state, GameAction.PlaceCat(p, autoMark = true)).state
                assertTrue(level.id, state.marks.none { it in puzzle.solution })
            }
            assertEquals(level.id, GameStatus.WON, state.status)
            // Com todos os carros estacionados, todas as outras células ficaram com X.
            assertEquals(level.id, puzzle.size * puzzle.size - puzzle.size, state.marks.size)
        }
    }

    @Test fun `estilo do carrinho persiste e configuracoes antigas continuam legiveis`() = runTest {
        val store = InMemoryKeyValueStore()
        val storage = VersionedStorage(store)
        val custom = UserSettings(car = CarStyle(body = CarBody.FORMULA, bodyColor = 0xFF1E88E5, number = 42))
        storage.save(Documents.SETTINGS, custom)
        assertEquals(custom, storage.load(Documents.SETTINGS).value)

        // Documento gravado pela versão anterior (sem campos do carro).
        store.putRaw("settings", """{"v":1,"data":{"soundEnabled":false}}""")
        val old = storage.load(Documents.SETTINGS).value
        assertFalse(old.soundEnabled)
        assertEquals(CarStyle(), old.car)
        assertTrue(old.autoMarkWithCar)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `numero do carro fora do limite`() {
        CarStyle(number = 100)
    }
}
