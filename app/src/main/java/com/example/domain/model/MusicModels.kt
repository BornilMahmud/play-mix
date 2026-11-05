package com.example.domain.model

enum class AudioCategory(
    val id: String,
    val displayName: String,
    val bengaliName: String,
    val description: String
) {
    MUSIC("MUSIC", "Music", "সংগীত", "Studio tracks, songs, and albums"),
    ALARMS("ALARMS", "Alarms", "অ্যালার্ম", "Wake-up tones and alarm sounds"),
    VOICE_RECORDINGS("VOICE_RECORDINGS", "Voice Recordings", "ভয়েস রেকর্ডিং", "Voice memos, call recordings, and field notes"),
    RINGTONES("RINGTONES", "Ringtones", "রিংটোন", "Incoming call ringtones"),
    NOTIFICATION_SOUNDS("NOTIFICATION_SOUNDS", "Notification Sounds", "নোটিফিকেশন সাউন্ড", "Short message and alert chimes"),
    PODCASTS("PODCASTS", "Podcasts", "পডকাস্ট", "Podcast episodes and talk shows"),
    AUDIOBOOKS("AUDIOBOOKS", "Audiobooks", "অডিওবুক", "Spoken-word chapters and audiobooks"),
    UNCLASSIFIED("UNCLASSIFIED", "Unclassified", "অশ্রেণীবদ্ধ", "Ambiguous or unknown audio files");

    companion object {
        fun fromId(raw: String?): AudioCategory {
            if (raw.isNullOrBlank()) return MUSIC
            return entries.firstOrNull { it.id.equals(raw.trim(), ignoreCase = true) } ?: UNCLASSIFIED
        }

        fun parseSetFromCsv(csv: String?): Set<AudioCategory> {
            if (csv.isNullOrBlank()) return emptySet()
            return csv.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapNotNull { token -> entries.firstOrNull { it.id.equals(token, ignoreCase = true) } }
                .toSet()
        }

        fun toCsv(categories: Set<AudioCategory>): String {
            return categories.joinToString(",") { it.id }
        }
    }
}

data class Song(
    val id: String,
    val mediaStoreId: Long = 0L,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String = artist,
    val genre: String = "Electronic",
    val durationMs: Long,
    val trackNumber: Int = 1,
    val year: Int = 2026,
    val albumArtUri: String? = null,
    val filePath: String = "",
    val folderName: String = "Music",
    val fileSize: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val isDemoTrack: Boolean = false,
    val embeddedLyrics: String? = null,
    val rating: Int = 0, // 0..5 stars
    val isCompilation: Boolean = false,
    val trackGainDb: Float = -1.2f,
    val albumGainDb: Float = -1.0f,
    val onlineProviderId: String? = null,
    val primaryCategory: AudioCategory = AudioCategory.MUSIC,
    val originalCategory: AudioCategory = AudioCategory.MUSIC,
    val isManualClassification: Boolean = false,
    val classificationReason: String = "MediaStore IS_MUSIC flag",
    val removedFromCategories: Set<AudioCategory> = emptySet(),
    val exclusionReason: String = "",
    val categoryUpdatedAt: Long = 0L,
    val mimeType: String = "audio/wav"
) {
    val isOnlineStream: Boolean
        get() = onlineProviderId != null

    val isRemovedOrHidden: Boolean
        get() = isHidden || removedFromCategories.isNotEmpty()

    fun isVisibleInCategory(category: AudioCategory): Boolean {
        return !isHidden && primaryCategory == category && category !in removedFromCategories
    }

    val formattedDuration: String
        get() = formatDurationMs(durationMs)

    val formattedFileSize: String
        get() = when {
            onlineProviderId != null -> "Authorized Stream"
            fileSize <= 0L -> "3.8 MB"
            fileSize >= 1024 * 1024 -> String.format("%.1f MB", fileSize / (1024f * 1024f))
            else -> String.format("%d KB", (fileSize / 1024L).coerceAtLeast(1L))
        }
}

