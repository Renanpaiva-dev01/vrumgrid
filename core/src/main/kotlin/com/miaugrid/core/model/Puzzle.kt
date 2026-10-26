package com.miaugrid.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class Difficulty(val order: Int) {
    EASY(0), MEDIUM(1), HARD(2), EXPERT(3)
}

/**
 * Solução oficial: para cada linha, a coluna do gato.
 */
class Solution(columnsByRow: IntArray) {
    private val columns = columnsByRow.copyOf()

    val size: Int get() = columns.size

    fun columnFor(row: Int): Int = columns[row]

    fun positions(): Set<Position> = columns.indices.map { Position(it, columns[it]) }.toSet()

    operator fun contains(position: Position): Boolean =
        position.row in columns.indices && columns[position.row] == position.col

    fun toList(): List<Int> = columns.toList()

    override fun equals(other: Any?): Boolean = other is Solution && other.columns.contentEquals(columns)
    override fun hashCode(): Int = columns.contentHashCode()
    override fun toString(): String = columns.joinToString(prefix = "Solution[", postfix = "]")

    companion object {
        fun fromPositions(size: Int, cats: Collection<Position>): Solution {
            require(cats.size == size) { "Uma solução precisa de exatamente $size gatos" }
            val cols = IntArray(size) { -1 }
            for (p in cats) {
                require(p.row in 0 until size && cols[p.row] == -1) { "Linha repetida ou inválida em $p" }
                cols[p.row] = p.col
            }
            return Solution(cols)
        }
    }
}

/**
 * Uma fase jogável: geometria + gatos fixos iniciais + solução oficial.
 */
data class Puzzle(
    val id: String,
    val layout: PuzzleLayout,
    val givens: Set<Position>,
    val solution: Solution,
    val difficulty: Difficulty,
) {
    val size: Int get() = layout.size

    init {
        require(solution.size == layout.size) { "Solução com tamanho diferente do tabuleiro" }
        require(givens.all { it in solution }) { "Todo gato fixo precisa fazer parte da solução" }
    }
}
