package com.miaugrid.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miaugrid.app.di.AppContainer
import com.miaugrid.core.levels.LevelInfo
import com.miaugrid.core.progress.ProgressRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val continueLevel: LevelInfo? = null,
    val continueIsResume: Boolean = false,
    val completed: Int = 0,
    val total: Int = 0,
    val warnings: List<String> = emptyList(),
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val levels = MutableStateFlow<List<LevelInfo>?>(null)

    init {
        viewModelScope.launch { levels.value = container.levels.await().levels }
    }

    val ui: StateFlow<HomeUiState> = combine(levels, container.playerData.progress, container.playerData.loaded) { list, progress, loaded ->
        if (list == null || !loaded) return@combine HomeUiState()
        val last = progress.lastPlayedLevelId?.let { id -> list.firstOrNull { it.id == id } }
        val resume = last != null && last.id !in progress.completed
        HomeUiState(
            loading = false,
            continueLevel = if (resume) last else ProgressRules.nextToPlay(progress, list),
            continueIsResume = resume,
            completed = list.count { it.id in progress.completed },
            total = list.size,
            warnings = container.playerData.loadWarnings,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
