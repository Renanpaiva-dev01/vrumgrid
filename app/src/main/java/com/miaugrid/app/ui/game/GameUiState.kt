package com.miaugrid.app.ui.game

import com.miaugrid.core.game.GameModeConfig
import com.miaugrid.core.game.GameState
import com.miaugrid.core.hints.Hint
import com.miaugrid.core.model.Position
import com.miaugrid.core.model.Puzzle
import com.miaugrid.core.settings.UserSettings
import com.miaugrid.core.solver.BoardUnit

/** Ferramenta do toque simples no modo de controle por seleção. */
enum class Tool { MARK, CAT }

sealed interface Overlay {
    data object None : Overlay
    data object Paused : Overlay
    data object ConfirmRestart : Overlay
    data class Victory(val info: VictoryInfo) : Overlay
    /** Tela de derrota; [notice] informa o resultado de uma tentativa de recuperação. */
    data class Defeat(val notice: String? = null) : Overlay
    /** Tela de recuperação: anúncio recompensado sendo carregado/exibido. */
    data object Recovering : Overlay
    data object HintAdOffer : Overlay
    data object HintAdLoading : Overlay
}

data class VictoryInfo(
    val timeMillis: Long,
    val mistakes: Int,
    val newRecord: Boolean,
    val hasNextLevel: Boolean,
    val note: String? = null,
)

/** Colocação recusada: célula, gatos em conflito e mensagem (em texto, não só cor). */
data class Feedback(val id: Long, val position: Position, val conflicts: Set<Position>, val message: String)

data class PlacementPulse(val id: Long, val position: Position)

data class HintDisplay(
    val hint: Hint,
    val text: String,
    val units: List<BoardUnit>,
    val cells: Set<Position>,
    val focus: Position?,
    val actionLabel: String?,
)

data class GameUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val title: String = "",
    val subtitle: String = "",
    val puzzle: Puzzle? = null,
    val config: GameModeConfig = GameModeConfig.CLASSIC,
    val game: GameState? = null,
    val blocked: Set<Position> = emptySet(),
    val selected: Position? = null,
    val tool: Tool = Tool.MARK,
    val settings: UserSettings = UserSettings(),
    val feedback: Feedback? = null,
    val pulse: PlacementPulse? = null,
    val hint: HintDisplay? = null,
    val overlay: Overlay = Overlay.None,
    val message: String? = null,
    val adsEnabled: Boolean = false,
    val canRecover: Boolean = false,
    val recoveriesLeft: Int = 0,
) {
    val interactive: Boolean
        get() = game?.status == com.miaugrid.core.game.GameStatus.PLAYING && overlay == Overlay.None
}
