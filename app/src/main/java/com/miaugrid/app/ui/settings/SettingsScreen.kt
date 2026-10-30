package com.miaugrid.app.ui.settings

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.miaugrid.app.BuildConfig
import com.miaugrid.app.ui.components.ScreenScaffold
import com.miaugrid.app.ui.components.CarIcon
import com.miaugrid.app.ui.components.LocalCarLook
import com.miaugrid.core.settings.CarSpeed
import com.miaugrid.core.settings.ControlMode

private fun CarSpeed.label() = when (this) {
    CarSpeed.CALM -> "passeio"
    CarSpeed.NORMAL -> "normal"
    CarSpeed.TURBO -> "turbo"
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onOpenGarage: () -> Unit = {}) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    ScreenScaffold(title = "Configurações", onBack = onBack) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Section("Som e toque")
            ToggleRow("Efeitos sonoros", "sound", settings.soundEnabled) { v -> viewModel.update { it.copy(soundEnabled = v) } }
            ToggleRow("Vibração", "haptics", settings.hapticsEnabled) { v -> viewModel.update { it.copy(hapticsEnabled = v) } }

            Section("Tabuleiro")
            ToggleRow("Mostrar cronômetro", "timer_toggle", settings.showTimer) { v -> viewModel.update { it.copy(showTimer = v) } }
            ToggleRow("Destacar linha e coluna selecionadas", "highlight", settings.highlightLines) { v -> viewModel.update { it.copy(highlightLines = v) } }
            ToggleRow("Mostrar células bloqueadas (pontos)", "blocked", settings.showBlockedCells) { v -> viewModel.update { it.copy(showBlockedCells = v) } }
            ToggleRow("Modo de cores acessível (padrões e letras)", "colorblind", settings.colorBlindAssist) { v -> viewModel.update { it.copy(colorBlindAssist = v) } }
            ToggleRow("Confirmar antes de reiniciar", "confirm_restart_toggle", settings.confirmRestart) { v -> viewModel.update { it.copy(confirmRestart = v) } }

            Section("Controles")
            ChoiceRow("Toque marca X, toque duplo estaciona o carro", "control_double", settings.controlMode == ControlMode.DOUBLE_TAP) {
                viewModel.update { it.copy(controlMode = ControlMode.DOUBLE_TAP) }
            }
            ChoiceRow("Escolher ferramenta (X ou carro) na barra", "control_tool", settings.controlMode == ControlMode.TOOL_SELECT) {
                viewModel.update { it.copy(controlMode = ControlMode.TOOL_SELECT) }
            }

            Section("Carrinho")
            ToggleRow("Carrinho marca os X sozinho ao estacionar", "auto_mark", settings.autoMarkWithCar) { v -> viewModel.update { it.copy(autoMarkWithCar = v) } }
            if (settings.autoMarkWithCar) {
                CarSpeed.entries.forEach { speed ->
                    ChoiceRow("Velocidade: ${speed.label()}", "speed_${speed.name.lowercase()}", settings.carSpeed == speed) {
                        viewModel.update { it.copy(carSpeed = speed) }
                    }
                }
            }
            OutlinedButton(onClick = onOpenGarage, modifier = Modifier.padding(top = 4.dp).testTag("settings_garage")) {
                CarIcon(LocalCarLook.current, Modifier.size(24.dp), rotation = 90f)
                Text("Abrir garagem", Modifier.padding(start = 8.dp))
            }

            Section("Privacidade e anúncios")
            Text(viewModel.adsDescription, style = MaterialTheme.typography.bodyMedium)
            Text(
                "O jogo não coleta dados pessoais. O SDK de anúncios do Google pode usar o identificador de publicidade do aparelho; veja o README/Política de privacidade.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            if (viewModel.privacyOptionsRequired && activity != null) {
                OutlinedButton(onClick = { viewModel.showPrivacyOptions(activity) }) { Text("Opções de privacidade de anúncios") }
            }

            Section("Dados")
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.testTag("reset_progress")) { Text("Apagar progresso e estatísticas") }
            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text("VrumGrid ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall)
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Apagar progresso?") },
            text = { Text("Fases concluídas, recordes, estatísticas e partidas salvas serão apagados. Não é possível desfazer.") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetProgress(); confirmReset = false }, modifier = Modifier.testTag("reset_confirm")) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
}

@Composable
private fun ToggleRow(label: String, tag: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ChoiceRow(label: String, tag: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 6.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, Modifier.padding(start = 8.dp))
    }
}
