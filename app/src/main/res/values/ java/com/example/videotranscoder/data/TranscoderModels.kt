package com.example.videotranscoder.data

import android.net.Uri

// ════════════════════════════════════════════════════════════════════════════
// STATE
// ════════════════════════════════════════════════════════════════════════════

/**
 * Single source of truth for the entire Transcoder UI.
 * Held in TranscoderViewModel as a StateFlow<TranscoderState>.
 */
data class TranscoderState(
    // ── Selected Video ──────────────────────────────────────────────────────
    val selectedVideoUri: Uri? = null,
    val videoFileName: String = "",
    val videoFileSizeBytes: Long = 0L,
    val videoDuration: Long = 0L,       // Total duration in milliseconds
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,

    // ── Trim Settings (milliseconds) ────────────────────────────────────────
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,

    // ── Output Settings ─────────────────────────────────────────────────────
    val outputFormat: OutputFormat = OutputFormat.MP4,
    val outputResolution: OutputResolution = OutputResolution.ORIGINAL,
    val audioOption: AudioOption = AudioOption.KEEP,
    val audioCodec: AudioCodec = AudioCodec.AAC,

    // ── Transcoding Status ──────────────────────────────────────────────────
    val transcodingState: TranscodingState = TranscodingState.IDLE,
    val transcodingProgress: Float = 0f,    // 0.0 → 1.0
    val elapsedTimeSeconds: Long = 0L,
    val outputFilePath: String = "",
    val errorMessage: String = ""
)

// ════════════════════════════════════════════════════════════════════════════
// ENUMS
// ════════════════════════════════════════════════════════════════════════════

/**
 * Target container/format for the output video.
 * [extension]    → file extension used when naming the output file
 * [displayName]  → label shown in the UI
 * [mimeType]     → used when inserting into MediaStore
 */
enum class OutputFormat(
    val extension: String,
    val displayName: String,
    val mimeType: String
) {
    MP4("mp4", "MP4", "video/mp4"),
    MKV("mkv", "MKV", "video/x-matroska"),
    AVI("avi", "AVI", "video/avi"),
    WEBM("webm", "WebM", "video/webm")
}

/**
 * Target output resolution.
 * [label]   → shown in the UI chips
 * [height]  → target height in pixels; -1 means keep original
 * FFmpeg uses scale=-2:<height> which preserves aspect ratio.
 */
enum class OutputResolution(val label: String, val height: Int) {
    ORIGINAL("Original", -1),
    FHD("1080p", 1080),
    HD("720p", 720),
    SD("480p", 480)
}

/**
 * What to do with the audio stream during transcoding.
 */
enum class AudioOption(val label: String) {
    KEEP("Keep Original"),
    MUTE("Mute Audio"),
    CHANGE_CODEC("Change Codec")
}

/**
 * Target audio codec when AudioOption.CHANGE_CODEC is chosen.
 * [ffmpegCodec] → the exact codec name passed to FFmpeg's -c:a flag
 */
enum class AudioCodec(val label: String, val ffmpegCodec: String) {
    AAC("AAC (Recommended)", "aac"),
    MP3("MP3", "libmp3lame"),
    OPUS("Opus (WebM)", "libopus"),
    VORBIS("Vorbis", "libvorbis")
}

/**
 * Represents every possible phase of the transcoding lifecycle.
 */
enum class TranscodingState {
    IDLE,       // Nothing happening — waiting for user input
    PREPARING,  // Copying the source video to a temp file for FFmpegKit
    PROCESSING, // FFmpegKit is actively transcoding
    COMPLETED,  // Finished successfully
    ERROR,      // Something went wrong — errorMessage has details
    CANCELLED   // Cancelled by user
}

// ════════════════════════════════════════════════════════════════════════════
// UTILITY EXTENSIONS
// ════════════════════════════════════════════════════════════════════════════

/**
 * Converts milliseconds to a human-readable time string.
 * Examples: 3723000 → "01:02:03", 125000 → "02:05"
 */
fun Long.formatAsTime(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * Converts a byte count to a human-readable file size string.
 * Examples: 1048576 → "1.0 MB", 2684354560 → "2.5 GB"
 */
fun Long.formatAsFileSize(): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        this < kb   -> "$this B"
        this < mb   -> String.format("%.1f KB", this / kb)
        this < gb   -> String.format("%.1f MB", this / mb)
        else        -> String.format("%.2f GB", this / gb)
    }
}

/**
 * Returns true when the app should show progress-related UI.
 */
fun TranscodingState.isActive(): Boolean =
    this == TranscodingState.PREPARING || this == TranscodingState.PROCESSING

/**
 * Returns true when the output settings should be disabled (locked during transcode).
 */
fun TranscodingState.isLocked(): Boolean = isActive()
