# Play Mix — Premium Native Android Music Player

**Play Mix** is a lightweight, audiophile-grade native Android music player engineered in **Kotlin** with **Jetpack Compose (Material 3)** and **Android Media3 ExoPlayer**. Designed with first-class background stability and battery efficiency for the **Realme GT Master Edition** (Android 8.0+ / API 24–36), Play Mix combines an **Intelligent 8-Category Audio Classification Engine**, **Non-Destructive Media Category Management**, a **5-Band Hardware DSP Equalizer**, **Synchronized LRC Lyrics**, and a **Three.js 3D Sound-Wave Intro**.

- **Target GitHub Repository**: [https://github.com/BornilMahmud/Play-Mix](https://github.com/BornilMahmud/Play-Mix)
- **License**: GNU General Public License v3.0 (`GPL-3.0`) — inspired by the open-source [OuterTune](https://github.com/OuterTune/OuterTune) project with 100% original branding, UI, and implementation.

---

## Key Features

### 1. Intelligent 8-Category Audio Classification Engine
Every discovered audio file on the device is automatically inspected and assigned to **one strictly separated primary category**:
1. **Music** (`IS_MUSIC` or verified music directories/tags)
2. **Alarms** (`IS_ALARM` or `/Alarms/` paths)
3. **Voice Recordings** (`IS_RECORDING` on Android 12+ or `/Recordings/`, `/CallRecordings/`, `/Voice Recorder/` paths)
4. **Ringtones** (`IS_RINGTONE` or `/Ringtones/` paths)
5. **Notification Sounds** (`IS_NOTIFICATION` or `/Notifications/` paths)
6. **Podcasts** (`IS_PODCAST` or `/Podcasts/` paths)
7. **Audiobooks** (`IS_AUDIOBOOK` on Android 10+ or `/Audiobooks/` paths / `.m4b`)
8. **Unclassified** (Fallback for ambiguous clips — uncertain files are **never** auto-classified as Music)

- **Strict Category Isolation**: Alarm tones, ringtones, notifications, and voice recordings never pollute your **Music Home** or **Music Library**.
- **Separate Category Libraries**: Dedicated search, sorting, filtering, file count badges, empty states, and batch management for all 8 categories, plus an **All Audio Inspector** and **Hidden / Removed Items** manager.

### 2. Non-Destructive Media Category Management
> **Core Principle: MOVING CHANGES CATEGORY. REMOVING CHANGES VISIBILITY. DELETING REMOVES THE ORIGINAL FILE.**

- **Move to Category**: Reassigns one or multiple files to any of the 8 categories (`isManualClassification = true`), preserving the physical file, audio tags, artwork, and play counts across rescans.
- **Remove from Category**: Hides an item from a specific category view without deleting the physical file or moving it to another category.
- **Restore to Any Category**: Restore single, multiple, or all removed/hidden items with a destination-category selector.
- **Delete from Device**: Clearly separated destructive action with a confirmation dialog displaying the exact filename.

### 3. Advanced Audio Playback & Hardware DSP Pipeline
- **Media3 Foreground Service (`PlayMixPlaybackService`)**: Gapless playback, configurable crossfade (0–12s), smooth fade-in/fade-out, A-B loop repeat, track bookmarks, and named queue snapshots.
- **5-Band Hardware Equalizer & DSP**: Preamp (-15 dB to +15 dB) with anti-clipping headroom limiter, Left/Right stereo balance, Mono downmix, ReplayGain (Track / Album / Off), BassBoost, Virtualizer, and LoudnessEnhancer.
- **Synchronized Lyrics**: Local `.lrc` parser (line + word-by-word karaoke timestamps), manual editor, timing offset (+/- ms), and online **LRCLIB** search.

### 4. Three.js 3D WebGL Intro & Lightweight Engineering
- **Three.js Cinematic Intro**: Local bundled `three.min.js` + `intro.html` rendering a 3D wireframe icosahedron audio core, concentric sound-wave rings, and particle field. Automatically disposes WebGL resources and destroys the `WebView` upon completion or **Skip**.
- **Lightweight Memory & Storage Footprint**: Bounded Coil memory/disk caches, downsampled list thumbnails (`144px`), paginated `LazyColumn`, incremental `MediaStore` `ContentObserver`, and R8 code/resource shrinking.

---

## Architecture Overview

```
com.example
├── core/
│   ├── database/        # Room v3 Database, Entities, DAO, Migrations (v1 -> v2 -> v3)
│   ├── datastore/       # Preferences DataStore (Theme, DSP, Classification, Intro state)
│   ├── i18n/            # English & Bangla (বাংলা) localization strings
│   └── ui/              # Shared Compose components, ArtworkThumbnail, Category Dialogs
├── data/
│   ├── library/         # AudioClassificationEngine, MediaStoreScanner, MusicLibraryRepository, DemoAudioSeeder
│   ├── lyrics/          # LyricsRepository & LRCLIB Retrofit client
│   └── online/          # OnlineMusicRepository (Internet Archive, Jamendo, Subsonic, Jellyfin, Piped)
├── domain/
│   └── model/           # AudioCategory, Song, Album, Artist, Playlist, PlaybackState, EqualizerState
├── feature/
│   ├── home/            # Music-only Home dashboard
│   ├── library/         # 8 Category Libraries, All Audio Inspector, Removed/Hidden Manager, Details
│   ├── onboarding/      # OnboardingScreen & ThreeJsIntroOverlay (WebGL + Native Compose fallback)
│   ├── online/          # Online Music Discovery & Stream Player
│   ├── player/          # NowPlayingScreen (Spinning Vinyl, Queue, Synced Lyrics, Equalizer sheets)
│   ├── search/          # Unified Search with History
│   └── settings/        # Settings, Backup/Restore JSON, Diagnostics, Realme UI Battery Guide
├── playback/
│   ├── controller/      # PlaybackConnectionManager, AudioEffectsController, SystemVolumeController
│   └── service/         # PlayMixPlaybackService (MediaSessionService, WakeLock, Noisy Receiver)
└── widget/              # PlayMixWidgetProvider (4x1 Home-Screen RemoteViews Widget)
```

---

## Build & Installation Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat or newer (JDK 17)
- Android SDK 36 (minSdk 24, targetSdk 36)

### Build Commands
```bash
# Run unit & Robolectric integration tests
gradle :app:testDebugUnitTest

# Run Android Lint verification
gradle lintDebug

# Build installable Debug APK
gradle assembleDebug
```

The generated APK is output to:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Realme GT Master Edition Optimization Notes
1. Open **Settings → Realme GT Master Edition Background Guide** inside Play Mix.
2. Enable **Allow background activity** under *App Info → Battery Usage* if Realme UI restricts background services after 30+ minutes of screen-off playback.
3. Play Mix automatically manages a partial wake lock (`C.WAKE_MODE_LOCAL`) and `AUDIO_BECOMING_NOISY` broadcast handling for instant pause on headphone/Bluetooth disconnect.

---

## License & Attribution
Licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See [LICENSE](LICENSE) for full terms.
Inspired by [OuterTune](https://github.com/OuterTune/OuterTune) (GPL-3.0). Play Mix is an independent project with original branding, UI, and source code.
