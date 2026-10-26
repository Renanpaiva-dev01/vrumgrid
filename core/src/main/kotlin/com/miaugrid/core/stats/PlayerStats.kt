package com.miaugrid.core.stats

import kotlinx.serialization.Serializable

/**
 * Estatísticas pessoais, guardadas apenas no aparelho (não há servidor nem ranking global).
 * "Concluídas" = partidas que terminaram em vitória ou derrota.
 */
@Serializable
data class PlayerStats(
    val gamesStarted: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val totalMistakes: Int = 0,
    val totalPlayMillis: Long = 0,
    val bestTimeBySize: Map<Int, Long> = emptyMap(),
    val hintsUsed: Int = 0,
    val recoveriesUsed: Int = 0,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val lastPlayedEpochDay: Long? = null,
) {
    val gamesCompleted: Int get() = wins + losses
    val bestTimeOverall: Long? get() = bestTimeBySize.values.minOrNull()
}

object StatsRules {

    /** Conta o início de uma partida e atualiza a sequência de dias jogados. */
    fun onGameStarted(stats: PlayerStats, epochDay: Long): PlayerStats =
        updateStreak(stats, epochDay).copy(gamesStarted = stats.gamesStarted + 1)

    /**
     * Sequência de dias: mesmo dia mantém, dia seguinte soma, intervalo maior reinicia.
     * Se o relógio voltar no tempo, a sequência não muda (evita manipulação simples).
     */
    fun updateStreak(stats: PlayerStats, epochDay: Long): PlayerStats {
        val last = stats.lastPlayedEpochDay
        val streak = when {
            last == null -> 1
            epochDay == last -> stats.currentStreakDays.coerceAtLeast(1)
            epochDay == last + 1 -> stats.currentStreakDays + 1
            epochDay > last + 1 -> 1
            else -> return stats
        }
        return stats.copy(
            currentStreakDays = streak,
            bestStreakDays = maxOf(stats.bestStreakDays, streak),
            lastPlayedEpochDay = epochDay,
        )
    }

    fun onWin(stats: PlayerStats, boardSize: Int, timeMillis: Long): PlayerStats {
        val best = stats.bestTimeBySize[boardSize]
        val bestTimes = if (best == null || timeMillis < best) stats.bestTimeBySize + (boardSize to timeMillis) else stats.bestTimeBySize
        return stats.copy(wins = stats.wins + 1, bestTimeBySize = bestTimes)
    }

    fun onLoss(stats: PlayerStats): PlayerStats = stats.copy(losses = stats.losses + 1)

    fun onMistake(stats: PlayerStats): PlayerStats = stats.copy(totalMistakes = stats.totalMistakes + 1)

    fun onHint(stats: PlayerStats): PlayerStats = stats.copy(hintsUsed = stats.hintsUsed + 1)

    fun onRecovery(stats: PlayerStats): PlayerStats = stats.copy(recoveriesUsed = stats.recoveriesUsed + 1)

    fun addPlayTime(stats: PlayerStats, millis: Long): PlayerStats =
        if (millis <= 0) stats else stats.copy(totalPlayMillis = stats.totalPlayMillis + millis)
}
