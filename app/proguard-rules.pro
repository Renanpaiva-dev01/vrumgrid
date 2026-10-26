# kotlinx.serialization: mantém os serializadores gerados das classes do núcleo.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.miaugrid.core.** {
    *** Companion;
}
-keepclasseswithmembers class com.miaugrid.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.miaugrid.core.**$$serializer { *; }
