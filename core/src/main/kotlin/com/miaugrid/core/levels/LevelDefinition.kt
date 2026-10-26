package com.miaugrid.core.levels

import com.miaugrid.core.model.Difficulty
import com.miaugrid.core.model.Position
import com.miaugrid.core.model.Puzzle
import com.miaugrid.core.model.PuzzleLayout
import com.miaugrid.core.model.Solution
import kotlinx.serialization.Serializable

/** Formato de conteúdo (JSON) de uma fase, independente do código da interface. */
@Serializable
data class LevelDefinition(
    val id: String,
    val number: Int,
    val size: Int,
    /** Uma string por linha; cada letra (A, B, C...) é o identificador lógico da região. */
    val regions: List<String>,
    val givens: List<Position> = emptyList(),
    /** Coluna do gato em cada linha. */
    val solution: List<Int>,
    val difficulty: Difficulty,
) {
    fun toPuzzle(): Puzzle = Puzzle(
        id = id,
        layout = PuzzleLayout.fromRows(regions),
        givens = givens.toSet(),
        solution = Solution(solution.toIntArray()),
        difficulty = difficulty,
    )

    companion object {
        fun from(puzzle: Puzzle, number: Int): LevelDefinition = LevelDefinition(
            id = puzzle.id,
            number = number,
            size = puzzle.size,
            regions = puzzle.layout.toRows(),
            givens = puzzle.givens.sortedWith(compareBy({ it.row }, { it.col })),
            solution = puzzle.solution.toList(),
            difficulty = puzzle.difficulty,
        )
    }
}

@Serializable
data class LevelPackDefinition(
    val packId: String,
    val contentVersion: Int,
    val levels: List<LevelDefinition>,
)
