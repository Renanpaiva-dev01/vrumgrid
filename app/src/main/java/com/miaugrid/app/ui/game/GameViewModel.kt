package com.miaugrid.app.ui.game

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miaugrid.app.audio.Sound
import com.miaugrid.app.di.AppContainer
import com.miaugrid.core.ads.AdPlacement
import com.miaugrid.core.ads.RewardedAdResult
import com.miaugrid.core.game.ActionOutcome
import com.miaugrid.core.game.GameAction
import com.miaugrid.core.game.GameEvent
import com.miaugrid.core.game.GameModeConfig
import com.miaugrid.core.game.GameModeId
import com.miaugrid.core.game.GameSession
import com.miaugrid.core.game.GameState
import com.miaugrid.core.game.GameStatus
import com.miaugrid.core.hints.Hint
import com.miaugrid.core.hints.HintEngine
import com.miaugrid.core.model.Position
import com.miaugrid.core.model.Puzzle
import com.miaugrid.core.persistence.SavedGame
import com.miaugrid.core.progress.ProgressRules
import com.miaugrid.core.recovery.RecoveryResult
import com.miaugrid.core.settings.ControlMode
import com.miaugrid.core.stats.StatsRules
import com.miaugrid.core.telemetry.AnalyticsEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

/** Efeitos de uma só vez para a interface (vibração). */
enum class HapticCue { LIGHT, ERROR }

/**
 * Liga a interface da partida ao núcleo. Não contém regras: toda jogada vira uma
 * [GameAction] reduzida por [GameSession]; aqui ficam efeitos (som, salvamento,
 * estatísticas, anúncios) e o estado de apresentação.
 */
