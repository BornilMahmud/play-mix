package com.example

import com.example.data.library.AudioClassificationEngine
import com.example.data.library.AudioClassificationInput
import com.example.data.lyrics.LyricsRepository
import com.example.domain.model.AccentColorOption
import com.example.domain.model.AppLanguage
import com.example.domain.model.AudioCategory
import com.example.domain.model.EqualizerPreset
import com.example.domain.model.LyricSource
import com.example.domain.model.OnlineProviderId
import com.example.domain.model.ReplayGainMode
import com.example.domain.model.formatDurationMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `formatDurationMs formats minutes and hours accurately`() {
        assertEquals("00:00", formatDurationMs(0L))
        assertEquals("03:45", formatDurationMs(225_000L))
        assertEquals("1:02:05", formatDurationMs(3_725_000L))
    }

    @Test
    fun `parseLrcToDomain parses line and word-by-word timestamps`() {
        val raw = """
            [00:01.50] <00:01.60>Neon <00:02.10>skies <00:02.80>tonight
            [00:05.00] Second line of lyrics
        """.trimIndent()

        val parsed = LyricsRepository.parseLrcToDomain(
            songId = "demo_1",
            rawContent = raw,
            offsetMs = 0L,
            source = LyricSource.DEMO
        )

        assertTrue(parsed.isSynced)
        assertEquals(2, parsed.lines.size)
        assertEquals(1500L, parsed.lines[0].timestampMs)
        assertEquals("Neon skies tonight", parsed.lines[0].text)
        assertEquals(3, parsed.lines[0].words.size)
        assertEquals("Neon", parsed.lines[0].words[0].text)
        assertEquals(1600L, parsed.lines[0].words[0].timestampMs)
    }

    @Test
    fun `equalizer presets have 5 normalized frequency bands`() {
        EqualizerPreset.entries.forEach { preset ->
            assertEquals(5, preset.bandGainsPercent.size)
        }
    }

    @Test
    fun `expanded domain enums for ReplayGain, AccentColor, AppLanguage, OnlineProviderId, and AudioCategory are complete`() {
        assertEquals(3, ReplayGainMode.entries.size)
        assertEquals(5, AccentColorOption.entries.size)
        assertEquals(2, AppLanguage.entries.size)
        assertEquals(5, OnlineProviderId.entries.size)
        assertEquals(8, AudioCategory.entries.size)
    }

    @Test
    fun `AudioClassificationEngine strictly separates all 8 categories and never auto-classifies uncertain files as MUSIC`() {
        // 1. Conflicting IS_MUSIC + IS_ALARM -> ALARMS wins via explicit precedence
        val alarmConflict = AudioClassificationEngine.classify(
            AudioClassificationInput(
                title = "Sunrise Wake Chime",
                artist = "<unknown>",
                album = "Alarms",
                genre = "Alarm",
                fileName = "sunrise_alarm.ogg",
                filePath = "/storage/emulated/0/Alarms/sunrise_alarm.ogg",
                folderName = "Alarms",
                mimeType = "audio/ogg",
                durationMs = 15_000L,
                isMusicFlag = true,
                isAlarmFlag = true
            )
        )
        assertEquals(AudioCategory.ALARMS, alarmConflict.primaryCategory)

        // 2. Voice recording in Recordings folder with IS_MUSIC=true -> VOICE_RECORDINGS wins
        val voiceRec = AudioClassificationEngine.classify(
            AudioClassificationInput(
                title = "AUD-20261105-WA0004",
                artist = "<unknown>",
                album = "WhatsApp Voice Notes",
                genre = "",
                fileName = "AUD-20261105-WA0004.opus",
                filePath = "/storage/emulated/0/Recordings/WhatsApp Voice Notes/AUD-20261105-WA0004.opus",
                folderName = "WhatsApp Voice Notes",
                mimeType = "audio/opus",
                durationMs = 42_000L,
                isMusicFlag = true,
                isRecordingFlag = false
            )
        )
        assertEquals(AudioCategory.VOICE_RECORDINGS, voiceRec.primaryCategory)

        // 3. Ringtone -> RINGTONES
        val ringtone = AudioClassificationEngine.classify(
            AudioClassificationInput(
                title = "Realme Tune",
                artist = "Realme",
                album = "Ringtones",
                genre = "Ringtone",
                fileName = "realme_tune.ogg",
                filePath = "/system/media/audio/ringtones/realme_tune.ogg",
                folderName = "ringtones",
                mimeType = "audio/ogg",
                durationMs = 24_000L,
                isRingtoneFlag = true
            )
        )
        assertEquals(AudioCategory.RINGTONES, ringtone.primaryCategory)

        // 4. Notification sound -> NOTIFICATION_SOUNDS
        val notif = AudioClassificationEngine.classify(
            AudioClassificationInput(
                title = "Crystal Ping",
                artist = "<unknown>",
                album = "Notifications",
                genre = "Notification",
                fileName = "notif_ping.wav",
                filePath = "/storage/emulated/0/Notifications/notif_ping.wav",
                folderName = "Notifications",
                mimeType = "audio/wav",
                durationMs = 2_000L,
                isNotificationFlag = true
            )
        )
        assertEquals(AudioCategory.NOTIFICATION_SOUNDS, notif.primaryCategory)

        // 5. Podcast -> PODCASTS
        val podcast = AudioClassificationEngine.classify(
            AudioClassificationInput(
                title = "Kotlin Audio Engineering Ep 42",
                artist = "Android Dev Cast",
                album = "Podcasts",
                genre = "Podcast",
                fileName = "kotlin_podcast_ep42.mp3",
                filePath = "/storage/emulated/0/Podcasts/kotlin_podcast_ep42.mp3",
                folderName = "Podcasts",
                mimeType = "audio/mpeg",
                durationMs = 1_800_000L,
                isPodcastFlag = true
            )
        )
        assertEquals(AudioCategory.PODCASTS, podcast.primaryCategory)

        // 6. Audiobook (.m4b) -> AUDIOBOOKS
        val audiobook = AudioClassificationEngine.classify(
            AudioClassificationInput(
                title = "The Art of Sound - Chapter 01",
                artist = "A. Author",
                album = "Audiobooks",
                genre = "Audiobook",
                fileName = "art_of_sound_ch01.m4b",
                filePath = "/storage/emulated/0/Audiobooks/art_of_sound_ch01.m4b",
                folderName = "Audiobooks",
                mimeType = "audio/mp4a-latm",
                durationMs = 950_000L
            )
        )
        assertEquals(AudioCategory.AUDIOBOOKS, audiobook.primaryCategory)

        // 7. Confident Music track -> MUSIC
        val music = AudioClassificationEngine.classify(
            AudioClassificationInput(
                title = "Electric Horizon",
                artist = "Nova Pulse",
                album = "Midnight Drive",
                genre = "Synthwave",
                fileName = "01_electric_horizon.flac",
                filePath = "/storage/emulated/0/Music/Nova Pulse/01_electric_horizon.flac",
                folderName = "Nova Pulse",
                mimeType = "audio/flac",
                durationMs = 215_000L,
                isMusicFlag = true
            )
        )
        assertEquals(AudioCategory.MUSIC, music.primaryCategory)

        // 8. Uncertain / ambiguous file -> UNCLASSIFIED (never auto-classified as MUSIC)
        val uncertain = AudioClassificationEngine.classify(
            AudioClassificationInput(
                title = "audio_99182",
                artist = "<unknown>",
                album = "<unknown>",
                genre = "",
                fileName = "audio_99182.bin",
                filePath = "/storage/emulated/0/Download/misc/audio_99182.bin",
                folderName = "Download",
                mimeType = "application/octet-stream",
                durationMs = 3_100L,
                isMusicFlag = false
            )
        )
        assertEquals(AudioCategory.UNCLASSIFIED, uncertain.primaryCategory)
        assertNotEquals(AudioCategory.MUSIC, uncertain.primaryCategory)

        // 9. User manual override -> takes highest precedence over all automatic rules
        val manualOverride = AudioClassificationEngine.classify(
            AudioClassificationInput(
                title = "Sunrise Wake Chime",
                artist = "<unknown>",
                album = "Alarms",
                genre = "Alarm",
                fileName = "sunrise_alarm.ogg",
                filePath = "/storage/emulated/0/Alarms/sunrise_alarm.ogg",
                folderName = "Alarms",
                mimeType = "audio/ogg",
                durationMs = 15_000L,
                isMusicFlag = false,
                isAlarmFlag = true,
                manualOverrideCategory = AudioCategory.MUSIC
            )
        )
        assertEquals(AudioCategory.MUSIC, manualOverride.primaryCategory)
        assertTrue(manualOverride.isManualOverride)
    }
}
