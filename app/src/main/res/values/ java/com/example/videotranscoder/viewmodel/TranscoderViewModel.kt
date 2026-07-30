package com.example.videotranscoder.viewmodel

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.videotranscoder.data.*
import com.example.videotranscoder.manager.TranscoderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * TranscoderViewModel
 *
 * The single ViewModel for the entire app. Owns [TranscoderState] and
 * exposes it as an immutable [StateFlow] to the UI.
 *
 * Responsibilities:
 *  • Load video metadata from a selected URI
 *  • Manage trim start/end time, format, resolution, and audio settings
 *  • Orchestrate the transcoding pipeline (prepare → transcode → register)
 *  • Expose cancel and reset actions
 *
 * Uses [AndroidViewModel] (not plain ViewModel) because [TranscoderManager]
 * needs an Application context that survives configuration changes.
 */
class TranscoderViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "TranscoderViewModel"
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private val _state = MutableStateFlow(TranscoderState())

    /**
     * The UI observes this. All mutations go through [_state.update { }].
     */
    val state: StateFlow<TranscoderState> = _state.asStateFlow()

    // ── Dependencies ──────────────────────────────────────────────────────────

    private val manager = TranscoderManager(application)

    // ── Active Jobs ───────────────────────────────────────────────────────────

    private var transcodingJob: Job? = null
    private var elapsedTimerJob: Job? = null

    // ════════════════════════════════════════════════════════════════════════
    // VIDEO SELECTION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Called by the UI after the user picks a video via the SAF file picker.
     * Extracts duration, resolution, file name, and file size from the URI.
     */
    fun onVideoSelected(uri: Uri) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val retriever = MediaMetadataRetriever()

            try {
                // MediaMetadataRetriever can read from a content URI directly
                retriever.setDataSource(context, uri)

                val durationMs = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L

                val width = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull() ?: 0

                val height = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0

                val fileName = queryFileName(uri) ?: "video.mp4"
                val fileSize = queryFileSize(uri)

                _state.update {
                    TranscoderState(           // Reset all settings when a new video is loaded
                        selectedVideoUri = uri,
                        videoFileName = fileName,
                        videoFileSizeBytes = fileSize,
                        videoDuration = durationMs,
                        videoWidth = width,
                        videoHeight = height,
                        startTimeMs = 0L,
                        endTimeMs = durationMs   // Default: full video selected
                    )
                }

                Log.i(TAG, "Video loaded: $fileName | ${width}x${height} | ${durationMs}ms")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to read video metadata", e)
                _state.update {
                    it.copy(
                        transcodingState = TranscodingState.ERROR,
                        errorMessage = "Cannot read video metadata: ${e.localizedMessage}"
                    )
                }
            } finally {
                retriever.release()
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // SETTINGS UPDATES (called directly from UI composables)
    // ════════════════════════════════════════════════════════════════════════

    fun setStartTime(ms: Long)                  = _state.update { it.copy(startTimeMs = ms) }
    fun setEndTime(ms: Long)                    = _state.update { it.copy(endTimeMs = ms) }
    fun setOutputFormat(f: OutputFormat)        = _state.update { it.copy(outputFormat = f) }
    fun setOutputResolution(r: OutputResolution)= _state.update { it.copy(outputResolution = r) }
    fun setAudioOption(o: AudioOption)          = _state.update { it.copy(audioOption = o) }
    fun setAudioCodec(c: AudioCodec)            = _state.update { it.copy(audioCodec = c) }

    // ════════════════════════════════════════════════════════════════════════
    // TRANSCODING PIPELINE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Starts the full transcoding pipeline:
     *
     * Step 1: PREPARING — Copy source URI → temp file in cache (FFmpegKit needs a real path)
     * Step 2: PROCESSING — Run FFmpeg command with progress callbacks
     * Step 3: On success — Register output in MediaStore, cleanup temp files
     */
    fun startTranscoding() {
        val currentState = _state.value
        val uri = currentState.selectedVideoUri ?: return

        transcodingJob = viewModelScope.launch {
            // ── Step 1: Prepare ────────────────────────────────────────────────
            _state.update {
                it.copy(
                    transcodingState = TranscodingState.PREPARING,
                    transcodingProgress = 0f,
                    errorMessage = "",
                    elapsedTimeSeconds = 0L
                )
            }

            val tempFile = manager.copyUriToTempFile(uri) { copyProgress ->
                // Map copy progress to 0%–10% of total UI progress
                _state.update { it.copy(transcodingProgress = copyProgress * 0.10f) }
            }

            if (tempFile == null) {
                _state.update {
                    it.copy(
                        transcodingState = TranscodingState.ERROR,
                        errorMessage = "Could not read the source video file. " +
                                "Try re-selecting it from storage."
                    )
                }
                return@launch
            }

            // ── Step 2: Build command & start FFmpeg ───────────────────────────
            val outputFile = manager.prepareOutputFile(currentState.outputFormat)
            val trimDurationMs = currentState.endTimeMs - currentState.startTimeMs

            val command = manager.buildFFmpegCommand(
                inputPath        = tempFile.absolutePath,
                outputPath       = outputFile.absolutePath,
                startTimeMs      = currentState.startTimeMs,
                endTimeMs        = currentState.endTimeMs,
                totalDurationMs  = currentState.videoDuration,
                resolution       = currentState.outputResolution,
                format           = currentState.outputFormat,
                audioOption      = currentState.audioOption,
                audioCodec       = currentState.audioCodec
            )

            // Start the elapsed-time counter (shown in the progress card)
            startElapsedTimer()

            _state.update { it.copy(transcodingState = TranscodingState.PROCESSING) }

            // FFmpegKit runs on its own threads; we use withContext(IO) to not block
            withContext(Dispatchers.IO) {
                manager.executeFFmpeg(
                    command          = command,
                    totalDurationMs  = trimDurationMs,
                    onProgress       = { ffmpegProgress ->
                        // Map FFmpeg progress 0–100% → 10%–100% of total UI progress
                        val uiProgress = 0.10f + (ffmpegProgress * 0.90f)
                        _state.update { it.copy(transcodingProgress = uiProgress.coerceIn(0f, 1f)) }
                    },
                    onComplete       = { success, errorMsg ->
                        stopElapsedTimer()
                        manager.cleanupTempFiles()

                        if (success) {
                            // ── Step 3: Register in MediaStore ─────────────────
                            manager.addFileToMediaStore(outputFile, currentState.outputFormat)

                            _state.update {
                                it.copy(
                                    transcodingState     = TranscodingState.COMPLETED,
                                    transcodingProgress  = 1f,
                                    outputFilePath       = outputFile.absolutePath
                                )
                            }
                        } else {
                            if (errorMsg == "Cancelled") {
                                _state.update {
                                    it.copy(
                                        transcodingState    = TranscodingState.CANCELLED,
                                        transcodingProgress = 0f
                                    )
                                }
                            } else {
                                _state.update {
                                    it.copy(
                                        transcodingState = TranscodingState.ERROR,
                                        errorMessage     = errorMsg ?: "Unknown error occurred"
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    /**
     * Cancels an in-progress transcoding operation.
     * FFmpegKit's cancel signal is asynchronous — the completion callback
     * will be called shortly after with a CANCEL return code.
     */
    fun cancelTranscoding() {
        manager.cancelTranscoding()
        stopElapsedTimer()
        transcodingJob?.cancel()
        _state.update {
            it.copy(
                transcodingState    = TranscodingState.CANCELLED,
                transcodingProgress = 0f
            )
        }
    }

    /**
     * Resets the entire state back to IDLE so the user can start fresh.
     */
    fun resetState() {
        transcodingJob?.cancel()
        stopElapsedTimer()
        manager.cleanupTempFiles()
        _state.update { TranscoderState() }
    }

    // ════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ════════════════════════════════════════════════════════════════════════

    /** Starts a 1-second ticker that updates [TranscoderState.elapsedTimeSeconds]. */
    private fun startElapsedTimer() {
        elapsedTimerJob?.cancel()
        elapsedTimerJob = viewModelScope.launch {
            var seconds = 0L
            while (true) {
                delay(1_000L)
                seconds++
                _state.update { it.copy(elapsedTimeSeconds = seconds) }
            }
        }
    }

    private fun stopElapsedTimer() {
        elapsedTimerJob?.cancel()
        elapsedTimerJob = null
    }

    /** Reads the display name of a content URI from the ContentResolver. */
    private suspend fun queryFileName(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            getApplication<Application>().contentResolver
                .query(uri, null, null, null, null)
                ?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx >= 0) cursor.getString(nameIdx) else null
                }
        } catch (e: Exception) { null }
    }

    /** Reads the byte size of a content URI via its file descriptor. */
    private suspend fun queryFileSize(uri: Uri): Long = withContext(Dispatchers.IO) {
        try {
            getApplication<Application>().contentResolver
                .openFileDescriptor(uri, "r")?.use { it.statSize } ?: 0L
        } catch (e: Exception) { 0L }
    }

    override fun onCleared() {
        super.onCleared()
        manager.cancelTranscoding()
        manager.cleanupTempFiles()
    }
}
