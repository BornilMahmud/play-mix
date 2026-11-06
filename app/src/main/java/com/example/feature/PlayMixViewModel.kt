package com.example.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.PlayMixAppContainer
import com.example.core.database.SafFolderEntity
import com.example.core.datastore.UserPreferences
import com.example.data.lyrics.LrcLibSearchResult
import com.example.data.online.OnlineCatalogSnapshot
import com.example.domain.model.AccentColorOption
import com.example.domain.model.Album
import com.example.domain.model.AppLanguage
import com.example.domain.model.Artist
import com.example.domain.model.AudioCategory
import com.example.domain.model.AudioEffectsState
import com.example.domain.model.CustomEqualizerPreset
import com.example.domain.model.EqualizerPreset
import com.example.domain.model.FolderItem
import com.example.domain.model.GenreItem
import com.example.domain.model.NamedQueue
import com.example.domain.model.PlaybackBookmark
import com.example.domain.model.Playlist
import com.example.domain.model.ReplayGainMode
import com.example.domain.model.SleepTimerOption
import com.example.domain.model.SmartPlaylistType
import com.example.domain.model.Song
import com.example.domain.model.SongLyrics
import com.example.domain.model.SortOption
import com.example.playback.controller.PlaybackUiState
import com.example.playback.controller.SystemVolumeState
import com.example.ui.theme.PlayMixThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchResultsUiState(
    val query: String = "",
    val matchingSongs: List<Song> = emptyList(),
    val matchingAlbums: List<Album> = emptyList(),
    val matchingArtists: List<Artist> = emptyList(),
    val matchingPlaylists: List<Playlist> = emptyList(),
    val matchingGenres: List<GenreItem> = emptyList()
)

data class OnlineLyricsSearchState(
    val isSearching: Boolean = false,
    val results: List<LrcLibSearchResult> = emptyList(),
    val errorMessage: String? = null
)

