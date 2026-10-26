package com.miaugrid.core

import com.miaugrid.core.Fixtures.p
import com.miaugrid.core.Fixtures.puzzle5
import com.miaugrid.core.Fixtures.puzzle5WithGiven
import com.miaugrid.core.game.ActionOutcome
import com.miaugrid.core.game.GameAction
import com.miaugrid.core.game.GameEvent
import com.miaugrid.core.game.GameModeConfig
import com.miaugrid.core.game.GameModeId
import com.miaugrid.core.game.GameSession
import com.miaugrid.core.game.GameState
import com.miaugrid.core.game.GameStatus
import com.miaugrid.core.game.IgnoreReason
import com.miaugrid.core.game.RecoveryPolicy
import com.miaugrid.core.game.ValidationPolicy
import com.miaugrid.core.rules.ViolationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionTest {

    private val session = GameSession(puzzle5, GameModeConfig.CLASSIC)

    private fun GameSession.play(state: GameState, vararg actions: GameAction): ActionOutcome {
        var outcome = ActionOutcome(state, emptyList())
        for (a in actions) outcome = reduce(outcome.state, a)
        return outcome
    }

    private fun fresh() = session.newGame("s1")

    private fun wrong(i: Int) = listOf(p(0, 0), p(0, 1), p(1, 0))[i]

    @Test fun `comeca com tres vidas`() {
        val state = fresh()
        assertEquals(3, state.lives)
        assertEquals(0, state.mistakes)
        assertEquals(GameStatus.PLAYING, state.status)
    }

    @Test fun `jogada correta nao consome vida`() {
        val out = session.reduce(fresh(), GameAction.PlaceCat(p(1, 2)))
        assertEquals(3, out.state.lives)
        assertTrue(p(1, 2) in out.state.cats)
        assertTrue(out.events.first() is GameEvent.CatPlaced)
    }

    @Test fun `colocacao incorreta consome uma vida e nao entra no tabuleiro`() {
        val out = session.reduce(fresh(), GameAction.PlaceCat(p(0, 0)))
        assertEquals(2, out.state.lives)
        assertEquals(1, out.state.mistakes)
        assertFalse(p(0, 0) in out.state.cats)
        val rejected = out.events.single() as GameEvent.CatRejected
        assertTrue(rejected.lifeLost)
        assertTrue(rejected.check.has(ViolationType.NOT_IN_SOLUTION))
    }

    @Test fun `marcacoes nao consomem vidas`() {
        val out = session.play(fresh(), GameAction.ToggleMark(p(0, 0)), GameAction.SetMarks(listOf(p(0, 1), p(0, 2)), true))
        assertEquals(3, out.state.lives)
        assertEquals(setOf(p(0, 0), p(0, 1), p(0, 2)), out.state.marks)
        assertTrue(out.state.cats.isEmpty())
    }

    @Test fun `marcacao X numa celula da solucao nao vira impossivel`() {
        val out = session.play(fresh(), GameAction.ToggleMark(p(1, 2)), GameAction.PlaceCat(p(1, 2)))
        assertTrue(p(1, 2) in out.state.cats)
        assertFalse(p(1, 2) in out.state.marks)
        assertEquals(3, out.state.lives)
    }

    @Test fun `derrota ao zerar vidas acontece uma unica vez`() {
        val out = session.play(fresh(), GameAction.PlaceCat(wrong(0)), GameAction.PlaceCat(wrong(1)), GameAction.PlaceCat(wrong(2)))
        assertEquals(0, out.state.lives)
        assertEquals(GameStatus.LOST, out.state.status)
        assertEquals(1, out.state.defeats)
        assertTrue(out.events.any { it is GameEvent.Lost })
        // Depois da derrota, nenhuma jogada é aceita e não há segunda derrota.
        val after = session.play(out.state, GameAction.PlaceCat(p(1, 2)), GameAction.PlaceCat(p(4, 4)), GameAction.ToggleMark(p(0, 0)), GameAction.Undo)
        assertEquals(out.state, after.state)
        assertEquals(1, after.state.defeats)
        assertEquals(GameEvent.Ignored(IgnoreReason.GAME_NOT_ACTIVE), after.events.single())
    }

    @Test fun `vitoria so com todas as condicoes`() {
        var state = fresh()
        val cats = Fixtures.solution5.positions().toList()
        for (c in cats.dropLast(1)) {
            state = session.reduce(state, GameAction.PlaceCat(c)).state
            assertEquals(GameStatus.PLAYING, state.status)
        }
        val out = session.reduce(state, GameAction.PlaceCat(cats.last()))
        assertEquals(GameStatus.WON, out.state.status)
        assertTrue(GameEvent.Won in out.events)
        assertEquals(GameEvent.Ignored(IgnoreReason.GAME_NOT_ACTIVE), session.reduce(out.state, GameAction.Undo).events.single())
    }

    @Test fun `modo somente regras aceita gato fora da solucao sem penalidade mas nao vence`() {
        val relaxed = GameSession(puzzle5, GameModeConfig(GameModeId.CLASSIC, validation = ValidationPolicy.RULES_ONLY))
        val out = relaxed.reduce(relaxed.newGame("r"), GameAction.PlaceCat(p(0, 0)))
        assertTrue(p(0, 0) in out.state.cats)
        assertEquals(3, out.state.lives)
        // Quebra de regra continua custando vida.
        val broken = relaxed.reduce(out.state, GameAction.PlaceCat(p(0, 3)))
        assertEquals(2, broken.state.lives)
        assertFalse(p(0, 3) in broken.state.cats)
    }

    @Test fun `desfazer restaura o estado anterior e nao devolve vida`() {
        val start = fresh()
        val afterMistake = session.reduce(start, GameAction.PlaceCat(p(0, 0))).state
        val placed = session.play(afterMistake, GameAction.ToggleMark(p(1, 2)), GameAction.PlaceCat(p(1, 2))).state
        val undone = session.reduce(placed, GameAction.Undo).state
        assertFalse(p(1, 2) in undone.cats)
        assertTrue("o X anterior volta", p(1, 2) in undone.marks)
        assertEquals(2, undone.lives)
        val undoneTwice = session.reduce(undone, GameAction.Undo).state
        assertTrue(undoneTwice.marks.isEmpty())
        assertEquals(2, undoneTwice.lives)
        assertEquals(GameEvent.Ignored(IgnoreReason.NOTHING_TO_UNDO), session.reduce(undoneTwice, GameAction.Undo).events.single())
    }

    @Test fun `desfazer arrasto restaura cada celula`() {
        val state = session.play(fresh(), GameAction.ToggleMark(p(0, 0)), GameAction.SetMarks(listOf(p(0, 0), p(0, 1)), true)).state
        val undone = session.reduce(state, GameAction.Undo).state
        assertEquals(setOf(p(0, 0)), undone.marks)
    }

    @Test fun `remover gato do jogador e permitido e fixo nao`() {
        val s = GameSession(puzzle5WithGiven, GameModeConfig.CLASSIC)
        val placed = s.reduce(s.newGame("g"), GameAction.PlaceCat(p(0, 4))).state
        assertFalse(p(0, 4) in s.reduce(placed, GameAction.RemoveCat(p(0, 4))).state.cats)
        assertEquals(GameEvent.Ignored(IgnoreReason.FIXED_CELL), s.reduce(placed, GameAction.RemoveCat(p(2, 0))).events.single())
        assertEquals(GameEvent.Ignored(IgnoreReason.FIXED_CELL), s.reduce(placed, GameAction.PlaceCat(p(2, 0))).events.single())
    }

    @Test fun `reiniciar restaura o estado inicial`() {
        val s = GameSession(puzzle5WithGiven, GameModeConfig.CLASSIC)
        val messy = s.play(
            s.newGame("g"),
            GameAction.PlaceCat(p(0, 4)), GameAction.ToggleMark(p(4, 4)), GameAction.PlaceCat(p(0, 0)),
            GameAction.Tick(5_000),
        ).state
        val restarted = s.reduce(messy, GameAction.Restart).state
        assertEquals(setOf(p(2, 0)), restarted.cats)
        assertEquals(setOf(p(2, 0)), restarted.fixedCats)
        assertTrue(restarted.marks.isEmpty())
        assertTrue(restarted.history.isEmpty())
        assertEquals(3, restarted.lives)
        assertEquals(0, restarted.mistakes)
        assertEquals(0L, restarted.elapsedMillis)
        assertEquals(GameStatus.PLAYING, restarted.status)
    }

    @Test fun `reiniciar apos derrota volta a jogar`() {
        val lost = session.play(fresh(), GameAction.PlaceCat(wrong(0)), GameAction.PlaceCat(wrong(1)), GameAction.PlaceCat(wrong(2))).state
        val restarted = session.reduce(lost, GameAction.Restart).state
        assertEquals(GameStatus.PLAYING, restarted.status)
        assertEquals(3, restarted.lives)
    }

    @Test fun `cronometro so corre durante a partida`() {
        val s = session.reduce(fresh(), GameAction.Tick(1_500)).state
        assertEquals(1_500L, s.elapsedMillis)
        val lost = session.play(s, GameAction.PlaceCat(wrong(0)), GameAction.PlaceCat(wrong(1)), GameAction.PlaceCat(wrong(2))).state
        assertEquals(1_500L, session.reduce(lost, GameAction.Tick(1_000)).state.elapsedMillis)
    }

    @Test fun `recuperacao sem recompensa nao e concedida`() {
        val lost = session.play(fresh(), GameAction.PlaceCat(wrong(0)), GameAction.PlaceCat(wrong(1)), GameAction.PlaceCat(wrong(2))).state
        // Um id que não corresponde ao evento atual (ex.: clique sem confirmação do SDK) é ignorado.
        val out = session.reduce(lost, GameAction.ApplyRecovery("qualquer"))
        assertEquals(GameStatus.LOST, out.state.status)
        assertEquals(GameEvent.Ignored(IgnoreReason.RECOVERY_WRONG_EVENT), out.events.single())
    }

    @Test fun `recuperacao com recompensa restaura vidas e preserva o tabuleiro`() {
        val withCat = session.reduce(fresh(), GameAction.PlaceCat(p(1, 2))).state
        val lost = session.play(withCat, GameAction.PlaceCat(wrong(0)), GameAction.PlaceCat(wrong(1)), GameAction.PlaceCat(wrong(2))).state
        assertTrue(session.canOfferRecovery(lost))
        val out = session.reduce(lost, GameAction.ApplyRecovery(lost.currentDefeatId!!))
        assertEquals(GameStatus.PLAYING, out.state.status)
        assertEquals(1, out.state.lives)
        assertEquals(setOf(p(1, 2)), out.state.cats)
        assertEquals(3, out.state.mistakes)
    }

    @Test fun `recompensa duplicada nao e aplicada duas vezes`() {
        val lost = session.play(fresh(), GameAction.PlaceCat(wrong(0)), GameAction.PlaceCat(wrong(1)), GameAction.PlaceCat(wrong(2))).state
        val id = lost.currentDefeatId!!
        val recovered = session.reduce(lost, GameAction.ApplyRecovery(id)).state
        val again = session.reduce(recovered, GameAction.ApplyRecovery(id))
        assertEquals(recovered, again.state)
        assertEquals(GameEvent.Ignored(IgnoreReason.RECOVERY_ALREADY_APPLIED), again.events.single())
    }

    @Test fun `limite de recuperacoes por partida`() {
        val lost1 = session.play(fresh(), GameAction.PlaceCat(wrong(0)), GameAction.PlaceCat(wrong(1)), GameAction.PlaceCat(wrong(2))).state
        val recovered = session.reduce(lost1, GameAction.ApplyRecovery(lost1.currentDefeatId!!)).state
        val lost2 = session.reduce(recovered, GameAction.PlaceCat(wrong(0))).state
        assertEquals(GameStatus.LOST, lost2.status)
        assertEquals(2, lost2.defeats)
        assertFalse(session.canOfferRecovery(lost2))
        val out = session.reduce(lost2, GameAction.ApplyRecovery(lost2.currentDefeatId!!))
        assertEquals(GameEvent.Ignored(IgnoreReason.RECOVERY_LIMIT_REACHED), out.events.single())
    }

    @Test fun `parametros de recuperacao sao configuraveis`() {
        val generous = GameSession(puzzle5, GameModeConfig(GameModeId.CLASSIC, recovery = RecoveryPolicy(maxRecoveriesPerGame = 2, livesRestored = 2)))
        var state = generous.newGame("x")
        repeat(2) {
            state = generous.play(state, *Array(state.lives) { GameAction.PlaceCat(wrong(it % 3)) }).state
            assertEquals(GameStatus.LOST, state.status)
            state = generous.reduce(state, GameAction.ApplyRecovery(state.currentDefeatId!!)).state
            assertEquals(2, state.lives)
        }
        val disabled = GameSession(puzzle5, GameModeConfig(GameModeId.CLASSIC, recovery = RecoveryPolicy(enabled = false)))
        val lost = disabled.play(disabled.newGame("d"), GameAction.PlaceCat(wrong(0)), GameAction.PlaceCat(wrong(1)), GameAction.PlaceCat(wrong(2))).state
        assertFalse(disabled.canOfferRecovery(lost))
    }

    @Test fun `dicas gratis e por anuncio`() {
        var state = fresh()
        repeat(3) { state = session.reduce(state, GameAction.ConsumeHint(paidWithRewardedAd = false)).state }
        assertEquals(0, state.freeHintsLeft)
        assertEquals(GameEvent.Ignored(IgnoreReason.NO_HINTS_LEFT), session.reduce(state, GameAction.ConsumeHint(false)).events.single())
        val paid = session.reduce(state, GameAction.ConsumeHint(paidWithRewardedAd = true)).state
        assertEquals(4, paid.hintsUsed)
    }

    @Test fun `revelar so aceita celulas da solucao`() {
        val out = session.reduce(fresh(), GameAction.RevealCat(p(0, 0)))
        assertEquals(GameEvent.Ignored(IgnoreReason.NOT_IN_SOLUTION), out.events.single())
        val ok = session.reduce(fresh(), GameAction.RevealCat(p(0, 4)))
        assertTrue(p(0, 4) in ok.state.cats)
        assertEquals(3, ok.state.lives)
    }

    @Test fun `estado restaurado incompativel e detectado`() {
        val other = GameSession(puzzle5WithGiven, GameModeConfig.CLASSIC).newGame("x")
        assertFalse(session.isCompatible(other))
        assertTrue(session.isCompatible(fresh()))
    }
}
