package com.miaugrid.app.ui.daily

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miaugrid.app.ui.components.CatIcon
import com.miaugrid.app.ui.components.ScreenScaffold
import com.miaugrid.app.ui.components.StatRow
import com.miaugrid.app.ui.components.formatDuration
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DailyScreen(viewModel: DailyViewModel, onBack: () -> Unit, onPlay: (Long) -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val date = ui.today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR")))
    ScreenScaffold(title = "Desafio diário", onBack = onBack) {
        CatIcon(Modifier.size(88.dp).align(Alignment.CenterHorizontally), fixed = true)
        Card(Modifier.fillMaxWidth().padding(16.dp)) {
            androidx.compose.foundation.layout.Column(Modifier.padding(16.dp)) {
                Text(date.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleLarge)
                Text("Um tabuleiro novo por dia, gerado no aparelho (funciona offline).", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                StatRow("Tabuleiro", "${ui.size}x${ui.size}")
                StatRow("Status", if (ui.completedToday) "Concluído" else "Pendente")
                ui.bestTimeToday?.let { StatRow("Seu tempo", formatDuration(it)) }
                StatRow("Sequência atual", "${ui.streak} dia(s)")
                StatRow("Melhor sequência", "${ui.bestStreak} dia(s)")
            }
        }
        if (!ui.clockTrusted) {
            Text(
                "A data do aparelho está anterior a uma data já registrada. Conclusões feitas assim não contam para a sequência.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        Button(
            onClick = { onPlay(viewModel.todayEpochDay) },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp).testTag("play_daily"),
        ) {
            Text(
                when {
                    ui.hasSavedGame -> "Continuar desafio de hoje"
                    ui.completedToday -> "Jogar novamente (não altera o recorde do dia)"
                    else -> "Jogar desafio de hoje"
                },
            )
        }
    }
}
