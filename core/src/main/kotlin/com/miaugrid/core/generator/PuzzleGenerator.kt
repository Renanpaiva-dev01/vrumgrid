package com.miaugrid.core.generator

import com.miaugrid.core.model.Difficulty
import com.miaugrid.core.model.Position
import com.miaugrid.core.model.Puzzle
import com.miaugrid.core.model.PuzzleLayout
import com.miaugrid.core.model.Solution
import com.miaugrid.core.solver.DeductionEngine
import com.miaugrid.core.solver.Solver
import kotlin.random.Random

/**
 * Gerador de fases. Cria primeiro uma solução válida, cresce regiões ao redor de cada gato
 * e depois ajusta as fronteiras até que o solucionador confirme solução única. A
 * dificuldade é classificada pelo motor de deduções lógicas.
 *
 * É determinístico para uma mesma semente, o que permite desafios diários offline.
 */
class PuzzleGenerator(private val random: Random) {

    constructor(seed: Long) : this(Random(seed))

    fun generate(
        size: Int,
        id: String,
        accept: (Difficulty) -> Boolean = { true },
        maxAttempts: Int = 400,
    ): Puzzle? {
        repeat(maxAttempts) {
            val solution = randomSolution(size) ?: return@repeat
            val layout = buildUniqueLayout(size, solution) ?: return@repeat
            val report = DeductionEngine.solveLogically(layout, emptySet())
            val difficulty = report.difficulty
            if (!accept(difficulty)) return@repeat
            // Verificação final independente (nunca entregar fase sem solução única).
            if (!Solver.verifySolution(layout, emptySet(), solution, requireUnique = true)) return@repeat
            return Puzzle(id, layout, emptySet(), solution, difficulty)
        }
        return null
    }

    /** Permutação de colunas em que gatos de linhas consecutivas não se tocam. */
    internal fun randomSolution(size: Int): Solution? {
        val cols = IntArray(size) { -1 }
        val used = BooleanArray(size)
        fun fill(row: Int): Boolean {
            if (row == size) return true
            for (c in (0 until size).shuffled(random)) {
                if (used[c]) continue
                if (row > 0 && kotlin.math.abs(cols[row - 1] - c) <= 1) continue
                cols[row] = c; used[c] = true
                if (fill(row + 1)) return true
                used[c] = false
            }
            return false
        }
        return if (fill(0)) Solution(cols) else null
    }

    private fun buildUniqueLayout(size: Int, solution: Solution): PuzzleLayout? {
        val regions = growRegions(size, solution)
        val seeds = solution.positions()
        repeat(size * size * 3) {
            val layout = PuzzleLayout(size, regions)
            val result = Solver.solve(layout, limit = 2)
            if (result.isUnique) return layout
            val other = result.solutions.firstOrNull { it != solution } ?: return null
            val candidates = other.positions().filter { it !in seeds }.shuffled(random)
            val moved = candidates.any { cell -> reassign(regions, size, cell, seeds) }
            if (!moved) return null
        }
        return null
    }

    /** Crescimento aleatório a partir de cada gato da solução. */
    private fun growRegions(size: Int, solution: Solution): IntArray {
        val regions = IntArray(size * size) { -1 }
        val ids = (0 until size).shuffled(random)
        for (row in 0 until size) regions[row * size + solution.columnFor(row)] = ids[row]
        var unassigned = size * size - size
        while (unassigned > 0) {
            val frontier = mutableListOf<Pair<Int, Int>>()
            for (i in regions.indices) {
                if (regions[i] != -1) continue
                for (nb in orthogonal(i, size)) if (regions[nb] != -1) frontier += i to regions[nb]
            }
            val (cell, region) = frontier[random.nextInt(frontier.size)]
            regions[cell] = region
            unassigned--
        }
        return regions
    }

    /** Move [cell] para uma região vizinha sem desconectar a região de origem. */
    private fun reassign(regions: IntArray, size: Int, cell: Position, seeds: Set<Position>): Boolean {
        val index = cell.row * size + cell.col
        val from = regions[index]
        val options = orthogonal(index, size).map { regions[it] }.filter { it != from }.distinct().shuffled(random)
        for (to in options) {
            regions[index] = to
            if (isConnected(regions, size, from) && regionHasSeed(regions, size, from, seeds)) return true
            regions[index] = from
        }
        return false
    }

    private fun regionHasSeed(regions: IntArray, size: Int, region: Int, seeds: Set<Position>): Boolean =
        seeds.any { regions[it.row * size + it.col] == region }

    private fun isConnected(regions: IntArray, size: Int, region: Int): Boolean {
        val cells = regions.indices.filter { regions[it] == region }
        if (cells.isEmpty()) return false
        val seen = HashSet<Int>()
        val stack = ArrayDeque(listOf(cells.first()))
        while (stack.isNotEmpty()) {
            val c = stack.removeLast()
            if (!seen.add(c)) continue
            for (nb in orthogonal(c, size)) if (regions[nb] == region && nb !in seen) stack.add(nb)
        }
        return seen.size == cells.size
    }

    private fun orthogonal(index: Int, size: Int): List<Int> {
        val r = index / size
        val c = index % size
        return buildList {
            if (r > 0) add(index - size)
            if (r < size - 1) add(index + size)
            if (c > 0) add(index - 1)
            if (c < size - 1) add(index + 1)
        }
    }
}
