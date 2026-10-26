package com.miaugrid.core

import com.miaugrid.core.Fixtures.p
import com.miaugrid.core.game.GameAction
import com.miaugrid.core.game.GameModeConfig
import com.miaugrid.core.game.GameModeId
import com.miaugrid.core.game.GameSession
import com.miaugrid.core.persistence.Document
import com.miaugrid.core.persistence.Documents
import com.miaugrid.core.persistence.InMemoryKeyValueStore
import com.miaugrid.core.persistence.LoadResult
import com.miaugrid.core.persistence.PlayerDataRepository
import com.miaugrid.core.persistence.SavedGame
import com.miaugrid.core.persistence.VersionedStorage
import com.miaugrid.core.progress.ProgressRules
import com.miaugrid.core.settings.UserSettings
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistenceTest {

    @Test fun `partida salva e restaurada identica`() = runTest {
        val store = InMemoryKeyValueStore()
        val session = GameSession(Fixtures.puzzle5WithGiven, GameModeConfig.CLASSIC)
        var state = session.newGame("abc")
        for (a in listOf(GameAction.PlaceCat(p(0, 4)), GameAction.ToggleMark(p(4, 4)), GameAction.PlaceCat(p(0, 0)), GameAction.Tick(12_345))) {
            state = session.reduce(state, a).state
        }
        PlayerDataRepository(VersionedStorage(store)).saveGame(SavedGame(state, contentVersion = 1))

        // "Fecha e reabre o aplicativo": novo repositório sobre o mesmo armazenamento.
        val restored = PlayerDataRepository(VersionedStorage(store)).loadSavedGame(GameModeId.CLASSIC)!!
        assertEquals(state, restored.state)
        assertEquals(2, restored.state.lives)
        assertEquals(1, restored.state.mistakes)
        assertEquals(setOf(p(4, 4)), restored.state.marks)
        assertEquals(12_345L, restored.state.elapsedMillis)
        assertTrue(session.isCompatible(restored.state))
        // O histórico também volta: desfazer continua funcionando.
        assertTrue(session.reduce(restored.state, GameAction.Undo).changed)
    }

    @Test fun `progresso e configuracoes persistem entre instancias`() = runTest {
        val store = InMemoryKeyValueStore()
        val first = PlayerDataRepository(VersionedStorage(store))
        first.load()
        first.updateProgress { ProgressRules.recordWin(it, "c001", 30_000, 1) }
        first.updateSettings { it.copy(soundEnabled = false) }

        val second = PlayerDataRepository(VersionedStorage(store))
        second.load()
        assertEquals(30_000L, second.progress.value.completed["c001"]!!.bestTimeMillis)
        assertEquals(false, second.settings.value.soundEnabled)
        assertTrue(second.loadWarnings.isEmpty())
    }

    @Test fun `documento corrompido usa a copia de seguranca sem afetar os outros`() = runTest {
        val store = InMemoryKeyValueStore()
        val repo = PlayerDataRepository(VersionedStorage(store))
        repo.load()
        repo.updateProgress { ProgressRules.recordWin(it, "c001", 10_000, 0) }
        repo.updateProgress { ProgressRules.recordWin(it, "c002", 20_000, 0) }
        repo.updateSettings { it.copy(showTimer = false) }
        store.putRaw("progress", "{isto não é json")

        val reopened = PlayerDataRepository(VersionedStorage(store))
        reopened.load()
        // A cópia de segurança tem o valor anterior à última gravação.
        assertEquals(setOf("c001"), reopened.progress.value.completed.keys)
        assertEquals(false, reopened.settings.value.showTimer)
        assertEquals(1, reopened.loadWarnings.size)
        assertEquals("{isto não é json", store.get("progress.corrupt"))
    }

    @Test fun `documento ilegivel sem copia volta ao padrao`() = runTest {
        val store = InMemoryKeyValueStore()
        store.putRaw("settings", "lixo")
        val result = VersionedStorage(store).load(Documents.SETTINGS)
        assertTrue(result is LoadResult.Recovered)
        assertEquals(UserSettings(), result.value)
    }

    @Test fun `atualizar antes de carregar nao apaga dados gravados`() = runTest {
        val store = InMemoryKeyValueStore()
        val first = PlayerDataRepository(VersionedStorage(store))
        first.updateProgress { ProgressRules.recordWin(it, "c001", 10_000, 0) }

        val second = PlayerDataRepository(VersionedStorage(store))
        // Sem chamar load(): a atualização precisa partir do que está no disco.
        second.updateProgress { ProgressRules.recordWin(it, "c002", 20_000, 0) }
        assertEquals(setOf("c001", "c002"), second.progress.value.completed.keys)
    }

    @Serializable data class ProfileV2(val name: String, val level: Int)

    @Test fun `migracao de esquema`() = runTest {
        val store = InMemoryKeyValueStore()
        store.putRaw("profile", """{"v":1,"data":{"name":"Mia"}}""")
        val doc = Document(
            key = "profile", serializer = ProfileV2.serializer(), version = 2, default = { ProfileV2("", 0) },
            migrations = mapOf(1 to { old -> JsonObject(old.jsonObject + ("level" to JsonPrimitive(1))) }),
        )
        val loaded = VersionedStorage(store).load(doc)
        assertTrue(loaded is LoadResult.Loaded)
        assertEquals(ProfileV2("Mia", 1), loaded.value)
    }

    @Test fun `sem partida salva retorna nulo`() = runTest {
        assertNull(PlayerDataRepository(VersionedStorage(InMemoryKeyValueStore())).loadSavedGame(GameModeId.DAILY))
    }
}
