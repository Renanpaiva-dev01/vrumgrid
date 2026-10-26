package com.miaugrid.core

import com.miaugrid.core.generator.PuzzleGenerator
import com.miaugrid.core.rules.RulesEngine
import com.miaugrid.core.solver.Solver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratorTest {

    @Test fun `gera puzzles validos e de solucao unica em varios tamanhos`() {
        for (size in 5..9) {
            repeat(3) { i ->
                val puzzle = PuzzleGenerator(size * 100L + i).generate(size, "g$size-$i")
                assertNotNull(puzzle)
                puzzle!!
                assertTrue(RulesEngine.evaluate(puzzle.layout, puzzle.solution.positions()).isSolved)
                assertTrue(Solver.solve(puzzle.layout, puzzle.givens).isUnique)
            }
        }
    }

    @Test fun `mesma semente gera o mesmo puzzle`() {
        assertEquals(PuzzleGenerator(42).generate(7, "a"), PuzzleGenerator(42).generate(7, "a"))
    }
}
