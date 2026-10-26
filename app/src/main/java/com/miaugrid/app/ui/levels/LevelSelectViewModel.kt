package com.miaugrid.app.ui.levels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miaugrid.app.di.AppContainer
import com.miaugrid.core.levels.LevelInfo
import com.miaugrid.core.model.Difficulty
import com.miaugrid.core.progress.LevelRecord
import com.miaugrid.core.progress.ProgressRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LevelItem(
    val id: String,
    val number: Int,
    val size: Int,
    val difficulty: Difficulty,
    val unlocked: Boolean,
    val record: LevelRecord?,
)

data class LevelSelectUiState(
    val loading: Boolean = true,
    val sections: Map<Int, List<LevelItem>> = emptyMap(),
    val completed: Int = 0,
    val total: Int = 0,
)

class LevelSelectViewModel(container: AppContainer) : ViewModel() {

    private val levels = MutableStateFlow<List<LevelInfo>?>(null)

    init {
        viewModelScope.launch { levels.value = container.levels.await().levels }
    }

    val ui: StateFlow<LevelSelectUiState> = combine(levels, container.playerData.progress) { list, progress ->
        if (list == null) return@combine LevelSelectUiState()
        val items = list.mapIndexed { i, info ->
            LevelItem(
                id = info.id,
                number = info.number,
                size = info.size,
                difficulty = info.definition.difficulty,
                unlocked = ProgressRules.isUnlocked(progress, list, i),
                record = progress.completed[info.id],
            )
        }
        LevelSelectUiState(
            loading = false,
            sections = items.groupBy { it.size },
            completed = items.count { it.record != null },
            total = items.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LevelSelectUiState())
}
