# Play Mix ProGuard / R8 Configuration Rules

-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep WebView JavaScript Bridge for Three.js Intro
-keepclassmembers class com.example.feature.onboarding.PlayMixIntroBridge {
    @android.webkit.JavascriptInterface <methods>;
    public *;
}

# Keep Room Database Entities & DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep Domain & Backup Serialization Models
-keep class com.example.domain.model.** { *; }
-keep class com.example.core.database.** { *; }
-keep class com.example.data.lyrics.LrcLibDto { *; }

# Keep Media3 ExoPlayer & Session Service components
-keep class com.example.playback.service.PlayMixPlaybackService { *; }
-keep class com.example.widget.PlayMixWidgetProvider { *; }
-dontwarn androidx.media3.**

# Retrofit / OkHttp / Moshi
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
