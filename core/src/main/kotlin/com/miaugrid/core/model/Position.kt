package com.miaugrid.core.model

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max

/** Coordenada de uma célula do tabuleiro (linha e coluna começam em zero). */
@Serializable
data class Position(val row: Int, val col: Int) {

    /** Distância de Chebyshev: `max(|r1 - r2|, |c1 - c2|)`. */
    fun chebyshevDistanceTo(other: Position): Int =
        max(abs(row - other.row), abs(col - other.col))

    /** Verdadeiro quando as células são distintas e se tocam, inclusive na diagonal. */
    fun touches(other: Position): Boolean = this != other && chebyshevDistanceTo(other) <= 1

    override fun toString(): String = "($row,$col)"
}
