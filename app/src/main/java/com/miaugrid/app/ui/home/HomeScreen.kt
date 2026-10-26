package com.miaugrid.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miaugrid.app.ui.components.CatIcon

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlayLevel: (String) -> Unit,
    onOpenLevels: () -> Unit,
    onOpenDaily: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 480.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy((-14).dp), verticalAlignment = Alignment.Bottom) {
                    CatIcon(Modifier.size(70.dp), tilt = -10f)
                    CatIcon(Modifier.size(96.dp))
                    CatIcon(Modifier.size(70.dp), fixed = true, tilt = 10f)
                }
                Text("MiauGrid", style = MaterialTheme.typography.displaySmall, modifier = Modifier.testTag("home_title"))
                Text(
                    "Coloque um gato em cada linha, coluna e região colorida. Gatos não podem se tocar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Spacer(Modifier.height(16.dp))

                val level = ui.continueLevel
                Button(
                    onClick = { level?.let { onPlayLevel(it.id) } },
                    enabled = level != null,
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("play"),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        when {
                            level == null -> "Carregando…"
                            ui.continueIsResume -> "Continuar fase ${level.number}"
                            else -> "Jogar fase ${level.number}"
                        },
                    )
                }
                if (ui.total > 0) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { ui.completed.toFloat() / ui.total }, modifier = Modifier.fillMaxWidth())
                    Text("${ui.completed} de ${ui.total} fases concluídas", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(16.dp))
                MenuButton("Campanha: escolher fase", Icons.Filled.GridView, "open_levels", onOpenLevels)
                MenuButton("Desafio diário", Icons.Filled.DateRange, "open_daily", onOpenDaily)
                MenuButton("Estatísticas", Icons.Filled.BarChart, "open_stats", onOpenStats)
                MenuButton("Configurações", Icons.Filled.Settings, "open_settings", onOpenSettings)

                ui.warnings.forEach {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(20.dp))
                // Modos planejados: apenas informativos, sem botões que pareçam iniciar partidas.
                Card(
                    Modifier.fillMaxWidth().testTag("planned_modes"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Em desenvolvimento", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Modo infinito, desafios cronometrados e eventos especiais ainda não estão disponíveis.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuButton(label: String, icon: ImageVector, tag: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(52.dp).testTag(tag)) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text(label, modifier = Modifier.weight(1f))
    }
}