data class LibraryDiagnosticsReport(
    val duplicateTracks: List<Song> = emptyList(),
    val missingFileTracks: List<Song> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PlayMixViewModel(
    private val container: PlayMixAppContainer
) : ViewModel() {

    private val libraryRepo = container.libraryRepository
    private val prefsRepo = container.preferencesRepository
    private val lyricsRepo = container.lyricsRepository
    private val onlineRepo = container.onlineMusicRepository
    val playbackManager = container.playbackManager
    val audioEffectsController = container.audioEffectsController
    val volumeController = container.systemVolumeController

    val preferencesState: StateFlow<UserPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val playbackState: StateFlow<PlaybackUiState> = playbackManager.playbackState
    val audioEffectsState: StateFlow<AudioEffectsState> = audioEffectsController.state
    val volumeState: StateFlow<SystemVolumeState> = volumeController.volumeState

    /**
     * Strict Music-Only Songs State: Used by Music Home Screen and Music Library.
     * Alarms, Recordings, Ringtones, Notifications, Podcasts, Audiobooks, and Unclassified files
     * are strictly excluded from this flow.
     */
    val songsState: StateFlow<List<Song>> = libraryRepo.visibleSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSongsLookupState: StateFlow<List<Song>> = libraryRepo.allSongsIncludingHiddenFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * All Audio Inspector State: All local audio files across all 8 categories.
     */
    val allAudioInspectorState: StateFlow<List<Song>> = libraryRepo.allAudioInspectorFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedAudioCategory = MutableStateFlow(AudioCategory.MUSIC)
    val selectedAudioCategory: StateFlow<AudioCategory> = _selectedAudioCategory.asStateFlow()

    /**
     * Independent Category Library Songs Flow for the currently selected AudioCategory.
     */
    val activeCategorySongsState: StateFlow<List<Song>> = _selectedAudioCategory
        .flatMapLatest { category ->
            libraryRepo.observeSongsForCategory(category)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Live visible file counts for each of the 8 AudioCategory libraries.
     */
    val categoryCountsState: StateFlow<Map<AudioCategory, Int>> = libraryRepo.allAudioInspectorFlow
        .map { allLocal ->
            AudioCategory.entries.associateWith { cat ->
                allLocal.count { it.isVisibleInCategory(cat) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * Hidden or Category-Removed Items State: Used by the Hidden / Removed Items screen.
     */
    val hiddenSongsState: StateFlow<List<Song>> = libraryRepo.hiddenSongsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoritesState: StateFlow<List<Song>> = libraryRepo.favoritesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyAddedState: StateFlow<List<Song>> = libraryRepo.recentlyAddedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayedState: StateFlow<List<Song>> = libraryRepo.recentlyPlayedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mostPlayedState: StateFlow<List<Song>> = libraryRepo.mostPlayedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albumsState: StateFlow<List<Album>> = libraryRepo.albumsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val artistsState: StateFlow<List<Artist>> = libraryRepo.artistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val genresState: StateFlow<List<GenreItem>> = libraryRepo.genresFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foldersState: StateFlow<List<FolderItem>> = libraryRepo.foldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customPlaylistsState: StateFlow<List<Playlist>> = libraryRepo.customPlaylistsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarksState: StateFlow<List<PlaybackBookmark>> = libraryRepo.bookmarksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val namedQueuesState: StateFlow<List<NamedQueue>> = libraryRepo.namedQueuesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val safFoldersState: StateFlow<List<SafFolderEntity>> = libraryRepo.safFoldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchHistoryState: StateFlow<List<String>> = libraryRepo.searchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyPlayedOnlineState: StateFlow<List<Song>> = onlineRepo.recentlyPlayedOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _onlineCatalogState = MutableStateFlow(OnlineCatalogSnapshot())
    val onlineCatalogState: StateFlow<OnlineCatalogSnapshot> = _onlineCatalogState.asStateFlow()

    private val _diagnosticsReport = MutableStateFlow(LibraryDiagnosticsReport())
    val diagnosticsReport: StateFlow<LibraryDiagnosticsReport> = _diagnosticsReport.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResultsState: StateFlow<SearchResultsUiState> = combine(
        _searchQuery,
        songsState,
        albumsState,
        artistsState,
        customPlaylistsState,
        genresState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val query = (values[0] as String).trim()
        @Suppress("UNCHECKED_CAST")
        val songs = values[1] as List<Song>
        @Suppress("UNCHECKED_CAST")
        val albums = values[2] as List<Album>
        @Suppress("UNCHECKED_CAST")
        val artists = values[3] as List<Artist>
        @Suppress("UNCHECKED_CAST")
        val playlists = values[4] as List<Playlist>
        @Suppress("UNCHECKED_CAST")
        val genres = values[5] as List<GenreItem>

        if (query.isBlank()) {
            SearchResultsUiState(query = "")
        } else {
            val q = query.lowercase()
            SearchResultsUiState(
                query = query,
                matchingSongs = songs.filter {
                    it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q) ||
                        it.genre.lowercase().contains(q)
                },
                matchingAlbums = albums.filter {
                    it.name.lowercase().contains(q) || it.artist.lowercase().contains(q)
                },
                matchingArtists = artists.filter {
                    it.name.lowercase().contains(q)
                },
                matchingPlaylists = playlists.filter {
                    it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
                },
                matchingGenres = genres.filter {
                    it.name.lowercase().contains(q)
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResultsUiState())

    val currentLyricsState: StateFlow<SongLyrics?> = playbackState
        .flatMapLatest { pb ->
            val currentSong = pb.currentSong
            if (currentSong == null) {
                flowOf(null)
            } else {
                viewModelScope.launch { lyricsRepo.ensureLocalLyricsLoaded(currentSong) }
                lyricsRepo.observeLyricsForSong(currentSong)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _onlineLyricsSearchState = MutableStateFlow(OnlineLyricsSearchState())
    val onlineLyricsSearchState: StateFlow<OnlineLyricsSearchState> = _onlineLyricsSearchState.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private var lastKnownStoragePermission = false
    private var pendingRescanWithPermission = false

    init {
        libraryRepo.registerMediaStoreObserverIfNeeded()
        triggerLibraryScan(hasStoragePermission = false)
        refreshOnlineDiscoveryCatalog()

        // Real-time auto-sync when new files are downloaded or MediaStore changes
        viewModelScope.launch {
            libraryRepo.mediaStoreChangeSignal
                .drop(1)
                .debounce(800L)
                .collect {
                    triggerLibraryScan(
                        hasStoragePermission = lastKnownStoragePermission,
                        showCompletionBanner = false
                    )
                }
        }

        // Notify user when newly downloaded music files are automatically added to Music list
        viewModelScope.launch {
            libraryRepo.newlyAddedSongsSignal
                .drop(1)
                .collect { addedTitles ->
                    if (addedTitles.isNotEmpty()) {
                        val preview = addedTitles.first()
                        _statusBannerMessage.value = if (addedTitles.size == 1) {
                            "Auto-added downloaded song: \"$preview\""
                        } else {
                            "Auto-added ${addedTitles.size} downloaded songs (including \"$preview\")"
                        }
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        libraryRepo.unregisterMediaStoreObserver()
    }

    fun selectAudioCategory(category: AudioCategory) {
        _selectedAudioCategory.value = category
    }

    fun triggerLibraryScan(hasStoragePermission: Boolean, showCompletionBanner: Boolean = false) {
        val upgradedPermission = hasStoragePermission && !lastKnownStoragePermission
        lastKnownStoragePermission = hasStoragePermission || lastKnownStoragePermission
        if (_isScanning.value) {
            if (hasStoragePermission || upgradedPermission) {
                pendingRescanWithPermission = true
            }
            return
        }
        viewModelScope.launch {
            _isScanning.value = true
            do {
                pendingRescanWithPermission = false
                val effectivePerm = lastKnownStoragePermission
                runCatching {
                    val totalDiscovered = libraryRepo.rescanLibrary(effectivePerm)
                    if (playbackState.value.queue.isEmpty()) {
                        val (persistedQueue, _) = libraryRepo.loadPersistedQueue()
                        if (persistedQueue.isNotEmpty()) {
                            playbackManager.connectToService()
                        }
                    }
                    if (showCompletionBanner) {
                        _statusBannerMessage.value = "Scanned library ($totalDiscovered audio files classified)"
                    }
                }.onFailure { err ->
                    _statusBannerMessage.value = "Library scan error: ${err.localizedMessage ?: "Unknown"}"
                }
            } while (pendingRescanWithPermission)
            _isScanning.value = false
        }
    }

    fun clearStatusBanner() {
        _statusBannerMessage.value = null
    }

    fun showStatusMessage(message: String) {
        _statusBannerMessage.value = message
    }

    // --- NON-DESTRUCTIVE CATEGORY MANAGEMENT & RESTORATION ---

    fun moveSongToCategory(song: Song, targetCategory: AudioCategory) {
        viewModelScope.launch {
            libraryRepo.moveSongsToCategory(setOf(song.id), targetCategory)
            _statusBannerMessage.value =
                "Moved \"${song.title}\" to ${targetCategory.displayName} (File preserved)"
        }
    }

    fun moveSelectedSongsToCategory(songIds: Set<String>, targetCategory: AudioCategory) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            libraryRepo.moveSongsToCategory(songIds, targetCategory)
            _statusBannerMessage.value =
                "Moved ${songIds.size} item(s) to ${targetCategory.displayName} (Files preserved)"
        }
    }

    fun removeSongFromCategory(song: Song, category: AudioCategory = song.primaryCategory) {
        viewModelScope.launch {
            libraryRepo.removeSongsFromCategory(setOf(song.id), category)
            _statusBannerMessage.value =
                "Removed \"${song.title}\" from ${category.displayName} view (File kept on device)"
        }
    }

    fun removeSelectedSongsFromCategory(songIds: Set<String>, category: AudioCategory) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            libraryRepo.removeSongsFromCategory(songIds, category)
            _statusBannerMessage.value =
                "Removed ${songIds.size} item(s) from ${category.displayName} view (Files kept on device)"
        }
    }

    fun restoreSongsToCategory(songIds: Set<String>, destinationCategory: AudioCategory? = null) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            libraryRepo.restoreSongsToCategory(songIds, destinationCategory)
            val destLabel = destinationCategory?.displayName ?: "their assigned categories"
            _statusBannerMessage.value = "Restored ${songIds.size} item(s) to $destLabel"
        }
    }

    fun deleteSongFromDevice(song: Song) {
        viewModelScope.launch {
            libraryRepo.deleteSongsFromDevice(setOf(song.id))
            _statusBannerMessage.value = "Deleted \"${song.title}\" from device"
        }
    }

    fun deleteSelectedSongsFromDevice(songIds: Set<String>) {
        if (songIds.isEmpty()) return
        viewModelScope.launch {
            val count = libraryRepo.deleteSongsFromDevice(songIds)
            _statusBannerMessage.value = "Deleted $count file(s) from device"
        }
    }

    // --- ONBOARDING, THREE.JS INTRO, THEME, LOCALIZATION & UX PREFERENCES ---
    fun completeOnboarding(hasStoragePermission: Boolean) {
        viewModelScope.launch {
            prefsRepo.setOnboardingCompleted(true)
            triggerLibraryScan(hasStoragePermission = hasStoragePermission)
        }
    }

    fun setShowThreeJsIntro(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setShowThreeJsIntro(enabled)
        }
    }

    fun markThreeJsIntroCompletedOnce() {
        viewModelScope.launch {
            prefsRepo.markThreeJsIntroCompletedOnce()
        }
    }

    fun setThemeMode(mode: PlayMixThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDynamicColor(enabled) }
    }

    fun setAccentColor(accent: AccentColorOption) {
        viewModelScope.launch { prefsRepo.setAccentColor(accent) }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            prefsRepo.setAppLanguage(language)
            _statusBannerMessage.value = "Language switched to ${language.displayName}"
        }
    }

    fun setSortOption(option: SortOption) {
        viewModelScope.launch { prefsRepo.setSortOption(option) }
    }

    fun setGroupByAlbumArtist(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setGroupByAlbumArtist(enabled) }
    }

    fun setGaplessPlayback(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setGaplessPlayback(enabled) }
    }

    fun setCrossfadeDurationSec(seconds: Int) {
        viewModelScope.launch { prefsRepo.setCrossfadeDurationSec(seconds) }
    }

    fun setFadeInOutEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setFadeInOutEnabled(enabled) }
    }

    fun setSkipIntervalSec(seconds: Int) {
        viewModelScope.launch { prefsRepo.setSkipIntervalSec(seconds) }
    }

    fun setResumeLastSession(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setResumeLastSession(enabled) }
    }

    fun setAutoRescanOnStartup(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoRescanOnStartup(enabled) }
    }

    fun setAnimatedVinylMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAnimatedVinylMode(enabled) }
    }

    fun setSwipeArtworkChangeTrack(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setSwipeArtworkChangeTrack(enabled) }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setHapticFeedbackEnabled(enabled) }
    }

    fun setHomeSectionVisibility(smartMixes: Boolean, albums: Boolean, artists: Boolean) {
        viewModelScope.launch {
            prefsRepo.setHomeSectionVisibility(smartMixes, albums, artists)
        }
    }

    fun setMinScanDurationSec(seconds: Int) {
        viewModelScope.launch { prefsRepo.setMinScanDurationSec(seconds) }
    }

    fun setIncludeDemoTracks(include: Boolean) {
        viewModelScope.launch { prefsRepo.setIncludeDemoTracks(include) }
    }

    // --- SONG, RATING, TAG EDITOR & DIAGNOSTICS ---
    fun toggleFavorite(songId: String) {
        viewModelScope.launch {
            val isNowFav = libraryRepo.toggleFavorite(songId)
            _statusBannerMessage.value = if (isNowFav) "Added to Favorites" else "Removed from Favorites"
        }
    }

    fun setSongRating(songId: String, rating: Int) {
        viewModelScope.launch {
            libraryRepo.setSongRating(songId, rating)
            _statusBannerMessage.value = "Rated $rating ★"
        }
    }

    fun updateSongTagsAndArtwork(
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
    ) {
        viewModelScope.launch {
            libraryRepo.updateSongTagsAndArtwork(
                songId, title, artist, album, albumArtist, genre, year, trackNumber, rating, isCompilation, customArtworkUri
            )
            _statusBannerMessage.value = "Updated metadata tags for \"$title\""
        }
    }

    fun bulkUpdateSongsMetadata(
        songIds: Set<String>,
        artist: String?,
        album: String?,
        albumArtist: String?,
        genre: String?,
        year: Int?,
        isCompilation: Boolean?
    ) {
        viewModelScope.launch {
            libraryRepo.bulkUpdateSongsMetadata(songIds, artist, album, albumArtist, genre, year, isCompilation)
            _statusBannerMessage.value = "Updated metadata for ${songIds.size} selected tracks"
        }
    }

    fun runLibraryDiagnostics() {
        viewModelScope.launch {
            val dups = libraryRepo.findDuplicateTracks()
            val missing = libraryRepo.findMissingFileTracks()
            _diagnosticsReport.value = LibraryDiagnosticsReport(
                duplicateTracks = dups,
                missingFileTracks = missing
            )
            _statusBannerMessage.value = "Diagnostics: ${dups.size} duplicates, ${missing.size} missing files"
        }
    }

    fun cleanUpMissingTracks() {
        viewModelScope.launch {
            val removed = libraryRepo.removeMissingTracks()
            runLibraryDiagnostics()
            _statusBannerMessage.value = "Cleaned up $removed missing track entries"
        }
    }

    fun hideSong(song: Song) {
        viewModelScope.launch {
            libraryRepo.setSongHidden(song.id, true)
            _statusBannerMessage.value = "Hidden \"${song.title}\" from library (File kept on device)"
        }
    }

    fun hideSelectedSongs(songIds: Set<String>) {
        viewModelScope.launch {
            songIds.forEach { libraryRepo.setSongHidden(it, true) }
            _statusBannerMessage.value = "Hidden ${songIds.size} selected tracks (Files kept on device)"
        }
    }

    fun unhideAllSongs() {
        viewModelScope.launch {
            libraryRepo.unhideAllSongs()
            _statusBannerMessage.value = "Restored all hidden and category-removed items"
        }
    }

    fun toggleFolderExclusion(folder: FolderItem) {
        viewModelScope.launch {
            libraryRepo.toggleFolderExclusion(folder.name, folder.isExcluded)
            _statusBannerMessage.value = if (folder.isExcluded) {
                "Included folder \"${folder.name}\""
            } else {
                "Excluded folder \"${folder.name}\""
            }
        }
    }

    fun addSafFolder(treeUri: String, displayName: String) {
        viewModelScope.launch {
            _isScanning.value = true
            val count = libraryRepo.addSafFolderAndScan(treeUri, displayName)
            _isScanning.value = false
            _statusBannerMessage.value = "Added folder \"$displayName\" ($count audio tracks)"
        }
    }

    fun removeSafFolder(treeUri: String) {
        viewModelScope.launch {
            libraryRepo.removeSafFolder(treeUri)
            _statusBannerMessage.value = "Removed folder from SAF scan list"
        }
    }

    // --- BOOKMARKS & NAMED QUEUES ---
    fun addBookmarkForCurrentSong(label: String) {
        val pb = playbackState.value
        val song = pb.currentSong ?: return
        viewModelScope.launch {
            libraryRepo.addBookmark(song.id, pb.currentPositionMs, label)
            _statusBannerMessage.value = "Added bookmark \"${label.ifBlank { "Marker" }}\""
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            libraryRepo.deleteBookmark(bookmarkId)
        }
    }

    fun saveCurrentAsNamedQueue(queueName: String) {
        val pb = playbackState.value
        if (pb.queue.isEmpty()) return
        viewModelScope.launch {
            libraryRepo.saveNamedQueue(queueName, pb.queue, pb.currentQueueIndex, pb.currentPositionMs)
            _statusBannerMessage.value = "Saved queue snapshot \"$queueName\""
        }
    }

    fun restoreNamedQueue(namedQueue: NamedQueue) {
        val songMap = allSongsLookupState.value.associateBy { it.id }
        val resolved = namedQueue.songIds.mapNotNull { songMap[it] }
        if (resolved.isNotEmpty()) {
            playbackManager.playSongFromQueue(
                songs = resolved,
                startIndex = namedQueue.currentIndex.coerceIn(0, resolved.lastIndex),
                startPositionMs = namedQueue.currentPositionMs
            )
            _statusBannerMessage.value = "Restored queue \"${namedQueue.name}\""
        }
    }

    fun deleteNamedQueue(queueId: Long) {
        viewModelScope.launch {
            libraryRepo.deleteNamedQueue(queueId)
        }
    }

    // --- PLAYLIST ACTIONS ---
    fun createPlaylist(name: String, description: String = "", initialSongIds: List<String> = emptyList()) {
        viewModelScope.launch {
            libraryRepo.createPlaylist(name, description, initialSongIds)
            _statusBannerMessage.value = "Created playlist \"$name\""
        }
    }

    fun saveCurrentQueueAsPlaylist(name: String) {
        val currentQueueIds = playbackState.value.queue.map { it.id }
        if (currentQueueIds.isEmpty()) {
            _statusBannerMessage.value = "Queue is currently empty"
            return
        }
        viewModelScope.launch {
            libraryRepo.createPlaylist(
                name = name.ifBlank { "Saved Queue" },
                description = "Saved from active playback queue (${currentQueueIds.size} tracks)",
                initialSongIds = currentQueueIds
            )
            _statusBannerMessage.value = "Saved queue as playlist \"$name\""
        }
    }

    fun renamePlaylist(playlistId: Long, newName: String, description: String = "") {
        viewModelScope.launch {
            libraryRepo.renamePlaylist(playlistId, newName, description)
            _statusBannerMessage.value = "Updated playlist \"$newName\""
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            libraryRepo.deletePlaylist(playlistId)
            _statusBannerMessage.value = "Deleted playlist"
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            libraryRepo.addSongToPlaylist(playlistId, song.id)
            _statusBannerMessage.value = "Added \"${song.title}\" to playlist"
        }
    }

    fun addSongsToPlaylist(playlistId: Long, songIds: Set<String>) {
        viewModelScope.launch {
            songIds.forEach { libraryRepo.addSongToPlaylist(playlistId, it) }
            _statusBannerMessage.value = "Added ${songIds.size} tracks to playlist"
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch {
            libraryRepo.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun reorderPlaylistSongs(playlistId: Long, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            libraryRepo.reorderPlaylistSongs(playlistId, fromIndex, toIndex)
        }
    }

    fun importM3uPlaylist(name: String, m3uContent: String) {
        viewModelScope.launch {
            libraryRepo.importM3uPlaylist(name, m3uContent)
            _statusBannerMessage.value = "Imported M3U/M3U8 playlist \"$name\""
        }
    }

    suspend fun exportPlaylistM3u(playlist: Playlist): String {
        return libraryRepo.exportPlaylistToM3u(playlist, utf8Header = true)
    }

    // --- BACKUP & RESTORE ---
    suspend fun exportFullBackupJson(): String {
        return libraryRepo.exportFullBackupJson()
    }

    fun restoreFullBackupJson(rawJson: String) {
        viewModelScope.launch {
            val ok = libraryRepo.restoreFullBackupJson(rawJson)
            _statusBannerMessage.value = if (ok) {
                "Restored playlists, favorites, category overrides, EQ presets, and settings"
            } else {
                "Invalid backup JSON format"
            }
        }
    }

    fun getSmartPlaylistSongs(type: SmartPlaylistType): List<Song> {
        return when (type) {
            SmartPlaylistType.FAVORITES -> favoritesState.value
            SmartPlaylistType.RECENTLY_PLAYED -> recentlyPlayedState.value
            SmartPlaylistType.MOST_PLAYED -> mostPlayedState.value
            SmartPlaylistType.RECENTLY_ADDED -> recentlyAddedState.value
        }
    }

    // --- SEARCH ---
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun commitSearchQueryToHistory(query: String) {
        viewModelScope.launch { libraryRepo.saveSearchQuery(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { libraryRepo.clearSearchHistory() }
    }

    // --- LYRICS ---
    fun searchOnlineLyricsForCurrentSong(customQuery: String? = null) {
        val song = playbackState.value.currentSong ?: return
        viewModelScope.launch {
            _onlineLyricsSearchState.update { it.copy(isSearching = true, errorMessage = null) }
            val result = lyricsRepo.searchOnlineLyrics(song, customQuery)
            result.onSuccess { list ->
                _onlineLyricsSearchState.update {
                    it.copy(
                        isSearching = false,
                        results = list,
                        errorMessage = if (list.isEmpty()) "No matching synchronized lyrics found on LRCLIB." else null
                    )
                }
            }.onFailure { err ->
                _onlineLyricsSearchState.update {
                    it.copy(
                        isSearching = false,
                        errorMessage = "LRCLIB lookup failed (${err.localizedMessage ?: "Check internet connection"})."
                    )
                }
            }
        }
    }

    fun applyOnlineLyricsResult(result: LrcLibSearchResult) {
        val song = playbackState.value.currentSong ?: return
        viewModelScope.launch {
            lyricsRepo.applyLrcLibResult(song.id, result)
            _onlineLyricsSearchState.update { OnlineLyricsSearchState() }
            _statusBannerMessage.value = "Cached synced lyrics for \"${song.title}\""
        }
    }

    fun saveManualLyricsForCurrentSong(rawContent: String) {
        val song = playbackState.value.currentSong ?: return
        viewModelScope.launch {
            lyricsRepo.saveManualLyrics(song.id, rawContent)
            _statusBannerMessage.value = "Saved lyrics for \"${song.title}\""
        }
    }

    fun adjustCurrentLyricsOffset(deltaMs: Long) {
        val current = currentLyricsState.value ?: return
        viewModelScope.launch {
            lyricsRepo.adjustLyricsOffset(current.songId, current.offsetMs + deltaMs)
        }
    }

    // --- EQUALIZER, PREAMP, BALANCE, REPLAYGAIN & SLEEP TIMER ---
    fun selectEqualizerPreset(preset: EqualizerPreset) {
        audioEffectsController.selectPreset(preset)
    }

    fun applyCustomEqualizerPreset(preset: CustomEqualizerPreset) {
        audioEffectsController.applyCustomPreset(preset)
    }

    fun saveCustomEqualizerPreset(name: String) {
        audioEffectsController.saveCurrentAsCustomPreset(name)
        _statusBannerMessage.value = "Saved custom EQ preset \"$name\""
    }

    fun deleteCustomEqualizerPreset(name: String) {
        audioEffectsController.deleteCustomPreset(name)
    }

    fun setEqualizerBandLevel(index: Int, levelMb: Int) {
        audioEffectsController.setBandLevel(index, levelMb)
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        audioEffectsController.setEqualizerEnabled(enabled)
    }

    fun setPreampAndBalance(preampDb: Float, balance: Float, mono: Boolean, clipPrevention: Boolean) {
        audioEffectsController.setPreampAndSpatialBalance(preampDb, balance, mono, clipPrevention)
    }

    fun setReplayGainMode(mode: ReplayGainMode) {
        audioEffectsController.setReplayGainMode(mode)
    }

    fun setBassBoost(strength: Int) {
        audioEffectsController.setBassBoostStrength(strength)
    }

    fun setVirtualizer(strength: Int) {
        audioEffectsController.setVirtualizerStrength(strength)
    }

    fun setLoudnessNormalization(enabled: Boolean) {
        audioEffectsController.setLoudnessNormalization(enabled)
    }

    fun resetAudioEffects() {
        audioEffectsController.resetToDefault()
        _statusBannerMessage.value = "Reset Equalizer & DSP Effects to Flat"
    }

    fun startSleepTimer(option: SleepTimerOption, customMinutes: Int = 20) {
        playbackManager.startSleepTimer(option, customMinutes)
        if (option == SleepTimerOption.OFF) {
            _statusBannerMessage.value = "Sleep timer turned off"
        } else {
            _statusBannerMessage.value = "Sleep timer set: ${option.label}"
        }
    }

    // --- UNIVERSAL ONLINE MUSIC DISCOVERY MODULE ---
    fun refreshOnlineDiscoveryCatalog() {
        viewModelScope.launch {
            _onlineCatalogState.update {
                it.copy(
                    providers = onlineRepo.buildProviderList(preferencesState.value),
                    isLoading = true,
                    networkMessage = null
                )
            }
            val snapshot = onlineRepo.fetchOnlineDiscoveryCatalog()
            _onlineCatalogState.value = snapshot
        }
    }

    fun searchOnlineCatalog(query: String) {
        viewModelScope.launch {
            _onlineCatalogState.update { it.copy(isLoading = true, networkMessage = null) }
            val res = onlineRepo.searchOnlineProviders(query)
            res.onSuccess { list ->
                _onlineCatalogState.update {
                    it.copy(
                        searchResults = list,
                        isLoading = false,
                        networkMessage = if (list.isEmpty()) "No authorized online streams matched \"$query\"." else null
                    )
                }
            }.onFailure { err ->
                _onlineCatalogState.update {
                    it.copy(
                        isLoading = false,
                        networkMessage = err.localizedMessage ?: "Online search unavailable"
                    )
                }
            }
        }
    }

    fun playOnlineSongs(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            songs.forEach { onlineRepo.cacheOnlineSongForPlayback(it) }
            playbackManager.playSongFromQueue(songs, startIndex)
        }
    }

    fun saveOnlineProviderSettings(
        jamendoClientId: String,
        spotifyClientId: String,
        soundCloudClientId: String,
        enabledProvidersCsv: String
    ) {
        viewModelScope.launch {
            prefsRepo.setOnlineProviderCredentials(
                jamendoClientId = jamendoClientId,
                spotifyClientId = spotifyClientId,
                soundCloudClientId = soundCloudClientId,
                enabledProvidersCsv = enabledProvidersCsv
            )
            _statusBannerMessage.value = "Updated Online Music Provider configuration"
            refreshOnlineDiscoveryCatalog()
        }
    }

    companion object {
        fun provideFactory(container: PlayMixAppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PlayMixViewModel(container) as T
                }
            }
    }
}
