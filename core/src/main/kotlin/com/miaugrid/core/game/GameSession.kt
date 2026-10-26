package com.miaugrid.core.game

import com.miaugrid.core.model.Position
import com.miaugrid.core.model.Puzzle
import com.miaugrid.core.rules.PlacementCheck
import com.miaugrid.core.rules.RulesEngine
import com.miaugrid.core.rules.ViolationType

sealed interface GameAction {
    data class PlaceCat(val position: Position) : GameAction
    data class RemoveCat(val position: Position) : GameAction
    data class ToggleMark(val position: Position) : GameAction
    /** Marca ou desmarca várias células de uma vez (gesto de arrastar). */
    data class SetMarks(val positions: List<Position>, val marked: Boolean) : GameAction
    data object Undo : GameAction
    data object Restart : GameAction
    data class Tick(val deltaMillis: Long) : GameAction
    /** Consome uma dica: grátis enquanto houver, ou paga por anúncio já confirmado. */
    data class ConsumeHint(val paidWithRewardedAd: Boolean) : GameAction
    /** Coloca um gato da solução oficial por meio de uma dica de revelação. */
    data class RevealCat(val position: Position) : GameAction
    /** Aplica a recuperação vinculada a [rewardId], depois da confirmação do SDK. */
    data class ApplyRecovery(val rewardId: String) : GameAction
}

enum class IgnoreReason {
    GAME_NOT_ACTIVE, OUT_OF_BOUNDS, FIXED_CELL, CELL_HAS_CAT, NO_CAT, REMOVAL_NOT_ALLOWED,
    NOTHING_TO_UNDO, NOTHING_CHANGED, NOT_IN_SOLUTION, CONFLICT, NO_HINTS_LEFT,
    RECOVERY_NOT_ALLOWED, RECOVERY_ALREADY_APPLIED, RECOVERY_LIMIT_REACHED, RECOVERY_WRONG_EVENT,
}

sealed interface GameEvent {
    data class CatPlaced(val position: Position, val revealed: Boolean) : GameEvent
    /** Colocação recusada. [lifeLost] diz se houve penalidade. */
    data class CatRejected(val check: PlacementCheck, val lifeLost: Boolean, val livesLeft: Int) : GameEvent
    data class CatRemoved(val position: Position) : GameEvent
    data class MarksChanged(val positions: List<Position>, val marked: Boolean) : GameEvent
    data class Undone(val move: Move) : GameEvent
    data object Restarted : GameEvent
    data object Won : GameEvent
    data class Lost(val defeatId: String) : GameEvent
    data class Recovered(val livesRestored: Int) : GameEvent
    data class HintConsumed(val paidWithRewardedAd: Boolean) : GameEvent
    data class Ignored(val reason: IgnoreReason) : GameEvent
}

data class ActionOutcome(val state: GameState, val events: List<GameEvent>) {
    val changed: Boolean get() = events.none { it is GameEvent.Ignored }
}

/**
 * Regras de uma partida, como função pura `(estado, ação) -> (novo estado, eventos)`.
 * Não depende de Android, o que permite testar vidas, derrota, desfazer, reinício e
 * recuperação sem interface.
 *
 * Políticas centrais (válidas para todos os modos atuais):
 * - Marcações X nunca custam vida e nunca são tratadas como "impossível" pelo motor.
 * - Uma colocação recusada não entra no tabuleiro nem no histórico; por isso **desfazer
 *   nunca devolve vidas**.
 * - A derrota acontece uma única vez por evento (vidas chegam a zero) e bloqueia jogadas
 *   até recuperação ou reinício.
 */
class GameSession(val puzzle: Puzzle, val config: GameModeConfig) {

    private val layout = puzzle.layout

    fun newGame(sessionId: String): GameState = GameState(
        levelId = puzzle.id,
        mode = config.id,
        size = puzzle.size,
        fixedCats = puzzle.givens,
        cats = puzzle.givens,
        lives = config.initialLives,
        freeHintsLeft = config.hints.freeHintsPerLevel,
        sessionId = sessionId,
    )

    /** Confere se um estado restaurado é compatível com esta fase. */
    fun isCompatible(state: GameState): Boolean =
        state.levelId == puzzle.id && state.size == puzzle.size && state.fixedCats == puzzle.givens &&
            state.cats.containsAll(puzzle.givens) && state.cats.all { layout.contains(it) } &&
            state.marks.all { layout.contains(it) && it !in state.cats } &&
            state.lives in 0..maxOf(config.initialLives, config.recovery.livesRestored)

    fun blockedCells(state: GameState): Set<Position> = RulesEngine.blockedCells(layout, state.cats)

    fun checkPlacement(state: GameState, position: Position): PlacementCheck =
        RulesEngine.checkPlacement(layout, state.cats, position, state.fixedCats, puzzle.solution)

