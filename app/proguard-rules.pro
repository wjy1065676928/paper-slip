# Kotlin
-keep interface kotlin.** { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep class names for reflection
-keepclasseswithmembernames class * {
    native <methods>;
}

