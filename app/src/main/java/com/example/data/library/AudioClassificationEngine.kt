package com.example.data.library

import com.example.domain.model.AudioCategory

data class AudioClassificationInput(
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val filePath: String,
    val relativePath: String = "",
    val folderName: String,
    val fileName: String,
    val mimeType: String,
    val durationMs: Long,
    val isMusicFlag: Boolean = false,
    val isAlarmFlag: Boolean = false,
    val isRingtoneFlag: Boolean = false,
    val isNotificationFlag: Boolean = false,
    val isPodcastFlag: Boolean = false,
    val isAudiobookFlag: Boolean = false,
    val isRecordingFlag: Boolean = false,
    val manualOverrideCategory: AudioCategory? = null
)

data class AudioClassificationResult(
    val primaryCategory: AudioCategory,
    val reason: String,
    val isManualOverride: Boolean
)

/**
 * Independent deterministic media classification engine.
 *
 * Precedence Order:
 * 1. User-defined manual classification override (persisted across rescans).
 * 2. Explicit non-music MediaStore flags & canonical system/app directory paths:
 *    - ALARMS > RINGTONES > NOTIFICATION_SOUNDS > VOICE_RECORDINGS > AUDIOBOOKS > PODCASTS
 *    (Takes precedence even if Android MediaStore also sets IS_MUSIC = 1 on a file in /Recordings or /Alarms).
 * 3. Confident MUSIC identification (IS_MUSIC flag, /Music or /Download song files with valid song duration and non-generic title, or artist/album metadata).
 * 4. UNCLASSIFIED fallback for ambiguous, untagged, or uncertain audio files. Never classifies uncertain files as MUSIC.
 */
object AudioClassificationEngine {

    private val alarmPathTokens = listOf(
        "/alarms", "/alarm/", "alarm_tones", "alarmclock", "morning_alarm"
    )
    private val ringtonePathTokens = listOf(
        "/ringtones", "/ringtone/", "incoming_ring"
    )
    private val notificationPathTokens = listOf(
        "/notifications", "/notification/", "/media/audio/ui", "notification_sound", "alert_tones"
    )
    private val recordingPathTokens = listOf(
        "/recordings", "/recording/", "/voice recorder", "/voicerecorder",
        "/sound_recorder", "/soundrecorder", "/callrecordings", "/call_recordings",
        "/call_rec", "/voicememos", "/voice_memos", "/whatsapp voice notes",
        "/whatsapp audio", "/telegram audio", "/miui/sound_recorder", "/music/recordings"
    )
    private val podcastPathTokens = listOf(
        "/podcasts", "/podcast/"
    )
    private val audiobookPathTokens = listOf(
        "/audiobooks", "/audiobook/", "/audible", "/spokenword"
    )
    private val musicPathTokens = listOf(
        "/music", "/songs", "/albums", "/download", "/downloads", "play mix studio demos", "playmix_demo_audio"
    )
    private val ambiguousFolderTokens = listOf(
        "temp", "tmp", "cache", "unknown", "misc"
    )
    private val standardMusicExtensions = setOf(
        "mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "oga", "weba"
    )

    private val recordingFilePrefixes = listOf(
        "rec_", "rec-", "voice_", "voice-", "vn-", "ptt-", "aud-",
        "call_recording", "callrec_", "memo_", "recording_"
    )
    private val alarmFilePrefixes = listOf("alarm_", "wakeup_", "wake_up_")
    private val ringtoneFilePrefixes = listOf("ringtone_", "ring_")
    private val notificationFilePrefixes = listOf("notif_", "notification_", "ding_", "chime_", "alert_")

