package com.miaugrid.core.rules

import com.miaugrid.core.model.Position
import com.miaugrid.core.model.PuzzleLayout
import com.miaugrid.core.model.Solution

enum class ViolationType {
    OUT_OF_BOUNDS,
    FIXED_CELL,
    CELL_OCCUPIED,
    ROW_OCCUPIED,
    COLUMN_OCCUPIED,
    REGION_OCCUPIED,
    ADJACENT_HORIZONTAL,
    ADJACENT_VERTICAL,
    ADJACENT_DIAGONAL,
    NOT_IN_SOLUTION,
}

/** Uma regra quebrada, com a célula que causou o conflito (quando existe). */
data class Violation(val type: ViolationType, val conflictWith: Position? = null)

/** Resultado estruturado da validação de uma colocação de gato. */
data class PlacementCheck(val position: Position, val violations: List<Violation>) {

    val isValid: Boolean get() = violations.isEmpty()

    /** Respeita as regras locais (linha, coluna, região, vizinhança), mesmo fora da solução. */
    val respectsRules: Boolean
        get() = violations.none { it.type != ViolationType.NOT_IN_SOLUTION }

    val conflictingCells: Set<Position> get() = violations.mapNotNull { it.conflictWith }.toSet()

    fun has(type: ViolationType): Boolean = violations.any { it.type == type }
}

/** Avaliação completa de um tabuleiro, usada para decidir a vitória. */
data class BoardEvaluation(
    val catCount: Int,
    val expectedCats: Int,
    val rowsSatisfied: Boolean,
    val columnsSatisfied: Boolean,
    val regionsSatisfied: Boolean,
    val noAdjacentCats: Boolean,
    val matchesSolution: Boolean?,
) {
    val isSolved: Boolean
        get() = catCount == expectedCats && rowsSatisfied && columnsSatisfied &&
            regionsSatisfied && noAdjacentCats && matchesSolution != false
}

/**
 * Motor de regras central. Toda verificação de jogada passa por aqui; a interface
 * apenas apresenta os resultados.
 */
object RulesEngine {

    /**
     * Valida a colocação de um gato em [position] considerando os gatos já presentes.
     * Quando [solution] é informada, também indica se a posição pertence à solução oficial.
     */
    fun checkPlacement(
        layout: PuzzleLayout,
        cats: Set<Position>,
        position: Position,
        fixed: Set<Position> = emptySet(),
        solution: Solution? = null,
    ): PlacementCheck {
        if (!layout.contains(position)) {
            return PlacementCheck(position, listOf(Violation(ViolationType.OUT_OF_BOUNDS)))
        }
        if (position in fixed) {
            return PlacementCheck(position, listOf(Violation(ViolationType.FIXED_CELL, position)))
        }
        if (position in cats) {
            return PlacementCheck(position, listOf(Violation(ViolationType.CELL_OCCUPIED, position)))
        }
        val violations = mutableListOf<Violation>()
        val region = layout.regionAt(position)
        for (cat in cats.sortedWith(compareBy({ it.row }, { it.col }))) {
            if (cat.row == position.row) violations += Violation(ViolationType.ROW_OCCUPIED, cat)
            if (cat.col == position.col) violations += Violation(ViolationType.COLUMN_OCCUPIED, cat)
            if (layout.contains(cat) && layout.regionAt(cat) == region) {
                violations += Violation(ViolationType.REGION_OCCUPIED, cat)
            }
            adjacencyType(position, cat)?.let { violations += Violation(it, cat) }
        }
        if (solution != null && position !in solution) {
            violations += Violation(ViolationType.NOT_IN_SOLUTION)
        }
        return PlacementCheck(position, violations)
    }

    /** Tipo de adjacência entre duas células distintas, ou null se não se tocam. */
    fun adjacencyType(a: Position, b: Position): ViolationType? {
        if (!a.touches(b)) return null
        return when {
            a.row == b.row -> ViolationType.ADJACENT_HORIZONTAL
            a.col == b.col -> ViolationType.ADJACENT_VERTICAL
            else -> ViolationType.ADJACENT_DIAGONAL
        }
    }

    /** Verifica todas as condições de vitória. Um tabuleiro "cheio" não basta. */
    fun evaluate(layout: PuzzleLayout, cats: Set<Position>, solution: Solution? = null): BoardEvaluation {
        val inside = cats.filter { layout.contains(it) }
        val rows = inside.groupingBy { it.row }.eachCount()
        val cols = inside.groupingBy { it.col }.eachCount()
        val regions = inside.groupingBy { layout.regionAt(it) }.eachCount()
        val n = layout.size
        val list = inside.toList()
        var noAdjacent = true
        loop@ for (i in list.indices) for (j in i + 1 until list.size) {
            if (list[i].touches(list[j])) { noAdjacent = false; break@loop }
        }
        return BoardEvaluation(
            catCount = cats.size,
            expectedCats = n,
            rowsSatisfied = inside.size == cats.size && rows.size == n && rows.values.all { it == 1 },
            columnsSatisfied = cols.size == n && cols.values.all { it == 1 },
            regionsSatisfied = regions.size == n && regions.values.all { it == 1 },
            noAdjacentCats = noAdjacent,
            matchesSolution = solution?.let { it.positions() == cats },
        )
    }

    /** Pares de gatos que quebram alguma regra entre si. */
    fun conflictingCats(layout: PuzzleLayout, cats: Set<Position>): Set<Position> {
        val result = HashSet<Position>()
        val list = cats.filter { layout.contains(it) }
        for (i in list.indices) for (j in i + 1 until list.size) {
            val a = list[i]
            val b = list[j]
            if (a.row == b.row || a.col == b.col || layout.regionAt(a) == layout.regionAt(b) || a.touches(b)) {
                result += a
                result += b
            }
        }
        return result
    }

    /**
     * Células vazias onde um gato é comprovadamente impossível por causa dos gatos já
     * colocados (mesma linha, coluna, região ou vizinhança). É o "bloqueio" confirmado pelo
     * motor, diferente das marcações X do jogador.
     */
    fun blockedCells(layout: PuzzleLayout, cats: Set<Position>): Set<Position> {
        if (cats.isEmpty()) return emptySet()
        val rows = cats.map { it.row }.toSet()
        val cols = cats.map { it.col }.toSet()
        val regions = cats.filter { layout.contains(it) }.map { layout.regionAt(it) }.toSet()
        return layout.allPositions().filterTo(HashSet()) { p ->
            p !in cats && (p.row in rows || p.col in cols || layout.regionAt(p) in regions ||
                cats.any { it.touches(p) })
        }
    }
}
