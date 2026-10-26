package com.miaugrid.core

import com.miaugrid.core.model.Difficulty
import com.miaugrid.core.model.Position
import com.miaugrid.core.model.Puzzle
import com.miaugrid.core.model.PuzzleLayout
import com.miaugrid.core.model.Solution

/** Tabuleiros escritos à mão para os testes (não dependem do gerador). */
object Fixtures {
    /**
     * 5x5 com solução única: (0,4) (1,2) (2,0) (3,3) (4,1).
     *   A A E E E
     *   A A C B E
     *   A A A B B
     *   A D B B B
     *   D D B B B
     */
    val layout5 = PuzzleLayout.fromRows(listOf("AAEEE", "AACBE", "AAABB", "ADBBB", "DDBBB"))
    val solution5 = Solution(intArrayOf(4, 2, 0, 3, 1))
    val puzzle5 = Puzzle("t5", layout5, emptySet(), solution5, Difficulty.EASY)

    /** Mesmo tabuleiro com um gato fixo em (2,0). */
    val puzzle5WithGiven = Puzzle("t5g", layout5, setOf(Position(2, 0)), solution5, Difficulty.EASY)

    /** Cada linha é uma região: duas soluções (2,4,1,3 e 3,1,4,2 em base 1). */
    val multiLayout = PuzzleLayout.fromRows(listOf("AAAA", "BBBB", "CCCC", "DDDD"))

    /** Regiões A e B têm uma célula cada e são vizinhas: impossível. */
    val impossibleLayout = PuzzleLayout.fromRows(listOf("ABCC", "CCCC", "DDDD", "DDDD"))

    fun p(row: Int, col: Int) = Position(row, col)
}