fun formatDurationMs(durationMs: Long): String {
    val safeMs = durationMs.coerceAtLeast(0L)
    val totalSeconds = safeMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

data class Album(
    val id: String,
    val name: String,
    val artist: String,
    val artworkUri: String?,
    val year: Int,
    val songs: List<Song>,
    val isCompilation: Boolean = false
) {
    val totalDurationMs: Long get() = songs.sumOf { it.durationMs }
}

data class Artist(
    val id: String,
    val name: String,
    val artworkUri: String?,
    val albumCount: Int,
    val songs: List<Song>
)

data class GenreItem(
    val name: String,
    val songs: List<Song>
)

data class FolderItem(
    val name: String,
    val path: String,
    val songs: List<Song>,
    val isExcluded: Boolean = false,
    val isSafFolder: Boolean = false
)

enum class SmartPlaylistType(val title: String, val subtitle: String) {
    FAVORITES("Favorites", "Your loved tracks"),
    RECENTLY_PLAYED("Recently Played", "Tracks you listened to recently"),
    MOST_PLAYED("Most Played", "Your heavy rotation"),
    RECENTLY_ADDED("Recently Added", "Fresh additions to your library")
}

data class Playlist(
    val id: Long,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isSmart: Boolean = false,
    val smartType: SmartPlaylistType? = null,
    val songs: List<Song> = emptyList()
)

enum class SortOption(val label: String) {
    TITLE_ASC("Title (A–Z)"),
    TITLE_DESC("Title (Z–A)"),
    ARTIST_ASC("Artist (A–Z)"),
    ALBUM_ASC("Album (A–Z)"),
    DURATION_DESC("Duration (Longest)"),
    DATE_ADDED_DESC("Recently Added"),
    PLAY_COUNT_DESC("Most Played"),
    RATING_DESC("Highest Rated (5★)")
}

enum class RepeatModeState {
    OFF,
    ALL,
    ONE
}

enum class ReplayGainMode(val label: String) {
    OFF("Off"),
    TRACK("Track Gain"),
    ALBUM("Album Gain")
}

enum class AccentColorOption(val label: String, val primaryHex: Long, val secondaryHex: Long) {
    ELECTRIC_VIOLET("Electric Violet", 0xFF7C4DFF, 0xFF00E5FF),
    SOFT_CYAN("Cyber Cyan", 0xFF00B8D4, 0xFF7C4DFF),
    NEON_ROSE("Neon Rose", 0xFFFF2E93, 0xFF00E5FF),
    EMERALD_PULSE("Emerald Pulse", 0xFF00E676, 0xFF38BDF8),
    SOLAR_AMBER("Solar Amber", 0xFFFFAB00, 0xFFFF4081)
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    BENGALI("bn", "বাংলা (Bengali)")
}

data class PlaybackBookmark(
    val id: Long,
    val songId: String,
    val positionMs: Long,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class NamedQueue(
    val id: Long,
    val name: String,
    val songIds: List<String>,
    val currentIndex: Int = 0,
    val currentPositionMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
)

data class CustomEqualizerPreset(
    val name: String,
    val bandLevelsMb: List<Int>
)

enum class LyricSource(val label: String) {
    LOCAL_FILE("Local .LRC"),
    EMBEDDED("Embedded Tag"),
    LRCLIB("LRCLIB Online"),
    MANUAL("Manual Entry"),
    DEMO("Built-in Synced LRC")
}

data class LyricWord(
    val timestampMs: Long,
    val text: String
)

data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList()
)

data class SongLyrics(
    val songId: String,
    val rawContent: String,
    val isSynced: Boolean,
    val lines: List<LyricLine>,
    val offsetMs: Long = 0L,
    val source: LyricSource = LyricSource.DEMO
)

enum class EqualizerPreset(
    val displayName: String,
    val bandGainsPercent: List<Float>
) {
    FLAT("Flat", listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)),
    ROCK("Rock", listOf(0.6f, 0.35f, -0.2f, 0.45f, 0.65f)),
    POP("Pop", listOf(-0.15f, 0.35f, 0.6f, 0.35f, -0.15f)),
    JAZZ("Jazz", listOf(0.45f, 0.25f, -0.25f, 0.3f, 0.55f)),
    CLASSICAL("Classical", listOf(0.55f, 0.4f, -0.15f, 0.4f, 0.5f)),
    ELECTRONIC("Electronic", listOf(0.65f, 0.45f, 0.0f, 0.35f, 0.7f)),
    BASS_BOOST("Bass Boost", listOf(0.85f, 0.6f, 0.1f, 0.0f, 0.0f)),
    VOCAL("Vocal", listOf(-0.3f, -0.1f, 0.65f, 0.5f, 0.1f)),
    CUSTOM("Custom", listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f))
}

data class EqualizerBandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val currentLevelMb: Int,
    val minLevelMb: Int = -1500,
    val maxLevelMb: Int = 1500
) {
    val label: String
        get() = if (centerFreqHz >= 1000) {
            String.format("%.1fkHz", centerFreqHz / 1000f).replace(".0kHz", "kHz")
        } else {
            "${centerFreqHz}Hz"
        }
}

