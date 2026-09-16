# --- kotlinx.serialization ---
# Сериализаторы генерируются в companion/`$serializer` и находятся рефлексией.
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisible*Annotations, AnnotationDefault
-dontnote kotlinx.serialization.**

-keepclassmembers class com.simply.app.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.simply.app.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.simply.app.data.**$$serializer { *; }
-keep class com.simply.app.data.** { *; }

# --- Compose ---
-dontwarn androidx.compose.**

# --- Kotlin ---
-dontwarn kotlin.**
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
