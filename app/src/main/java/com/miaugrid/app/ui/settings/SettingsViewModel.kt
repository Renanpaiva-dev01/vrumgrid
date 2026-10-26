package com.miaugrid.app.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miaugrid.app.BuildConfig
import com.miaugrid.app.di.AppContainer
import com.miaugrid.core.daily.DailyData
import com.miaugrid.core.game.GameModeId
import com.miaugrid.core.progress.ProgressData
import com.miaugrid.core.settings.UserSettings
import com.miaugrid.core.stats.PlayerStats
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<UserSettings> = container.playerData.settings

    val privacyOptionsRequired: Boolean get() = container.consentManager.isPrivacyOptionsRequired

    val adsDescription: String
        get() = when {
            !BuildConfig.ADS_ENABLED -> "Publicidade desativada nesta versão."
            BuildConfig.ADS_ARE_TEST -> "Versão de desenvolvimento: somente anúncios de TESTE do Google."
            else -> "Anúncios opcionais: recuperar vidas, dicas extras e entre fases."
        }

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { container.playerData.updateSettings(transform) }
    }

    fun showPrivacyOptions(activity: Activity) {
        container.consentManager.showPrivacyOptions(activity) {}
    }

    /** Apaga progresso, estatísticas, desafios e partidas salvas (mantém as configurações). */
    fun resetProgress() {
        viewModelScope.launch {
            container.playerData.updateProgress { ProgressData() }
            container.playerData.updateStats { PlayerStats() }
            container.playerData.updateDaily { old -> DailyData(maxSeenEpochDay = old.maxSeenEpochDay) }
            GameModeId.entries.forEach { container.playerData.clearSavedGame(it) }
        }
    }
}