data class AudioEffectsState(
    val isSupported: Boolean = true,
    val isEnabled: Boolean = false,
    val activePreset: EqualizerPreset = EqualizerPreset.FLAT,
    val activeCustomPresetName: String? = null,
    val customPresets: List<CustomEqualizerPreset> = emptyList(),
    val bands: List<EqualizerBandInfo> = listOf(
        EqualizerBandInfo(0, 60, 0),
        EqualizerBandInfo(1, 230, 0),
        EqualizerBandInfo(2, 910, 0),
        EqualizerBandInfo(3, 3600, 0),
        EqualizerBandInfo(4, 14000, 0)
    ),
    val preampDb: Float = 0.0f, // -12.0f .. +12.0f dB
    val leftRightBalance: Float = 0.0f, // -1.0f (Left) .. +1.0f (Right)
    val monoAudioEnabled: Boolean = false,
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF,
    val clippingPreventionEnabled: Boolean = true,
    val bassBoostSupported: Boolean = true,
    val bassBoostStrength: Int = 0, // 0..1000
    val virtualizerSupported: Boolean = true,
    val virtualizerStrength: Int = 0, // 0..1000
    val loudnessNormalizationEnabled: Boolean = false,
    val loudnessGainMb: Int = 0, // 0..1000
    val audioOutputDeviceName: String = "Device Speaker",
    val audioOutputType: String = "Built-in Stereo Speaker",
    val hardwareSampleRateHz: Int = 48000,
    val hardwareBufferFrames: Int = 256,
    val audioSessionId: Int = 0
) {
    val preampGainDb: Float get() = preampDb
    val audioClippingPrevention: Boolean get() = clippingPreventionEnabled
    val isBassBoostSupported: Boolean get() = bassBoostSupported
    val isVirtualizerSupported: Boolean get() = virtualizerSupported
    val hardwareFramesPerBuffer: Int get() = hardwareBufferFrames
}

enum class SleepTimerOption(val label: String, val minutes: Int) {
    OFF("Off", 0),
    MIN_5("5 minutes", 5),
    MIN_10("10 minutes", 10),
    MIN_15("15 minutes", 15),
    MIN_30("30 minutes", 30),
    MIN_45("45 minutes", 45),
    MIN_60("60 minutes", 60),
    END_OF_TRACK("End of current track", -1)
}

// --- UNIVERSAL ONLINE MUSIC DISCOVERY MODELS ---

enum class OnlineProviderId(val title: String) {
    ITUNES_AUTHORIZED("Apple iTunes Authorized Previews"),
    INTERNET_ARCHIVE("Internet Archive Open Audio"),
    JAMENDO_CC("Jamendo Independent Music"),
    SPOTIFY_WEB_API("Spotify Web API Connector"),
    SOUNDCLOUD_API("SoundCloud Official API")
}

data class OnlineProviderInfo(
    val id: OnlineProviderId,
    val name: String,
    val description: String,
    val endpointBaseUrl: String,
    val requiresCredentials: Boolean,
    val subscriptionRequirement: String,
    val isEnabled: Boolean,
    val hasValidCredentials: Boolean,
    val credentialSetupInstructions: String
) {
    val isReadyForStreaming: Boolean
        get() = isEnabled && (!requiresCredentials || hasValidCredentials)

    val isConfigured: Boolean
        get() = isReadyForStreaming

    val requiresApiKey: Boolean
        get() = requiresCredentials

    val statusMessage: String
        get() = "$subscriptionRequirement • $credentialSetupInstructions"
}

data class OnlineMusicAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val providerId: OnlineProviderId,
    val releaseYear: Int = 2026,
    val trackCount: Int = 8
) {
    val artworkUri: String? get() = artworkUrl
}

data class OnlineMusicArtist(
    val id: String,
    val name: String,
    val genre: String,
    val artworkUrl: String?,
    val providerId: OnlineProviderId,
    val trackCount: Int = 8
) {
    val artworkUri: String? get() = artworkUrl
}

data class OnlineMusicPlaylist(
    val id: String,
    val title: String,
    val subtitle: String,
    val providerId: OnlineProviderId,
    val tracks: List<Song>
) {
    val description: String get() = subtitle
    val songs: List<Song> get() = tracks
    val artworkUri: String? get() = tracks.firstOrNull()?.albumArtUri
}
