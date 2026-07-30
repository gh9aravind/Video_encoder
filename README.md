# 🎬 Video Transcoder for Android

A free, open-source Android video transcoder built with **Kotlin** + **Jetpack Compose** + **FFmpegKit**.  
Inspired by [Branden Archer's Video Transcoder](https://github.com/brarcher/video-transcoder).

---

## ✨ Features

| Feature | Details |
|---|---|
| **Video Selection** | SAF file picker — no storage permission needed |
| **Trim** | RangeSlider to pick start/end time |
| **Output Format** | MP4, MKV, AVI, WebM |
| **Resolution** | Original, 1080p, 720p, 480p |
| **Audio** | Keep, Mute, or change to AAC / MP3 / Opus |
| **Progress** | Real-time progress bar with elapsed time |
| **About Screen** | Version, GPLv3 license, third-party credits |

---

## 🏗️ Architecture

```
com.example.videotranscoder/
├── data/
│   └── TranscoderModels.kt       ← Enums, state data class, utility extensions
├── manager/
│   └── TranscoderManager.kt      ← FFmpeg command builder + executor
├── viewmodel/
│   └── TranscoderViewModel.kt    ← StateFlow state, business logic
├── ui/
│   ├── theme/
│   │   └── Theme.kt              ← Material3 dynamic color theme
│   └── screens/
│       ├── TranscoderScreen.kt   ← Main UI (Compose)
│       └── AboutScreen.kt        ← About + GPLv3 + library credits
└── MainActivity.kt               ← NavHost, single Activity
```

**Tech Stack:**
- Kotlin + Coroutines
- Jetpack Compose + Material Design 3
- AndroidX Navigation Compose
- [FFmpegKit Full](https://github.com/arthenica/ffmpeg-kit) for transcoding
- Coil for video thumbnails
- MVVM with StateFlow

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 35
- Min SDK 24 (Android 7.0)

### Build

```bash
git clone https://github.com/YOUR_USERNAME/VideoTranscoder.git
cd VideoTranscoder
./gradlew assembleDebug
```

> **Note:** FFmpegKit-full is a large library (~150 MB native libs).  
> The first build will take some time to download. ABI splits are enabled  
> so each device downloads only the native lib it needs.

---

## 📋 Permissions

| Permission | Why |
|---|---|
| `READ_MEDIA_VIDEO` (API 33+) | Read selected video file |
| `READ_EXTERNAL_STORAGE` (API ≤ 32) | Read selected video file |
| `WRITE_EXTERNAL_STORAGE` (API ≤ 28) | Save output on old devices |
| `FOREGROUND_SERVICE` | Reserved for future background transcoding |

> The app uses **Storage Access Framework (SAF)** for file picking,  
> so no storage permission popup appears on Android 10+.

---

## 📄 License

```
Copyright (C) 2024 Your Name

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

Full text: [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.html)

---

## 🙏 Third-Party Libraries

- [FFmpegKit](https://github.com/arthenica/ffmpeg-kit) — LGPL v3.0 / GPL v3.0
- [Jetpack Compose](https://developer.android.com/jetpack/compose) — Apache 2.0
- [Material3](https://m3.material.io/) — Apache 2.0
- [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines) — Apache 2.0
- [Coil](https://github.com/coil-kt/coil) — Apache 2.0
- [AndroidX Navigation](https://developer.android.com/guide/navigation) — Apache 2.0
