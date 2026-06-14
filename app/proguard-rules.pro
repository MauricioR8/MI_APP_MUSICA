# Keep generic signatures and annotations used by reflection-based libs.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# ----- jaudiotagger -----
# jaudiotagger uses reflection and references desktop-only classes (java.awt /
# javax.imageio) that do not exist on Android. We keep its classes and silence
# warnings about the missing desktop references (we use AndroidArtwork at runtime).
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
-dontwarn java.beans.**

# ----- Moshi (reflective adapter) -----
-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-dontwarn com.squareup.moshi.**

# ----- OkHttp / Okio -----
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# ----- Kotlin coroutines / metadata -----
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlin.Metadata { *; }

# ----- Media3 -----
-dontwarn androidx.media3.**
