@file:Suppress("unused", "UNUSED_PARAMETER")
package android.app
import android.content.Context
import android.os.Bundle
open class Activity : Context() {
    open val isFinishing: Boolean get() = false
    open val isDestroyed: Boolean get() = false
    open val application: Application get() = TODO()
}
open class Application : Context() {
    open fun onCreate() {}
    fun registerActivityLifecycleCallbacks(callback: ActivityLifecycleCallbacks) {}
    interface ActivityLifecycleCallbacks {
        fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?)
        fun onActivityStarted(activity: Activity)
        fun onActivityResumed(activity: Activity)
        fun onActivityPaused(activity: Activity)
        fun onActivityStopped(activity: Activity)
        fun onActivitySaveInstanceState(activity: Activity, outState: Bundle)
        fun onActivityDestroyed(activity: Activity)
    }
}
