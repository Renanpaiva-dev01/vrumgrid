@file:Suppress("unused", "UNUSED_PARAMETER")
package androidx.activity
import android.os.Bundle
open class ComponentActivity : android.app.Activity() { protected open fun onCreate(savedInstanceState: Bundle?) {} }
fun ComponentActivity.enableEdgeToEdge() {}
