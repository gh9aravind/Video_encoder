package com.example.videotranscoder

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity

/**
 * CrashActivity — TEMPORARY DEBUGGING TOOL
 *
 * When the app crashes anywhere (MainActivity, ViewModel, Compose, FFmpegKit, etc.),
 * [CrashHandler] intercepts it and launches this Activity instead of showing the
 * generic "App has stopped" system dialog.
 *
 * This Activity displays the FULL exception stack trace as plain, selectable text
 * — no Android Studio / logcat / ADB required. Just screenshot this screen.
 *
 * Built with plain Android Views (not Compose) intentionally — if the crash was
 * caused by something in Compose/Theme setup, this screen must still render.
 *
 * ⚠️ Remove this file + CrashHandler.kt + the Application class registration
 * once debugging is done — it's a development aid, not for production use.
 */
class CrashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE)
            ?: "No stack trace available."

        val textView = TextView(this).apply {
            text = "⚠️ App Crashed\n\nPlease screenshot this entire screen " +
                    "(scroll down if needed) and share it for debugging:\n\n" +
                    "────────────────────────\n\n$stackTrace"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1C1B1F"))
            textSize = 12f
            setPadding(32, 64, 32, 64)
            setTextIsSelectable(true)   // Allows long-press to copy the text
            gravity = Gravity.START
        }

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#1C1B1F"))
            addView(textView)
        }

        setContentView(scrollView)
    }

    companion object {
        const val EXTRA_STACK_TRACE = "extra_stack_trace"
    }
}
