package com.miaugrid.app.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miaugrid.app.di.AppContainer
import com.miaugrid.core.daily.DailyChallenges
import com.miaugrid.core.game.GameModeId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DailyUiState(
    val today: LocalDate = LocalDate.now(),
    val size: Int = 0,
    val completedToday: Boolean = false,
    val bestTimeToday: Long? = null,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val clockTrusted: Boolean = true,
    val hasSavedGame: Boolean = false,
)

class DailyViewModel(private val container: AppContainer) : ViewModel() {

    private val todayDay = LocalDate.now().toEpochDay()
    private val trusted = MutableStateFlow(true)
    private val saved = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            var ok = true
            container.playerData.updateDaily { data ->
                val check = container.dailyChallenges.checkClock(data, todayDay)
                ok = check.trusted
                check.data
            }
            trusted.value = ok
            saved.value = container.playerData.loadSavedGame(GameModeId.DAILY)?.state?.levelId == DailyChallenges.idFor(todayDay)
        }
    }

    val ui: StateFlow<DailyUiState> = combine(container.playerData.daily, trusted, saved) { data, ok, hasSaved ->
        DailyUiState(
            today = LocalDate.ofEpochDay(todayDay),
            size = DailyChallenges.defaultSize(todayDay),
            completedToday = todayDay in data.completedDays,
            bestTimeToday = data.bestTimes[todayDay],
            streak = container.dailyChallenges.visibleStreak(data, todayDay),
            bestStreak = data.bestStreak,
            clockTrusted = ok,
            hasSavedGame = hasSaved,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyUiState())

    val todayEpochDay: Long get() = todayDay
}
