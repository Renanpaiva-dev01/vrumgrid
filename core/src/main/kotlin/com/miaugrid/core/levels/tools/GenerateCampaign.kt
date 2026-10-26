package com.miaugrid.core.levels.tools

import com.miaugrid.core.generator.PuzzleGenerator
import com.miaugrid.core.levels.LevelDefinition
import com.miaugrid.core.levels.LevelPackDefinition
import com.miaugrid.core.levels.LevelValidator
import com.miaugrid.core.model.Difficulty
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Gera o pacote da campanha de forma reprodutível (sementes fixas) e falha se alguma
 * fase não passar no validador. Uso: `gradle -p core generateLevels`.
 */
private data class Stage(val size: Int, val count: Int, val difficulties: Set<Difficulty>)

private val PLAN = listOf(
    Stage(5, 8, setOf(Difficulty.EASY)),
    Stage(6, 10, setOf(Difficulty.EASY, Difficulty.MEDIUM)),
    Stage(7, 12, setOf(Difficulty.MEDIUM, Difficulty.EASY)),
    Stage(8, 12, setOf(Difficulty.MEDIUM, Difficulty.HARD)),
    Stage(9, 10, setOf(Difficulty.HARD, Difficulty.MEDIUM)),
    Stage(10, 8, setOf(Difficulty.HARD, Difficulty.EXPERT)),
)

const val CAMPAIGN_CONTENT_VERSION = 1

fun main(args: Array<String>) {
    val output = File(args.firstOrNull() ?: "campaign.json")
    val levels = mutableListOf<LevelDefinition>()
    var number = 1
    var seed = 1_000L
    for (stage in PLAN) {
        repeat(stage.count) {
            var puzzle = null as com.miaugrid.core.model.Puzzle?
            while (puzzle == null) {
                val id = "c%03d".format(number)
                puzzle = PuzzleGenerator(seed++).generate(stage.size, id, accept = { it in stage.difficulties }, maxAttempts = 50)
            }
            levels += LevelDefinition.from(puzzle, number)
            println("fase $number: ${stage.size}x${stage.size} ${puzzle.difficulty}")
            number++
        }
    }
    // Dentro de cada tamanho, ordena da mais fácil para a mais difícil.
    val ordered = levels.sortedWith(compareBy({ it.size }, { it.difficulty.order }, { it.number }))
        .mapIndexed { i, def -> def.copy(number = i + 1, id = "c%03d".format(i + 1)) }
    val pack = LevelPackDefinition("campaign", CAMPAIGN_CONTENT_VERSION, ordered)
    val problems = LevelValidator.problems(pack)
    check(problems.isEmpty()) { "Fases inválidas:\n" + problems.joinToString("\n") }
    val json = Json { prettyPrint = true }
    output.writeText(json.encodeToString(LevelPackDefinition.serializer(), pack) + "\n")
    println("${ordered.size} fases gravadas em ${output.absolutePath}")
}
