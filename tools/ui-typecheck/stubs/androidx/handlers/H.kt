@file:Suppress("unused", "UNUSED_PARAMETER")
package androidx.datastore.core.handlers
import androidx.datastore.core.CorruptionException
class ReplaceFileCorruptionHandler<T>(produceNewData: (CorruptionException) -> T)
