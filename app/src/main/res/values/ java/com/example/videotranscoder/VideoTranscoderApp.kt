package com.example.videotranscoder

import android.app.Application

/**
 * VideoTranscoderApp — Custom Application class.
 *
 * Registers [CrashHandler] as early as possible in the app lifecycle
 * (before MainActivity or any ViewModel is created), so that even
 * crashes during Activity/ViewModel initialization are caught and
 * displayed via [CrashActivity].
 *
 * ⚠️ TEMPORARY DEBUGGING SETUP — see CrashHandler.kt for details.
 */
class VideoTranscoderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(applicationContext))
    }
}
