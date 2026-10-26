// STUBS de androidx.lifecycle (artefato só no Google Maven). Apenas para checagem de tipos.
@file:Suppress("unused", "UNUSED_PARAMETER")
package androidx.lifecycle
import kotlinx.coroutines.CoroutineScope
abstract class Lifecycle {
    enum class Event { ON_CREATE, ON_START, ON_RESUME, ON_PAUSE, ON_STOP, ON_DESTROY, ON_ANY }
    enum class State { DESTROYED, INITIALIZED, CREATED, STARTED, RESUMED }
    abstract val currentState: State
}
interface LifecycleOwner { val lifecycle: Lifecycle }
abstract class ViewModel { protected open fun onCleared() {} }
val ViewModel.viewModelScope: CoroutineScope get() = TODO()
class SavedStateHandle { operator fun <T> get(key: String): T? = null }
class ViewModelStore
interface ViewModelStoreOwner { val viewModelStore: ViewModelStore }
interface HasDefaultViewModelProviderFactory
class ViewModelProvider { interface Factory }
fun androidx.lifecycle.viewmodel.CreationExtras.createSavedStateHandle(): SavedStateHandle = TODO()
