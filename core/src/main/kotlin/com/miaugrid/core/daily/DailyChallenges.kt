package com.miaugrid.core.daily

import com.miaugrid.core.generator.PuzzleGenerator
import com.miaugrid.core.model.Difficulty
import com.miaugrid.core.model.Puzzle
import kotlinx.serialization.Serializable

@Serializable
data class DailyData(
    val completedDays: Set<Long> = emptySet(),
    val bestTimes: Map<Long, Long> = emptyMap(),
    /** Maior data já vista neste aparelho; detecta relógio voltando no tempo. */
    val maxSeenEpochDay: Long? = null,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
)

data class ClockCheck(val data: DailyData, val trusted: Boolean)

/**
 * Desafios diários gerados offline e de forma determinística a partir da data.
 *
 * Proteção contra mudanças simples no relógio: cada data só conta uma vez e, se o relógio
 * do aparelho voltar para antes da maior data já vista, conclusões não contam para a
 * sequência. Sem servidor não há como impedir que alguém adiante o relógio; isso está
 * documentado no README.
 */
class DailyChallenges(private val sizeForDay: (Long) -> Int = ::defaultSize) {

    fun puzzleFor(epochDay: Long): Puzzle {
        val size = sizeForDay(epochDay)
        var attempt = 0L
        while (true) {
            val seed = epochDay * 7919L + attempt
            PuzzleGenerator(seed).generate(size, idFor(epochDay), accept = { it != Difficulty.EXPERT }, maxAttempts = 30)
                ?.let { return it }
            attempt++
        }
    }

    fun checkClock(data: DailyData, today: Long): ClockCheck {
        val max = data.maxSeenEpochDay
        return if (max == null || today >= max) {
            ClockCheck(data.copy(maxSeenEpochDay = today), trusted = true)
        } else {
            ClockCheck(data, trusted = false)
        }
    }

    /** Registra a conclusão; devolve os dados e se a conclusão foi contabilizada. */
    fun recordCompletion(data: DailyData, day: Long, today: Long, timeMillis: Long): Pair<DailyData, Boolean> {
        val clock = checkClock(data, today)
        if (!clock.trusted || day != today || day in data.completedDays) return clock.data to false
        val streak = if ((day - 1) in data.completedDays) data.currentStreak + 1 else 1
        val best = data.bestTimes[day]
        val updated = clock.data.copy(
            completedDays = data.completedDays + day,
            bestTimes = data.bestTimes + (day to if (best == null) timeMillis else minOf(best, timeMillis)),
            currentStreak = streak,
            bestStreak = maxOf(data.bestStreak, streak),
        )
        return updated to true
    }

    /** Sequência visível hoje (zera se o jogador pulou ontem). */
    fun visibleStreak(data: DailyData, today: Long): Int =
        if (today in data.completedDays || (today - 1) in data.completedDays) data.currentStreak else 0

    companion object {
        fun idFor(epochDay: Long) = "daily-$epochDay"

        /** Tamanho varia com o dia da semana (6x6 a 8x8). 1970-01-01 foi quinta-feira. */
        fun defaultSize(epochDay: Long): Int = when (Math.floorMod(epochDay + 3, 7L).toInt()) {
            0, 1 -> 6
            2, 3, 4 -> 7
            else -> 8
        }
    }
}
