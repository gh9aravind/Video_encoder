package com.example.videotranscoder

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import java.io.File

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
        // Build the full, readable stack trace string first — before anything
        // else can go wrong.
        val fullTrace = Log.getStackTraceString(throwable)

        // ── Backup #1: Write the trace to a plain text file ────────────────
        // Even if starting CrashActivity fails for any reason, this file
        // survives on disk and can be pulled with a file manager app.
        // Saved to the PUBLIC Downloads folder so it's visible without
        // needing to browse the hidden Android/data folder.
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val logFile = File(downloadsDir, "VideoTranscoder_crash_log.txt")
            logFile.writeText(fullTrace)
        } catch (ignored: Throwable) {
            // Best-effort only — if this fails (e.g. no storage permission
            // on some OEM skins), we still try the Activity route below.
        }

        // ── Backup #2: Show it on screen via CrashActivity ─────────────────
        try {
            val intent = Intent(appContext, CrashActivity::class.java).apply {
                putExtra(CrashActivity.EXTRA_STACK_TRACE, fullTrace)
                // Required flags since we're starting an Activity from outside
                // of any Activity context (the app process is about to die)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            appContext.startActivity(intent)

            // ⚠️ CRITICAL FIX: startActivity() is asynchronous — it just
            // *requests* that the system launch the activity, it does not
            // wait for it to actually happen. If we kill this process
            // immediately afterward, the request may never reach
            // ActivityManagerService in time, and CrashActivity never opens.
            // Sleeping briefly gives the system enough time to act on it.
            Thread.sleep(1000)

        } catch (e: Throwable) {
            // If even the crash handler fails, fall back to the system default
            defaultHandler?.uncaughtException(thread, throwable)
        } finally {
            // Kill the current (crashed) process so CrashActivity starts fresh
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(1)
        }
    }
}
