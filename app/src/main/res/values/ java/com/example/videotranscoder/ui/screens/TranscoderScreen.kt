package com.example.videotranscoder.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.decode.VideoFrameDecoder
import com.example.videotranscoder.data.*
import com.example.videotranscoder.viewmodel.TranscoderViewModel

// ════════════════════════════════════════════════════════════════════════════
// ROOT SCREEN
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranscoderScreen(
    viewModel: TranscoderViewModel,
    onNavigateToAbout: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // ── File Picker ───────────────────────────────────────────────────────────
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // Persist read permission across app restarts
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) { /* SAF already persisted it */ }
            viewModel.onVideoSelected(it)
        }
    }

    // ── Snackbar notifications ────────────────────────────────────────────────
    LaunchedEffect(state.transcodingState) {
        when (state.transcodingState) {
            TranscodingState.COMPLETED ->
                snackbarHostState.showSnackbar("✅ Transcoding complete!", duration = SnackbarDuration.Long)
            TranscodingState.CANCELLED ->
                snackbarHostState.showSnackbar("Transcoding cancelled.", duration = SnackbarDuration.Short)
            TranscodingState.ERROR ->
                snackbarHostState.showSnackbar("❌ ${state.errorMessage}", duration = SnackbarDuration.Long)
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Video Transcoder", style = MaterialTheme.typography.titleLarge)
                        Text("Powered by FFmpegKit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(Icons.Default.Info, contentDescription = "About")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── 1. Video Selection ────────────────────────────────────────────
            item {
                VideoSelectionCard(
                    state = state,
                    onPickVideo = { videoPickerLauncher.launch(arrayOf("video/*")) }
                )
            }

            // ── 2–6. Settings (visible only after a video is selected) ────────
            if (state.selectedVideoUri != null && !state.transcodingState.isLocked()) {

                item {
                    TrimCard(
                        duration    = state.videoDuration,
                        startTimeMs = state.startTimeMs,
                        endTimeMs   = state.endTimeMs,
                        onRangeChange = { s, e ->
                            viewModel.setStartTime(s)
                            viewModel.setEndTime(e)
                        }
                    )
                }

                item {
                    FormatCard(
                        selected   = state.outputFormat,
                        onChange   = viewModel::setOutputFormat
                    )
                }

                item {
                    ResolutionCard(
                        selected      = state.outputResolution,
                        videoHeight   = state.videoHeight,
                        onChange      = viewModel::setOutputResolution
                    )
                }

                item {
                    AudioCard(
                        audioOption  = state.audioOption,
                        audioCodec   = state.audioCodec,
                        format       = state.outputFormat,
                        onOptionChange = viewModel::setAudioOption,
                        onCodecChange  = viewModel::setAudioCodec
                    )
                }

                item {
                    // ── Start Button ─────────────────────────────────────────
                    Button(
                        onClick   = { viewModel.startTranscoding() },
                        modifier  = Modifier.fillMaxWidth().height(56.dp),
                        shape     = RoundedCornerShape(14.dp),
                        colors    = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Start Transcoding",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── 7. Progress / Result Card ─────────────────────────────────────
            if (state.transcodingState != TranscodingState.IDLE) {
                item {
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn() + slideInVertically { it / 2 },
                        exit    = fadeOut()
                    ) {
                        ProgressCard(
                            state    = state,
                            onCancel = { viewModel.cancelTranscoding() },
                            onReset  = { viewModel.resetState() }
                        )
                    }
                }
            }

            // Bottom padding so FAB/nav doesn't overlap last item
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// CARD 1 — VIDEO SELECTION
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun VideoSelectionCard(
    state: TranscoderState,
    onPickVideo: () -> Unit
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.VideoFile,
                contentDescription = null,
                tint   = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Source Video", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(12.dp))

        if (state.selectedVideoUri == null) {
            // ── Empty state ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width  = 2.dp,
                        color  = MaterialTheme.colorScheme.outlineVariant,
                        shape  = RoundedCornerShape(12.dp)
                    )
                    .clickable { onPickVideo() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Tap to select a video",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // ── Video info with thumbnail ─────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Thumbnail using Coil's VideoFrameDecoder
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(state.selectedVideoUri)
                        .decoderFactory { result, options, _ ->
                            VideoFrameDecoder(result.source, options)
                        }
                        .crossfade(true)
                        .build(),
                    contentDescription = "Video thumbnail",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.videoFileName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    InfoLabel("📐", "${state.videoWidth} × ${state.videoHeight}")
                    InfoLabel("⏱", state.videoDuration.formatAsTime())
                    InfoLabel("💾", state.videoFileSizeBytes.formatAsFileSize())
                }
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick   = onPickVideo,
                modifier  = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Change Video")
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// CARD 2 — TRIM SLIDER
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrimCard(
    duration: Long,
    startTimeMs: Long,
    endTimeMs: Long,
    onRangeChange: (Long, Long) -> Unit
) {
    // Convert ms → float (0.0..1.0) for the RangeSlider
    var sliderRange by remember(startTimeMs, endTimeMs, duration) {
        mutableStateOf(
            if (duration > 0L) {
                (startTimeMs.toFloat() / duration) .. (endTimeMs.toFloat() / duration)
            } else {
                0f..1f
            }
        )
    }

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ContentCut,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Trim Video", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(14.dp))

        RangeSlider(
            value    = sliderRange,
            onValueChange = { range ->
                sliderRange = range
                // Convert back to milliseconds and report
                val newStart = (range.start * duration).toLong()
                val newEnd   = (range.endInclusive * duration).toLong()
                onRangeChange(newStart, newEnd)
            },
            modifier     = Modifier.fillMaxWidth(),
            valueRange   = 0f..1f,
            steps        = 0,    // Smooth slider
        )

        Spacer(Modifier.height(4.dp))

        // Time markers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TimeBadge(label = "Start", time = startTimeMs)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Duration",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    (endTimeMs - startTimeMs).formatAsTime(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            TimeBadge(label = "End", time = endTimeMs)
        }
    }
}

@Composable
private fun TimeBadge(label: String, time: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(time.formatAsTime(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// CARD 3 — OUTPUT FORMAT
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun FormatCard(
    selected: OutputFormat,
    onChange: (OutputFormat) -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VideoSettings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Output Format", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutputFormat.values().forEach { format ->
                FilterChip(
                    selected = format == selected,
                    onClick  = { onChange(format) },
                    label    = { Text(format.displayName) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = if (format == selected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// CARD 4 — OUTPUT RESOLUTION
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun ResolutionCard(
    selected: OutputResolution,
    videoHeight: Int,
    onChange: (OutputResolution) -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AspectRatio,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Output Resolution", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutputResolution.values().forEach { resolution ->
                // Hide resolutions higher than the source video
                val isUnavailable = resolution.height > 0 && videoHeight > 0
                        && resolution.height > videoHeight

                val isSelected = resolution == selected

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .clickable(enabled = !isUnavailable) { onChange(resolution) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick  = { if (!isUnavailable) onChange(resolution) },
                        enabled  = !isUnavailable
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = resolution.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isUnavailable)
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                        if (isUnavailable) {
                            Text("Higher than source ($videoHeight p)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// CARD 5 — AUDIO OPTIONS
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun AudioCard(
    audioOption: AudioOption,
    audioCodec: AudioCodec,
    format: OutputFormat,
    onOptionChange: (AudioOption) -> Unit,
    onCodecChange: (AudioCodec) -> Unit
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Audiotrack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Audio Options", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(12.dp))

        // Audio option chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AudioOption.values().forEach { option ->
                FilterChip(
                    selected = option == audioOption,
                    onClick  = { onOptionChange(option) },
                    label    = {
                        Text(
                            when (option) {
                                AudioOption.KEEP         -> "Keep"
                                AudioOption.MUTE         -> "Mute"
                                AudioOption.CHANGE_CODEC -> "Codec"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.weight(1f),
                    leadingIcon = if (option == audioOption) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )
            }
        }

        // Codec selector (visible only when CHANGE_CODEC is selected)
        AnimatedVisibility(visible = audioOption == AudioOption.CHANGE_CODEC) {
            Column {
                Spacer(Modifier.height(12.dp))
                Text("Select Codec",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))

                // Filter incompatible codecs (Vorbis/Opus only for WebM)
                val compatibleCodecs = when (format) {
                    OutputFormat.WEBM -> AudioCodec.values().toList()
                    else              -> AudioCodec.values()
                        .filter { it != AudioCodec.VORBIS && it != AudioCodec.OPUS }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    compatibleCodecs.forEach { codec ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCodecChange(codec) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = codec == audioCodec,
                                onClick  = { onCodecChange(codec) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(codec.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// CARD 6 — PROGRESS & RESULT
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProgressCard(
    state: TranscoderState,
    onCancel: () -> Unit,
    onReset: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue   = state.transcodingProgress,
        label         = "ProgressAnimation"
    )

    SectionCard {
        when (state.transcodingState) {

            TranscodingState.PREPARING, TranscodingState.PROCESSING -> {
                // ── Active progress ───────────────────────────────────────────
                val label = if (state.transcodingState == TranscodingState.PREPARING)
                    "Preparing input file…"
                else
                    "Transcoding… ${(animatedProgress * 100).toInt()}%"

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress     = { animatedProgress },
                    modifier     = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    trackColor   = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Elapsed: ${state.elapsedTimeSeconds}s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(14.dp))

                OutlinedButton(
                    onClick  = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cancel")
                }
            }

            TranscodingState.COMPLETED -> {
                // ── Success ───────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Transcoding Complete!",
                        style     = MaterialTheme.typography.titleMedium,
                        color     = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(10.dp))

                InfoLabel("📁", "Saved to your Movies folder")
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = state.outputFilePath,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick   = onReset,
                    modifier  = Modifier.fillMaxWidth(),
                    colors    = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor   = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Transcode Another Video")
                }
            }

            TranscodingState.ERROR -> {
                // ── Error ─────────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Transcoding Failed",
                        style     = MaterialTheme.typography.titleMedium,
                        color     = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                Text(state.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Try Again")
                }
            }

            TranscodingState.CANCELLED -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cancel,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cancelled",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                    Text("Start Over")
                }
            }

            else -> {}
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// REUSABLE COMPONENTS
// ════════════════════════════════════════════════════════════════════════════

/** A rounded Material3 card that wraps a section of content. */
@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content  = content
        )
    }
}

/** One line of metadata: emoji icon + text */
@Composable
private fun InfoLabel(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(6.dp))
        Text(text,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
