package com.miaugrid.core

import com.miaugrid.core.daily.DailyChallenges
import com.miaugrid.core.daily.DailyData
import com.miaugrid.core.levels.LevelRepository
import com.miaugrid.core.progress.ProgressData
import com.miaugrid.core.progress.ProgressRules
import com.miaugrid.core.solver.Solver
import com.miaugrid.core.stats.PlayerStats
import com.miaugrid.core.stats.StatsRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressStatsDailyTest {

    private val levels = LevelRepository.loadCampaign().levels

    @Test fun `desbloqueio sequencial`() {
        var progress = ProgressData()
        assertTrue(ProgressRules.isUnlocked(progress, levels, 0))
        assertFalse(ProgressRules.isUnlocked(progress, levels, 1))
        progress = ProgressRules.recordWin(progress, levels[0].id, 50_000, 2)
        assertTrue(ProgressRules.isUnlocked(progress, levels, 1))
        assertFalse(ProgressRules.isUnlocked(progress, levels, 2))
        assertEquals(levels[1], ProgressRules.nextToPlay(progress, levels))
    }

    @Test fun `recordes guardam o melhor resultado`() {
        var progress = ProgressRules.recordWin(ProgressData(), "c001", 50_000, 2)
        progress = ProgressRules.recordWin(progress, "c001", 70_000, 0)
        val record = progress.completed.getValue("c001")
        assertEquals(50_000L, record.bestTimeMillis)
        assertEquals(0, record.fewestMistakes)
        assertEquals(2, record.completions)
    }

    @Test fun `sequencia de dias`() {
        var stats = StatsRules.onGameStarted(PlayerStats(), 100)
        stats = StatsRules.onGameStarted(stats, 100)
        stats = StatsRules.onGameStarted(stats, 101)
        assertEquals(2, stats.currentStreakDays)
        assertEquals(3, stats.gamesStarted)
        stats = StatsRules.onGameStarted(stats, 99) // relógio voltou: não muda
        assertEquals(2, stats.currentStreakDays)
        assertEquals(101L, stats.lastPlayedEpochDay)
        stats = StatsRules.onGameStarted(stats, 105)
        assertEquals(1, stats.currentStreakDays)
        assertEquals(2, stats.bestStreakDays)
    }

    @Test fun `estatisticas de vitoria e derrota`() {
        var stats = StatsRules.onWin(PlayerStats(), 5, 40_000)
        stats = StatsRules.onWin(stats, 5, 30_000)
        stats = StatsRules.onLoss(StatsRules.onMistake(stats))
        assertEquals(2, stats.wins)
        assertEquals(1, stats.losses)
        assertEquals(3, stats.gamesCompleted)
        assertEquals(30_000L, stats.bestTimeBySize[5])
        assertEquals(1, stats.totalMistakes)
    }

    @Test fun `desafio diario e deterministico e valido`() {
        val daily = DailyChallenges()
        val a = daily.puzzleFor(20_000)
        val b = daily.puzzleFor(20_000)
        assertEquals(a, b)
        assertTrue(Solver.verifySolution(a.layout, a.givens, a.solution))
        assertTrue(a.layout != daily.puzzleFor(20_001).layout)
    }

    @Test fun `desafio diario conta uma vez por data`() {
        val daily = DailyChallenges()
        val (first, counted) = daily.recordCompletion(DailyData(), 200, 200, 60_000)
        assertTrue(counted)
        val (_, again) = daily.recordCompletion(first, 200, 200, 30_000)
        assertFalse(again)
    }

    @Test fun `relogio voltando no tempo nao permite reivindicar`() {
        val daily = DailyChallenges()
        val (data, _) = daily.recordCompletion(DailyData(), 200, 200, 60_000)
        // Usuário volta o relógio para o dia 150 e tenta concluir o desafio daquele dia.
        val (_, counted) = daily.recordCompletion(data, 150, 150, 10_000)
        assertFalse(counted)
        assertFalse(daily.checkClock(data, 150).trusted)
        assertTrue(daily.checkClock(data, 201).trusted)
    }

    @Test fun `sequencia diaria`() {
        val daily = DailyChallenges()
        var data = DailyData()
        for (d in 300L..302L) data = daily.recordCompletion(data, d, d, 1_000).first
        assertEquals(3, data.currentStreak)
        assertEquals(3, daily.visibleStreak(data, 303))
        assertEquals(0, daily.visibleStreak(data, 305))
    }
}