class GameViewModel(
    private val container: AppContainer,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val mode: GameModeId = GameModeId.valueOf(checkNotNull(savedStateHandle.get<String>(ARG_MODE)))
    private val key: String = checkNotNull(savedStateHandle.get<String>(ARG_KEY))
    private val config = GameModeConfig.forId(mode)

    private val _ui = MutableStateFlow(GameUiState(config = config))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private val _haptics = MutableStateFlow<Pair<Long, HapticCue>?>(null)
    val haptics: StateFlow<Pair<Long, HapticCue>?> = _haptics.asStateFlow()

    private var session: GameSession? = null
    private var contentVersion = 0
    private var nextLevelId: String? = null
    private var timerJob: Job? = null
    private var inForeground = true
    private var lastTick = 0L
    private var lastSavedAt = 0L
    private var accountedPlayMillis = 0L
    private var eventCounter = 0L
    private var busy = false // impede transições duplicadas (anúncio, navegação)

    init {
        viewModelScope.launch {
            container.playerData.settings.collect { s -> _ui.update { it.copy(settings = s) } }
        }
        viewModelScope.launch { load() }
    }

    // ---------------------------------------------------------------- carregamento

    private suspend fun load() {
        try {
            val puzzle: Puzzle
            when (mode) {
                GameModeId.CLASSIC -> {
                    val repo = container.levels.await()
                    val info = repo.level(key) ?: return fail("Fase não encontrada.")
                    puzzle = repo.puzzle(key) ?: return fail("Fase não encontrada.")
                    contentVersion = repo.contentVersion
                    nextLevelId = repo.next(key)?.id
                    _ui.update { it.copy(title = "Fase ${info.number}", subtitle = "${info.size}x${info.size}") }
                }
                GameModeId.DAILY -> {
                    val day = key.toLongOrNull() ?: return fail("Desafio inválido.")
                    puzzle = withContext(Dispatchers.Default) { container.dailyChallenges.puzzleFor(day) }
                    contentVersion = DAILY_CONTENT_VERSION
                    _ui.update { it.copy(title = "Desafio diário", subtitle = "${LocalDate.ofEpochDay(day)} · ${puzzle.size}x${puzzle.size}") }
                }
            }
            val session = GameSession(puzzle, config).also { this.session = it }
            val saved = container.playerData.loadSavedGame(mode)
            val restored = saved?.takeIf {
                it.contentVersion == contentVersion && session.isCompatible(it.state)
            }?.state
            val state = restored ?: session.newGame(UUID.randomUUID().toString())
            accountedPlayMillis = state.elapsedMillis
            if (restored == null) onNewGameStarted(state)
            _ui.update {
                it.copy(
                    loading = false,
                    puzzle = puzzle,
                    game = state,
                    blocked = session.blockedCells(state),
                    overlay = if (state.status == GameStatus.LOST) Overlay.Defeat() else Overlay.None,
                ).withRecovery(session, state)
            }
            if (state.status == GameStatus.WON) {
                // Partida já vencida (salvamento antigo): começa de novo.
                dispatch(GameAction.Restart)
            }
            container.adService.preload()
            startTimer()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            fail("Não foi possível carregar a fase.")
        }
    }

    private fun fail(message: String) {
        _ui.update { it.copy(loading = false, error = message) }
    }

    private suspend fun onNewGameStarted(state: GameState) {
        val today = LocalDate.now().toEpochDay()
        container.playerData.updateStats { StatsRules.onGameStarted(it, today) }
        if (mode == GameModeId.CLASSIC) container.playerData.updateProgress { it.copy(lastPlayedLevelId = key) }
        container.analytics.track(AnalyticsEvent.LevelStarted(state.levelId))
        save(state)
    }

    // ---------------------------------------------------------------- toques

    fun onCellTap(position: Position) {
        val state = _ui.value
        if (!state.interactive) return
        val game = state.game ?: return
        _ui.update { it.copy(selected = position) }
        val useCatTool = state.settings.controlMode == ControlMode.TOOL_SELECT && state.tool == Tool.CAT
        when {
            position in game.cats -> if (useCatTool) dispatch(GameAction.RemoveCat(position))
            useCatTool -> dispatch(GameAction.PlaceCat(position))
            else -> dispatch(GameAction.ToggleMark(position))
        }
    }

    /** Toque duplo: coloca um gato (ou remove um gato do jogador). */
    fun onCellDoubleTap(position: Position) {
        val state = _ui.value
        if (!state.interactive) return
        val game = state.game ?: return
        _ui.update { it.copy(selected = position) }
        if (position in game.cats) dispatch(GameAction.RemoveCat(position)) else dispatch(GameAction.PlaceCat(position))
    }

    fun onDragMarks(cells: List<Position>, marked: Boolean) {
        if (!_ui.value.interactive || cells.isEmpty()) return
        dispatch(GameAction.SetMarks(cells, marked))
    }

    fun onSelectTool(tool: Tool) = _ui.update { it.copy(tool = tool) }

    fun onUndo() {
        if (_ui.value.interactive) dispatch(GameAction.Undo)
    }

    fun onRestartRequested() {
        val state = _ui.value
        val game = state.game ?: return
        val needsConfirm = state.settings.confirmRestart && game.status == GameStatus.PLAYING && game.history.isNotEmpty()
        if (needsConfirm) _ui.update { it.copy(overlay = Overlay.ConfirmRestart) } else restart()
    }

    fun onRestartConfirmed() = restart()

    private fun restart() {
        // Contabiliza o tempo da tentativa anterior antes de zerar o cronômetro.
        _ui.value.game?.let { previous -> viewModelScope.launch { addPlayTime(previous) } }
        _ui.update { it.copy(overlay = Overlay.None, hint = null, feedback = null, selected = null) }
        dispatch(GameAction.Restart)
    }

    fun onPause() {
        if (_ui.value.game?.status == GameStatus.PLAYING) _ui.update { it.copy(overlay = Overlay.Paused) }
        saveNow()
    }

    fun onDismissOverlay() {
        _ui.update { s ->
            when (s.overlay) {
                Overlay.Paused, Overlay.ConfirmRestart, Overlay.HintAdOffer -> s.copy(overlay = Overlay.None)
                else -> s
            }
        }
    }

    fun onMessageShown() = _ui.update { it.copy(message = null) }

    // ---------------------------------------------------------------- dicas

    fun onHint() {
        val state = _ui.value
        if (!state.interactive || state.hint != null) return
        val game = state.game ?: return
        when {
            game.freeHintsLeft > 0 -> viewModelScope.launch { revealHint(paidWithAd = false) }
            config.hints.rewardedAdForExtraHints && container.adService.isEnabled ->
                _ui.update { it.copy(overlay = Overlay.HintAdOffer) }
            else -> _ui.update { it.copy(message = "Sem dicas disponíveis nesta fase.") }
        }
    }

    fun onWatchAdForHint() {
        if (busy) return
        busy = true
        _ui.update { it.copy(overlay = Overlay.HintAdLoading) }
        viewModelScope.launch {
            try {
                val result = container.adService.showRewarded(AdPlacement.EXTRA_HINT)
                container.analytics.track(AnalyticsEvent.AdResult("EXTRA_HINT", result.javaClass.simpleName))
                when (result) {
                    is RewardedAdResult.Rewarded -> {
                        _ui.update { it.copy(overlay = Overlay.None) }
                        revealHint(paidWithAd = true)
                    }
                    RewardedAdResult.DismissedWithoutReward ->
                        _ui.update { it.copy(overlay = Overlay.None, message = "O anúncio foi fechado antes do fim. Nenhuma dica foi liberada.") }
                    RewardedAdResult.NotAvailable ->
                        _ui.update { it.copy(overlay = Overlay.None, message = "Nenhum anúncio disponível agora. Tente de novo em instantes.") }
                    is RewardedAdResult.Failed ->
                        _ui.update { it.copy(overlay = Overlay.None, message = "Não foi possível exibir o anúncio.") }
                }
            } finally {
                busy = false
            }
        }
    }

    private suspend fun revealHint(paidWithAd: Boolean) {
        val session = session ?: return
        val game = _ui.value.game ?: return
        val hint = withContext(Dispatchers.Default) { HintEngine.findHint(session.puzzle, game) }
        if (hint == null) {
            _ui.update { it.copy(message = "Nada a sugerir: o tabuleiro já está completo.") }
            return
        }
        val outcome = dispatch(GameAction.ConsumeHint(paidWithAd)) ?: return
        if (outcome.events.none { it is GameEvent.HintConsumed }) return
        container.playerData.updateStats(StatsRules::onHint)
        container.analytics.track(AnalyticsEvent.HintUsed(game.levelId, hint.javaClass.simpleName))
        container.soundPlayer.play(Sound.HINT)
        _ui.update { it.copy(hint = HintFormatter.describe(hint, config.hints.allowReveal)) }
    }

    fun onHintAction() {
        val hint = _ui.value.hint?.hint ?: return
        val game = _ui.value.game ?: return
        _ui.update { it.copy(hint = null) }
        when (hint) {
            is Hint.MarkedSolutionCell -> dispatch(GameAction.SetMarks(hint.cells.toList(), false))
            is Hint.WrongCats -> hint.cells.forEach { dispatch(GameAction.RemoveCat(it)) }
            is Hint.ForcedPlacement -> if (config.hints.allowReveal) dispatch(GameAction.RevealCat(hint.position))
            is Hint.CorrectPosition -> if (config.hints.allowReveal) dispatch(GameAction.RevealCat(hint.position))
            is Hint.Elimination -> dispatch(GameAction.SetMarks(hint.cells.filter { it !in game.cats }, true))
        }
    }

    fun onDismissHint() = _ui.update { it.copy(hint = null) }

    // ---------------------------------------------------------------- derrota e recuperação

    fun onRecoverWithAd() {
        val session = session ?: return
        val game = _ui.value.game ?: return
        val defeatId = game.currentDefeatId ?: return
        if (busy || !session.canOfferRecovery(game)) return
        busy = true
        _ui.update { it.copy(overlay = Overlay.Recovering) }
        viewModelScope.launch {
            try {
                val result = container.recoveryCoordinator.request(defeatId)
                container.analytics.track(AnalyticsEvent.AdResult("RECOVERY", result.javaClass.simpleName))
                when (result) {
                    is RecoveryResult.Granted -> {
                        val outcome = dispatch(GameAction.ApplyRecovery(result.defeatId))
                        if (outcome?.events?.any { it is GameEvent.Recovered } == true) {
                            container.playerData.updateStats(StatsRules::onRecovery)
                            _ui.update { it.copy(overlay = Overlay.None, message = "Você ganhou mais uma chance!") }
                        } else {
                            _ui.update { it.copy(overlay = Overlay.Defeat("A recuperação não pôde ser aplicada.")) }
                        }
                    }
                    RecoveryResult.NoReward ->
                        _ui.update { it.copy(overlay = Overlay.Defeat("O anúncio foi fechado antes do fim, então nenhuma vida foi recuperada.")) }
                    RecoveryResult.AdUnavailable ->
                        _ui.update { it.copy(overlay = Overlay.Defeat("Nenhum anúncio disponível agora. Você pode tentar de novo, reiniciar ou sair.")) }
                    is RecoveryResult.AdFailed ->
                        _ui.update { it.copy(overlay = Overlay.Defeat("Não foi possível exibir o anúncio. Você pode reiniciar ou sair.")) }
                    RecoveryResult.AlreadyGranted, RecoveryResult.Busy ->
                        _ui.update { it.copy(overlay = Overlay.Defeat()) }
                }
            } finally {
                busy = false
            }
        }
    }

    // ---------------------------------------------------------------- saída e próxima fase

    /**
     * Sai da partida. Depois de uma vitória pode exibir um intersticial (conforme a
     * frequência); a navegação acontece mesmo se o anúncio falhar.
     */
    fun onLeave(navigate: () -> Unit) = leave(navigate)

    fun onNextLevel(navigate: (levelId: String) -> Unit) {
        val next = nextLevelId ?: return
        leave { navigate(next) }
    }

    private fun leave(navigate: () -> Unit) {
        if (busy) return
        busy = true
        saveNow()
        val won = _ui.value.game?.status == GameStatus.WON
        viewModelScope.launch {
            try {
                if (won) container.adService.maybeShowInterstitial()
            } finally {
                navigate()
                busy = false
            }
        }
    }

    // ---------------------------------------------------------------- ciclo de vida

    fun onAppPaused() {
        inForeground = false
        saveNow()
    }

    fun onAppResumed() {
        inForeground = true
        lastTick = SystemClock.elapsedRealtime()
    }

    override fun onCleared() {
        // viewModelScope já foi cancelado: grava no escopo da aplicação.
        val game = _ui.value.game ?: return
        container.applicationScope.launch { persist(game) }
    }

    // ---------------------------------------------------------------- núcleo da reação a jogadas

    private fun dispatch(action: GameAction): ActionOutcome? {
        val session = session ?: return null
        val current = _ui.value.game ?: return null
        val outcome = session.reduce(current, action)
        if (!outcome.changed) return outcome
        val state = outcome.state
        _ui.update { ui ->
            ui.copy(
                game = state,
                blocked = if (state.cats != current.cats) session.blockedCells(state) else ui.blocked,
                hint = if (action is GameAction.Tick || action is GameAction.ConsumeHint) ui.hint else null,
            ).withRecovery(session, state)
        }
        outcome.events.forEach { handleEvent(it, state) }
        if (action !is GameAction.Tick) save(state)
        return outcome
    }

    private fun handleEvent(event: GameEvent, state: GameState) {
        when (event) {
            is GameEvent.CatPlaced -> {
                container.soundPlayer.play(Sound.PLACE)
                haptic(HapticCue.LIGHT)
                _ui.update { it.copy(pulse = PlacementPulse(++eventCounter, event.position), feedback = null) }
            }
            is GameEvent.CatRejected -> {
                container.soundPlayer.play(Sound.ERROR)
                haptic(HapticCue.ERROR)
                val message = HintFormatter.rejection(event.check) + if (event.lifeLost) " −1 vida" else ""
                _ui.update {
                    it.copy(feedback = Feedback(++eventCounter, event.check.position, event.check.conflictingCells, message))
                }
                viewModelScope.launch { container.playerData.updateStats(StatsRules::onMistake) }
            }
            is GameEvent.MarksChanged -> container.soundPlayer.play(Sound.MARK)
            is GameEvent.CatRemoved -> container.soundPlayer.play(Sound.MARK)
            is GameEvent.Undone -> _ui.update { it.copy(feedback = null) }
            GameEvent.Restarted -> {
                accountedPlayMillis = 0
                _ui.update { it.copy(overlay = Overlay.None, feedback = null, hint = null) }
            }
            GameEvent.Won -> onWon(state)
            is GameEvent.Lost -> {
                container.soundPlayer.play(Sound.LOSE)
                _ui.update { it.copy(overlay = Overlay.Defeat(), hint = null) }
                viewModelScope.launch { container.playerData.updateStats(StatsRules::onLoss) }
                container.analytics.track(AnalyticsEvent.LevelLost(state.levelId))
            }
            is GameEvent.Recovered, is GameEvent.HintConsumed, is GameEvent.Ignored -> Unit
        }
    }

    private fun onWon(state: GameState) {
        container.soundPlayer.play(Sound.WIN)
        container.adService.onLevelCompleted()
        container.analytics.track(AnalyticsEvent.LevelWon(state.levelId, state.elapsedMillis, state.mistakes))
        viewModelScope.launch {
            var newRecord = false
            var note: String? = null
            when (mode) {
                GameModeId.CLASSIC -> {
                    val previous = container.playerData.progress.value.completed[state.levelId]
                    newRecord = previous != null && state.elapsedMillis < previous.bestTimeMillis
                    container.playerData.updateProgress {
                        ProgressRules.recordWin(it, state.levelId, state.elapsedMillis, state.mistakes)
                    }
                }
                GameModeId.DAILY -> {
                    val day = key.toLong()
                    val today = LocalDate.now().toEpochDay()
                    var counted = false
                    container.playerData.updateDaily { data ->
                        val (updated, ok) = container.dailyChallenges.recordCompletion(data, day, today, state.elapsedMillis)
                        counted = ok
                        updated
                    }
                    note = if (counted) "Desafio de hoje concluído!" else "Este desafio não conta para a sequência (já concluído ou data diferente de hoje)."
                }
            }
            container.playerData.updateStats { StatsRules.onWin(it, state.size, state.elapsedMillis) }
            addPlayTime(state)
            container.playerData.clearSavedGame(mode)
            _ui.update {
                it.copy(
                    hint = null,
                    overlay = Overlay.Victory(
                        VictoryInfo(state.elapsedMillis, state.mistakes, newRecord, hasNextLevel = nextLevelId != null && mode == GameModeId.CLASSIC, note = note),
                    ),
                )
            }
        }
    }

    private fun haptic(cue: HapticCue) {
        if (_ui.value.settings.hapticsEnabled) _haptics.value = (++eventCounter) to cue
    }

    // ---------------------------------------------------------------- cronômetro

    private fun startTimer() {
        timerJob?.cancel()
        lastTick = SystemClock.elapsedRealtime()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(TICK_MS)
                val now = SystemClock.elapsedRealtime()
                val delta = now - lastTick
                lastTick = now
                val ui = _ui.value
                if (inForeground && ui.overlay == Overlay.None && ui.game?.status == GameStatus.PLAYING) {
                    dispatch(GameAction.Tick(delta))
                    if (now - lastSavedAt > PERIODIC_SAVE_MS) saveNow()
                }
            }
        }
    }

    // ---------------------------------------------------------------- persistência

    private fun save(state: GameState) {
        lastSavedAt = SystemClock.elapsedRealtime()
        viewModelScope.launch { persist(state) }
    }

    private fun saveNow() {
        _ui.value.game?.let { save(it) }
    }

    private suspend fun persist(state: GameState) {
        if (state.status == GameStatus.WON) return
        container.playerData.saveGame(SavedGame(state, contentVersion))
        addPlayTime(state)
    }

    private suspend fun addPlayTime(state: GameState) {
        val delta = state.elapsedMillis - accountedPlayMillis
        if (delta > 0) {
            accountedPlayMillis = state.elapsedMillis
            container.playerData.updateStats { StatsRules.addPlayTime(it, delta) }
        }
    }

    private fun GameUiState.withRecovery(session: GameSession, state: GameState) = copy(
        adsEnabled = container.adService.isEnabled,
        canRecover = session.canOfferRecovery(state),
        recoveriesLeft = (config.recovery.maxRecoveriesPerGame - state.recoveriesUsed).coerceAtLeast(0),
    )

    companion object {
        const val ARG_MODE = "mode"
        const val ARG_KEY = "key"
        private const val TICK_MS = 1_000L
        private const val PERIODIC_SAVE_MS = 15_000L
        /** Desafios diários são gerados pelo algoritmo; mudar o gerador exige mudar esta versão. */
        private const val DAILY_CONTENT_VERSION = 1

        fun dailyKey(epochDay: Long) = epochDay.toString()
    }
}
