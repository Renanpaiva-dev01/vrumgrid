package com.miaugrid.core.solver

import com.miaugrid.core.model.Position
import com.miaugrid.core.model.PuzzleLayout
import com.miaugrid.core.model.Solution

/** Resultado de uma busca de soluções. */
data class SolveResult(
    /** Soluções encontradas (no máximo o limite pedido). */
    val solutions: List<Solution>,
    /** Verdadeiro quando a busca parou por atingir o limite (pode haver mais). */
    val limitReached: Boolean,
    /** Nós visitados pela busca; útil como medida de esforço. */
    val nodes: Long,
) {
    val count: Int get() = solutions.size
    val isSolvable: Boolean get() = solutions.isNotEmpty()
    val isUnique: Boolean get() = solutions.size == 1 && !limitReached
}

/**
 * Solucionador por backtracking. A cada passo escolhe a região sem gato com menos
 * células candidatas (heurística MRV), o que mantém tabuleiros de até 12x12 instantâneos.
 *
 * Como existem N regiões e cada gato ocupa linha e coluna exclusivas, colocar um gato
 * por região produz automaticamente um gato por linha e por coluna.
 */
object Solver {

    fun solve(
        layout: PuzzleLayout,
        givens: Set<Position> = emptySet(),
        excluded: Set<Position> = emptySet(),
        limit: Int = 2,
    ): SolveResult {
        require(limit >= 1)
        return Search(layout, excluded, limit).run(givens)
    }

    /** Conta soluções até [limit]; para verificar unicidade use o limite 2. */
    fun countSolutions(layout: PuzzleLayout, givens: Set<Position> = emptySet(), limit: Int = 2): Int =
        solve(layout, givens, limit = limit).count

    fun hasUniqueSolution(layout: PuzzleLayout, givens: Set<Position> = emptySet()): Boolean =
        solve(layout, givens, limit = 2).isUnique

    /** Confere se [solution] é solução válida (e, se pedido, a única) do tabuleiro. */
    fun verifySolution(
        layout: PuzzleLayout,
        givens: Set<Position>,
        solution: Solution,
        requireUnique: Boolean = true,
    ): Boolean {
        if (!givens.all { it in solution }) return false
        val result = solve(layout, givens, limit = 2)
        if (solution !in result.solutions) return false
        return !requireUnique || result.isUnique
    }

    private class Search(val layout: PuzzleLayout, excluded: Set<Position>, val limit: Int) {
        val n = layout.size
        val rowUsed = BooleanArray(n)
        val colUsed = BooleanArray(n)
        val regionUsed = BooleanArray(n)
        val cat = BooleanArray(n * n)
        val excludedCells = BooleanArray(n * n).also { arr -> excluded.forEach { if (layout.contains(it)) arr[it.row * n + it.col] = true } }
        val regionIndices: Array<IntArray> = Array(n) { r ->
            layout.regionCells[r].map { it.row * n + it.col }.toIntArray()
        }
        val solutions = mutableListOf<Solution>()
        var nodes = 0L
        var stop = false

        fun run(givens: Set<Position>): SolveResult {
            for (g in givens) {
                if (!layout.contains(g) || !canPlace(g.row * n + g.col)) {
                    return SolveResult(emptyList(), false, 0)
                }
                place(g.row * n + g.col)
            }
            search(n - givens.size)
            return SolveResult(solutions.toList(), stop, nodes)
        }

        fun canPlace(index: Int): Boolean {
            val r = index / n
            val c = index % n
            if (excludedCells[index] || rowUsed[r] || colUsed[c] || regionUsed[layout.regionAt(r, c)]) return false
            for (dr in -1..1) for (dc in -1..1) {
                val rr = r + dr
                val cc = c + dc
                if ((dr != 0 || dc != 0) && rr in 0 until n && cc in 0 until n && cat[rr * n + cc]) return false
            }
            return true
        }

        fun place(index: Int) {
            val r = index / n
            val c = index % n
            rowUsed[r] = true; colUsed[c] = true; regionUsed[layout.regionAt(r, c)] = true; cat[index] = true
        }

        fun unplace(index: Int) {
            val r = index / n
            val c = index % n
            rowUsed[r] = false; colUsed[c] = false; regionUsed[layout.regionAt(r, c)] = false; cat[index] = false
        }

        fun search(remaining: Int) {
            nodes++
            if (remaining == 0) {
                val cols = IntArray(n)
                for (i in cat.indices) if (cat[i]) cols[i / n] = i % n
                solutions += Solution(cols)
                if (solutions.size >= limit) stop = true
                return
            }
            // MRV: região livre com menos candidatos.
            var bestRegion = -1
            var bestCount = Int.MAX_VALUE
            for (region in 0 until n) {
                if (regionUsed[region]) continue
                var count = 0
                for (idx in regionIndices[region]) if (canPlace(idx)) count++
                if (count == 0) return
                if (count < bestCount) { bestCount = count; bestRegion = region }
            }
            // Poda: toda linha e coluna livre ainda precisa ter algum candidato.
            if (!linesStillCoverable()) return
            for (idx in regionIndices[bestRegion]) {
                if (stop) return
                if (!canPlace(idx)) continue
                place(idx)
                search(remaining - 1)
                unplace(idx)
            }
        }

        fun linesStillCoverable(): Boolean {
            val rowHas = BooleanArray(n)
            val colHas = BooleanArray(n)
            for (i in 0 until n * n) {
                if (!cat[i] && canPlace(i)) { rowHas[i / n] = true; colHas[i % n] = true }
            }
            for (k in 0 until n) {
                if (!rowUsed[k] && !rowHas[k]) return false
                if (!colUsed[k] && !colHas[k]) return false
            }
            return true
        }
    }
}
