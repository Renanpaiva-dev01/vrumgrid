package com.miaugrid.core.solver

import com.miaugrid.core.model.Difficulty
import com.miaugrid.core.model.Position
import com.miaugrid.core.model.PuzzleLayout

/** Grupo do tabuleiro que precisa de exatamente um gato. */
sealed interface BoardUnit {
    data class Row(val index: Int) : BoardUnit
    data class Column(val index: Int) : BoardUnit
    data class Region(val id: Int) : BoardUnit
}

/** Técnicas de dedução, da mais simples para a mais avançada. */
enum class Technique(val level: Int) {
    /** Um gato elimina a própria linha, coluna, região e vizinhança. */
    CAT_ELIMINATION(0),
    /** Um grupo com uma única célula possível. */
    SINGLE_CANDIDATE(0),
    /** Todos os candidatos de um grupo estão dentro de outro grupo. */
    CONFINEMENT(1),
    /** k grupos cujos candidatos cabem em exatamente k linhas/colunas/regiões. */
    GROUP_CONFINEMENT(2),
    /** Um gato nesta célula deixaria algum grupo sem nenhuma possibilidade. */
    CONTRADICTION(2),
}

sealed interface DeductionStep {
    val technique: Technique

    /** Um gato é obrigatório em [position] porque [unit] só tem essa possibilidade. */
    data class Place(val position: Position, val unit: BoardUnit) : DeductionStep {
        override val technique: Technique get() = Technique.SINGLE_CANDIDATE
    }

    /**
     * As [cells] não podem ter gato. [sources] são os grupos (ou a célula, via [cause])
     * que justificam a eliminação; [targets] são os grupos onde ocorre a eliminação.
     */
    data class Eliminate(
        val cells: Set<Position>,
        override val technique: Technique,
        val sources: List<BoardUnit> = emptyList(),
        val targets: List<BoardUnit> = emptyList(),
        val cause: Position? = null,
    ) : DeductionStep
}

/**
 * Estado de raciocínio: gatos confirmados e células ainda possíveis. Usado pelo
 * classificador de dificuldade e pelo sistema de dicas. Só realiza deduções corretas,
 * portanto nunca contradiz a solução de um puzzle de solução única.
 */
class DeductionState private constructor(
    val layout: PuzzleLayout,
    private val candidate: BooleanArray,
    private val catCells: MutableSet<Position>,
) {
    val n: Int get() = layout.size
    val cats: Set<Position> get() = catCells

    fun isCandidate(p: Position): Boolean = candidate[p.row * n + p.col]

    fun candidates(unit: BoardUnit): List<Position> = cellsOf(unit).filter { isCandidate(it) }

    fun hasCat(unit: BoardUnit): Boolean = catCells.any { it in unit }

    fun isSolved(): Boolean = catCells.size == n

    fun copy(): DeductionState = DeductionState(layout, candidate.copyOf(), catCells.toMutableSet())

    fun apply(step: DeductionStep) {
        when (step) {
            is DeductionStep.Place -> placeCat(step.position)
            is DeductionStep.Eliminate -> step.cells.forEach { eliminate(it) }
        }
    }

    fun placeCat(p: Position) {
        catCells += p
        candidate[p.row * n + p.col] = false
    }

    fun eliminate(p: Position) {
        candidate[p.row * n + p.col] = false
    }

    fun cellsOf(unit: BoardUnit): List<Position> = when (unit) {
        is BoardUnit.Row -> List(n) { Position(unit.index, it) }
        is BoardUnit.Column -> List(n) { Position(it, unit.index) }
        is BoardUnit.Region -> layout.regionCells[unit.id]
    }

    fun unitContains(unit: BoardUnit, p: Position): Boolean = unit.contains(p)

    private operator fun BoardUnit.contains(p: Position): Boolean = when (this) {
        is BoardUnit.Row -> p.row == index
        is BoardUnit.Column -> p.col == index
        is BoardUnit.Region -> layout.regionAt(p) == id
    }

    fun allUnits(): List<BoardUnit> =
        List(n) { BoardUnit.Region(it) } + List(n) { BoardUnit.Row(it) } + List(n) { BoardUnit.Column(it) }

    companion object {
        fun initial(layout: PuzzleLayout, cats: Set<Position> = emptySet()): DeductionState {
            val state = DeductionState(layout, BooleanArray(layout.cellCount) { true }, mutableSetOf())
            cats.forEach { state.placeCat(it) }
            return state
        }
    }
}

data class LogicalSolveReport(
    val solved: Boolean,
    val steps: List<DeductionStep>,
    val hardestTechnique: Technique?,
    val cats: Set<Position>,
) {
    val difficulty: Difficulty
        get() = when {
            !solved -> Difficulty.EXPERT
            hardestTechnique == null || hardestTechnique.level == 0 -> Difficulty.EASY
            hardestTechnique.level == 1 -> Difficulty.MEDIUM
            else -> Difficulty.HARD
        }
}

object DeductionEngine {

    private const val MAX_GROUP = 3

