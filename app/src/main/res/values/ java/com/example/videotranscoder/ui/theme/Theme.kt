package com.example.videotranscoder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Typography

// ── Seed Colors ───────────────────────────────────────────────────────────────
// Deep Indigo / Teal palette — professional feel for a media tool

private val Indigo10  = Color(0xFF0D0049)
private val Indigo20  = Color(0xFF250080)
private val Indigo40  = Color(0xFF4B35C4)
private val Indigo80  = Color(0xFFBEB3FF)
private val Indigo90  = Color(0xFFE3DFFF)

private val Teal40    = Color(0xFF006A62)
private val Teal80    = Color(0xFF4FDBD0)
private val Teal90    = Color(0xFF70F8EC)

private val Error40   = Color(0xFFBA1A1A)
private val Error80   = Color(0xFFFFB4AB)

// ── Static Color Schemes (fallback for Android < 12) ─────────────────────────

private val LightColorScheme = lightColorScheme(
    primary          = Indigo40,
    onPrimary        = Color.White,
    primaryContainer = Indigo90,
    onPrimaryContainer = Indigo10,

    secondary        = Teal40,
    onSecondary      = Color.White,
    secondaryContainer = Teal90,
    onSecondaryContainer = Color(0xFF00201E),

    error            = Error40,
    onError          = Color.White,
    errorContainer   = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background       = Color(0xFFFFFBFF),
    onBackground     = Color(0xFF1C1B1F),
    surface          = Color(0xFFFFFBFF),
    onSurface        = Color(0xFF1C1B1F),
    surfaceVariant   = Color(0xFFE5E0EC),
    onSurfaceVariant = Color(0xFF48454E)
)

private val DarkColorScheme = darkColorScheme(
    primary          = Indigo80,
    onPrimary        = Indigo20,
    primaryContainer = Color(0xFF3319AC),
    onPrimaryContainer = Indigo90,

    secondary        = Teal80,
    onSecondary      = Color(0xFF00383399),
    secondaryContainer = Color(0xFF00504B),
    onSecondaryContainer = Teal90,

    error            = Error80,
    onError          = Color(0xFF690005),
    errorContainer   = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background       = Color(0xFF1C1B1F),
    onBackground     = Color(0xFFE6E1E5),
    surface          = Color(0xFF1C1B1F),
    onSurface        = Color(0xFFE6E1E5),
    surfaceVariant   = Color(0xFF48454E),
    onSurfaceVariant = Color(0xFFC9C4D0)
)

// ── Typography ────────────────────────────────────────────────────────────────

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 28.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize   = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize   = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 16.sp
    )
)

// ── Theme Entry Point ─────────────────────────────────────────────────────────

/**
 * The root theme composable. Wrap your entire app in this.
 *
 * On Android 12+ (API 31+), uses Material You dynamic colors extracted
 * from the device wallpaper. Falls back to the hand-crafted indigo/teal
 * palette on older Android versions.
 */
@Composable
fun VideoTranscoderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,   // Set to false to always use the static palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}
