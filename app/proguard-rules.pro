# ============================================================
# 纸条 (paper-slip) — R8 / ProGuard 优化规则
# 目标：极致瘦身 + 混淆保护 + 框架兼容
#
# ⚠️ 原则：不写宽泛的 -keep 规则，让 R8 自动 shrink 未用代码。
#    大多数库（Ktor、ML Kit、Room、Compose）已内嵌自己的 keep 规则。
# ============================================================

# ── 基础保留 ──────────────────────────────────────────────

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ── Kotlin ───────────────────────────────────────────────

-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ── Kotlin 序列化 (kotlinx.serialization) ────────────────
# 仅保留项目中使用了 @Serializable 的类的序列化器

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class io.github.wjy.meditate.**$$serializer { *; }
-keepclassmembers class io.github.wjy.meditate.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.wjy.meditate.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ─── 日志移除 ────────────────────────────────────────────

-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static java.lang.String getStackTraceString(java.lang.Throwable);
}

# ─── R8 全模式优化 ───────────────────────────────────────

-allowaccessmodification

