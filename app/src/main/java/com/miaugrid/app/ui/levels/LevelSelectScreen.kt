package com.miaugrid.app.ui.levels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miaugrid.app.ui.components.CarIcon
import com.miaugrid.app.ui.components.LoadingState
import com.miaugrid.app.ui.components.LocalCarLook
import com.miaugrid.app.ui.components.ScreenScaffold
import com.miaugrid.app.ui.components.formatDuration
import com.miaugrid.core.model.Difficulty

fun Difficulty.label(): String = when (this) {
    Difficulty.EASY -> "Fácil"
    Difficulty.MEDIUM -> "Médio"
    Difficulty.HARD -> "Difícil"
    Difficulty.EXPERT -> "Especialista"
}

@Composable
fun LevelSelectScreen(viewModel: LevelSelectViewModel, onBack: () -> Unit, onPlay: (String) -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    ScreenScaffold(title = "Campanha", onBack = onBack) {
        if (ui.loading) {
            LoadingState("Carregando fases…")
            return@ScreenScaffold
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("${ui.completed} de ${ui.total} concluídas", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = { if (ui.total == 0) 0f else ui.completed.toFloat() / ui.total }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 84.dp),
            modifier = Modifier.fillMaxSize().testTag("level_grid"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ui.sections.forEach { (size, items) ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "header-$size") {
                    Text("Cidades ${size}x$size", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                }
                items(items, key = { it.id }) { item -> LevelCard(item, onPlay) }
            }
        }
    }
}

@Composable
private fun LevelCard(item: LevelItem, onPlay: (String) -> Unit) {
    val status = when {
        !item.unlocked -> "bloqueada"
        item.record != null -> "concluída"
        else -> "disponível"
    }
    Card(
        onClick = { onPlay(item.id) },
        enabled = item.unlocked,
        modifier = Modifier
            .aspectRatio(0.85f)
            .testTag("level_${item.number}")
            .semantics { contentDescription = "Fase ${item.number}, ${item.difficulty.label()}, $status" },
        colors = CardDefaults.cardColors(
            containerColor = if (item.record != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            Text("${item.number}", style = MaterialTheme.typography.titleLarge)
            when {
                !item.unlocked -> Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                item.record != null -> CarIcon(LocalCarLook.current, Modifier.size(30.dp), rotation = 90f)
                else -> Text(item.difficulty.label(), style = MaterialTheme.typography.labelSmall)
            }
            Text(item.record?.let { formatDuration(it.bestTimeMillis) } ?: item.difficulty.label(), style = MaterialTheme.typography.labelSmall)
        }
    }
}
