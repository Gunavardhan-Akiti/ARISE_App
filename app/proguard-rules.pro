# ─── Solo Leveling SYSTEM ProGuard Rules ───

# Preserve line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─── Room ───
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ─── Health Connect ───
-keep class androidx.health.connect.client.** { *; }
-keep class androidx.health.platform.client.** { *; }

# ─── Hilt ───
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ─── Kotlin Serialization ───
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ─── Lottie ───
-dontwarn com.airbnb.lottie.**
-keep class com.airbnb.lottie.** { *; }

# ─── Coroutines ───
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ─── DataStore ───
-keep class androidx.datastore.** { *; }

# ─── Accessibility & Device Admin ───
-keep class com.hunter.system.features.lock.LockAccessibilityService { *; }
-keep class com.hunter.system.features.lock.DeviceAdminReceiver { *; }
-keep class com.hunter.system.features.notifications.NotificationGuardService { *; }

