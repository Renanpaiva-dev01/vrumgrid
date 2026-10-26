@file:Suppress("unused", "UNUSED_PARAMETER")
package androidx.lifecycle.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
abstract class CreationExtras { object Empty : CreationExtras() }
class InitializerViewModelFactoryBuilder {
    fun <T : ViewModel> addInitializer(clazz: kotlin.reflect.KClass<T>, initializer: CreationExtras.() -> T) {}
}
inline fun viewModelFactory(builder: InitializerViewModelFactoryBuilder.() -> Unit): ViewModelProvider.Factory = TODO()
inline fun <reified VM : ViewModel> InitializerViewModelFactoryBuilder.initializer(noinline initializer: CreationExtras.() -> VM) {
    addInitializer(VM::class, initializer)
}