    fun reduce(state: GameState, action: GameAction): ActionOutcome = when (action) {
        is GameAction.PlaceCat -> placeCat(state, action.position)
        is GameAction.RemoveCat -> removeCat(state, action.position)
        is GameAction.ToggleMark -> toggleMark(state, action.position)
        is GameAction.SetMarks -> setMarks(state, action.positions, action.marked)
        GameAction.Undo -> undo(state)
        GameAction.Restart -> restart(state)
        is GameAction.Tick -> tick(state, action.deltaMillis)
        is GameAction.ConsumeHint -> consumeHint(state, action.paidWithRewardedAd)
        is GameAction.RevealCat -> reveal(state, action.position)
        is GameAction.ApplyRecovery -> applyRecovery(state, action.rewardId)
    }

    private fun ignored(state: GameState, reason: IgnoreReason) =
        ActionOutcome(state, listOf(GameEvent.Ignored(reason)))

    private fun placeCat(state: GameState, position: Position): ActionOutcome {
        if (state.status != GameStatus.PLAYING) return ignored(state, IgnoreReason.GAME_NOT_ACTIVE)
        val check = checkPlacement(state, position)
        when {
            check.has(ViolationType.OUT_OF_BOUNDS) -> return ignored(state, IgnoreReason.OUT_OF_BOUNDS)
            check.has(ViolationType.FIXED_CELL) -> return ignored(state, IgnoreReason.FIXED_CELL)
            check.has(ViolationType.CELL_OCCUPIED) -> return ignored(state, IgnoreReason.CELL_HAS_CAT)
        }
        val penalized = when (config.validation) {
            ValidationPolicy.STRICT_SOLUTION -> !check.isValid
            ValidationPolicy.RULES_ONLY -> !check.respectsRules
        }
        if (penalized) return penalize(state, check)
        val hadMark = position in state.marks
        val placed = state.copy(
            cats = state.cats + position,
            marks = state.marks - position,
            history = state.history + Move.PlaceCat(position, hadMark),
        )
        return withWinCheck(placed, GameEvent.CatPlaced(position, revealed = false))
    }

    private fun penalize(state: GameState, check: PlacementCheck): ActionOutcome {
        val lives = (state.lives - 1).coerceAtLeast(0)
        var next = state.copy(lives = lives, mistakes = state.mistakes + 1)
        val events = mutableListOf<GameEvent>(GameEvent.CatRejected(check, lifeLost = true, livesLeft = lives))
        if (lives == 0) {
            next = next.copy(status = GameStatus.LOST, defeats = next.defeats + 1)
            events += GameEvent.Lost(next.currentDefeatId!!)
        }
        return ActionOutcome(next, events)
    }

    private fun withWinCheck(state: GameState, event: GameEvent): ActionOutcome {
        val evaluation = RulesEngine.evaluate(layout, state.cats, puzzle.solution)
        return if (evaluation.isSolved) {
            ActionOutcome(state.copy(status = GameStatus.WON), listOf(event, GameEvent.Won))
        } else {
            ActionOutcome(state, listOf(event))
        }
    }

    private fun removeCat(state: GameState, position: Position): ActionOutcome {
        if (state.status != GameStatus.PLAYING) return ignored(state, IgnoreReason.GAME_NOT_ACTIVE)
        if (!layout.contains(position)) return ignored(state, IgnoreReason.OUT_OF_BOUNDS)
        if (position in state.fixedCats) return ignored(state, IgnoreReason.FIXED_CELL)
        if (position !in state.cats) return ignored(state, IgnoreReason.NO_CAT)
        if (!config.allowRemovingCats) return ignored(state, IgnoreReason.REMOVAL_NOT_ALLOWED)
        val next = state.copy(cats = state.cats - position, history = state.history + Move.RemoveCat(position))
        return ActionOutcome(next, listOf(GameEvent.CatRemoved(position)))
    }

    private fun toggleMark(state: GameState, position: Position): ActionOutcome =
        setMarks(state, listOf(position), position !in state.marks)

    private fun setMarks(state: GameState, positions: List<Position>, marked: Boolean): ActionOutcome {
        if (state.status != GameStatus.PLAYING) return ignored(state, IgnoreReason.GAME_NOT_ACTIVE)
        if (positions.any { !layout.contains(it) }) return ignored(state, IgnoreReason.OUT_OF_BOUNDS)
        val changed = positions.distinct().filter { it !in state.cats && (it in state.marks) != marked }
        if (changed.isEmpty()) {
            val reason = if (positions.any { it in state.cats }) IgnoreReason.CELL_HAS_CAT else IgnoreReason.NOTHING_CHANGED
            return ignored(state, reason)
        }
        val marks = if (marked) state.marks + changed else state.marks - changed.toSet()
        val move = Move.SetMarks(changed, marked, previous = changed.map { it in state.marks })
        val next = state.copy(marks = marks, history = state.history + move)
        return ActionOutcome(next, listOf(GameEvent.MarksChanged(changed, marked)))
    }

