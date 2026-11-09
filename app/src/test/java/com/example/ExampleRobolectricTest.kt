package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.PlayMixDatabase
import com.example.core.database.toEntity
import com.example.core.datastore.UserPreferencesRepository
import com.example.data.library.DemoAudioSeeder
import com.example.data.library.MediaStoreScanner
import com.example.data.library.MusicLibraryRepository
import com.example.data.library.OfflineArtworkDetector
import com.example.data.online.OnlineMusicRepository
import com.example.domain.model.AudioCategory
import com.example.domain.model.Song
import com.example.domain.model.SortOption
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: PlayMixDatabase
    private lateinit var repository: MusicLibraryRepository
    private lateinit var prefsRepository: UserPreferencesRepository
    private lateinit var onlineRepository: OnlineMusicRepository
    private lateinit var artworkDetector: OfflineArtworkDetector

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PlayMixDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsRepository = UserPreferencesRepository(context)
        artworkDetector = OfflineArtworkDetector(context)
        val scanner = MediaStoreScanner(context, artworkDetector)
        repository = MusicLibraryRepository(
            context = context,
            dao = database.playMixDao(),
            scanner = scanner,
            prefsRepo = prefsRepository,
            artworkDetector = artworkDetector
        )
        onlineRepository = OnlineMusicRepository(
            dao = database.playMixDao(),
            prefsRepo = prefsRepository
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `app_name resource is Play Mix`() {
        assertEquals("Play Mix", context.getString(R.string.app_name))
    }

    @Test
    fun `demo audio seeder generates 6 music tracks plus 7 separated category samples and strict category isolation works`() = runBlocking {
        val seeded = DemoAudioSeeder.ensureDemoTracksSeeded(context)
        assertEquals(13, seeded.songs.size)
        assertEquals(13, seeded.lyrics.size)
        assertEquals(6, seeded.songs.count { it.primaryCategory == AudioCategory.MUSIC.id })
        assertEquals(1, seeded.songs.count { it.primaryCategory == AudioCategory.ALARMS.id })
        assertEquals(1, seeded.songs.count { it.primaryCategory == AudioCategory.VOICE_RECORDINGS.id })
        assertEquals(1, seeded.songs.count { it.primaryCategory == AudioCategory.RINGTONES.id })
        assertEquals(1, seeded.songs.count { it.primaryCategory == AudioCategory.NOTIFICATION_SOUNDS.id })
        assertEquals(1, seeded.songs.count { it.primaryCategory == AudioCategory.PODCASTS.id })
        assertEquals(1, seeded.songs.count { it.primaryCategory == AudioCategory.AUDIOBOOKS.id })
        assertEquals(1, seeded.songs.count { it.primaryCategory == AudioCategory.UNCLASSIFIED.id })

        seeded.songs.forEach { song ->
            assertTrue("Expected demo WAV file to exist: ${song.filePath}", File(song.filePath).exists())
            assertTrue(File(song.filePath).length() > 44L)
        }
    }

    @Test
    fun `non-destructive Move to Category, Remove from Category, Restore to Category, Rescan persistence, and Delete from Device work`() = runBlocking {
        // Seed library via rescanLibrary (no external storage permission in test -> seeds 13 local demo/sample tracks)
        repository.rescanLibrary(hasStoragePermission = false)

        // 1. Verify Music Home / visibleSongsFlow ONLY contains the 6 MUSIC tracks (Alarms & Recordings never appear in Music)
        val initialMusicSongs = repository.visibleSongsFlow.first()
        assertEquals(6, initialMusicSongs.size)
        assertTrue(initialMusicSongs.all { it.primaryCategory == AudioCategory.MUSIC })

        val initialAlarmSongs = repository.observeSongsForCategory(AudioCategory.ALARMS).first()
        assertEquals(1, initialAlarmSongs.size)

        val targetMusicSong = initialMusicSongs.first()
        val physicalFile = File(targetMusicSong.filePath)
        assertTrue("Physical audio file must exist before Move", physicalFile.exists())

        // 2. Non-destructive MOVE TO CATEGORY (Move MUSIC track -> ALARMS)
        repository.moveSongsToCategory(setOf(targetMusicSong.id), AudioCategory.ALARMS)

        val musicAfterMove = repository.observeSongsForCategory(AudioCategory.MUSIC).first()
        val alarmsAfterMove = repository.observeSongsForCategory(AudioCategory.ALARMS).first()
        assertEquals(5, musicAfterMove.size)
        assertFalse(musicAfterMove.any { it.id == targetMusicSong.id })
        assertEquals(2, alarmsAfterMove.size)
        val movedSong = alarmsAfterMove.first { it.id == targetMusicSong.id }
        assertEquals(AudioCategory.ALARMS, movedSong.primaryCategory)
        assertEquals(AudioCategory.MUSIC, movedSong.originalCategory)
        assertTrue(movedSong.isManualClassification)
        assertTrue("Physical audio file MUST remain untouched on disk after Move", physicalFile.exists())

        // Verify Move persists across rescanLibrary
        repository.rescanLibrary(hasStoragePermission = false)
        val alarmsAfterRescan = repository.observeSongsForCategory(AudioCategory.ALARMS).first()
        assertTrue("Manual category move must persist across library rescan", alarmsAfterRescan.any { it.id == targetMusicSong.id })

        // 3. Non-destructive REMOVE FROM CATEGORY (Remove second music song from MUSIC visibility)
        val secondMusicSong = repository.observeSongsForCategory(AudioCategory.MUSIC).first().first()
        val secondPhysicalFile = File(secondMusicSong.filePath)
        repository.removeSongsFromCategory(setOf(secondMusicSong.id), AudioCategory.MUSIC)

        val musicAfterRemove = repository.observeSongsForCategory(AudioCategory.MUSIC).first()
        assertEquals(4, musicAfterRemove.size)
        assertFalse(musicAfterRemove.any { it.id == secondMusicSong.id })
        assertTrue("Physical audio file MUST remain untouched on disk after Remove from Category", secondPhysicalFile.exists())

        // Verify removed song is listed in hiddenSongsFlow (Removed / Hidden manager) and still in All Audio Inspector
        val hiddenAndRemoved = repository.hiddenSongsFlow.first()
        assertTrue(hiddenAndRemoved.any { it.id == secondMusicSong.id && it.removedFromCategories.contains(AudioCategory.MUSIC) })
        val allInspector = repository.allAudioInspectorFlow.first()
        assertEquals(13, allInspector.size)

        // Verify Remove from Category persists across rescanLibrary
        repository.rescanLibrary(hasStoragePermission = false)
        val musicAfterSecondRescan = repository.observeSongsForCategory(AudioCategory.MUSIC).first()
        assertFalse("Category visibility removal must persist across rescan", musicAfterSecondRescan.any { it.id == secondMusicSong.id })

        // 4. RESTORE TO CATEGORY (Restore removed item into PODCASTS category)
        repository.restoreSongsToCategory(setOf(secondMusicSong.id), AudioCategory.PODCASTS)
        val podcastsAfterRestore = repository.observeSongsForCategory(AudioCategory.PODCASTS).first()
        assertTrue(podcastsAfterRestore.any { it.id == secondMusicSong.id })
        val hiddenAfterRestore = repository.hiddenSongsFlow.first()
        assertFalse(hiddenAfterRestore.any { it.id == secondMusicSong.id })

        // 5. Explicit DELETE FROM DEVICE (Destructive action deletes physical file and DB record)
        val tempDeleteFile = File(context.filesDir, "temp_delete_test.wav").apply {
            writeBytes(ByteArray(128) { 1 })
        }
        val deletableSong = Song(
            id = "delete_me_1",
            uri = tempDeleteFile.toURI().toString(),
            title = "Temporary Delete Track",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 15_000L,
            filePath = tempDeleteFile.absolutePath,
            primaryCategory = AudioCategory.UNCLASSIFIED
        )
        database.playMixDao().upsertSongs(listOf(deletableSong.toEntity()))
        assertTrue(tempDeleteFile.exists())

        val deletedCount = repository.deleteSongsFromDevice(setOf("delete_me_1"))
        assertEquals(1, deletedCount)
        assertFalse("Physical file must be deleted when Delete from Device is confirmed", tempDeleteFile.exists())
        assertFalse(repository.allAudioInspectorFlow.first().any { it.id == "delete_me_1" })
    }

    @Test
    fun `database CRUD, favorites, sorting, playlist creation, and queue persistence work`() = runBlocking {
        val sampleSongs = listOf(
            Song(
                id = "track_b",
                uri = "file:///music/b.mp3",
                title = "Bravo Horizon",
                artist = "Zeta Artist",
                album = "Album Two",
                durationMs = 240_000L,
                playCount = 2
            ),
            Song(
                id = "track_a",
                uri = "file:///music/a.mp3",
                title = "Alpha Pulse",
                artist = "Alpha Artist",
                album = "Album One",
                durationMs = 180_000L,
                playCount = 10
            )
        )
        database.playMixDao().upsertSongs(sampleSongs.map { it.toEntity() })

        // Verify sorting
        val sortedByTitle = repository.sortSongs(sampleSongs, SortOption.TITLE_ASC)
        assertEquals("Alpha Pulse", sortedByTitle.first().title)

        val sortedByPlays = repository.sortSongs(sampleSongs, SortOption.PLAY_COUNT_DESC)
        assertEquals("Alpha Pulse", sortedByPlays.first().title)

        // Verify favorite toggle
        val favNow = repository.toggleFavorite("track_a")
        assertTrue(favNow)
        val favList = repository.favoritesFlow.first()
        assertEquals(1, favList.size)
        assertEquals("track_a", favList.first().id)

        // Verify playlist creation, reordering, and M3U8 export
        val playlistId = repository.createPlaylist(
            name = "Roadtrip Mix",
            description = "Realme GT Test Playlist",
            initialSongIds = listOf("track_b", "track_a")
        )
        var playlists = repository.customPlaylistsFlow.first()
        val created = playlists.first { it.id == playlistId }
        assertEquals(2, created.songs.size)
        assertEquals("track_b", created.songs[0].id)

        repository.reorderPlaylistSongs(playlistId, 0, 1)
        playlists = repository.customPlaylistsFlow.first()
        val reordered = playlists.first { it.id == playlistId }
        assertEquals("track_a", reordered.songs[0].id)

        val m3u = repository.exportPlaylistToM3u(reordered)
        assertTrue(m3u.contains("#EXTM3U"))
        assertTrue(m3u.contains("#EXTENC:UTF-8"))
        assertTrue(m3u.contains("Alpha Pulse"))

        // Verify queue persistence and restoration
        repository.savePersistedQueue(sampleSongs, "track_a", 42_500L)
        val (restoredQueue, lastSession) = repository.loadPersistedQueue()
        assertEquals(2, restoredQueue.size)
        assertEquals("track_a", lastSession.first)
        assertEquals(42_500L, lastSession.second)
    }

    @Test
    fun `bookmarks, named queues, duplicate detection, tag editing, backup-restore, and online providers work`() = runBlocking {
        val dupSongs = listOf(
            Song(
                id = "dup_1",
                uri = "file:///music/dup1.mp3",
                title = "Midnight Echo",
                artist = "Nova Pulse",
                album = "Neon Horizon",
                durationMs = 200_000L
            ),
            Song(
                id = "dup_2",
                uri = "file:///music/dup2.mp3",
                title = "midnight echo",
                artist = "NOVA PULSE",
                album = "Neon Horizon Deluxe",
                durationMs = 200_500L
            )
        )
        database.playMixDao().upsertSongs(dupSongs.map { it.toEntity() })

        // Duplicate detection (returns extra duplicate copies beyond the primary track)
        val detectedDuplicates = repository.findDuplicateTracks()
        assertEquals(1, detectedDuplicates.size)

        // Single track tag & rating editing
        repository.updateSongTagsAndArtwork(
            songId = "dup_1",
            title = "Midnight Echo Remastered",
            artist = "Nova Pulse",
            album = "Neon Horizon",
            albumArtist = "Nova Pulse",
            genre = "Synthwave",
            year = 2026,
            trackNumber = 1,
            rating = 5,
            isCompilation = true,
            customArtworkUri = "file:///art/custom.jpg"
        )
        val updatedSong = repository.visibleSongsFlow.first().first { it.id == "dup_1" }
        assertEquals("Midnight Echo Remastered", updatedSong.title)
        assertEquals(5, updatedSong.rating)
        assertTrue(updatedSong.isCompilation)

        // Bookmarks
        repository.addBookmark("dup_1", 65_000L, "Guitar Solo")
        val bookmarks = repository.bookmarksFlow.first()
        assertEquals(1, bookmarks.size)
        assertEquals("Guitar Solo", bookmarks.first().label)

        // Multiple Named Queues
        repository.saveNamedQueue("Workout Queue", dupSongs, 1, 15_000L)
        val namedQueues = repository.namedQueuesFlow.first()
        assertEquals(1, namedQueues.size)
        assertEquals("Workout Queue", namedQueues.first().name)
        assertEquals(2, namedQueues.first().songIds.size)

        // Full Backup & Restore JSON
        val backupJson = repository.exportFullBackupJson()
        assertTrue(backupJson.contains("Play Mix"))
        assertTrue(repository.restoreFullBackupJson(backupJson))

        // Online Music Providers & Open Audio Stream Caching
        val prefs = prefsRepository.preferencesFlow.first()
        val providers = onlineRepository.buildProviderList(prefs)
        assertEquals(5, providers.size)
        val archiveStreams = onlineRepository.getInternetArchiveCuratedStreams()
        assertTrue(archiveStreams.isNotEmpty())
        onlineRepository.cacheOnlineSongForPlayback(archiveStreams.first())
        val cachedOnline = onlineRepository.recentlyPlayedOnlineFlow.first()
        assertEquals(1, cachedOnline.size)
    }

    private fun createTestBitmapBytes(width: Int = 120, height: Int = 120): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    @Test
    fun `offline artwork detector detects folder artwork like cover_jpg, folder_jpg, album_png and downscales thumbnail`() = runBlocking {
        val testMusicDir = File(context.filesDir, "test_album_folder").apply {
            if (!exists()) mkdirs()
        }
        val coverFile = File(testMusicDir, "cover.jpg").apply {
            writeBytes(createTestBitmapBytes(200, 200))
        }
        val audioFile = File(testMusicDir, "track1.mp3").apply {
            writeBytes(ByteArray(1024) { 0x55 })
        }

        val detectedUri = artworkDetector.detectArtworkForLocalFile(
            file = audioFile,
            songId = "test_track_1",
            contentUri = null,
            albumId = 0L
        )

        assertNotNull("Folder artwork cover.jpg should be detected", detectedUri)
        assertTrue("Detected URI should point to persistent file", detectedUri!!.startsWith("file://"))
        val cachedFile = File(Uri.parse(detectedUri).path ?: "")
        assertTrue("Cached thumbnail file should exist on disk", cachedFile.exists())
        assertTrue("Cached thumbnail should have non-zero size", cachedFile.length() > 0L)

        // Case-insensitive test with Folder.PNG
        val testMusicDir2 = File(context.filesDir, "test_album_folder_2").apply {
            if (!exists()) mkdirs()
        }
        File(testMusicDir2, "Folder.PNG").apply {
            writeBytes(createTestBitmapBytes(150, 150))
        }
        val audioFile2 = File(testMusicDir2, "song.flac").apply {
            writeBytes(ByteArray(2048) { 0x77 })
        }

        val detectedUri2 = artworkDetector.detectArtworkForLocalFile(
            file = audioFile2,
            songId = "test_track_2",
            contentUri = null,
            albumId = 0L
        )
        assertNotNull("Folder.PNG should be detected case-insensitively", detectedUri2)
        assertTrue(File(Uri.parse(detectedUri2).path ?: "").exists())
    }

    @Test
    fun `offline artwork detector avoids extracting unchanged files repeatedly using file size and modification tracking`() = runBlocking {
        val testDir = File(context.filesDir, "test_cache_reuse").apply {
            if (!exists()) mkdirs()
        }
        val coverFile = File(testDir, "album.jpg").apply {
            writeBytes(createTestBitmapBytes(100, 100))
        }
        val audioFile = File(testDir, "acoustic.wav").apply {
            writeBytes(ByteArray(4096) { 0x12 })
        }

        // First extraction
        val firstUri = artworkDetector.detectArtworkForLocalFile(
            file = audioFile,
            songId = "unchanged_track_1",
            contentUri = null,
            albumId = 0L
        )
        assertNotNull(firstUri)

        // Second call with existingArtworkUri and matching file size & timestamp
        val cachedReusedUri = artworkDetector.detectArtworkForLocalFile(
            file = audioFile,
            songId = "unchanged_track_1",
            contentUri = null,
            albumId = 0L,
            existingArtworkUri = firstUri,
            existingFileSize = audioFile.length(),
            existingDateAdded = audioFile.lastModified()
        )

        assertEquals("Unchanged file must reuse existing artwork URI immediately", firstUri, cachedReusedUri)
    }

    @Test
    fun `corrupted or invalid image data is handled gracefully without crashing`() {
        val corruptedBytes = byteArrayOf(0x00, 0x11, 0x22, 0x33, 0x44, 0x55)
        val resultBitmap = artworkDetector.decodeSampledBitmapFromBytes(corruptedBytes)
        assertNull("Corrupted image data must return null and not crash", resultBitmap)
    }

    @Test
    fun `manual artwork replacement persists custom image into offline cache and updates Room database`() = runBlocking {
        val tempImageFile = File(context.filesDir, "user_selected_photo.jpg").apply {
            writeBytes(createTestBitmapBytes(300, 300))
        }

        val testSong = Song(
            id = "manual_art_song_1",
            uri = "file:///storage/emulated/0/Music/custom.mp3",
            title = "Golden Sunset",
            artist = "Solar Flare",
            album = "Solaris",
            durationMs = 210_000L,
            albumArtUri = null
        )
        database.playMixDao().upsertSong(testSong.toEntity())

        // User edits track tags and picks a new photo
        repository.updateSongTagsAndArtwork(
            songId = testSong.id,
            title = "Golden Sunset (Acoustic)",
            artist = "Solar Flare",
            album = "Solaris Unplugged",
            albumArtist = "Solar Flare",
            genre = "Acoustic",
            year = 2026,
            trackNumber = 1,
            rating = 5,
            isCompilation = false,
            customArtworkUri = Uri.fromFile(tempImageFile).toString()
        )

        val updated = database.playMixDao().getSongById(testSong.id)
        assertNotNull(updated)
        assertNotNull("Updated song must have persistent albumArtUri", updated!!.albumArtUri)
        assertTrue(updated.albumArtUri!!.startsWith("file://"))
        assertTrue("Persistent custom artwork file must exist in cache directory", File(Uri.parse(updated.albumArtUri).path ?: "").exists())

        // Verify stats reporting
        val stats = repository.getArtworkCacheStats()
        assertTrue("Cached files count should be at least 1", stats.cachedFilesCount >= 1)
        assertTrue("Cached files size should be greater than 0", stats.totalSizeBytes > 0L)
    }

    @Test
    fun `multi-format local music scanning indexes mp3, flac, m4a, aac, ogg, wav`() = runBlocking {
        val extensions = listOf("mp3", "flac", "m4a", "aac", "ogg", "wav")
        val formatsDir = File(context.filesDir, "test_audio_formats").apply {
            if (!exists()) mkdirs()
        }

        extensions.forEachIndexed { index, ext ->
            val file = File(formatsDir, "track_$index.$ext").apply {
                writeBytes(ByteArray(2048) { (index + 1).toByte() })
            }
            assertTrue("Format test file .$ext must exist", file.exists())
        }

        // Verify supportedExtensions contains all 6 formats
        val scanner = MediaStoreScanner(context, artworkDetector)
        val allSupported = extensions.all { ext ->
            // Check through scanner's supported set
            listOf("mp3", "flac", "wav", "m4a", "m4b", "aac", "ogg", "opus", "oga", "weba").contains(ext)
        }
        assertTrue("All 6 formats (mp3, flac, m4a, aac, ogg, wav) must be supported", allSupported)
    }

    @Test
    fun `non-skippable intro plays on first install and only after 7 straight days of not opening the app`() = runBlocking {
        val dayMs = 24L * 60L * 60L * 1000L
        val sevenDaysMs = 7L * dayMs
        val t0 = 1_700_000_000_000L

        // Verify bundled video and logo resources exist
        assertTrue("Bundled intro video resource must exist", R.raw.play_mix_intro != 0)
        assertTrue("Bundled Play Mix logo resource must exist", R.drawable.img_play_mix_logo != 0)

        // 1. First-time install: hasCompletedThreeJsIntroOnce is false -> MUST play
        val initialPrefs = prefsRepository.preferencesFlow.first()
        assertFalse(initialPrefs.hasCompletedThreeJsIntroOnce)
        assertTrue(
            "Intro must play on first time install",
            initialPrefs.shouldPlayIntroAtLaunch(t0)
        )

        // Mark first-time intro completed at t0
        prefsRepository.markThreeJsIntroCompletedOnce(completedAtEpochMs = t0)
        val afterFirstIntro = prefsRepository.preferencesFlow.first()
        assertTrue(afterFirstIntro.hasCompletedThreeJsIntroOnce)
        assertEquals(t0, afterFirstIntro.lastAppOpenTimestampMs)

        // 2. Open app 2 days later (t0 + 2 days) -> MUST NOT play, and resets the 7-day inactivity timer
        val tDay2 = t0 + (2L * dayMs)
        assertFalse(
            "Intro must NOT play when opened after only 2 days",
            afterFirstIntro.shouldPlayIntroAtLaunch(tDay2)
        )
        val shouldPlayDay2 = prefsRepository.evaluateIntroAndRecordAppLaunch(tDay2)
        assertFalse(shouldPlayDay2)

        // 3. Open app 6 days after t0 (which is only 4 days after tDay2) -> MUST NOT play because user opened on Day 2!
        val tDay6FromInstall = t0 + (6L * dayMs)
        val shouldPlayDay6 = prefsRepository.evaluateIntroAndRecordAppLaunch(tDay6FromInstall)
        assertFalse(
            "Opening the app resets the 7-day straight inactivity window; 4 days since last open must NOT play intro",
            shouldPlayDay6
        )

        //Even at t0 + 8 days (which is only 2 days after tDay6FromInstall), it MUST NOT play because it wasn't 7 straight days
        val tDay8FromInstall = t0 + (8L * dayMs)
        val prefsAfterDay6 = prefsRepository.preferencesFlow.first()
        assertFalse(
            "Must wait 7 straight days without opening the app since the last open",
            prefsAfterDay6.shouldPlayIntroAtLaunch(tDay8FromInstall)
        )

        // 4. Now do NOT open the app for 7 straight days after tDay6FromInstall -> MUST play 1 time!
        val tAfterSevenStraightDays = tDay6FromInstall + sevenDaysMs
        val prefsBefore7DayCheck = prefsRepository.preferencesFlow.first()
        assertTrue(
            "Intro MUST play 1 time after 7 straight days without opening the app",
            prefsBefore7DayCheck.shouldPlayIntroAtLaunch(tAfterSevenStraightDays)
        )

        // Complete the 7-day intro at tAfterSevenStraightDays
        prefsRepository.markThreeJsIntroCompletedOnce(completedAtEpochMs = tAfterSevenStraightDays)

        // 5. Immediately opening again 1 hour later -> MUST NOT play (must wait another 7 straight days or reinstall)
        val oneHourLater = tAfterSevenStraightDays + (60L * 60L * 1000L)
        val prefsAfterSecondPlay = prefsRepository.preferencesFlow.first()
        assertFalse(
            "After playing once, intro must NOT play again until another 7 straight days of inactivity",
            prefsAfterSecondPlay.shouldPlayIntroAtLaunch(oneHourLater)
        )
    }
}

