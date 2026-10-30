package com.miaugrid.app.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miaugrid.app.ui.components.CarIcon
import com.miaugrid.app.ui.components.Confetti
import com.miaugrid.app.ui.components.DrivingRoad
import com.miaugrid.app.ui.components.LocalCarLook
import com.miaugrid.app.ui.components.formatDuration

/** Pausa, confirmação de reinício, vitória, derrota, recuperação e oferta de dica. */
@Composable
fun GameOverlays(
    ui: GameUiState,
    onResume: () -> Unit,
    onRestartRequested: () -> Unit,
    onRestartConfirmed: () -> Unit,
    onOpenSettings: () -> Unit,
    onExit: () -> Unit,
    onNextLevel: () -> Unit,
    onRecover: () -> Unit,
    onWatchAdForHint: () -> Unit,
) {
    val look = LocalCarLook.current
    when (val overlay = ui.overlay) {
        Overlay.None -> Unit
        Overlay.Paused -> OverlayCard("pause_menu") {
            Text("Pit stop", style = MaterialTheme.typography.headlineMedium)
            DrivingRoad(look, height = 56.dp, periodMillis = 4200)
            Button(onClick = onResume, modifier = Modifier.fillMaxWidth().testTag("resume")) { Text("Continuar") }
            OutlinedButton(onClick = onRestartRequested, modifier = Modifier.fillMaxWidth().testTag("pause_restart")) { Text("Reiniciar fase") }
            OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().testTag("pause_settings")) { Text("Configurações") }
            TextButton(onClick = onExit, modifier = Modifier.testTag("pause_exit")) { Text("Sair (o progresso fica salvo)") }
        }
        Overlay.ConfirmRestart -> OverlayCard("confirm_restart") {
            Text("Reiniciar a fase?", style = MaterialTheme.typography.titleLarge)
            Text("Os carros e marcações desta tentativa serão apagados.", textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.CenterHorizontally)) {
                OutlinedButton(onClick = onResume) { Text("Cancelar") }
                Button(onClick = onRestartConfirmed, modifier = Modifier.testTag("confirm_restart_yes")) { Text("Reiniciar") }
            }
        }
        is Overlay.Victory -> OverlayCard("victory", background = { Confetti(overlay, Modifier.fillMaxSize()) }) {
            DrivingRoad(look, height = 64.dp, periodMillis = 1800)
            Text("Todos estacionados!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text("Tempo: ${formatDuration(overlay.info.timeMillis)} · Erros: ${overlay.info.mistakes}")
            if (overlay.info.newRecord) Text("Novo recorde de tempo!", color = MaterialTheme.colorScheme.primary)
            overlay.info.note?.let { Text(it, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium) }
            if (overlay.info.hasNextLevel) {
                Button(onClick = onNextLevel, modifier = Modifier.fillMaxWidth().testTag("next_level")) { Text("Próxima fase") }
            }
            OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().testTag("victory_exit")) { Text("Voltar ao menu") }
        }
        is Overlay.Defeat -> OverlayCard("defeat") {
            CarIcon(look, Modifier.size(72.dp), rotation = -35f)
            Text("Pneus furados!", style = MaterialTheme.typography.headlineMedium)
            Text("O tabuleiro fica como está. Escolha como continuar.", textAlign = TextAlign.Center)
            overlay.notice?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.testTag("defeat_notice")) }
            when {
                ui.canRecover && ui.adsEnabled -> Button(onClick = onRecover, modifier = Modifier.fillMaxWidth().testTag("recover")) {
                    Text("Assistir anúncio: +${ui.config.recovery.livesRestored} pneu")
                }
                ui.canRecover -> Text(
                    "A recuperação por anúncio está indisponível agora (sem conexão ou sem anúncios).",
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                )
                else -> Text("Você já usou a recuperação desta partida.", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onRestartConfirmed, modifier = Modifier.fillMaxWidth().testTag("defeat_restart")) { Text("Reiniciar fase") }
            TextButton(onClick = onExit, modifier = Modifier.testTag("defeat_exit")) { Text("Sair") }
        }
        Overlay.Recovering -> OverlayCard("recovering") {
            Text("Carregando anúncio…", style = MaterialTheme.typography.titleLarge)
            CircularProgressIndicator()
            Text("O pneu novo só é colocado quando o anúncio confirma a recompensa.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
        }
        Overlay.HintAdOffer -> OverlayCard("hint_offer") {
            Text("Sem dicas grátis", style = MaterialTheme.typography.titleLarge)
            Text("Assista a um anúncio curto para liberar uma dica.", textAlign = TextAlign.Center)
            Button(onClick = onWatchAdForHint, modifier = Modifier.fillMaxWidth().testTag("hint_watch_ad")) { Text("Assistir anúncio") }
            TextButton(onClick = onResume) { Text("Agora não") }
        }
        Overlay.HintAdLoading -> OverlayCard("hint_loading") {
            Text("Carregando anúncio…", style = MaterialTheme.typography.titleLarge)
            CircularProgressIndicator()
        }
    }
}
