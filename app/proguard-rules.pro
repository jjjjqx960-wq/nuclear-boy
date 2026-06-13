# ── Nuclear Boy ProGuard Rules ──────────────────────────

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.nuclearboy.**$$serializer { *; }
-keepclassmembers class com.nuclearboy.** {
    *** Companion;
}
-keepclasseswithmembers class com.nuclearboy.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data classes used for serialization
-keep class com.nuclearboy.common.** { *; }
-keep class com.nuclearboy.api.deepseek.** { *; }

# Chaquopy Python
-keep class com.chaquo.python.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Compose
-dontwarn androidx.compose.**

# General
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service

# ── Tink / EncryptedSharedPreferences（errorprone 是编译期注解，运行期不存在）──
-dontwarn com.google.errorprone.annotations.**
-keep class com.google.crypto.tink.** { *; }

# ── ZXing 扫码（远程电脑配对）──
-keep class com.journeyapps.barcodescanner.** { *; }
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# ── kotlinx.serialization：保留所有 @Serializable 的 $$serializer（跨模块）──
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.nuclearboy.agent.** { *; }
-keep class com.nuclearboy.memory.** { *; }
-keep class com.nuclearboy.remotepc.** { *; }

# Tink KeysDownloader 的可选依赖（EncryptedSharedPreferences 不用，运行期不存在）
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**
