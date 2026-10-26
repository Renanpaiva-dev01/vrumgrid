package com.miaugrid.core.game

import com.miaugrid.core.model.Position
import kotlinx.serialization.Serializable

@Serializable
enum class GameStatus { PLAYING, WON, LOST }

/** Jogadas reversíveis registradas no histórico de desfazer. */
@Serializable
sealed interface Move {
    /** Gato colocado; [clearedMark] indica que havia um X na célula. */
    @Serializable
    data class PlaceCat(val position: Position, val clearedMark: Boolean, val revealed: Boolean = false) : Move

    @Serializable
    data class RemoveCat(val position: Position) : Move

    /** Marcações alteradas (um toque ou um arrasto). Guarda o estado anterior de cada célula. */
    @Serializable
    data class SetMarks(val positions: List<Position>, val marked: Boolean, val previous: List<Boolean>) : Move
}

/**
 * Estado serializável de uma partida. A geometria e a solução ficam no [com.miaugrid.core.model.Puzzle];
 * aqui ficam apenas os dados que mudam durante o jogo. Os conjuntos são separados de propósito:
 * gato, marcação X do jogador e gato fixo nunca se misturam. O bloqueio visual confirmado pelo
 * motor é derivado (ver [GameSession.blockedCells]).
 */
@Serializable
data class GameState(
    val levelId: String,
    val mode: GameModeId,
    val size: Int,
    val fixedCats: Set<Position>,
    val cats: Set<Position>,
    val marks: Set<Position> = emptySet(),
    val lives: Int,
    val mistakes: Int = 0,
    val status: GameStatus = GameStatus.PLAYING,
    val history: List<Move> = emptyList(),
    val elapsedMillis: Long = 0,
    val hintsUsed: Int = 0,
    val freeHintsLeft: Int = 0,
    /** Número de derrotas nesta partida; identifica cada evento de derrota. */
    val defeats: Int = 0,
    val recoveriesUsed: Int = 0,
    /** Ids de recompensas já aplicadas (impede concessão dupla). */
    val appliedRecoveryIds: Set<String> = emptySet(),
    /** Identificador da partida, estável entre salvamentos. */
    val sessionId: String,
) {
    val playerCats: Set<Position> get() = cats - fixedCats
    val isFinished: Boolean get() = status != GameStatus.PLAYING

    /** Id do evento de derrota atual, usado para vincular uma recompensa a ele. */
    val currentDefeatId: String? get() = if (status == GameStatus.LOST) "$sessionId#defeat$defeats" else null
}
