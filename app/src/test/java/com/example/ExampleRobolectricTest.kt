package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.PlayMixDatabase
import com.example.core.database.toEntity
import com.example.core.datastore.UserPreferencesRepository
import com.example.data.library.DemoAudioSeeder
import com.example.data.library.MediaStoreScanner
import com.example.data.library.MusicLibraryRepository
import com.example.data.online.OnlineMusicRepository
import com.example.domain.model.AudioCategory
import com.example.domain.model.Song
import com.example.domain.model.SortOption
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, PlayMixDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsRepository = UserPreferencesRepository(context)
        repository = MusicLibraryRepository(
            context = context,
            dao = database.playMixDao(),
            scanner = MediaStoreScanner(context),
            prefsRepo = prefsRepository
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
}
