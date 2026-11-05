package com.example.data.library

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.example.core.database.BookmarkEntity
import com.example.core.database.ExcludedFolderEntity
import com.example.core.database.NamedQueueEntity
import com.example.core.database.PlayMixDao
import com.example.core.database.PlaybackHistoryEntity
import com.example.core.database.PlaylistEntity
import com.example.core.database.PlaylistSongCrossRef
import com.example.core.database.QueueItemEntity
import com.example.core.database.SafFolderEntity
import com.example.core.database.SearchHistoryEntity
import com.example.core.datastore.UserPreferencesRepository
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.AudioCategory
import com.example.domain.model.FolderItem
import com.example.domain.model.GenreItem
import com.example.domain.model.NamedQueue
import com.example.domain.model.PlaybackBookmark
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.domain.model.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MusicLibraryRepository(
    private val context: Context,
    private val dao: PlayMixDao,
    private val scanner: MediaStoreScanner,
    private val prefsRepo: UserPreferencesRepository
) {

    private val _mediaStoreChangeSignal = MutableStateFlow(0L)
    val mediaStoreChangeSignal: StateFlow<Long> = _mediaStoreChangeSignal.asStateFlow()

    private var mediaStoreObserverRegistered = false
    private val mediaStoreContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            _mediaStoreChangeSignal.value = System.currentTimeMillis()
        }
    }

    fun registerMediaStoreObserverIfNeeded() {
        if (mediaStoreObserverRegistered) return
        runCatching {
            context.contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaStoreContentObserver
            )
            mediaStoreObserverRegistered = true
        }
    }

    fun unregisterMediaStoreObserver() {
        if (!mediaStoreObserverRegistered) return
        runCatching {
            context.contentResolver.unregisterContentObserver(mediaStoreContentObserver)
            mediaStoreObserverRegistered = false
        }
    }

    val allSongsIncludingHiddenFlow: Flow<List<Song>> = combine(
        dao.observeAllSongs(),
        prefsRepo.preferencesFlow
    ) { entities, prefs ->
        entities
            .filter { prefs.includeDemoTracks || !it.isDemoTrack }
            .map { it.toDomain() }
    }

    /**
     * All Audio Inspector Flow: Includes every local audio file across all 8 categories
     * (excluding online stream cache entries) so the user can inspect classification reasons and categories.
     */
    val allAudioInspectorFlow: Flow<List<Song>> = combine(
        allSongsIncludingHiddenFlow,
        prefsRepo.preferencesFlow
    ) { songs, prefs ->
        val localSongs = songs.filter { it.onlineProviderId == null }
        sortSongs(localSongs, prefs.sortOption)
    }

    /**
     * Strict Music-Only Flow: Used by Music Home screen, Music Library, Albums, Artists, and Genres.
     * Alarms, Voice Recordings, Ringtones, Notification Sounds, Podcasts, Audiobooks, and Unclassified
     * files NEVER appear in this flow.
     */
    val visibleSongsFlow: Flow<List<Song>> = observeSongsForCategory(AudioCategory.MUSIC)

    /**
     * Independent Category Library Flow for any of the 8 AudioCategory values.
     */
    fun observeSongsForCategory(category: AudioCategory): Flow<List<Song>> = combine(
        allSongsIncludingHiddenFlow,
        dao.observeExcludedFolders(),
        prefsRepo.preferencesFlow
    ) { songs, excludedEntities, prefs ->
        val excludedPaths = excludedEntities.map { it.folderPath }.toSet()
        val filtered = songs.filter { song ->
            song.onlineProviderId == null &&
                song.isVisibleInCategory(category) &&
                !excludedPaths.contains(song.folderName) &&
                !excludedPaths.contains(song.filePath)
        }
        sortSongs(filtered, prefs.sortOption)
    }

    /**
     * Hidden or Category-Removed Items Flow: Lists all tracks that have been removed from a category's
     * visible view (`removedFromCategories.isNotEmpty()`) or globally hidden (`isHidden == true`),
     * without having been deleted from the device.
     */
    val hiddenSongsFlow: Flow<List<Song>> = allSongsIncludingHiddenFlow.map { songs ->
        songs.filter { it.onlineProviderId == null && (it.isHidden || it.removedFromCategories.isNotEmpty()) }
            .sortedByDescending { it.categoryUpdatedAt.coerceAtLeast(it.dateAdded) }
    }

    val favoritesFlow: Flow<List<Song>> = visibleSongsFlow.map { songs ->
        songs.filter { it.isFavorite }
    }

    val recentlyAddedFlow: Flow<List<Song>> = visibleSongsFlow.map { songs ->
        songs.sortedByDescending { it.dateAdded }.take(25)
    }

    val recentlyPlayedFlow: Flow<List<Song>> = visibleSongsFlow.map { songs ->
        songs.filter { it.lastPlayedAt > 0L }
            .sortedByDescending { it.lastPlayedAt }
            .take(25)
    }

    val mostPlayedFlow: Flow<List<Song>> = visibleSongsFlow.map { songs ->
        songs.filter { it.playCount > 0 }
            .sortedByDescending { it.playCount }
            .take(25)
    }

    val albumsFlow: Flow<List<Album>> = combine(
        visibleSongsFlow,
        prefsRepo.preferencesFlow
    ) { songs, prefs ->
        songs.groupBy { song ->
            val groupArtist = if (song.isCompilation) {
                "various artists"
            } else if (prefs.groupByAlbumArtist) {
                song.albumArtist.ifBlank { song.artist }.lowercase().trim()
            } else {
                song.artist.lowercase().trim()
            }
            "${song.album.lowercase().trim()}__$groupArtist"
        }.map { (key, albumSongs) ->
            val sortedTracks = albumSongs.sortedBy { it.trackNumber }
            val first = sortedTracks.first()
            val distinctArtists = sortedTracks.map { it.artist.lowercase() }.distinct()
            val isComp = first.isCompilation || distinctArtists.size > 1
            Album(
                id = key,
                name = first.album,
                artist = if (isComp) "Various Artists" else first.albumArtist.ifBlank { first.artist },
                artworkUri = sortedTracks.firstNotNullOfOrNull { it.albumArtUri },
                year = first.year,
                songs = sortedTracks,
                isCompilation = isComp
            )
        }.sortedBy { it.name.lowercase() }
    }

    val artistsFlow: Flow<List<Artist>> = visibleSongsFlow.map { songs ->
        songs.groupBy { it.artist.trim() }
            .map { (artistName, artistSongs) ->
                val albumCount = artistSongs.map { it.album.lowercase() }.distinct().size
                Artist(
                    id = artistName.lowercase(),
                    name = artistName,
                    artworkUri = artistSongs.firstNotNullOfOrNull { it.albumArtUri },
                    albumCount = albumCount,
                    songs = artistSongs.sortedBy { it.title.lowercase() }
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val genresFlow: Flow<List<GenreItem>> = visibleSongsFlow.map { songs ->
        songs.groupBy { it.genre.ifBlank { "Other" } }
            .map { (genreName, genreSongs) ->
                GenreItem(
                    name = genreName,
                    songs = genreSongs.sortedBy { it.title.lowercase() }
                )
            }
            .sortedByDescending { it.songs.size }
    }

    val foldersFlow: Flow<List<FolderItem>> = combine(
        allSongsIncludingHiddenFlow,
        dao.observeExcludedFolders(),
        dao.observeSafFolders()
    ) { songs, excludedEntities, safEntities ->
        val excludedPaths = excludedEntities.map { it.folderPath }.toSet()
        val safNames = safEntities.map { it.displayName }.toSet()
        songs.filter { !it.isHidden && it.onlineProviderId == null }
            .groupBy { it.folderName.ifBlank { "Music" } }
            .map { (folderName, folderSongs) ->
                val samplePath = folderSongs.first().filePath.substringBeforeLast('/', folderName)
                FolderItem(
                    name = folderName,
                    path = samplePath,
                    songs = folderSongs,
                    isExcluded = excludedPaths.contains(folderName) || excludedPaths.contains(samplePath),
                    isSafFolder = safNames.contains(folderName)
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    val customPlaylistsFlow: Flow<List<Playlist>> = combine(
        dao.observePlaylists(),
        dao.observeAllPlaylistRefs(),
        allSongsIncludingHiddenFlow
    ) { playlists, refs, songs ->
        val songMap = songs.associateBy { it.id }
        val refsByPlaylist = refs.groupBy { it.playlistId }
        playlists.map { entity ->
            val orderedSongs = refsByPlaylist[entity.id]
                .orEmpty()
                .sortedBy { it.position }
                .mapNotNull { songMap[it.songId] }
            Playlist(
                id = entity.id,
                name = entity.name,
                description = entity.description,
                createdAt = entity.createdAt,
                isSmart = false,
                smartType = null,
                songs = orderedSongs
            )
        }
    }

    val bookmarksFlow: Flow<List<PlaybackBookmark>> = dao.observeAllBookmarks().map { list ->
        list.map { it.toDomain() }
    }

    val namedQueuesFlow: Flow<List<NamedQueue>> = dao.observeNamedQueues().map { list ->
        list.map { it.toDomain() }
    }

    val searchHistoryFlow: Flow<List<String>> = dao.observeSearchHistory().map { list ->
        list.map { it.query }
    }

    val safFoldersFlow: Flow<List<SafFolderEntity>> = dao.observeSafFolders()

    fun sortSongs(songs: List<Song>, sortOption: SortOption): List<Song> {
        return when (sortOption) {
            SortOption.TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> songs.sortedByDescending { it.title.lowercase() }
            SortOption.ARTIST_ASC -> songs.sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
            SortOption.ALBUM_ASC -> songs.sortedWith(compareBy({ it.album.lowercase() }, { it.trackNumber }))
            SortOption.DURATION_DESC -> songs.sortedByDescending { it.durationMs }
            SortOption.DATE_ADDED_DESC -> songs.sortedByDescending { it.dateAdded }
            SortOption.PLAY_COUNT_DESC -> songs.sortedByDescending { it.playCount }
            SortOption.RATING_DESC -> songs.sortedWith(compareByDescending<Song> { it.rating }.thenBy { it.title.lowercase() })
        }
    }

    suspend fun rescanLibrary(hasStoragePermission: Boolean): Int = withContext(Dispatchers.IO) {
        val prefs = prefsRepo.getSnapshot()
        val existingMap = dao.getAllSongsSnapshot().associateBy { it.id }
        val excludedSet = dao.getExcludedFoldersSnapshot().map { it.folderPath }.toSet()

        val demoSeed = DemoAudioSeeder.ensureDemoTracksSeeded(context)
        demoSeed.lyrics.forEach { lyricEntity ->
            if (dao.getLyricsForSong(lyricEntity.songId) == null) {
                dao.upsertLyrics(lyricEntity)
            }
        }

        val mediaStoreSongs = if (hasStoragePermission) {
            scanner.scanDeviceMediaStore(
                minDurationSec = prefs.minScanDurationSec,
                excludedFolders = excludedSet
            )
        } else {
            emptyList()
        }

        val safFolders = dao.getSafFoldersSnapshot()
        val safSongs = safFolders.flatMap { folder ->
            scanner.scanSafTreeFolder(
                treeUriString = folder.treeUri,
                minDurationSec = prefs.minScanDurationSec
            )
        }

        val allDiscovered = (demoSeed.songs + mediaStoreSongs + safSongs)
            .distinctBy { it.id }
            .map { fresh ->
                val prev = existingMap[fresh.id]
                if (prev != null) {
                    val preservedPrimaryCategory = if (prev.isManualClassification) {
                        prev.primaryCategory
                    } else {
                        fresh.primaryCategory
                    }
                    val preservedReason = if (prev.isManualClassification) {
                        prev.classificationReason
                    } else {
                        fresh.classificationReason
                    }
                    fresh.copy(
                        title = prev.title.ifBlank { fresh.title },
                        artist = prev.artist.ifBlank { fresh.artist },
                        album = prev.album.ifBlank { fresh.album },
                        albumArtist = prev.albumArtist.ifBlank { fresh.albumArtist },
                        genre = prev.genre.ifBlank { fresh.genre },
                        year = if (prev.year > 0) prev.year else fresh.year,
                        albumArtUri = prev.albumArtUri ?: fresh.albumArtUri,
                        isFavorite = prev.isFavorite,
                        isHidden = prev.isHidden,
                        playCount = prev.playCount,
                        lastPlayedAt = prev.lastPlayedAt,
                        rating = prev.rating,
                        isCompilation = prev.isCompilation,
                        trackGainDb = prev.trackGainDb,
                        albumGainDb = prev.albumGainDb,
                        primaryCategory = preservedPrimaryCategory,
                        originalCategory = prev.originalCategory.ifBlank { fresh.originalCategory },
                        isManualClassification = prev.isManualClassification,
                        classificationReason = preservedReason,
                        removedFromCategoriesCsv = prev.removedFromCategoriesCsv,
                        exclusionReason = prev.exclusionReason,
                        categoryUpdatedAt = prev.categoryUpdatedAt,
                        mimeType = prev.mimeType.ifBlank { fresh.mimeType }
                    )
                } else {
                    fresh.copy(rating = if (fresh.isFavorite) 5 else 4)
                }
            }

        dao.upsertSongs(allDiscovered)

        if (dao.getAllSongsSnapshot().isNotEmpty() && dao.getPlaylistSongRefs(1L).isEmpty()) {
            val defaultPlaylistId = dao.insertPlaylist(
                PlaylistEntity(
                    id = 1L,
                    name = "Late Night Synth & Focus",
                    description = "Curated Play Mix session ready for background playback"
                )
            )
            val musicDemos = demoSeed.songs.filter { it.primaryCategory == AudioCategory.MUSIC.id }
            val starterRefs = musicDemos.take(4).mapIndexed { index, song ->
                PlaylistSongCrossRef(
                    playlistId = if (defaultPlaylistId > 0L) defaultPlaylistId else 1L,
                    songId = song.id,
                    position = index
                )
            }
            dao.insertPlaylistSongRefs(starterRefs)
        }

        allDiscovered.size
    }

    // --- NON-DESTRUCTIVE CATEGORY MANAGEMENT ---

    /**
     * MOVING CHANGES CATEGORY:
     * Updates primaryCategory in Room, marks isManualClassification = true so rescans preserve it,
     * removes any category-visibility exclusion on the destination category, and preserves the physical file,
     * metadata, artwork, play history, and playlists.
     */
    suspend fun moveSongsToCategory(
        songIds: Set<String>,
        targetCategory: AudioCategory
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        songIds.forEach { songId ->
            val current = dao.getSongById(songId) ?: return@forEach
            val existingRemoved = AudioCategory.parseSetFromCsv(current.removedFromCategoriesCsv)
            val updatedRemoved = existingRemoved - targetCategory
            val reason = "User manual override → ${targetCategory.displayName} (originally ${AudioCategory.fromId(current.originalCategory).displayName})"
            dao.updateSongCategory(
                songId = songId,
                targetCategoryId = targetCategory.id,
                reason = reason,
                updatedRemovedCsv = AudioCategory.toCsv(updatedRemoved),
                updatedAt = now
            )
        }
    }

    /**
     * REMOVING CHANGES VISIBILITY:
     * Hides the track from the specified category's visible list (`removedFromCategoriesCsv`)
     * WITHOUT deleting the physical file, WITHOUT deleting metadata/artwork, and WITHOUT moving it
     * to another category. Persists across rescans until restored.
     */
    suspend fun removeSongsFromCategory(
        songIds: Set<String>,
        category: AudioCategory,
        customReason: String = ""
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        songIds.forEach { songId ->
            val current = dao.getSongById(songId) ?: return@forEach
            val updatedRemoved = AudioCategory.parseSetFromCsv(current.removedFromCategoriesCsv) + category
            val reason = customReason.ifBlank {
                "Removed from ${category.displayName} category view (File kept on device)"
            }
            dao.updateSongCategoryVisibility(
                songId = songId,
                updatedRemovedCsv = AudioCategory.toCsv(updatedRemoved),
                reason = reason,
                updatedAt = now
            )
        }
    }

    /**
     * RESTORE FUNCTIONALITY:
     * Restores one or more removed/hidden items. Allows the user to optionally choose a destination category
     * upon restoration, or restore directly to the item's existing/original category.
     */
    suspend fun restoreSongsToCategory(
        songIds: Set<String>,
        destinationCategory: AudioCategory? = null
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        songIds.forEach { songId ->
            val current = dao.getSongById(songId) ?: return@forEach
            val targetCat = destinationCategory ?: AudioCategory.fromId(current.primaryCategory)
            val reason = if (destinationCategory != null) {
                "Restored by user into ${destinationCategory.displayName}"
            } else {
                current.classificationReason.ifBlank { "Restored to ${targetCat.displayName}" }
            }
            dao.updateSongCategory(
                songId = songId,
                targetCategoryId = targetCat.id,
                reason = reason,
                updatedRemovedCsv = "",
                updatedAt = now
            )
        }
    }

    /**
     * DELETE FROM DEVICE (DESTRUCTIVE ACTION):
     * Explicitly deletes the track from the database, playlists, and physical file storage when authorized.
     * Never triggered by "Move to Category" or "Remove from Category".
     */
    suspend fun deleteSongsFromDevice(
        songIds: Set<String>
    ): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        songIds.forEach { songId ->
            val entity = dao.getSongById(songId) ?: return@forEach
            // Attempt physical file deletion for local file paths or ContentResolver URIs
            runCatching {
                if (entity.filePath.isNotBlank() && !entity.filePath.startsWith("content://")) {
                    val file = File(entity.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } else if (entity.uri.startsWith("content://")) {
                    context.contentResolver.delete(Uri.parse(entity.uri), null, null)
                }
            }
            dao.removeSongFromAllPlaylists(songId)
            dao.deleteSongById(songId)
            deletedCount++
        }
        deletedCount
    }

    suspend fun toggleFavorite(songId: String): Boolean = withContext(Dispatchers.IO) {
        val current = dao.getSongById(songId) ?: return@withContext false
        val next = !current.isFavorite
        dao.setFavorite(songId, next)
        next
    }

    suspend fun setSongRating(songId: String, rating: Int) = withContext(Dispatchers.IO) {
        dao.setRating(songId, rating.coerceIn(0, 5))
    }

    suspend fun updateSongTagsAndArtwork(
        songId: String,
        title: String,
        artist: String,
        album: String,
        albumArtist: String,
        genre: String,
        year: Int,
        trackNumber: Int,
        rating: Int,
        isCompilation: Boolean,
        customArtworkUri: String?
    ) = withContext(Dispatchers.IO) {
        val existing = dao.getSongById(songId) ?: return@withContext
        val updated = existing.copy(
            title = title.trim().ifBlank { existing.title },
            artist = artist.trim().ifBlank { existing.artist },
            album = album.trim().ifBlank { existing.album },
            albumArtist = albumArtist.trim().ifBlank { artist.trim().ifBlank { existing.albumArtist } },
            genre = genre.trim().ifBlank { existing.genre },
            year = year.coerceIn(1900, 2100),
            trackNumber = trackNumber.coerceAtLeast(1),
            rating = rating.coerceIn(0, 5),
            isCompilation = isCompilation,
            albumArtUri = customArtworkUri?.takeIf { it.isNotBlank() } ?: existing.albumArtUri
        )
        dao.upsertSong(updated)
    }

    suspend fun bulkUpdateSongsMetadata(
        songIds: Set<String>,
        artist: String?,
        album: String?,
        albumArtist: String?,
        genre: String?,
        year: Int?,
        isCompilation: Boolean?
    ) = withContext(Dispatchers.IO) {
        val all = dao.getAllSongsSnapshot()
        val updated = all.mapNotNull { entity ->
            if (entity.id !in songIds) null
            else entity.copy(
                artist = artist?.takeIf { it.isNotBlank() } ?: entity.artist,
                album = album?.takeIf { it.isNotBlank() } ?: entity.album,
                albumArtist = albumArtist?.takeIf { it.isNotBlank() } ?: entity.albumArtist,
                genre = genre?.takeIf { it.isNotBlank() } ?: entity.genre,
                year = year ?: entity.year,
                isCompilation = isCompilation ?: entity.isCompilation
            )
        }
        if (updated.isNotEmpty()) {
            dao.upsertSongs(updated)
        }
    }

    suspend fun findDuplicateTracks(): List<Song> = withContext(Dispatchers.IO) {
        val all = dao.getAllSongsSnapshot().filter { !it.isHidden && it.onlineProviderId == null }
        all.groupBy { "${it.title.lowercase().trim()}__${it.artist.lowercase().trim()}" }
            .values
            .filter { it.size > 1 }
            .flatMap { group -> group.drop(1) }
            .map { it.toDomain() }
    }

    suspend fun findMissingFileTracks(): List<Song> = withContext(Dispatchers.IO) {
        val all = dao.getAllSongsSnapshot().filter { it.onlineProviderId == null }
        all.filter { entity ->
            if (entity.filePath.isNotBlank() && !entity.filePath.startsWith("content://")) {
                !File(entity.filePath).exists()
            } else {
                false
            }
        }.map { it.toDomain() }
    }

    suspend fun removeMissingTracks(): Int = withContext(Dispatchers.IO) {
        val missing = findMissingFileTracks()
        missing.forEach { dao.deleteSongById(it.id) }
        missing.size
    }

    suspend fun setSongHidden(songId: String, hidden: Boolean) = withContext(Dispatchers.IO) {
        dao.setHidden(
            songId = songId,
            isHidden = hidden,
            reason = if (hidden) "Hidden from library by user (File kept on device)" else "",
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun unhideAllSongs() = withContext(Dispatchers.IO) {
        dao.unhideAllSongs()
    }

    suspend fun recordSongPlayed(songId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.incrementPlayCount(songId, now)
        dao.insertPlaybackHistory(PlaybackHistoryEntity(songId = songId, playedAt = now))
    }

    // --- BOOKMARKS ---
    suspend fun addBookmark(songId: String, positionMs: Long, label: String) = withContext(Dispatchers.IO) {
        dao.insertBookmark(
            BookmarkEntity(
                songId = songId,
                positionMs = positionMs.coerceAtLeast(0L),
                label = label.trim().ifBlank { "Bookmark" }
            )
        )
    }

    suspend fun deleteBookmark(bookmarkId: Long) = withContext(Dispatchers.IO) {
        dao.deleteBookmark(bookmarkId)
    }

    // --- NAMED QUEUES / QUEUE HISTORY ---
    suspend fun saveNamedQueue(name: String, songs: List<Song>, currentIndex: Int, currentPositionMs: Long) =
        withContext(Dispatchers.IO) {
            if (songs.isEmpty()) return@withContext
            dao.upsertNamedQueue(
                NamedQueueEntity(
                    name = name.trim().ifBlank { "Saved Queue" },
                    songIdsCsv = songs.joinToString(",") { it.id },
                    currentIndex = currentIndex.coerceIn(0, songs.lastIndex),
                    currentPositionMs = currentPositionMs.coerceAtLeast(0L)
                )
            )
        }

    suspend fun deleteNamedQueue(queueId: Long) = withContext(Dispatchers.IO) {
        dao.deleteNamedQueue(queueId)
    }

    suspend fun toggleFolderExclusion(folderName: String, currentlyExcluded: Boolean) =
        withContext(Dispatchers.IO) {
            if (currentlyExcluded) {
                dao.includeFolder(folderName)
            } else {
                dao.excludeFolder(ExcludedFolderEntity(folderPath = folderName))
            }
        }

    suspend fun addSafFolderAndScan(treeUri: String, displayName: String): Int =
        withContext(Dispatchers.IO) {
            dao.addSafFolder(SafFolderEntity(treeUri = treeUri, displayName = displayName))
            val prefs = prefsRepo.getSnapshot()
            val discovered = scanner.scanSafTreeFolder(treeUri, prefs.minScanDurationSec)
            if (discovered.isNotEmpty()) {
                dao.upsertSongs(discovered)
            }
            discovered.size
        }

    suspend fun removeSafFolder(treeUri: String) = withContext(Dispatchers.IO) {
        dao.removeSafFolder(treeUri)
    }

    // --- PLAYLIST OPERATIONS ---
    suspend fun createPlaylist(name: String, description: String = "", initialSongIds: List<String> = emptyList()): Long =
        withContext(Dispatchers.IO) {
            val cleanName = name.trim().ifBlank { "New Playlist" }
            val playlistId = dao.insertPlaylist(
                PlaylistEntity(name = cleanName, description = description.trim())
            )
            if (initialSongIds.isNotEmpty()) {
                val refs = initialSongIds.distinct().mapIndexed { idx, songId ->
                    PlaylistSongCrossRef(playlistId = playlistId, songId = songId, position = idx)
                }
                dao.insertPlaylistSongRefs(refs)
            }
            playlistId
        }

    suspend fun renamePlaylist(playlistId: Long, newName: String, description: String = "") =
        withContext(Dispatchers.IO) {
            dao.renamePlaylist(playlistId, newName.trim().ifBlank { "Untitled Playlist" }, description.trim())
        }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        dao.clearPlaylistSongs(playlistId)
        dao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        val currentRefs = dao.getPlaylistSongRefs(playlistId)
        if (currentRefs.any { it.songId == songId }) return@withContext
        val nextPosition = (currentRefs.maxOfOrNull { it.position } ?: -1) + 1
        dao.insertPlaylistSongRef(
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                position = nextPosition
            )
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) = withContext(Dispatchers.IO) {
        dao.removeSongFromPlaylist(playlistId, songId)
        val remaining = dao.getPlaylistSongRefs(playlistId).sortedBy { it.position }
        val reindexed = remaining.mapIndexed { idx, ref -> ref.copy(position = idx) }
        dao.clearPlaylistSongs(playlistId)
        dao.insertPlaylistSongRefs(reindexed)
    }

    suspend fun reorderPlaylistSongs(playlistId: Long, fromIndex: Int, toIndex: Int) =
        withContext(Dispatchers.IO) {
            val current = dao.getPlaylistSongRefs(playlistId).sortedBy { it.position }.toMutableList()
            if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return@withContext
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            val updated = current.mapIndexed { index, ref -> ref.copy(position = index) }
            dao.clearPlaylistSongs(playlistId)
            dao.insertPlaylistSongRefs(updated)
        }

    suspend fun exportPlaylistToM3u(playlist: Playlist, utf8Header: Boolean = true): String =
        withContext(Dispatchers.IO) {
            buildString {
                appendLine("#EXTM3U")
                if (utf8Header) {
                    appendLine("#EXTENC:UTF-8")
                }
                appendLine("#PLAYLIST:${playlist.name}")
                playlist.songs.forEach { song ->
                    val durationSec = (song.durationMs / 1000L).coerceAtLeast(1L)
                    appendLine("#EXTINF:$durationSec,${song.artist} - ${song.title}")
                    appendLine(song.filePath.ifBlank { song.uri })
                }
            }
        }

    suspend fun importM3uPlaylist(playlistName: String, m3uContent: String): Long =
        withContext(Dispatchers.IO) {
            val allSongs = dao.getAllSongsSnapshot()
            val matchedIds = mutableListOf<String>()
            m3uContent.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                    val match = allSongs.firstOrNull {
                        it.filePath.equals(trimmed, ignoreCase = true) ||
                            it.uri.equals(trimmed, ignoreCase = true) ||
                            it.title.equals(trimmed.substringAfterLast('/').substringBeforeLast('.'), ignoreCase = true)
                    }
                    if (match != null) matchedIds.add(match.id)
                } else if (trimmed.startsWith("#EXTINF:")) {
                    val titlePart = trimmed.substringAfter(',', "").substringAfter(" - ", "").trim()
                    if (titlePart.isNotBlank()) {
                        val match = allSongs.firstOrNull { it.title.equals(titlePart, ignoreCase = true) }
                        if (match != null) matchedIds.add(match.id)
                    }
                }
            }
            val finalIds = matchedIds.distinct().ifEmpty { allSongs.take(3).map { it.id } }
            createPlaylist(
                name = playlistName.ifBlank { "Imported M3U/M3U8 Playlist" },
                description = "Imported from M3U/M3U8 (${finalIds.size} tracks)",
                initialSongIds = finalIds
            )
        }

    // --- BACKUP & RESTORE (Playlists, Favorites, Ratings, Category Overrides, EQ Presets, Settings) ---
    suspend fun exportFullBackupJson(): String = withContext(Dispatchers.IO) {
        val prefs = prefsRepo.getSnapshot()
        val songs = dao.getAllSongsSnapshot()
        val playlists = dao.getPlaylistsSnapshot()

        val root = JSONObject()
        root.put("app", "Play Mix")
        root.put("version", 3)
        root.put("exportedAt", System.currentTimeMillis())

        val settingsObj = JSONObject().apply {
            put("theme_mode", prefs.themeMode.name)
            put("accent_color", prefs.accentColor.name)
            put("app_language", prefs.appLanguage.name)
            put("sort_option", prefs.sortOption.name)
            put("eq_preset", prefs.equalizerPreset.name)
            put("eq_band_levels", prefs.bandLevelsMb.joinToString(","))
            put("custom_eq_presets", prefs.customEqPresetsRaw)
            put("crossfade_sec", prefs.crossfadeDurationSec.toString())
            put("skip_interval_sec", prefs.skipIntervalSec.toString())
            put("replay_gain_mode", prefs.replayGainMode.name)
        }
        root.put("settings", settingsObj)

        val favsArray = JSONArray()
        songs.filter {
            it.isFavorite || it.rating > 0 || it.isManualClassification || it.removedFromCategoriesCsv.isNotBlank() || it.isHidden
        }.forEach { s ->
            favsArray.put(
                JSONObject().apply {
                    put("songId", s.id)
                    put("isFavorite", s.isFavorite)
                    put("rating", s.rating)
                    put("primaryCategory", s.primaryCategory)
                    put("isManualClassification", s.isManualClassification)
                    put("removedFromCategoriesCsv", s.removedFromCategoriesCsv)
                    put("isHidden", s.isHidden)
                }
            )
        }
        root.put("favoritesAndRatings", favsArray)

        val playlistsArray = JSONArray()
        playlists.forEach { pl ->
            val refs = dao.getPlaylistSongRefs(pl.id).sortedBy { it.position }
            val songIdsJson = JSONArray()
            refs.forEach { songIdsJson.put(it.songId) }
            playlistsArray.put(
                JSONObject().apply {
                    put("name", pl.name)
                    put("description", pl.description)
                    put("songIds", songIdsJson)
                }
            )
        }
        root.put("playlists", playlistsArray)

        root.toString(2)
    }

    suspend fun restoreFullBackupJson(rawJson: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(rawJson)
            val settingsObj = root.optJSONObject("settings")
            if (settingsObj != null) {
                val map = mutableMapOf<String, String>()
                settingsObj.keys().forEach { k ->
                    map[k] = settingsObj.optString(k)
                }
                prefsRepo.restoreFromBackupMap(map)
            }

            val favsArray = root.optJSONArray("favoritesAndRatings")
            if (favsArray != null) {
                for (i in 0 until favsArray.length()) {
                    val obj = favsArray.getJSONObject(i)
                    val songId = obj.optString("songId")
                    if (songId.isNotBlank()) {
                        dao.setFavorite(songId, obj.optBoolean("isFavorite", false))
                        dao.setRating(songId, obj.optInt("rating", 0))
                        if (obj.optBoolean("isManualClassification", false)) {
                            val catId = obj.optString("primaryCategory", AudioCategory.MUSIC.id)
                            dao.updateSongCategory(
                                songId = songId,
                                targetCategoryId = catId,
                                reason = "Restored from Play Mix backup",
                                updatedRemovedCsv = obj.optString("removedFromCategoriesCsv", "")
                            )
                        }
                    }
                }
            }

            val playlistsArray = root.optJSONArray("playlists")
            if (playlistsArray != null) {
                val existingNames = dao.getPlaylistsSnapshot().map { it.name.lowercase() }.toSet()
                for (i in 0 until playlistsArray.length()) {
                    val plObj = playlistsArray.getJSONObject(i)
                    val name = plObj.optString("name")
                    val desc = plObj.optString("description")
                    val idsJson = plObj.optJSONArray("songIds")
                    val ids = mutableListOf<String>()
                    if (idsJson != null) {
                        for (j in 0 until idsJson.length()) {
                            ids.add(idsJson.getString(j))
                        }
                    }
                    if (name.isNotBlank() && name.lowercase() !in existingNames) {
                        createPlaylist(name, desc, ids)
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

    // --- SEARCH HISTORY ---
    suspend fun saveSearchQuery(query: String) = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.length >= 2) {
            dao.insertSearchQuery(SearchHistoryEntity(query = clean))
        }
    }

    suspend fun deleteSearchQuery(query: String) = withContext(Dispatchers.IO) {
        dao.deleteSearchQuery(query)
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        dao.clearSearchHistory()
    }

    // --- QUEUE PERSISTENCE ---
    suspend fun savePersistedQueue(songs: List<Song>, currentSongId: String, currentPositionMs: Long) =
        withContext(Dispatchers.IO) {
            val items = songs.mapIndexed { idx, song ->
                QueueItemEntity(queueIndex = idx, songId = song.id)
            }
            dao.replacePersistedQueue(items)
            prefsRepo.saveLastPlaybackSession(currentSongId, currentPositionMs)
        }

    suspend fun loadPersistedQueue(): Pair<List<Song>, Pair<String, Long>> = withContext(Dispatchers.IO) {
        val prefs = prefsRepo.getSnapshot()
        val allSongsMap = dao.getAllSongsSnapshot().associate { it.id to it.toDomain() }
        val queueItems = dao.getPersistedQueue()
            .sortedBy { it.queueIndex }
            .mapNotNull { allSongsMap[it.songId] }
        val finalQueue = queueItems.ifEmpty {
            allSongsMap.values.filter {
                it.onlineProviderId == null && it.isVisibleInCategory(AudioCategory.MUSIC)
            }.toList()
        }
        val lastSongId = prefs.lastSongId.ifBlank { finalQueue.firstOrNull()?.id.orEmpty() }
        finalQueue to (lastSongId to prefs.lastPositionMs)
    }
}
