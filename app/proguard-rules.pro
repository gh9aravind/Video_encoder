# ── FFmpegKit ──────────────────────────────────────────────────────────────
# Keep all FFmpegKit classes so ProGuard does not strip them
-keep class com.arthenica.ffmpegkit.** { *; }
-keep interface com.arthenica.ffmpegkit.** { *; }

# ── Kotlin Coroutines ───────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── Coil ───────────────────────────────────────────────────────────────────
-keep class coil.** { *; }

# ── General Android / Compose ───────────────────────────────────────────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
