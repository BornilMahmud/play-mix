package com.example.data.library

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.core.database.LyricsEntity
import com.example.core.database.SongEntity
import com.example.domain.model.AudioCategory
import com.example.domain.model.LyricSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object DemoAudioSeeder {

    data class DemoSeedResult(
        val songs: List<SongEntity>,
        val lyrics: List<LyricsEntity>
    )

    suspend fun ensureDemoTracksSeeded(context: Context): DemoSeedResult = withContext(Dispatchers.IO) {
        val demoDir = File(context.filesDir, "playmix_demo_audio").apply {
            if (!exists()) mkdirs()
        }

        val neonCoverUri = "android.resource://${context.packageName}/${R.drawable.img_album_neon_horizon_1793893899079}"
        val velvetCoverUri = "android.resource://${context.packageName}/${R.drawable.img_album_velvet_echoes_1793893928723}"

        val specs = listOf(
            // --- 6 STUDIO MUSIC TRACKS (AudioCategory.MUSIC) ---
            DemoTrackSpec(
                id = "demo_electric_horizon",
                fileName = "01_electric_horizon.wav",
                title = "Electric Horizon",
                artist = "Nova Pulse",
                album = "Neon Horizon",
                genre = "Synthwave",
                folderName = "Play Mix Studio Demos",
                category = AudioCategory.MUSIC,
                classificationReason = "MediaStore IS_MUSIC flag + verified studio metadata",
                trackNumber = 1,
                year = 2026,
                durationSec = 36,
                artworkUri = neonCoverUri,
                chordFreqs = listOf(
                    doubleArrayOf(110.0, 220.0, 261.63, 329.63, 659.25), // Am
                    doubleArrayOf(87.31, 174.61, 220.0, 261.63, 523.25), // F
                    doubleArrayOf(65.41, 130.81, 261.63, 329.63, 783.99), // C
                    doubleArrayOf(98.00, 196.00, 246.94, 293.66, 587.33)  // G
                ),
                tempoBpm = 118,
                lrcContent = """
                    [00:00.00] <00:00.20>Neon <00:01.00>skies <00:01.80>awaken <00:02.60>in <00:03.10>the <00:03.50>night
                    [00:04.50] <00:04.70>Electric <00:05.50>violet <00:06.30>floods <00:07.10>the <00:07.60>city <00:08.20>light
                    [00:09.00] <00:09.20>Every <00:10.00>frequency <00:11.00>begins <00:11.80>to <00:12.40>hum
                    [00:13.50] <00:13.70>Tracing <00:14.50>waves <00:15.30>of <00:15.90>what <00:16.50>we <00:17.10>have <00:17.60>become
                    [00:18.00] <00:18.20>Lock <00:18.90>the <00:19.40>screen <00:20.20>and <00:20.80>let <00:21.30>the <00:21.80>rhythm <00:22.40>drive
                    [00:22.80] <00:23.00>Through <00:23.70>the <00:24.20>midnight <00:25.10>pulse <00:25.80>we <00:26.30>feel <00:26.80>alive
                    [00:27.50] <00:27.70>Soft <00:28.50>cyan <00:29.40>echoes <00:30.30>on <00:30.90>the <00:31.40>open <00:31.90>road
                    [00:32.20] <00:32.50>Drifting <00:33.30>past <00:34.00>the <00:34.60>stars <00:35.20>in <00:35.60>stereo
                """.trimIndent()
            ),
            DemoTrackSpec(
                id = "demo_cyan_drift",
                fileName = "02_cyan_drift.wav",
                title = "Cyan Drift",
                artist = "Nova Pulse",
                album = "Neon Horizon",
                genre = "Electronic",
                folderName = "Play Mix Studio Demos",
                category = AudioCategory.MUSIC,
                classificationReason = "MediaStore IS_MUSIC flag + verified studio metadata",
                trackNumber = 2,
                year = 2026,
                durationSec = 34,
                artworkUri = neonCoverUri,
                chordFreqs = listOf(
                    doubleArrayOf(73.42, 146.83, 293.66, 349.23, 440.0),  // Dm
                    doubleArrayOf(58.27, 116.54, 233.08, 293.66, 349.23), // Bb
                    doubleArrayOf(87.31, 174.61, 261.63, 349.23, 523.25), // F
                    doubleArrayOf(65.41, 130.81, 261.63, 329.63, 392.00)  // C
                ),
                tempoBpm = 124,
                lrcContent = """
                    [00:00.00] <00:00.30>Drifting <00:01.20>through <00:01.90>an <00:02.40>obsidian <00:03.40>sea
                    [00:04.20] <00:04.50>Sub-bass <00:05.40>rolling <00:06.20>wild <00:06.90>and <00:07.50>free
                    [00:08.50] <00:08.80>Crystal <00:09.60>highs <00:10.40>and <00:11.00>velvet <00:11.80>tone
                    [00:12.80] <00:13.10>Playing <00:13.90>on <00:14.50>when <00:15.10>you're <00:15.80>alone
                    [00:17.00] <00:17.30>Dial <00:18.10>the <00:18.70>equalizer <00:19.70>to <00:20.20>the <00:20.70>sky
                    [00:21.50] <00:21.80>Watch <00:22.60>the <00:23.20>cyan <00:24.00>spectrum <00:24.80>fly
                    [00:26.00] <00:26.40>Zero <00:27.20>dropouts <00:28.10>in <00:28.70>the <00:29.20>dark
                    [00:30.00] <00:30.40>Every <00:31.20>beat <00:31.90>leaves <00:32.50>a <00:33.10>spark
                """.trimIndent()
            ),
            DemoTrackSpec(
                id = "demo_velvet_echoes",
                fileName = "03_velvet_echoes.wav",
                title = "Velvet Echoes",
                artist = "Aether & Glass",
                album = "Velvet Echoes",
                genre = "Ambient",
                folderName = "Play Mix Studio Demos",
                category = AudioCategory.MUSIC,
                classificationReason = "MediaStore IS_MUSIC flag + verified studio metadata",
                trackNumber = 1,
                year = 2025,
                durationSec = 38,
                artworkUri = velvetCoverUri,
                chordFreqs = listOf(
                    doubleArrayOf(82.41, 164.81, 246.94, 329.63, 493.88), // Em
                    doubleArrayOf(65.41, 130.81, 196.00, 246.94, 392.00), // Cmaj7
                    doubleArrayOf(98.00, 196.00, 293.66, 392.00, 587.33), // G
                    doubleArrayOf(73.42, 146.83, 220.00, 293.66, 369.99)  // D
                ),
                tempoBpm = 92,
                lrcContent = """
                    [00:00.00] <00:00.40>Glass <00:01.30>spheres <00:02.20>floating <00:03.10>in <00:03.70>the <00:04.20>void
                    [00:05.00] <00:05.30>Pure <00:06.10>acoustics <00:07.20>unalloyed
                    [00:09.50] <00:09.80>Warm <00:10.60>harmonics <00:11.60>softly <00:12.50>bloom
                    [00:14.00] <00:14.30>Filling <00:15.10>every <00:16.00>quiet <00:16.90>room
                    [00:19.00] <00:19.30>Close <00:20.10>your <00:20.80>eyes <00:21.60>and <00:22.80>let <00:23.40>it <00:23.80>play
                    [00:24.20] <00:24.50>As <00:25.20>the <00:25.90>outside <00:26.80>fades <00:27.60>away
                    [00:29.00] <00:29.40>Velvet <00:30.30>echoes <00:31.20>in <00:31.90>the <00:32.50>night
                    [00:33.50] <00:33.90>Guided <00:34.80>by <00:35.50>a <00:36.10>sapphire <00:37.00>light
                """.trimIndent()
            ),
            DemoTrackSpec(
                id = "demo_sapphire_nocturne",
                fileName = "04_sapphire_nocturne.wav",
                title = "Sapphire Nocturne",
                artist = "Aether & Glass",
                album = "Velvet Echoes",
                genre = "Lo-Fi Jazz",
                folderName = "Play Mix Studio Demos",
                category = AudioCategory.MUSIC,
                classificationReason = "MediaStore IS_MUSIC flag + verified studio metadata",
                trackNumber = 2,
                year = 2025,
                durationSec = 32,
                artworkUri = velvetCoverUri,
                chordFreqs = listOf(
                    doubleArrayOf(65.41, 130.81, 196.00, 246.94, 329.63),
                    doubleArrayOf(55.00, 110.00, 164.81, 220.00, 261.63),
                    doubleArrayOf(73.42, 146.83, 220.00, 261.63, 349.23),
                    doubleArrayOf(98.00, 196.00, 246.94, 293.66, 349.23)
                ),
                tempoBpm = 86,
                lrcContent = """
                    [00:00.00] Late night rain against the window pane
                    [00:04.50] Vinyl grooves and a gentle refrain
                    [00:09.00] Sapphire chords on a midnight train
                    [00:13.50] Washing away all the static and strain
                    [00:18.00] Brass and keys in a mellow embrace
                    [00:22.50] Finding calm in a timeless space
                    [00:27.00] Play Mix spinning till the break of dawn
                """.trimIndent()
            ),
            DemoTrackSpec(
                id = "demo_sub_gravity",
                fileName = "05_sub_gravity.wav",
                title = "Sub Gravity (Bass Test)",
                artist = "Kinetix Audio Lab",
                album = "Neon Horizon",
                genre = "Bass & Dub",
                folderName = "Play Mix Studio Demos",
                category = AudioCategory.MUSIC,
                classificationReason = "MediaStore IS_MUSIC flag + verified studio metadata",
                trackNumber = 3,
                year = 2026,
                durationSec = 30,
                artworkUri = neonCoverUri,
                chordFreqs = listOf(
                    doubleArrayOf(55.00, 110.00, 164.81, 220.00, 880.00),
                    doubleArrayOf(49.00, 98.00, 146.83, 196.00, 783.99),
                    doubleArrayOf(43.65, 87.31, 130.81, 174.61, 698.46),
                    doubleArrayOf(65.41, 130.81, 196.00, 261.63, 1046.50)
                ),
                tempoBpm = 132,
                lrcContent = """
                    [00:00.00] Sub-60 Hertz low-end calibration
                    [00:05.00] Feel the bass boost resonance vibration
                    [00:10.00] Clean headroom across all five bands
                    [00:15.00] Precision audio in the palm of your hands
                    [00:20.00] From deep sub-bass to the 14 kilohertz air
                    [00:25.00] Studio reference sound everywhere
                """.trimIndent()
            ),
            DemoTrackSpec(
                id = "demo_solaris_overture",
                fileName = "06_solaris_overture.wav",
                title = "Solaris Overture",
                artist = "Kinetix Audio Lab",
                album = "Velvet Echoes",
                genre = "Classical Crossover",
                folderName = "Play Mix Studio Demos",
                category = AudioCategory.MUSIC,
                classificationReason = "MediaStore IS_MUSIC flag + verified studio metadata",
                trackNumber = 3,
                year = 2026,
                durationSec = 35,
                artworkUri = velvetCoverUri,
                chordFreqs = listOf(
                    doubleArrayOf(98.00, 196.00, 293.66, 392.00, 783.99),
                    doubleArrayOf(73.42, 146.83, 220.00, 369.99, 739.99),
                    doubleArrayOf(82.41, 164.81, 246.94, 329.63, 659.25),
                    doubleArrayOf(65.41, 130.81, 196.00, 329.63, 659.25)
                ),
                tempoBpm = 108,
                lrcContent = """
                    [00:00.00] Strings and synths in rising harmony
                    [00:05.00] Across the horizon of a golden sea
                    [00:10.00] Seamless transitions from track to track
                    [00:15.00] Pure AMOLED contrast in pitch black
                    [00:20.00] Every crescendo rings crystal clear
                    [00:25.00] The soundtrack to your atmosphere
                    [00:30.00] Solaris shining through the night
                """.trimIndent()
            ),

            // --- NON-MUSIC CATEGORY SAMPLES (STRICTLY SEPARATED FROM MUSIC LIBRARY) ---
            DemoTrackSpec(
                id = "cat_alarm_sunrise_chime",
                fileName = "alarm_sunrise_harmonic.wav",
                title = "Sunrise Harmonic Alarm",
                artist = "Realme System Tones",
                album = "Alarms",
                genre = "Alarm",
                folderName = "Alarms",
                category = AudioCategory.ALARMS,
                classificationReason = "MediaStore IS_ALARM flag & /Alarms folder",
                trackNumber = 1,
                year = 2026,
                durationSec = 14,
                artworkUri = neonCoverUri,
                chordFreqs = listOf(
                    doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
                ),
                tempoBpm = 128,
                lrcContent = "[00:00.00] Gentle harmonic wake-up chime"
            ),
            DemoTrackSpec(
                id = "cat_recording_studio_memo",
                fileName = "voice_memo_2026_session.wav",
                title = "Voice Memo — Synth Patch Notes",
                artist = "Voice Recorder",
                album = "Recordings",
                genre = "Voice Memo",
                folderName = "Recordings",
                category = AudioCategory.VOICE_RECORDINGS,
                classificationReason = "MediaStore IS_RECORDING flag & /Recordings directory",
                trackNumber = 1,
                year = 2026,
                durationSec = 16,
                artworkUri = velvetCoverUri,
                chordFreqs = listOf(
                    doubleArrayOf(196.00, 246.94, 293.66, 392.00, 493.88)
                ),
                tempoBpm = 96,
                lrcContent = "[00:00.00] Field voice memo recorded in Voice Recorder"
            ),
            DemoTrackSpec(
                id = "cat_ringtone_cyber_pulse",
                fileName = "ringtone_cyber_pulse.wav",
                title = "Cyber Pulse Ringtone",
                artist = "Play Mix System Audio",
                album = "Ringtones",
                genre = "Ringtone",
                folderName = "Ringtones",
                category = AudioCategory.RINGTONES,
                classificationReason = "MediaStore IS_RINGTONE flag & /Ringtones folder",
                trackNumber = 1,
                year = 2026,
                durationSec = 15,
                artworkUri = neonCoverUri,
                chordFreqs = listOf(
                    doubleArrayOf(440.0, 554.37, 659.25, 880.0, 1108.73)
                ),
                tempoBpm = 136,
                lrcContent = "[00:00.00] Incoming call ringtone sequence"
            ),
            DemoTrackSpec(
                id = "cat_notification_crystal_ping",
                fileName = "notif_crystal_ping.wav",
                title = "Crystal Ping Alert",
                artist = "System UI",
                album = "Notifications",
                genre = "Notification",
                folderName = "Notifications",
                category = AudioCategory.NOTIFICATION_SOUNDS,
                classificationReason = "MediaStore IS_NOTIFICATION flag & /Notifications folder",
                trackNumber = 1,
                year = 2026,
                durationSec = 6,
                artworkUri = velvetCoverUri,
                chordFreqs = listOf(
                    doubleArrayOf(880.0, 1174.66, 1318.51, 1760.0, 2093.0)
                ),
                tempoBpm = 140,
                lrcContent = "[00:00.00] Short notification chime"
            ),
            DemoTrackSpec(
                id = "cat_podcast_audio_engineering",
                fileName = "podcast_ep12_mastering_headroom.wav",
                title = "Ep. 12 — Mastering Headroom & ReplayGain",
                artist = "Audio Engineering Weekly",
                album = "Podcasts",
                genre = "Podcast",
                folderName = "Podcasts",
                category = AudioCategory.PODCASTS,
                classificationReason = "MediaStore IS_PODCAST flag & /Podcasts folder",
                trackNumber = 12,
                year = 2026,
                durationSec = 24,
                artworkUri = neonCoverUri,
                chordFreqs = listOf(
                    doubleArrayOf(130.81, 196.00, 246.94, 329.63, 392.00)
                ),
                tempoBpm = 90,
                lrcContent = "[00:00.00] Welcome back to Audio Engineering Weekly Episode 12"
            ),
            DemoTrackSpec(
                id = "cat_audiobook_architecture_of_sound",
                fileName = "audiobook_ch01_architecture_of_sound.wav",
                title = "Chapter 1 — The Architecture of Sound",
                artist = "Dr. Clara Vance",
                album = "Audiobooks",
                genre = "Audiobook",
                folderName = "Audiobooks",
                category = AudioCategory.AUDIOBOOKS,
                classificationReason = "MediaStore IS_AUDIOBOOK flag & /Audiobooks folder",
                trackNumber = 1,
                year = 2026,
                durationSec = 26,
                artworkUri = velvetCoverUri,
                chordFreqs = listOf(
                    doubleArrayOf(146.83, 220.00, 261.63, 329.63, 440.00)
                ),
                tempoBpm = 84,
                lrcContent = "[00:00.00] Chapter One: The Architecture of Sound"
            ),
            DemoTrackSpec(
                id = "cat_unclassified_mystery_sample",
                fileName = "audio_sample_8842.wav",
                title = "audio_sample_8842",
                artist = "Unknown Artist",
                album = "Unknown Album",
                genre = "Unknown",
                folderName = "Download",
                category = AudioCategory.UNCLASSIFIED,
                classificationReason = "Ambiguous filename and missing artist/album tags — routed to Unclassified",
                trackNumber = 1,
                year = 2026,
                durationSec = 12,
                artworkUri = neonCoverUri,
                chordFreqs = listOf(
                    doubleArrayOf(164.81, 246.94, 329.63, 493.88, 659.25)
                ),
                tempoBpm = 110,
                lrcContent = "[00:00.00] Unclassified audio sample in Download folder"
            )
        )

        val now = System.currentTimeMillis()
        val songs = mutableListOf<SongEntity>()
        val lyrics = mutableListOf<LyricsEntity>()

        specs.forEachIndexed { index, spec ->
            val categorySubDir = File(demoDir, spec.folderName.lowercase().replace(" ", "_")).apply {
                if (!exists()) mkdirs()
            }
            val targetFile = File(categorySubDir, spec.fileName)
            if (!targetFile.exists() || targetFile.length() < 1024L) {
                writeSynthesizedStereoWav(
                    file = targetFile,
                    durationSec = spec.durationSec,
                    chordFreqs = spec.chordFreqs,
                    tempoBpm = spec.tempoBpm
                )
            }
            val fileUri = Uri.fromFile(targetFile).toString()
            val isMusicTrack = spec.category == AudioCategory.MUSIC
            songs.add(
                SongEntity(
                    id = spec.id,
                    mediaStoreId = -(index + 1L),
                    uri = fileUri,
                    title = spec.title,
                    artist = spec.artist,
                    album = spec.album,
                    albumArtist = spec.artist,
                    genre = spec.genre,
                    durationMs = spec.durationSec * 1000L,
                    trackNumber = spec.trackNumber,
                    year = spec.year,
                    albumArtUri = spec.artworkUri,
                    filePath = targetFile.absolutePath,
                    folderName = spec.folderName,
                    fileSize = targetFile.length(),
                    dateAdded = now - (index * 3600_000L),
                    isFavorite = isMusicTrack && index < 2,
                    isHidden = false,
                    playCount = if (isMusicTrack) (6 - index) * 3 else 1,
                    lastPlayedAt = if (isMusicTrack && index < 3) now - (index * 1800_000L) else 0L,
                    isDemoTrack = true,
                    embeddedLyrics = spec.lrcContent,
                    primaryCategory = spec.category.id,
                    originalCategory = spec.category.id,
                    isManualClassification = false,
                    classificationReason = spec.classificationReason,
                    mimeType = "audio/wav"
                )
            )
            lyrics.add(
                LyricsEntity(
                    songId = spec.id,
                    rawContent = spec.lrcContent,
                    isSynced = true,
                    offsetMs = 0L,
                    sourceName = LyricSource.DEMO.name,
                    updatedAt = now
                )
            )
        }

        DemoSeedResult(songs = songs, lyrics = lyrics)
    }

    private data class DemoTrackSpec(
        val id: String,
        val fileName: String,
        val title: String,
        val artist: String,
        val album: String,
        val genre: String,
        val folderName: String,
        val category: AudioCategory,
        val classificationReason: String,
        val trackNumber: Int,
        val year: Int,
        val durationSec: Int,
        val artworkUri: String,
        val chordFreqs: List<DoubleArray>,
        val tempoBpm: Int,
        val lrcContent: String
    )

    /**
     * Generates a clean, warm stereo 22050Hz 16-bit PCM WAV file with sub-bass, chord pads,
     * and arpeggiated high frequencies so Equalizer bands (60Hz..14kHz), Bass Boost, and Virtualizer
     * all have rich harmonic content to sculpt.
     */
    private fun writeSynthesizedStereoWav(
        file: File,
        durationSec: Int,
        chordFreqs: List<DoubleArray>,
        tempoBpm: Int
    ) {
        val sampleRate = 22050
        val numChannels = 2
        val bitsPerSample = 16
        val totalSamples = sampleRate * durationSec
        val dataSize = totalSamples * numChannels * (bitsPerSample / 8)
        val byteRate = sampleRate * numChannels * (bitsPerSample / 8)
        val blockAlign = (numChannels * (bitsPerSample / 8)).toShort()

        FileOutputStream(file).buffered(65536).use { out ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(Charsets.US_ASCII))
            header.putInt(36 + dataSize)
            header.put("WAVE".toByteArray(Charsets.US_ASCII))
            header.put("fmt ".toByteArray(Charsets.US_ASCII))
            header.putInt(16) // PCM chunk size
            header.putShort(1) // PCM format
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(byteRate)
            header.putShort(blockAlign)
            header.putShort(bitsPerSample.toShort())
            header.put("data".toByteArray(Charsets.US_ASCII))
            header.putInt(dataSize)
            out.write(header.array())

            val beatDurationSec = 60.0 / tempoBpm
            val barDurationSec = beatDurationSec * 4.0
            val buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val barIndex = (t / barDurationSec).toInt() % chordFreqs.size
                val chord = chordFreqs[barIndex]

                val beatPhase = (t % beatDurationSec) / beatDurationSec
                val arpStep = ((t / (beatDurationSec / 2.0)).toInt()) % chord.size

                // Sub-bass pulse (60-110Hz)
                val bassEnv = (1.0 - beatPhase * 0.65).coerceIn(0.2, 1.0)
                val subWave = sin(2.0 * PI * chord[0] * t) * 0.32 * bassEnv

                // Warm stereo chord pad (mid frequencies)
                val padLeft = (sin(2.0 * PI * chord[1] * t) + 0.7 * sin(2.0 * PI * chord[2] * t)) * 0.18
                val padRight = (sin(2.0 * PI * (chord[1] * 1.003) * t) + 0.7 * sin(2.0 * PI * chord[3] * t)) * 0.18

                // Arpeggio + high harmonic shimmer
                val arpNotePhase = (t % (beatDurationSec / 2.0)) / (beatDurationSec / 2.0)
                val arpEnv = (1.0 - arpNotePhase).coerceIn(0.0, 1.0)
                val arpFreq = chord[arpStep] * 2.0
                val arpWave = (sin(2.0 * PI * arpFreq * t) + 0.35 * sin(2.0 * PI * arpFreq * 3.0 * t)) * 0.16 * arpEnv

                // Smooth fade-in and fade-out
                val masterEnv = when {
                    t < 0.8 -> t / 0.8
                    t > durationSec - 1.2 -> ((durationSec - t) / 1.2).coerceIn(0.0, 1.0)
                    else -> 1.0
                }

                val leftVal = ((subWave + padLeft + arpWave * 0.9) * masterEnv * 28000.0)
                    .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                val rightVal = ((subWave + padRight + arpWave * 1.1) * masterEnv * 28000.0)
                    .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()

                if (buffer.remaining() < 4) {
                    out.write(buffer.array(), 0, buffer.position())
                    buffer.clear()
                }
                buffer.putShort(leftVal)
                buffer.putShort(rightVal)
            }
            if (buffer.position() > 0) {
                out.write(buffer.array(), 0, buffer.position())
            }
        }
    }
}
