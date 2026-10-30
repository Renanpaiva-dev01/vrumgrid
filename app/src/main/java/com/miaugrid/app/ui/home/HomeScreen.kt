package com.miaugrid.app.ui.home

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miaugrid.app.ui.components.CarIcon
import com.miaugrid.app.ui.components.DrivingRoad
import com.miaugrid.app.ui.components.LocalCarLook
import com.miaugrid.app.ui.components.pressScale
import com.miaugrid.app.ui.theme.Road

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlayLevel: (String) -> Unit,
    onOpenLevels: () -> Unit,
    onOpenDaily: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGarage: () -> Unit = {},
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val look = LocalCarLook.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 480.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "VrumGrid",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("home_title"),
                )
                Text(
                    "Estacione um carro em cada linha, coluna e bairro. Carros não podem se encostar, nem na diagonal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                DrivingRoad(look, height = 64.dp)
                Spacer(Modifier.height(16.dp))

                Staggered(0) {
                    val level = ui.continueLevel
                    PlayButton(
                        label = when {
                            level == null -> "Carregando…"
                            ui.continueIsResume -> "Continuar fase ${level.number}"
                            else -> "Jogar fase ${level.number}"
                        },
                        enabled = level != null,
                        onClick = { level?.let { onPlayLevel(it.id) } },
                    )
                }
                if (ui.total > 0) {
                    Spacer(Modifier.height(10.dp))
                    MileageBar(ui.completed, ui.total)
                }
                Spacer(Modifier.height(16.dp))

                Staggered(1) {
                    GarageCard(onOpenGarage)
                }
                Spacer(Modifier.height(12.dp))
                Staggered(2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MenuTile("Fases", Icons.Filled.GridView, Road.SignBlue, "open_levels", Modifier.weight(1f), onOpenLevels)
                        MenuTile("Desafio diário", Icons.Filled.DateRange, Road.RacingOrange, "open_daily", Modifier.weight(1f), onOpenDaily)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Staggered(3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MenuTile("Estatísticas", Icons.Filled.BarChart, Road.SignGreen, "open_stats", Modifier.weight(1f), onOpenStats)
                        MenuTile("Configurações", Icons.Filled.Settings, Road.Asphalt, "open_settings", Modifier.weight(1f), onOpenSettings)
                    }
                }

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
                        Text("Em obras", style = MaterialTheme.typography.titleMedium)
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

/** Entrada em cascata: cada bloco sobe e aparece um pouco depois do anterior. */
@Composable
private fun Staggered(index: Int, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(300, delayMillis = 80 * index)) + slideInVertically(tween(380, delayMillis = 80 * index)) { it / 3 },
    ) { content() }
}

@Composable
private fun PlayButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .pressScale(interaction)
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.horizontalGradient(listOf(Road.RacingOrange, Color(0xFFE0461F))))
            .clickable(enabled = enabled, interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.6f)
            .padding(horizontal = 20.dp)
            .testTag("play"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        Spacer(Modifier.size(8.dp))
        Text(label, style = MaterialTheme.typography.titleLarge, color = Color.White)
    }
}

/** Fases concluídas como um hodômetro: barra de asfalto com a faixa amarela crescendo. */
@Composable
private fun MileageBar(completed: Int, total: Int) {
    val fraction by animateFloatAsState(completed.toFloat() / total, spring(stiffness = 60f), label = "mileage")
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Road.Asphalt)) {
            Box(Modifier.fillMaxWidth(fraction).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Road.TaxiYellow))
        }
        Text("$completed de $total fases concluídas", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun GarageCard(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clip(RoundedCornerShape(20.dp))
            .background(Road.Asphalt)
            .border(2.dp, Road.TaxiYellow, RoundedCornerShape(20.dp))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(14.dp)
            .testTag("open_garage"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CarIcon(LocalCarLook.current, Modifier.size(64.dp), rotation = 90f)
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Garagem", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text("Troque modelo, cor, adesivo e número do seu carrinho", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun MenuTile(label: String, icon: ImageVector, color: Color, tag: String, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .height(96.dp)
            .pressScale(interaction)
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(14.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}
