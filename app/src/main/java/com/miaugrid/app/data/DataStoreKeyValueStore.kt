package com.miaugrid.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.miaugrid.core.persistence.KeyValueStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.miauDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "miaugrid",
    // Se o arquivo inteiro ficar ilegível, recomeça vazio em vez de travar o app.
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * Implementação Android do armazenamento do núcleo. DataStore grava de forma atômica
 * (arquivo temporário + rename), fora da thread principal.
 */
class DataStoreKeyValueStore(context: Context) : KeyValueStore {

    private val dataStore = context.applicationContext.miauDataStore

    override suspend fun get(key: String): String? =
        dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .first()[stringPreferencesKey(key)]

    override suspend fun putAll(entries: Map<String, String>) {
        dataStore.edit { prefs -> entries.forEach { (k, v) -> prefs[stringPreferencesKey(k)] = v } }
    }

    override suspend fun remove(key: String) {
        dataStore.edit { it.remove(stringPreferencesKey(key)) }
    }
}
