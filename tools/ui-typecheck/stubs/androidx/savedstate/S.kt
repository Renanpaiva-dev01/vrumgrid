@file:Suppress("unused")
package androidx.savedstate
class SavedStateRegistry
interface SavedStateRegistryOwner : androidx.lifecycle.LifecycleOwner { val savedStateRegistry: SavedStateRegistry }
