package com.miaugrid.core.persistence

import com.miaugrid.core.daily.DailyData
import com.miaugrid.core.game.GameModeId
import com.miaugrid.core.game.GameState
import com.miaugrid.core.progress.ProgressData
import com.miaugrid.core.settings.UserSettings
import com.miaugrid.core.stats.PlayerStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable

/** Partida em andamento salva, com a versão do conteúdo em que foi criada. */
@Serializable
data class SavedGame(val state: GameState, val contentVersion: Int)

object Documents {
    val PROGRESS = Document("progress", ProgressData.serializer(), version = 1, default = { ProgressData() })
    val STATS = Document("stats", PlayerStats.serializer(), version = 1, default = { PlayerStats() })
    val SETTINGS = Document("settings", UserSettings.serializer(), version = 1, default = { UserSettings() })
    val DAILY = Document("daily", DailyData.serializer(), version = 1, default = { DailyData() })

    fun savedGame(mode: GameModeId): Document<SavedGame?> = Document(
        key = "saved_game_${mode.name.lowercase()}",
        serializer = SavedGame.serializer().nullable,
        version = 1,
        default = { null },
    )
}

/**
 * Fonte única dos dados do jogador. Mantém os valores em memória (StateFlow) e grava a
 * cada alteração. Atualizações são serializadas por documento para evitar perda de dados.
 */
class PlayerDataRepository(private val storage: VersionedStorage) {

    private val lock = Mutex()
    private val _progress = MutableStateFlow(ProgressData())
    private val _stats = MutableStateFlow(PlayerStats())
    private val _settings = MutableStateFlow(UserSettings())
    private val _daily = MutableStateFlow(DailyData())
    private val _loaded = MutableStateFlow(false)

    val progress: StateFlow<ProgressData> = _progress.asStateFlow()
    val stats: StateFlow<PlayerStats> = _stats.asStateFlow()
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()
    val daily: StateFlow<DailyData> = _daily.asStateFlow()
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    /** Problemas encontrados no carregamento (exibidos de forma discreta na interface). */
    var loadWarnings: List<String> = emptyList()
        private set

    /** (Re)carrega todos os documentos. Atualizações feitas antes disso carregam primeiro. */
    suspend fun load() = lock.withLock { loadLocked() }

    private suspend fun loadLocked() {
        val warnings = mutableListOf<String>()
        fun <T> LoadResult<T>.track(name: String): T {
            if (this is LoadResult.Recovered) {
                warnings += if (fromBackup) "$name restaurado da cópia de segurança" else "$name ilegível; valores padrão usados"
            }
            return value
        }
        _progress.value = storage.load(Documents.PROGRESS).track("Progresso")
        _stats.value = storage.load(Documents.STATS).track("Estatísticas")
        _settings.value = storage.load(Documents.SETTINGS).track("Configurações")
        _daily.value = storage.load(Documents.DAILY).track("Desafio diário")
        loadWarnings = warnings
        _loaded.value = true
    }

    suspend fun updateProgress(transform: (ProgressData) -> ProgressData) =
        update(_progress, Documents.PROGRESS, transform)

    suspend fun updateStats(transform: (PlayerStats) -> PlayerStats) =
        update(_stats, Documents.STATS, transform)

    suspend fun updateSettings(transform: (UserSettings) -> UserSettings) =
        update(_settings, Documents.SETTINGS, transform)

    suspend fun updateDaily(transform: (DailyData) -> DailyData) =
        update(_daily, Documents.DAILY, transform)

    suspend fun loadSavedGame(mode: GameModeId): SavedGame? = storage.load(Documents.savedGame(mode)).value

    suspend fun saveGame(game: SavedGame): Boolean = storage.save(Documents.savedGame(game.state.mode), game)

    suspend fun clearSavedGame(mode: GameModeId) = storage.clear(Documents.savedGame(mode))

    private suspend fun <T> update(flow: MutableStateFlow<T>, doc: Document<T>, transform: (T) -> T): T =
        lock.withLock {
            // Nunca grava sobre dados ainda não lidos do disco.
            if (!_loaded.value) loadLocked()
            val next = transform(flow.value)
            if (next != flow.value) {
                flow.value = next
                storage.save(doc, next)
            }
            next
        }
}