    private fun undo(state: GameState): ActionOutcome {
        if (state.status != GameStatus.PLAYING) return ignored(state, IgnoreReason.GAME_NOT_ACTIVE)
        val move = state.history.lastOrNull() ?: return ignored(state, IgnoreReason.NOTHING_TO_UNDO)
        val history = state.history.dropLast(1)
        val next = when (move) {
            is Move.PlaceCat -> state.copy(
                cats = state.cats - move.position,
                marks = if (move.clearedMark) state.marks + move.position else state.marks,
            )
            is Move.RemoveCat -> state.copy(cats = state.cats + move.position)
            is Move.SetMarks -> {
                var marks = state.marks
                move.positions.forEachIndexed { i, p -> marks = if (move.previous[i]) marks + p else marks - p }
                state.copy(marks = marks)
            }
        }.copy(history = history)
        return ActionOutcome(next, listOf(GameEvent.Undone(move)))
    }

    private fun restart(state: GameState): ActionOutcome {
        val fresh = newGame(state.sessionId)
        val next = fresh.copy(
            lives = if (config.restartRestoresLives) fresh.lives else state.lives.coerceAtLeast(1),
            // Dicas grátis não são recarregadas ao reiniciar, para não virarem dicas infinitas.
            freeHintsLeft = state.freeHintsLeft,
            hintsUsed = state.hintsUsed,
            defeats = state.defeats,
            appliedRecoveryIds = state.appliedRecoveryIds,
        )
        return ActionOutcome(next, listOf(GameEvent.Restarted))
    }

    private fun tick(state: GameState, delta: Long): ActionOutcome {
        if (state.status != GameStatus.PLAYING || delta <= 0) return ignored(state, IgnoreReason.GAME_NOT_ACTIVE)
        return ActionOutcome(state.copy(elapsedMillis = state.elapsedMillis + delta), emptyList())
    }

    private fun consumeHint(state: GameState, paidWithAd: Boolean): ActionOutcome {
        if (state.status != GameStatus.PLAYING) return ignored(state, IgnoreReason.GAME_NOT_ACTIVE)
        val next = when {
            paidWithAd && config.hints.rewardedAdForExtraHints -> state.copy(hintsUsed = state.hintsUsed + 1)
            !paidWithAd && state.freeHintsLeft > 0 ->
                state.copy(hintsUsed = state.hintsUsed + 1, freeHintsLeft = state.freeHintsLeft - 1)
            else -> return ignored(state, IgnoreReason.NO_HINTS_LEFT)
        }
        return ActionOutcome(next, listOf(GameEvent.HintConsumed(paidWithAd)))
    }

    private fun reveal(state: GameState, position: Position): ActionOutcome {
        if (state.status != GameStatus.PLAYING) return ignored(state, IgnoreReason.GAME_NOT_ACTIVE)
        if (position !in puzzle.solution) return ignored(state, IgnoreReason.NOT_IN_SOLUTION)
        if (position in state.cats) return ignored(state, IgnoreReason.CELL_HAS_CAT)
        val check = RulesEngine.checkPlacement(layout, state.cats, position, state.fixedCats)
        if (!check.isValid) return ignored(state, IgnoreReason.CONFLICT)
        val hadMark = position in state.marks
        val next = state.copy(
            cats = state.cats + position,
            marks = state.marks - position,
            history = state.history + Move.PlaceCat(position, hadMark, revealed = true),
        )
        return withWinCheck(next, GameEvent.CatPlaced(position, revealed = true))
    }

    private fun applyRecovery(state: GameState, rewardId: String): ActionOutcome {
        val policy = config.recovery
        return when {
            rewardId in state.appliedRecoveryIds -> ignored(state, IgnoreReason.RECOVERY_ALREADY_APPLIED)
            !policy.enabled || state.status != GameStatus.LOST -> ignored(state, IgnoreReason.RECOVERY_NOT_ALLOWED)
            rewardId != state.currentDefeatId -> ignored(state, IgnoreReason.RECOVERY_WRONG_EVENT)
            state.recoveriesUsed >= policy.maxRecoveriesPerGame -> ignored(state, IgnoreReason.RECOVERY_LIMIT_REACHED)
            else -> ActionOutcome(
                state.copy(
                    status = GameStatus.PLAYING,
                    lives = policy.livesRestored,
                    recoveriesUsed = state.recoveriesUsed + 1,
                    appliedRecoveryIds = state.appliedRecoveryIds + rewardId,
                ),
                listOf(GameEvent.Recovered(policy.livesRestored)),
            )
        }
    }

    /** A recuperação pode ser oferecida neste momento? */
    fun canOfferRecovery(state: GameState): Boolean =
        config.recovery.enabled && state.status == GameStatus.LOST &&
            state.recoveriesUsed < config.recovery.maxRecoveriesPerGame
}
