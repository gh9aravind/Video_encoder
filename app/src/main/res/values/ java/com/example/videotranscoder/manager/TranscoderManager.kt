package com.example.videotranscoder.manager

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.videotranscoder.data.AudioCodec
import com.example.videotranscoder.data.AudioOption
import com.example.videotranscoder.data.OutputFormat
import com.example.videotranscoder.data.OutputResolution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * TranscoderManager
 *
 * Responsible for all FFmpeg-related operations:
 *  1. Copying the source video URI to a temporary file (FFmpegKit needs a real path)
 *  2. Building the complete FFmpeg command string
 *  3. Executing FFmpeg asynchronously with progress/completion callbacks
 *  4. Managing output files and MediaStore registration
 *  5. Cleaning up temporary files
 */
class TranscoderManager(private val context: Context) {

    companion object {
        private const val TAG = "TranscoderManager"
        private const val TEMP_INPUT_PREFIX = "transcode_input_"
        private const val OUTPUT_SUBFOLDER = "VideoTranscoder"
    }

    // ── 1. INPUT PREPARATION ─────────────────────────────────────────────────

    /**
     * Copies a content:// URI to a temporary file in the app's cache directory.
     *
     * Why? FFmpegKit works best with real filesystem paths. Content URIs are
     * Android-specific and not understood by native FFmpeg code.
     *
     * @param uri              The content URI of the selected video
     * @param progressCallback Reports copy progress from 0.0 to 1.0
     * @return                 The temp [File], or null on failure
     */
    suspend fun copyUriToTempFile(
        uri: Uri,
        progressCallback: (Float) -> Unit = {}
    ): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext null.also {
                    Log.e(TAG, "Cannot open input stream for URI: $uri")
                }

