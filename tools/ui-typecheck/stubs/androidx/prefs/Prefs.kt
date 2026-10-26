@file:Suppress("unused", "UNUSED_PARAMETER")
package androidx.datastore.preferences.core
import androidx.datastore.core.DataStore
abstract class Preferences {
    class Key<T>(val name: String)
    abstract operator fun <T> get(key: Key<T>): T?
}
abstract class MutablePreferences : Preferences() {
    abstract operator fun <T> set(key: Key<T>, value: T)
    abstract fun <T> remove(key: Key<T>): T
}
fun stringPreferencesKey(name: String): Preferences.Key<String> = Preferences.Key(name)
fun emptyPreferences(): Preferences = TODO()
suspend fun DataStore<Preferences>.edit(transform: suspend (MutablePreferences) -> Unit): Preferences = TODO()
