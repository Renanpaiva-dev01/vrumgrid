package com.miaugrid.core

import com.miaugrid.core.Fixtures.layout5
import com.miaugrid.core.Fixtures.p
import com.miaugrid.core.Fixtures.solution5
import com.miaugrid.core.model.PuzzleLayout
import com.miaugrid.core.rules.RulesEngine
import com.miaugrid.core.rules.ViolationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesEngineTest {

    private fun check(cats: Set<com.miaugrid.core.model.Position>, at: com.miaugrid.core.model.Position) =
        RulesEngine.checkPlacement(layout5, cats, at)

    @Test fun `linha ocupada e recusada`() {
        val result = check(setOf(p(0, 4)), p(0, 0))
        assertTrue(result.has(ViolationType.ROW_OCCUPIED))
        assertEquals(p(0, 4), result.violations.first { it.type == ViolationType.ROW_OCCUPIED }.conflictWith)
        assertFalse(result.respectsRules)
    }

    @Test fun `coluna ocupada e recusada`() {
        val result = check(setOf(p(0, 4)), p(3, 4))
        assertTrue(result.has(ViolationType.COLUMN_OCCUPIED))
        assertFalse(result.has(ViolationType.ROW_OCCUPIED))
    }

    @Test fun `regiao ocupada e recusada mesmo distante`() {
        // (0,0) e (2,2) são da região A, sem linha, coluna ou vizinhança em comum.
        val result = check(setOf(p(0, 0)), p(2, 2))
        assertTrue(result.has(ViolationType.REGION_OCCUPIED))
        assertFalse(result.has(ViolationType.ROW_OCCUPIED))
        assertFalse(result.has(ViolationType.COLUMN_OCCUPIED))
    }

    @Test fun `adjacencia horizontal`() {
        val result = check(setOf(p(2, 2)), p(2, 3))
        assertTrue(result.has(ViolationType.ADJACENT_HORIZONTAL))
    }

    @Test fun `adjacencia vertical`() {
        val result = check(setOf(p(2, 2)), p(3, 2))
        assertTrue(result.has(ViolationType.ADJACENT_VERTICAL))
    }

    @Test fun `adjacencia diagonal sem outras regras`() {
        // (1,2) região C e (2,3) região B: só a diagonal é violada.
        val result = check(setOf(p(1, 2)), p(2, 3))
        assertEquals(listOf(ViolationType.ADJACENT_DIAGONAL), result.violations.map { it.type })
    }

    @Test fun `distancia de chebyshev igual a 2 e permitida`() {
        val result = check(setOf(p(1, 2)), p(3, 3))
        assertTrue(result.violations.none { it.type.name.startsWith("ADJACENT") })
    }

    @Test fun `gato fixo nao pode ser alterado`() {
        val result = RulesEngine.checkPlacement(layout5, setOf(p(2, 0)), p(2, 0), fixed = setOf(p(2, 0)))
        assertEquals(ViolationType.FIXED_CELL, result.violations.single().type)
    }

    @Test fun `coordenadas fora do tabuleiro`() {
        listOf(p(-1, 0), p(0, -1), p(5, 0), p(0, 5)).forEach {
            assertEquals(ViolationType.OUT_OF_BOUNDS, check(emptySet(), it).violations.single().type)
        }
        assertFalse(layout5.contains(p(5, 5)))
    }

    @Test fun `posicao fora da solucao oficial e sinalizada`() {
        val result = RulesEngine.checkPlacement(layout5, emptySet(), p(0, 0), solution = solution5)
        assertEquals(ViolationType.NOT_IN_SOLUTION, result.violations.single().type)
        assertTrue(result.respectsRules)
        assertFalse(result.isValid)
    }

    @Test fun `solucao correta vence`() {
        val evaluation = RulesEngine.evaluate(layout5, solution5.positions(), solution5)
        assertTrue(evaluation.isSolved)
    }

    @Test fun `solucao incompleta nao vence`() {
        val partial = solution5.positions().drop(1).toSet()
        val evaluation = RulesEngine.evaluate(layout5, partial, solution5)
        assertFalse(evaluation.isSolved)
        assertFalse(evaluation.rowsSatisfied)
    }

    @Test fun `tabuleiro cheio mas invalido nao vence`() {
        // 5 gatos numa diagonal: um por linha e coluna, mas vizinhos e regiões repetidas.
        val diagonal = (0 until 5).map { p(it, it) }.toSet()
        val evaluation = RulesEngine.evaluate(layout5, diagonal)
        assertEquals(5, evaluation.catCount)
        assertTrue(evaluation.rowsSatisfied && evaluation.columnsSatisfied)
        assertFalse(evaluation.noAdjacentCats)
        assertFalse(evaluation.isSolved)
    }

    @Test fun `celulas bloqueadas incluem vizinhanca linha coluna e regiao`() {
        val blocked = RulesEngine.blockedCells(layout5, setOf(p(1, 2)))
        assertTrue(p(1, 0) in blocked)   // linha
        assertTrue(p(4, 2) in blocked)   // coluna
        assertTrue(p(0, 1) in blocked)   // diagonal
        assertFalse(p(1, 2) in blocked)  // o próprio gato
        assertFalse(p(3, 4) in blocked)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `regiao desconexa e rejeitada`() {
        PuzzleLayout.fromRows(listOf("ABAC", "DDDD", "DDDD", "DDDD"))
    }
}
