package com.miaugrid.core

import com.miaugrid.core.levels.LevelRepository
import com.miaugrid.core.levels.LevelValidator
import com.miaugrid.core.solver.DeductionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelPackTest {

    private val repo = LevelRepository.loadCampaign()

    @Test fun `todas as fases da campanha sao validas e de solucao unica`() {
        val problems = LevelValidator.problems(repo.pack)
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
        assertTrue(repo.levels.size >= 50)
    }

    @Test fun `metadados consistentes`() {
        repo.levels.forEachIndexed { i, level ->
            assertEquals(i + 1, level.number)
            assertEquals(level.size, level.definition.regions.size)
            assertEquals(level.size, level.definition.solution.size)
        }
        assertTrue(repo.contentVersion >= 1)
        assertEquals(repo.levels.size, repo.levels.map { it.id }.toSet().size)
    }

    @Test fun `dificuldade declarada bate com o classificador`() {
        for (level in repo.levels) {
            val puzzle = repo.puzzle(level.id)!!
            val report = DeductionEngine.solveLogically(puzzle.layout, puzzle.givens)
            assertEquals(level.id, level.definition.difficulty, report.difficulty)
            if (report.solved) assertEquals(puzzle.solution.positions(), report.cats)
        }
    }

    @Test fun `tamanhos crescem ao longo da campanha`() {
        val sizes = repo.levels.map { it.size }
        assertEquals(sizes.sorted(), sizes)
    }
}