    /** Próxima dedução válida a partir do estado, ou null se não houver (ou se for contraditório). */
    fun nextStep(state: DeductionState): DeductionStep? =
        catElimination(state)
            ?: singleCandidate(state)
            ?: confinement(state, 1)
            ?: (2..MAX_GROUP).firstNotNullOfOrNull { confinement(state, it) }
            ?: contradiction(state)

    /** Resolve só com lógica, registrando os passos. */
    fun solveLogically(layout: PuzzleLayout, givens: Set<Position>, maxSteps: Int = 10_000): LogicalSolveReport {
        val state = DeductionState.initial(layout, givens)
        val steps = mutableListOf<DeductionStep>()
        var hardest: Technique? = null
        while (!state.isSolved() && steps.size < maxSteps) {
            if (isContradictory(state)) break
            val step = nextStep(state) ?: break
            steps += step
            if (hardest == null || step.technique.level > hardest.level) hardest = step.technique
            state.apply(step)
        }
        return LogicalSolveReport(state.isSolved(), steps, hardest, state.cats)
    }

    fun isContradictory(state: DeductionState): Boolean =
        state.allUnits().any { !state.hasCat(it) && state.candidates(it).isEmpty() }

    private fun catElimination(state: DeductionState): DeductionStep? {
        val layout = state.layout
        for (cat in state.cats.sortedWith(compareBy({ it.row }, { it.col }))) {
            val region = layout.regionAt(cat)
            val cells = layout.allPositions().filterTo(LinkedHashSet()) { p ->
                state.isCandidate(p) && (p.row == cat.row || p.col == cat.col ||
                    layout.regionAt(p) == region || p.touches(cat))
            }
            if (cells.isNotEmpty()) {
                return DeductionStep.Eliminate(cells, Technique.CAT_ELIMINATION, cause = cat)
            }
        }
        return null
    }

    private fun singleCandidate(state: DeductionState): DeductionStep? {
        for (unit in state.allUnits()) {
            if (state.hasCat(unit)) continue
            val c = state.candidates(unit)
            if (c.size == 1) return DeductionStep.Place(c.single(), unit)
        }
        return null
    }

    /**
     * Confinamento de k grupos: se os candidatos de k grupos "fonte" (regiões, linhas ou
     * colunas) estão todos dentro de exatamente k grupos "alvo" de outro tipo, esses k alvos
     * ficam reservados para as fontes, e os demais candidatos dos alvos são eliminados.
     */
    private fun confinement(state: DeductionState, k: Int): DeductionStep? {
        val n = state.n
        val kinds: List<Pair<(Int) -> BoardUnit, (Position) -> BoardUnit>> = listOf(
            { i: Int -> BoardUnit.Region(i) } to { p: Position -> BoardUnit.Row(p.row) },
            { i: Int -> BoardUnit.Region(i) } to { p: Position -> BoardUnit.Column(p.col) },
            { i: Int -> BoardUnit.Row(i) } to { p: Position -> BoardUnit.Region(state.layout.regionAt(p)) },
            { i: Int -> BoardUnit.Column(i) } to { p: Position -> BoardUnit.Region(state.layout.regionAt(p)) },
            { i: Int -> BoardUnit.Row(i) } to { p: Position -> BoardUnit.Column(p.col) },
            { i: Int -> BoardUnit.Column(i) } to { p: Position -> BoardUnit.Row(p.row) },
        )
        for ((sourceOf, targetOf) in kinds) {
            val open = (0 until n).map(sourceOf).filter { !state.hasCat(it) }
            if (open.size <= k) continue
            for (combo in combinations(open, k)) {
                val cells = combo.flatMap { state.candidates(it) }
                if (cells.isEmpty()) continue
                val targets = cells.map(targetOf).distinct()
                if (targets.size != k) continue
                val sourceCells = cells.toSet()
                val eliminated = targets.flatMap { state.candidates(it) }.filterTo(LinkedHashSet()) { it !in sourceCells }
                if (eliminated.isNotEmpty()) {
                    val technique = if (k == 1) Technique.CONFINEMENT else Technique.GROUP_CONFINEMENT
                    return DeductionStep.Eliminate(eliminated, technique, sources = combo, targets = targets)
                }
            }
        }
        return null
    }

    private fun contradiction(state: DeductionState): DeductionStep? {
        val layout = state.layout
        for (p in layout.allPositions()) {
            if (!state.isCandidate(p)) continue
            val trial = state.copy()
            trial.placeCat(p)
            catElimination(trial)?.let { trial.apply(it) }
            if (isContradictory(trial)) {
                return DeductionStep.Eliminate(setOf(p), Technique.CONTRADICTION, cause = p)
            }
        }
        return null
    }

    private fun <T> combinations(items: List<T>, k: Int): Sequence<List<T>> = sequence {
        val idx = IntArray(k) { it }
        val n = items.size
        if (k > n) return@sequence
        while (true) {
            yield(idx.map { items[it] })
            var i = k - 1
            while (i >= 0 && idx[i] == n - k + i) i--
            if (i < 0) break
            idx[i]++
            for (j in i + 1 until k) idx[j] = idx[j - 1] + 1
        }
    }
}
