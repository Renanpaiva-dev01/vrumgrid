package com.miaugrid.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miaugrid.app.di.AppContainer
import com.miaugrid.core.daily.DailyData
import com.miaugrid.core.stats.PlayerStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(val stats: PlayerStats = PlayerStats(), val daily: DailyData = DailyData())

class StatsViewModel(container: AppContainer) : ViewModel() {
    val ui: StateFlow<StatsUiState> = combine(container.playerData.stats, container.playerData.daily) { s, d -> StatsUiState(s, d) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}