    fun classify(input: AudioClassificationInput): AudioClassificationResult {
        // 1. User-defined manual override always takes top priority
        if (input.manualOverrideCategory != null) {
            return AudioClassificationResult(
                primaryCategory = input.manualOverrideCategory,
                reason = "User manual classification override (${input.manualOverrideCategory.displayName})",
                isManualOverride = true
            )
        }

        val combinedPath = "${input.filePath}/${input.relativePath}/${input.folderName}".lowercase()
        val fileNameLower = input.fileName.lowercase().ifBlank {
            input.filePath.substringAfterLast('/').lowercase()
        }
        val titleLower = input.title.lowercase().trim()
        val genreLower = input.genre.lowercase().trim()
        val albumLower = input.album.lowercase().trim()
        val mimeLower = input.mimeType.lowercase().trim()
        val ext = fileNameLower.substringAfterLast('.', "")

        // 2. ALARMS (MediaStore IS_ALARM or recognized alarm directories/filenames)
        if (input.isAlarmFlag) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.ALARMS,
                reason = "MediaStore IS_ALARM flag",
                isManualOverride = false
            )
        }
        if (alarmPathTokens.any { combinedPath.contains(it) } ||
            alarmFilePrefixes.any { fileNameLower.startsWith(it) || titleLower.startsWith(it) } ||
            genreLower == "alarm"
        ) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.ALARMS,
                reason = "Recognized Alarm directory or filename pattern",
                isManualOverride = false
            )
        }

        // 3. RINGTONES (MediaStore IS_RINGTONE or recognized ringtone directories/filenames)
        if (input.isRingtoneFlag) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.RINGTONES,
                reason = "MediaStore IS_RINGTONE flag",
                isManualOverride = false
            )
        }
        if (ringtonePathTokens.any { combinedPath.contains(it) } ||
            ringtoneFilePrefixes.any { fileNameLower.startsWith(it) || titleLower.startsWith(it) } ||
            genreLower == "ringtone"
        ) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.RINGTONES,
                reason = "Recognized Ringtone directory or filename pattern",
                isManualOverride = false
            )
        }

        // 4. NOTIFICATION SOUNDS (MediaStore IS_NOTIFICATION or notification directories/short alert clips)
        if (input.isNotificationFlag) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.NOTIFICATION_SOUNDS,
                reason = "MediaStore IS_NOTIFICATION flag",
                isManualOverride = false
            )
        }
        if (notificationPathTokens.any { combinedPath.contains(it) } ||
            notificationFilePrefixes.any { fileNameLower.startsWith(it) || titleLower.startsWith(it) } ||
            genreLower == "notification"
        ) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.NOTIFICATION_SOUNDS,
                reason = "Recognized Notification directory or alert tone pattern",
                isManualOverride = false
            )
        }

        // 5. VOICE RECORDINGS (MediaStore IS_RECORDING, recording folders, AMR/3GPP speech MIME, or voice note patterns)
        if (input.isRecordingFlag) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.VOICE_RECORDINGS,
                reason = "MediaStore IS_RECORDING flag",
                isManualOverride = false
            )
        }
        if (recordingPathTokens.any { combinedPath.contains(it) } ||
            recordingFilePrefixes.any { fileNameLower.startsWith(it) || titleLower.startsWith(it) } ||
            mimeLower == "audio/amr" || mimeLower == "audio/3gpp" || mimeLower == "audio/amr-wb" ||
            ext == "amr" || ext == "3gp" || genreLower.contains("voice recording") || genreLower.contains("voice memo")
        ) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.VOICE_RECORDINGS,
                reason = "Recognized Voice Recording folder, speech MIME type, or voice-note filename",
                isManualOverride = false
            )
        }

        // 6. AUDIOBOOKS (MediaStore IS_AUDIOBOOK, .m4b container, or recognized audiobook folders/metadata)
        if (input.isAudiobookFlag || ext == "m4b" || mimeLower == "audio/mp4a-latm-audiobook") {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.AUDIOBOOKS,
                reason = if (input.isAudiobookFlag) "MediaStore IS_AUDIOBOOK flag" else "Audiobook M4B container",
                isManualOverride = false
            )
        }
        if (audiobookPathTokens.any { combinedPath.contains(it) } ||
            genreLower.contains("audiobook") ||
            albumLower.contains("audiobook") ||
            titleLower.contains("audiobook")
        ) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.AUDIOBOOKS,
                reason = "Recognized Audiobook directory or metadata tag",
                isManualOverride = false
            )
        }

        // 7. PODCASTS (MediaStore IS_PODCAST or recognized podcast folders/metadata)
        if (input.isPodcastFlag) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.PODCASTS,
                reason = "MediaStore IS_PODCAST flag",
                isManualOverride = false
            )
        }
        if (podcastPathTokens.any { combinedPath.contains(it) } ||
            genreLower.contains("podcast") ||
            albumLower.contains("podcast") ||
            titleLower.contains("podcast ep")
        ) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.PODCASTS,
                reason = "Recognized Podcast directory or episode metadata",
                isManualOverride = false
            )
        }

        // 8. AMBIGUOUS / UNCERTAIN CHECK BEFORE CLASSIFYING AS MUSIC
        val hasKnownArtist = input.artist.isNotBlank() &&
            !input.artist.equals("Unknown Artist", ignoreCase = true) &&
            !input.artist.equals("<unknown>", ignoreCase = true)
        val hasKnownAlbum = input.album.isNotBlank() &&
            !input.album.equals("Unknown Album", ignoreCase = true) &&
            !input.album.equals("<unknown>", ignoreCase = true) &&
            !input.album.equals("Download", ignoreCase = true) &&
            !input.album.equals("Downloads", ignoreCase = true)
        val isGenericTitle = titleLower.matches(Regex("^(audio|track|clip|file|sample|sound|unknown|tmp)[_\\-\\s]?\\d*.*"))
        val isInAmbiguousFolder = ambiguousFolderTokens.any { input.folderName.lowercase().contains(it) }
        val isInMusicOrDownloadFolder = musicPathTokens.any { combinedPath.contains(it) }
        val isStandardAudioFormat = ext in standardMusicExtensions || mimeLower.startsWith("audio/")

        // Very short audio (< 12 seconds) without explicit music metadata is never auto-classified as music
        if (input.durationMs in 1..11_999L && !hasKnownArtist) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.UNCLASSIFIED,
                reason = "Short duration (${input.durationMs / 1000}s) without artist metadata — routed to Unclassified",
                isManualOverride = false
            )
        }

        // Generic machine/clip filenames (e.g., audio_clip_8841, tmp_99182) without artist/album metadata -> Unclassified
        if (!hasKnownArtist && !hasKnownAlbum && isGenericTitle) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.UNCLASSIFIED,
                reason = "Ambiguous filename and missing artist/album tags — routed to Unclassified",
                isManualOverride = false
            )
        }

        // 9. CONFIDENT MUSIC IDENTIFICATION (Includes songs downloaded to /Download or /Music)
        if (input.isMusicFlag && !isGenericTitle && !isInAmbiguousFolder && input.durationMs >= 12_000L) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.MUSIC,
                reason = "MediaStore IS_MUSIC flag + verified song format",
                isManualOverride = false
            )
        }

        if (isInMusicOrDownloadFolder && isStandardAudioFormat && !isGenericTitle && !isInAmbiguousFolder && input.durationMs >= 15_000L) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.MUSIC,
                reason = "Recognized Music/Download directory + song duration",
                isManualOverride = false
            )
        }

        if ((hasKnownArtist || hasKnownAlbum) && isStandardAudioFormat && input.durationMs in 15_000L..2_400_000L && !isInAmbiguousFolder) {
            return AudioClassificationResult(
                primaryCategory = AudioCategory.MUSIC,
                reason = "Artist/Album metadata with song duration",
                isManualOverride = false
            )
        }

        // 10. FALLBACK: Never classify an uncertain file as MUSIC automatically
        return AudioClassificationResult(
            primaryCategory = AudioCategory.UNCLASSIFIED,
            reason = "Uncertain audio origin or incomplete metadata — requires user classification",
            isManualOverride = false
        )
    }
}
