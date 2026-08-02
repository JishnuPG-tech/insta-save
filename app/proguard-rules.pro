# R8 / ProGuard rules for InstaSave

# Keep Room entities and DAOs
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep OkHttp
-dontwarn okhttp3.internal.platform.**

# Keep kotlinx.serialization
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    *** Companion;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# Keep yt-dlp native libraries and JNI interfaces
-keep class com.yausername.youtubedl_android.** { *; }
-dontwarn com.yausername.youtubedl_android.**

# Strip Logcat debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
