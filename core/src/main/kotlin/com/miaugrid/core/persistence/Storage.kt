package com.miaugrid.core.persistence

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Armazenamento chave-valor de texto. No Android é implementado com DataStore. */
interface KeyValueStore {
    suspend fun get(key: String): String?
    /** Grava todas as entradas de forma atômica (todas ou nenhuma). */
    suspend fun putAll(entries: Map<String, String>)
    suspend fun remove(key: String)
}

class InMemoryKeyValueStore : KeyValueStore {
    private val map = HashMap<String, String>()
    private val lock = Mutex()
    override suspend fun get(key: String): String? = lock.withLock { map[key] }
    override suspend fun putAll(entries: Map<String, String>) = lock.withLock { map.putAll(entries) }
    override suspend fun remove(key: String) { lock.withLock { map.remove(key) } }
    suspend fun putRaw(key: String, value: String) = putAll(mapOf(key to value))
}

/**
 * Documento persistido com versão de esquema. [migrations] transforma o JSON da versão
 * `n` para `n + 1`.
 */
class Document<T>(
    val key: String,
    val serializer: KSerializer<T>,
    val version: Int,
    val default: () -> T,
    val migrations: Map<Int, (JsonElement) -> JsonElement> = emptyMap(),
)

sealed interface LoadResult<T> {
    val value: T
    data class Loaded<T>(override val value: T) : LoadResult<T>
    data class Missing<T>(override val value: T) : LoadResult<T>
    /** O documento principal estava ilegível; [fromBackup] diz se a cópia de segurança foi usada. */
    data class Recovered<T>(override val value: T, val fromBackup: Boolean, val error: String) : LoadResult<T>
}

/**
 * Leitura e gravação versionadas, com cópia de segurança. Cada tipo de dado (progresso,
 * estatísticas, partida salva...) fica numa chave própria: uma falha num documento não
 * afeta os outros. Um documento ilegível é preservado em `<chave>.corrupt` para diagnóstico.
 */
class VersionedStorage(
    private val store: KeyValueStore,
    private val onError: (key: String, message: String) -> Unit = { _, _ -> },
) {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun <T> load(doc: Document<T>): LoadResult<T> {
        val raw = runCatching { store.get(doc.key) }.getOrElse {
            onError(doc.key, "leitura: ${it.message}")
            null
        } ?: return LoadResult.Missing(doc.default())
        decode(doc, raw).onSuccess { return LoadResult.Loaded(it) }.onFailure { error ->
            onError(doc.key, "decodificação: ${error.message}")
            runCatching { store.putAll(mapOf("${doc.key}.corrupt" to raw)) }
            val backup = runCatching { store.get("${doc.key}.bak") }.getOrNull()
            val restored = backup?.let { decode(doc, it).getOrNull() }
            return if (restored != null) {
                LoadResult.Recovered(restored, fromBackup = true, error = error.message.orEmpty())
            } else {
                LoadResult.Recovered(doc.default(), fromBackup = false, error = error.message.orEmpty())
            }
        }
        error("inalcançável")
    }

    /** Grava o novo valor e, na mesma operação atômica, a cópia de segurança do valor anterior. */
    suspend fun <T> save(doc: Document<T>, value: T): Boolean {
        val encoded = encode(doc, value)
        return runCatching {
            val previous = store.get(doc.key)
            val entries = buildMap {
                put(doc.key, encoded)
                if (previous != null && decode(doc, previous).isSuccess) put("${doc.key}.bak", previous)
            }
            store.putAll(entries)
        }.onFailure { onError(doc.key, "gravação: ${it.message}") }.isSuccess
    }

    suspend fun clear(doc: Document<*>) {
        runCatching { store.remove(doc.key) }
    }

    fun <T> encode(doc: Document<T>, value: T): String {
        val envelope = buildJsonObject {
            put(VERSION, JsonPrimitive(doc.version))
            put(DATA, json.encodeToJsonElement(doc.serializer, value))
        }
        return json.encodeToString(JsonObject.serializer(), envelope)
    }

    fun <T> decode(doc: Document<T>, raw: String): Result<T> = runCatching {
        val envelope = json.parseToJsonElement(raw).jsonObject
        var version = envelope[VERSION]?.jsonPrimitive?.intOrNull ?: error("sem versão")
        var data = envelope[DATA] ?: error("sem dados")
        require(version <= doc.version) { "versão $version mais nova que a suportada (${doc.version})" }
        while (version < doc.version) {
            val migration = doc.migrations[version] ?: error("sem migração da versão $version")
            data = migration(data)
            version++
        }
        json.decodeFromJsonElement(doc.serializer, data)
    }

    private companion object {
        const val VERSION = "v"
        const val DATA = "data"
    }
}
