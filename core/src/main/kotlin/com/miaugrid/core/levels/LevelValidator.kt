package com.miaugrid.core.levels

import com.miaugrid.core.rules.RulesEngine
import com.miaugrid.core.solver.Solver

/** Confere se uma fase é jogável: geometria válida, solução oficial correta e única. */
object LevelValidator {

    fun problems(definition: LevelDefinition): List<String> {
        val puzzle = try {
            definition.toPuzzle()
        } catch (e: IllegalArgumentException) {
            return listOf("${definition.id}: ${e.message}")
        }
        val issues = mutableListOf<String>()
        if (puzzle.size != definition.size) issues += "${definition.id}: tamanho declarado diferente do tabuleiro"
        val evaluation = RulesEngine.evaluate(puzzle.layout, puzzle.solution.positions())
        if (!evaluation.isSolved) issues += "${definition.id}: a solução oficial quebra as regras"
        val result = Solver.solve(puzzle.layout, puzzle.givens, limit = 2)
        when {
            !result.isSolvable -> issues += "${definition.id}: sem solução"
            !result.isUnique -> issues += "${definition.id}: múltiplas soluções"
            result.solutions.single() != puzzle.solution -> issues += "${definition.id}: solução oficial difere da encontrada"
        }
        return issues
    }

    fun problems(pack: LevelPackDefinition): List<String> {
        val issues = mutableListOf<String>()
        val duplicated = pack.levels.groupBy { it.id }.filterValues { it.size > 1 }.keys
        duplicated.forEach { issues += "id duplicado: $it" }
        pack.levels.forEach { issues += problems(it) }
        return issues
    }
}
