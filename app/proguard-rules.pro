# Retrofit + OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** { *** Companion; }

# TrainBot models
-keep class com.luca.trainbot.core.network.** { *; }
-keep class com.luca.trainbot.core.data.** { *; }

# Referințe compile-only (autovalue/javapoet shaded, via MediaPipe/TFLite) —
# javax.lang.model nu există pe Android și nici nu e folosit la runtime.
-dontwarn javax.lang.model.**
