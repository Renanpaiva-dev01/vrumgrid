package com.miaugrid.app.ui.garage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miaugrid.app.di.AppContainer
import com.miaugrid.core.settings.CarStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Garagem: edita o carrinho do jogador. A prévia muda na hora; a gravação é agrupada
 * (arrastar o seletor de cor não grava a cada quadro) e roda no escopo do aplicativo,
 * para não se perder se o jogador sair da tela logo depois de mexer.
 */
class GarageViewModel(private val container: AppContainer) : ViewModel() {

    private val draft = MutableStateFlow<CarStyle?>(null)
    private var saveJob: Job? = null

    val style: StateFlow<CarStyle> = combine(draft, container.playerData.settings) { edited, settings -> edited ?: settings.car }
        .stateIn(viewModelScope, SharingStarted.Eagerly, container.playerData.settings.value.car)

    /** Só permite editar depois que os dados salvos foram lidos (evita sobrescrever o carro salvo). */
    val ready: StateFlow<Boolean> = container.playerData.loaded

    fun edit(transform: (CarStyle) -> CarStyle) {
        if (!ready.value) return
        val next = transform(style.value)
        if (next == style.value) return
        draft.value = next
        saveJob?.cancel()
        saveJob = container.applicationScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            container.playerData.updateSettings { it.copy(car = next) }
        }
    }

    fun reset() = edit { CarStyle() }

    private companion object {
        const val SAVE_DEBOUNCE_MS = 300L
    }
}