            // Get file size for progress reporting
            val fileSize = try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
            } catch (e: Exception) { 0L }

            val tempFile = File(context.cacheDir, "$TEMP_INPUT_PREFIX${System.currentTimeMillis()}.tmp")

            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(256 * 1024) // 256 KB buffer
                var bytesRead: Int
                var totalRead = 0L

                inputStream.use { input ->
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (fileSize > 0) {
                            progressCallback(totalRead.toFloat() / fileSize.toFloat())
                        }
                    }
                }
            }

            Log.d(TAG, "Copied input to temp file: ${tempFile.absolutePath} (${tempFile.length()} bytes)")
            tempFile

        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy URI to temp file", e)
            null
        }
    }

    // ── 2. OUTPUT PREPARATION ────────────────────────────────────────────────

    /**
     * Creates the output File object in app-specific external storage.
     * No WRITE_EXTERNAL_STORAGE permission is needed for this location.
     * Path: /sdcard/Android/data/com.example.videotranscoder/files/Movies/
     */
    fun prepareOutputFile(format: OutputFormat): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: context.filesDir    // Fallback to internal storage
        dir.mkdirs()
        val timestamp = System.currentTimeMillis()
        return File(dir, "transcoded_$timestamp.${format.extension}")
    }

    // ── 3. COMMAND BUILDING ──────────────────────────────────────────────────

    /**
     * Builds the full FFmpeg command string based on user settings.
     *
     * Command anatomy:
     *   ffmpeg [-ss <startSecs>] -i <input> [-t <durationSecs>]
     *          -c:v <videoCodec> [-vf scale=-2:<height>]
     *          [-an | -c:a <audioCodec> -b:a 128k]
     *          [-f <formatFlag>] -pix_fmt yuv420p -y <output>
     *
     * Key decisions:
     *   • -ss BEFORE -i   → fast keyframe-based seek (slightly imprecise at edges)
     *   • -c:v libx264    → H.264 for MP4/MKV/AVI (widely compatible)
     *   • -c:v libvpx-vp9 → VP9 for WebM
     *   • scale=-2:N      → scales to height N while preserving aspect ratio
     *   • -pix_fmt yuv420p→ ensures compatibility with most players/devices
     *   • -crf 23         → constant rate factor; 18=best quality, 28=small size
     *
     * @return The complete FFmpeg command as a single string
     */
    fun buildFFmpegCommand(
        inputPath: String,
        outputPath: String,
        startTimeMs: Long,
        endTimeMs: Long,
        totalDurationMs: Long,
        resolution: OutputResolution,
        format: OutputFormat,
        audioOption: AudioOption,
        audioCodec: AudioCodec
    ): String {
        val cmd = StringBuilder()

        // ── Start-time seek (before -i = fast seek) ──────────────────────────
        if (startTimeMs > 0L) {
            cmd.append("-ss ${startTimeMs / 1000.0} ")
        }

        // ── Input ─────────────────────────────────────────────────────────────
        cmd.append("-i \"$inputPath\" ")

        // ── Duration (how long to encode from the seek point) ─────────────────
        val selectedDurationMs = endTimeMs - startTimeMs
        val needsTrim = startTimeMs > 0L || endTimeMs < totalDurationMs
        if (needsTrim) {
            cmd.append("-t ${selectedDurationMs / 1000.0} ")
        }

        // ── Video Codec & Quality ─────────────────────────────────────────────
        when (format) {
            OutputFormat.WEBM -> {
                // VP9 encoder for WebM container
                cmd.append("-c:v libvpx-vp9 ")
                cmd.append("-b:v 0 ")          // Variable bitrate
                cmd.append("-crf 33 ")         // Quality (0=best, 63=worst for VP9)
                cmd.append("-row-mt 1 ")       // Multi-threaded row encoding
            }
            else -> {
                // H.264 (libx264) for MP4, MKV, AVI
                cmd.append("-c:v libx264 ")
                cmd.append("-preset fast ")    // Encode speed: ultrafast/fast/medium/slow
                cmd.append("-crf 23 ")         // Quality: 18=best, 28=fast/small
                cmd.append("-pix_fmt yuv420p ") // Ensures wide device compatibility
            }
        }

        // ── Resolution Scaling ────────────────────────────────────────────────
        // scale=-2:H keeps aspect ratio; -2 means "auto-calculate to be divisible by 2"
        if (resolution != OutputResolution.ORIGINAL) {
            cmd.append("-vf \"scale=-2:${resolution.height}\" ")
        }

        // ── Audio Options ─────────────────────────────────────────────────────
        when (audioOption) {
            AudioOption.MUTE -> {
                // -an removes all audio streams entirely
                cmd.append("-an ")
            }
            AudioOption.KEEP -> {
                // Try to copy audio without re-encoding (fastest, lossless)
                // Exception: WebM needs Opus; AVI traditionally uses MP3
                when (format) {
                    OutputFormat.WEBM -> cmd.append("-c:a libopus -b:a 128k ")
                    OutputFormat.AVI  -> cmd.append("-c:a libmp3lame -b:a 128k ")
                    else              -> cmd.append("-c:a copy ")
                }
            }
            AudioOption.CHANGE_CODEC -> {
                // Re-encode with the user-selected codec
                cmd.append("-c:a ${audioCodec.ffmpegCodec} ")
                cmd.append("-b:a 128k ")
            }
        }

        // ── Container Format Flag ─────────────────────────────────────────────
        when (format) {
            OutputFormat.AVI  -> cmd.append("-f avi ")
            OutputFormat.MKV  -> cmd.append("-f matroska ")
            OutputFormat.WEBM -> cmd.append("-f webm ")
            OutputFormat.MP4  -> { /* MP4 is inferred from extension */ }
        }

        // ── Overwrite & Output Path ───────────────────────────────────────────
        cmd.append("-y ")                        // Overwrite output without prompting
        cmd.append("\"$outputPath\"")

        val finalCommand = cmd.toString().trim()
        Log.d(TAG, "Generated FFmpeg command:\n  $finalCommand")
        return finalCommand
    }

    // ── 4. FFMPEG EXECUTION ──────────────────────────────────────────────────

    /**
     * Executes the FFmpeg command asynchronously.
     *
     * FFmpegKit runs FFmpeg on its own internal thread pool, so this call
     * returns immediately. Progress and completion are reported via callbacks.
     *
     * @param command          The full FFmpeg command string
     * @param totalDurationMs  Duration of the selected clip (for % calculation)
     * @param onProgress       Called with progress 0.0–1.0 on FFmpegKit's thread
     * @param onComplete       Called when done; (success, errorMessage?)
     */
    fun executeFFmpeg(
        command: String,
        totalDurationMs: Long,
        onProgress: (Float) -> Unit,
        onComplete: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        FFmpegKit.executeAsync(
            command,
            { session ->
                // ── Completion Callback ────────────────────────────────────────
                val rc = session.returnCode
                when {
                    ReturnCode.isSuccess(rc) -> {
                        Log.i(TAG, "FFmpeg completed successfully in ${session.duration}ms")
                        onProgress(1.0f)
                        onComplete(true, null)
                    }
                    ReturnCode.isCancel(rc) -> {
                        Log.i(TAG, "FFmpeg was cancelled by user")
                        onComplete(false, "Cancelled")
                    }
                    else -> {
                        val errMsg = "FFmpeg failed (rc=${rc?.value}). " +
                                "Check logs for details."
                        Log.e(TAG, errMsg)
                        Log.e(TAG, "Last 5 log lines:\n${session.logsAsString}")
                        onComplete(false, errMsg)
                    }
                }
            },
            { log ->
                // ── Log Callback (verbose, for debugging) ─────────────────────
                Log.v(TAG, "[FFmpeg] ${log.message?.trimEnd()}")
            },
            { statistics ->
                // ── Statistics Callback (progress) ─────────────────────────────
                // statistics.time = milliseconds of video processed so far
                // We compare it against totalDurationMs (selected trim length)
                val processed = statistics.time   // Long (ms)
                if (totalDurationMs > 0L && processed > 0L) {
                    val progress = (processed.toFloat() / totalDurationMs.toFloat())
                        .coerceIn(0f, 0.99f) // Clamp; 100% is set in completion callback
                    onProgress(progress)
                }
            }
        )
    }

    /**
     * Cancels any running FFmpeg session immediately.
     * FFmpegKit will call the completeCallback with a CANCEL return code.
     */
    fun cancelTranscoding() {
        FFmpegKit.cancel()
        Log.i(TAG, "Cancellation signal sent to FFmpegKit")
    }

    // ── 5. MEDIA STORE REGISTRATION ──────────────────────────────────────────

    /**
     * Makes the transcoded file visible to gallery apps and file managers
     * by adding it to the system MediaStore.
     *
     * Android 10+ (API 29+): Uses MediaStore Content Provider API
     * Android < 10:           Sends ACTION_MEDIA_SCANNER_SCAN_FILE broadcast
     */
    fun addFileToMediaStore(outputFile: File, format: OutputFormat) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, outputFile.name)
                    put(MediaStore.Video.Media.MIME_TYPE, format.mimeType)
                    put(
                        MediaStore.Video.Media.RELATIVE_PATH,
                        "${Environment.DIRECTORY_MOVIES}/$OUTPUT_SUBFOLDER"
                    )
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                }
                val uri = context.contentResolver
                    .insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                Log.i(TAG, "Added to MediaStore: $uri")
            } else {
                @Suppress("DEPRECATION")
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE
                ).apply { data = Uri.fromFile(outputFile) }
                context.sendBroadcast(intent)
                Log.i(TAG, "Media scan broadcast sent for: ${outputFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register with MediaStore: ${e.message}")
        }
    }

    // ── 6. CLEANUP ───────────────────────────────────────────────────────────

    /**
     * Deletes all temporary input files from the app's cache directory.
     * Should be called after transcoding completes, fails, or is cancelled.
     */
    fun cleanupTempFiles() {
        try {
            val deleted = context.cacheDir
                .listFiles { f -> f.name.startsWith(TEMP_INPUT_PREFIX) }
                ?.onEach { it.delete() }
                ?.size ?: 0
            if (deleted > 0) Log.d(TAG, "Cleaned up $deleted temp file(s)")
        } catch (e: Exception) {
            Log.e(TAG, "Temp file cleanup failed: ${e.message}")
        }
    }
}
