package com.example.videotranscoder.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// ════════════════════════════════════════════════════════════════════════════
// ABOUT SCREEN
// ════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    var showLicenseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier        = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding  = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // ── App Identity ───────────────────────────────────────────────────
            item {
                Card(
                    modifier  = Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(16.dp),
                    colors    = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.VideoFile,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint     = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Video Transcoder",
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign  = TextAlign.Center
                        )
                        Text(
                            "Version 1.0.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "A free and open-source video transcoder\ninspired by Branden Archer's Video Transcoder.",
                            style     = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color     = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // ── Links ──────────────────────────────────────────────────────────
            item {
                AboutSectionCard(title = "Links", icon = Icons.Default.Link) {
                    LinkRow(
                        icon  = Icons.Default.Code,
                        label = "Source Code",
                        url   = "https://github.com/gh9aravind/Video_encoder",
                        tint  = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color    = MaterialTheme.colorScheme.outlineVariant
                    )
                    LinkRow(
                        icon  = Icons.Default.BugReport,
                        label = "Report a Bug",
                        url   = "https://github.com/YOUR_USERNAME/VideoTranscoder/issues",
                        tint  = MaterialTheme.colorScheme.error
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color    = MaterialTheme.colorScheme.outlineVariant
                    )
                    LinkRow(
                        icon  = Icons.Default.Star,
                        label = "Star on GitHub",
                        url   = "https://github.com/YOUR_USERNAME/VideoTranscoder",
                        tint  = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // ── License ────────────────────────────────────────────────────────
            item {
                AboutSectionCard(title = "License", icon = Icons.Default.Gavel) {
                    Text(
                        "This application is licensed under the GNU General Public License v3 (GPLv3).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick  = { showLicenseDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("View License")
                        }
                        OutlinedButton(
                            onClick  = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html")
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Full Text")
                        }
                    }
                }
            }

            // ── Third-Party Libraries ──────────────────────────────────────────
            item {
                AboutSectionCard(
                    title = "Open-Source Libraries",
                    icon  = Icons.Default.LibraryBooks
                ) {
                    val libraries = listOf(
                        LibraryInfo(
                            name    = "FFmpegKit",
                            version = "6.0-2",
                            author  = "Arthenica",
                            license = "LGPL v3.0 / GPL v3.0",
                            url     = "https://github.com/arthenica/ffmpeg-kit",
                            desc    = "FFmpeg wrapper for Android. Powers all video transcoding in this app."
                        ),
                        LibraryInfo(
                            name    = "Jetpack Compose",
                            version = "BOM 2024.12.01",
                            author  = "Google / AOSP",
                            license = "Apache 2.0",
                            url     = "https://developer.android.com/jetpack/compose",
                            desc    = "Modern declarative UI toolkit for Android."
                        ),
                        LibraryInfo(
                            name    = "Material3",
                            version = "1.3.x",
                            author  = "Google",
                            license = "Apache 2.0",
                            url     = "https://m3.material.io/",
                            desc    = "Material Design 3 components for Compose."
                        ),
                        LibraryInfo(
                            name    = "Kotlin Coroutines",
                            version = "1.9.0",
                            author  = "JetBrains",
                            license = "Apache 2.0",
                            url     = "https://github.com/Kotlin/kotlinx.coroutines",
                            desc    = "Asynchronous programming library for Kotlin."
                        ),
                        LibraryInfo(
                            name    = "AndroidX Navigation",
                            version = "2.8.4",
                            author  = "Google / AOSP",
                            license = "Apache 2.0",
                            url     = "https://developer.android.com/guide/navigation",
                            desc    = "Navigation component for Jetpack Compose."
                        ),
                        LibraryInfo(
                            name    = "Coil",
                            version = "2.7.0",
                            author  = "Coil Contributors",
                            license = "Apache 2.0",
                            url     = "https://github.com/coil-kt/coil",
                            desc    = "Image loading for Android and Compose. Used for video thumbnails."
                        ),
                        LibraryInfo(
                            name    = "AndroidX Lifecycle",
                            version = "2.8.7",
                            author  = "Google / AOSP",
                            license = "Apache 2.0",
                            url     = "https://developer.android.com/topic/libraries/architecture/lifecycle",
                            desc    = "ViewModel and lifecycle-aware state management."
                        )
                    )

                    libraries.forEachIndexed { i, lib ->
                        LibraryItem(lib = lib, onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(lib.url)))
                        })
                        if (i < libraries.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color    = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }

            // ── Credits ────────────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Inspired by Branden Archer's Video Transcoder",
                            style     = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Built with ❤️ using Kotlin & Jetpack Compose",
                            style     = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // ── GPLv3 License Dialog ────────────────────────────────────────────────
    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = {
                Text("GNU GPL v3.0", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        GPLv3_SUMMARY,
                        style    = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicenseDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showLicenseDialog = false
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html")
                    )
                    context.startActivity(intent)
                }) {
                    Text("Read Full Text")
                }
            }
        )
    }
}

// ════════════════════════════════════════════════════════════════════════════
// REUSABLE COMPONENTS
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun AboutSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun LinkRow(
    icon: ImageVector,
    label: String,
    url: String,
    tint: androidx.compose.ui.graphics.Color
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label,
            modifier = Modifier.weight(1f),
            style    = MaterialTheme.typography.bodyMedium)
        Icon(Icons.Default.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint     = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LibraryItem(lib: LibraryInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(lib.name,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Surface(
                shape  = RoundedCornerShape(4.dp),
                color  = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    "v${lib.version}",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Text("by ${lib.author}  •  ${lib.license}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(lib.desc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// DATA
// ════════════════════════════════════════════════════════════════════════════

private data class LibraryInfo(
    val name: String,
    val version: String,
    val author: String,
    val license: String,
    val url: String,
    val desc: String
)

/** Condensed GPLv3 summary shown in the in-app dialog. */
private const val GPLv3_SUMMARY = """
GNU GENERAL PUBLIC LICENSE
Version 3, 29 June 2007

Copyright (C) 2007 Free Software Foundation, Inc.

Everyone is permitted to copy and distribute verbatim copies of this license document, but changing it is not allowed.

PREAMBLE

The GNU General Public License is a free, copyleft license for software and other kinds of works.

When we speak of free software, we are referring to freedom, not price. Our General Public Licenses are designed to make sure that you have the freedom to distribute copies of free software, to receive source code or be able to get it if you want, to change the software or use pieces of it in new free programs, and to know you can do these things.

KEY PERMISSIONS:
• Run the program for any purpose
• Study and modify the source code
• Redistribute copies of the original program
• Distribute modified versions

KEY CONDITIONS:
• Source code must be made available when distributing
• Modifications must also be licensed under GPLv3
• License and copyright notices must be preserved
• Patent rights must be granted to users

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

Full license text: https://www.gnu.org/licenses/gpl-3.0.html
"""
