package com.example.videotranscoder

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * CrashHandler — TEMPORARY DEBUGGING TOOL
 *
 * Registered as the app's global [Thread.UncaughtExceptionHandler] in
 * [VideoTranscoderApp.onCreate]. Whenever ANY unhandled exception occurs
 * anywhere in the app (MainActivity, ViewModel, Compose composition,
 * FFmpegKit callbacks, coroutines, etc.), this class intercepts it,
 * captures the full stack trace, and launches [CrashActivity] to display
 * it — instead of the generic system "App has stopped" dialog that gives
 * no useful information without ADB/logcat.
 *
 * ⚠️ This is a development aid only. Remove before publishing a release build.
 */
class CrashHandler(private val appContext: Context) : Thread.UncaughtExceptionHandler {

    // Keep a reference to the system's original handler as a fallback
    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // Build the full, readable stack trace string
            val fullTrace = Log.getStackTraceString(throwable)

            val intent = Intent(appContext, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_STACK_TRACE, fullTrace)
                // Required flags since we're starting an Activity from outside
                // of any Activity context (the app process is about to die)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            appContext.startActivity(intent)

        } catch (e: Exception) {
            // If even the crash handler fails, fall back to the system default
            defaultHandler?.uncaughtException(thread, throwable)
        } finally {
            // Kill the current (crashed) process so CrashActivity starts fresh
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(1)
        }
    }
}
