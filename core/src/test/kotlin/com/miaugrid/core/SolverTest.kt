package com.miaugrid.core

import com.miaugrid.core.Fixtures.p
import com.miaugrid.core.levels.LevelRepository
import com.miaugrid.core.solver.Solver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverTest {

    @Test fun `encontra a solucao unica`() {
        val result = Solver.solve(Fixtures.layout5)
        assertTrue(result.isUnique)
        assertEquals(Fixtures.solution5, result.solutions.single())
        assertTrue(Solver.verifySolution(Fixtures.layout5, emptySet(), Fixtures.solution5))
    }

    @Test fun `detecta puzzle sem solucao`() {
        val result = Solver.solve(Fixtures.impossibleLayout)
        assertFalse(result.isSolvable)
        assertEquals(0, result.count)
    }

    @Test fun `detecta multiplas solucoes e para em duas`() {
        val result = Solver.solve(Fixtures.multiLayout, limit = 2)
        assertEquals(2, result.count)
        assertTrue(result.limitReached)
        assertFalse(result.isUnique)
        assertEquals(2, Solver.solve(Fixtures.multiLayout, limit = 10).count)
    }

    @Test fun `posicoes iniciais invalidas tornam o puzzle impossivel`() {
        val result = Solver.solve(Fixtures.layout5, givens = setOf(p(0, 0), p(0, 1)))
        assertFalse(result.isSolvable)
    }

    @Test fun `gatos iniciais restringem as solucoes`() {
        val result = Solver.solve(Fixtures.multiLayout, givens = setOf(p(0, 1)), limit = 5)
        assertTrue(result.isUnique)
    }

    @Test fun `solucao oficial errada e rejeitada`() {
        val wrong = com.miaugrid.core.model.Solution(intArrayOf(0, 2, 4, 1, 3))
        assertFalse(Solver.verifySolution(Fixtures.layout5, emptySet(), wrong))
    }

    @Test fun `tabuleiro 10x10 resolve rapido`() {
        val repo = LevelRepository.loadCampaign()
        val biggest = repo.levels.last()
        val start = System.nanoTime()
        val result = Solver.solve(repo.puzzle(biggest.id)!!.layout)
        val ms = (System.nanoTime() - start) / 1_000_000
        assertTrue(result.isUnique)
        assertTrue("levou $ms ms", ms < 2_000)
    }
}
