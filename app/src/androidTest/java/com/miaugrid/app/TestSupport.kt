package com.miaugrid.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.miaugrid.core.daily.DailyData
import com.miaugrid.core.game.GameModeId
import com.miaugrid.core.levels.LevelRepository
import com.miaugrid.core.model.Position
import com.miaugrid.core.model.Puzzle
import com.miaugrid.core.progress.ProgressData
import com.miaugrid.core.settings.UserSettings
import com.miaugrid.core.stats.PlayerStats
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Zera os dados do jogador antes de abrir a Activity (o contêiner vive no processo inteiro). */
class ResetPlayerDataRule(private val settings: UserSettings = UserSettings()) : TestWatcher() {
    override fun starting(description: Description) {
        val app = ApplicationProvider.getApplicationContext<MiauGridApp>()
        val data = app.container.playerData
        runBlocking {
            data.load()
            data.updateProgress { ProgressData() }
            data.updateStats { PlayerStats() }
            data.updateDaily { DailyData() }
            data.updateSettings { settings }
            GameModeId.entries.forEach { data.clearSavedGame(it) }
        }
    }
}

object Levels {
    val repo: LevelRepository by lazy { LevelRepository.loadCampaign() }
    fun puzzle(number: Int): Puzzle = repo.puzzle(repo.levels[number - 1].id)!!
    fun wrongCells(puzzle: Puzzle): List<Position> =
        puzzle.layout.allPositions().filter { it !in puzzle.solution }
}

private fun TouchInjectionScope.cellCenter(p: Position, n: Int) =
    Offset((p.col + 0.5f) * width / n, (p.row + 0.5f) * height / n)

fun SemanticsNodeInteraction.tapCell(p: Position, n: Int) = performTouchInput { click(cellCenter(p, n)) }

fun SemanticsNodeInteraction.doubleTapCell(p: Position, n: Int) = performTouchInput { doubleClick(cellCenter(p, n)) }
