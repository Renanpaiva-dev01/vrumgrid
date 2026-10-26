package com.miaugrid.core.hints

import com.miaugrid.core.game.GameState
import com.miaugrid.core.model.Position
import com.miaugrid.core.model.Puzzle
import com.miaugrid.core.solver.BoardUnit
import com.miaugrid.core.solver.DeductionEngine
import com.miaugrid.core.solver.DeductionState
import com.miaugrid.core.solver.DeductionStep
import com.miaugrid.core.solver.Technique

/** Tipos de dica. Todas derivam do motor de regras e da solução, nunca de sorteio. */
sealed interface Hint {
    /** O jogador marcou X numa célula que precisa de gato. */
    data class MarkedSolutionCell(val cells: Set<Position>) : Hint

    /** Gatos que não fazem parte da solução (só ocorre no modo [com.miaugrid.core.game.ValidationPolicy.RULES_ONLY]). */
    data class WrongCats(val cells: Set<Position>) : Hint

    /** Um grupo só tem uma posição possível. */
    data class ForcedPlacement(val position: Position, val unit: BoardUnit) : Hint

    /** Células comprovadamente impossíveis, com a justificativa da dedução. */
    data class Elimination(
        val cells: Set<Position>,
        val technique: Technique,
        val sources: List<BoardUnit>,
        val targets: List<BoardUnit>,
        val cause: Position?,
    ) : Hint

    /** Nenhuma dedução simples disponível: indica uma posição correta da solução. */
    data class CorrectPosition(val position: Position) : Hint
}

object HintEngine {

    fun findHint(puzzle: Puzzle, state: GameState): Hint? {
        val solution = puzzle.solution
        val markedOnSolution = state.marks.filterTo(LinkedHashSet()) { it in solution }
        if (markedOnSolution.isNotEmpty()) return Hint.MarkedSolutionCell(markedOnSolution)

        val wrongCats = state.cats.filterTo(LinkedHashSet()) { it !in solution }
        if (wrongCats.isNotEmpty()) return Hint.WrongCats(wrongCats)

        if (state.cats.size == puzzle.size) return null

        val deduction = DeductionState.initial(puzzle.layout, state.cats)
        repeat(puzzle.size * puzzle.size * 4) {
            val step = DeductionEngine.nextStep(deduction) ?: return fallback(puzzle, state)
            when (step) {
                is DeductionStep.Place ->
                    if (step.position !in state.cats) return Hint.ForcedPlacement(step.position, step.unit)
                is DeductionStep.Eliminate -> {
                    // Eliminações por um gato já são visíveis no tabuleiro (bloqueio); não viram dica.
                    val news = step.cells.filterTo(LinkedHashSet()) { it !in state.marks }
                    if (step.technique != Technique.CAT_ELIMINATION && news.isNotEmpty()) {
                        return Hint.Elimination(step.cells, step.technique, step.sources, step.targets, step.cause)
                    }
                }
            }
            deduction.apply(step)
        }
        return fallback(puzzle, state)
    }

    /** Posição para uma dica de revelação: a próxima colocação lógica, ou outra da solução. */
    fun revealTarget(puzzle: Puzzle, state: GameState): Position? {
        val hint = findHint(puzzle, state)
        if (hint is Hint.ForcedPlacement) return hint.position
        if (hint is Hint.CorrectPosition) return hint.position
        if (state.cats.any { it !in puzzle.solution }) return null
        return missingSolutionCells(puzzle, state).firstOrNull()
    }

    private fun fallback(puzzle: Puzzle, state: GameState): Hint? =
        missingSolutionCells(puzzle, state).firstOrNull()?.let { Hint.CorrectPosition(it) }

    private fun missingSolutionCells(puzzle: Puzzle, state: GameState): List<Position> =
        puzzle.solution.positions().filter { it !in state.cats }.sortedWith(compareBy({ it.row }, { it.col }))
}
