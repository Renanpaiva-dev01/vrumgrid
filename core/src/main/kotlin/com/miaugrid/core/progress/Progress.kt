package com.miaugrid.core.progress

import com.miaugrid.core.levels.LevelInfo
import kotlinx.serialization.Serializable

@Serializable
data class LevelRecord(
    val bestTimeMillis: Long,
    val fewestMistakes: Int,
    val completions: Int = 1,
)

@Serializable
data class ProgressData(
    val completed: Map<String, LevelRecord> = emptyMap(),
    /** Fase da campanha em andamento (para "Continuar"). */
    val lastPlayedLevelId: String? = null,
)

/** Regras de progressão e desbloqueio da campanha. */
object ProgressRules {

    fun isCompleted(progress: ProgressData, levelId: String): Boolean = levelId in progress.completed

    /** A primeira fase está sempre liberada; as seguintes, quando a anterior foi concluída. */
    fun isUnlocked(progress: ProgressData, levels: List<LevelInfo>, index: Int): Boolean {
        if (index !in levels.indices) return false
        return index == 0 || levels[index - 1].id in progress.completed || levels[index].id in progress.completed
    }

    fun recordWin(progress: ProgressData, levelId: String, timeMillis: Long, mistakes: Int): ProgressData {
        val old = progress.completed[levelId]
        val record = if (old == null) {
            LevelRecord(timeMillis, mistakes)
        } else {
            LevelRecord(
                bestTimeMillis = minOf(old.bestTimeMillis, timeMillis),
                fewestMistakes = minOf(old.fewestMistakes, mistakes),
                completions = old.completions + 1,
            )
        }
        return progress.copy(completed = progress.completed + (levelId to record))
    }

    /** Primeira fase liberada e ainda não concluída (ou a última, se tudo foi concluído). */
    fun nextToPlay(progress: ProgressData, levels: List<LevelInfo>): LevelInfo? =
        levels.firstOrNull { it.id !in progress.completed } ?: levels.lastOrNull()

    fun completionRatio(progress: ProgressData, levels: List<LevelInfo>): Float =
        if (levels.isEmpty()) 0f else levels.count { it.id in progress.completed }.toFloat() / levels.size
}
