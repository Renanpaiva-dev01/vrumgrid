package com.miaugrid.app.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miaugrid.app.ui.components.ScreenScaffold
import com.miaugrid.app.ui.components.StatRow
import com.miaugrid.app.ui.components.formatDuration

@Composable
fun StatsScreen(viewModel: StatsViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val s = ui.stats
    ScreenScaffold(title = "Estatísticas", onBack = onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp).testTag("stats")) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    StatRow("Partidas iniciadas", "${s.gamesStarted}")
                    StatRow("Partidas concluídas", "${s.gamesCompleted}")
                    StatRow("Vitórias", "${s.wins}")
                    StatRow("Derrotas", "${s.losses}")
                    StatRow("Total de erros", "${s.totalMistakes}")
                    StatRow("Dicas usadas", "${s.hintsUsed}")
                    StatRow("Recuperações", "${s.recoveriesUsed}")
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    StatRow("Tempo de jogo", formatDuration(s.totalPlayMillis))
                    StatRow("Melhor tempo", s.bestTimeOverall?.let(::formatDuration) ?: "—")
                    s.bestTimeBySize.toSortedMap().forEach { (size, time) -> StatRow("  Melhor em ${size}x$size", formatDuration(time)) }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    StatRow("Dias seguidos jogando", "${s.currentStreakDays}")
                    StatRow("Melhor sequência de dias", "${s.bestStreakDays}")
                    StatRow("Desafios diários concluídos", "${ui.daily.completedDays.size}")
                }
            }
            Text(
                "Estatísticas exclusivamente locais, guardadas apenas neste aparelho. Não há ranking global.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
