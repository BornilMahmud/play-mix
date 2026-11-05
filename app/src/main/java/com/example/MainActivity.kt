package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.AddToPlaylistDialog
import com.example.core.ui.CompactMiniPlayer
import com.example.core.ui.TagAndArtworkEditorDialog
import com.example.domain.model.Playlist
import com.example.domain.model.SmartPlaylistType
import com.example.domain.model.Song
import com.example.feature.PlayMixViewModel
import com.example.feature.home.HomeScreen
import com.example.feature.library.AlbumDetailScreen
import com.example.feature.library.ArtistDetailScreen
import com.example.feature.library.LibraryScreen
import com.example.feature.library.PlaylistDetailScreen
import com.example.feature.onboarding.OnboardingScreen
import com.example.feature.onboarding.ThreeJsIntroOverlay
import com.example.feature.online.OnlineMusicScreen
import com.example.feature.player.EqualizerAndEffectsContent
import com.example.feature.player.NowPlayingScreen
import com.example.feature.search.SearchScreen
import com.example.feature.settings.SettingsScreen
import com.example.ui.theme.LocalPlayMixStrings
import com.example.ui.theme.PlayMixTheme
import com.example.ui.theme.SoftCyan
import kotlinx.coroutines.delay

enum class MainDestination {
    HOME,
    LIBRARY,
    ONLINE,
    EQUALIZER,
    SETTINGS
}

sealed interface SubScreenRoute {
    data object None : SubScreenRoute
    data object Search : SubScreenRoute
    data class AlbumDetail(val albumId: String) : SubScreenRoute
    data class ArtistDetail(val artistId: String) : SubScreenRoute
    data class CustomPlaylistDetail(val playlistId: Long) : SubScreenRoute
    data class SmartPlaylistDetail(val smartType: SmartPlaylistType) : SubScreenRoute
}

class MainActivity : ComponentActivity() {

    private val viewModel: PlayMixViewModel by viewModels {
        PlayMixViewModel.provideFactory((application as PlayMixApplication).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by viewModel.preferencesState.collectAsStateWithLifecycle()
            PlayMixTheme(
                themeMode = prefs.themeMode,
                dynamicColor = prefs.dynamicColor,
                accentColor = prefs.accentColor,
                appLanguage = prefs.appLanguage
            ) {
                PlayMixAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun PlayMixAppRoot(viewModel: PlayMixViewModel) {
    val context = LocalContext.current
    val strings = LocalPlayMixStrings.current
    val prefs by viewModel.preferencesState.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val effectsState by viewModel.audioEffectsState.collectAsStateWithLifecycle()
    val volumeState by viewModel.volumeState.collectAsStateWithLifecycle()
    val songs by viewModel.songsState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedAudioCategory.collectAsStateWithLifecycle()
    val activeCategorySongs by viewModel.activeCategorySongsState.collectAsStateWithLifecycle()
    val categoryCounts by viewModel.categoryCountsState.collectAsStateWithLifecycle()
    val allAudioInspectorSongs by viewModel.allAudioInspectorState.collectAsStateWithLifecycle()
    val hiddenSongs by viewModel.hiddenSongsState.collectAsStateWithLifecycle()
    val favorites by viewModel.favoritesState.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAddedState.collectAsStateWithLifecycle()
    val recentlyPlayed by viewModel.recentlyPlayedState.collectAsStateWithLifecycle()
    val albums by viewModel.albumsState.collectAsStateWithLifecycle()
    val artists by viewModel.artistsState.collectAsStateWithLifecycle()
    val genres by viewModel.genresState.collectAsStateWithLifecycle()
    val folders by viewModel.foldersState.collectAsStateWithLifecycle()
    val customPlaylists by viewModel.customPlaylistsState.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val namedQueues by viewModel.namedQueuesState.collectAsStateWithLifecycle()
    val safFolders by viewModel.safFoldersState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResultsState.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistoryState.collectAsStateWithLifecycle()
    val currentLyrics by viewModel.currentLyricsState.collectAsStateWithLifecycle()
    val onlineLyricsSearch by viewModel.onlineLyricsSearchState.collectAsStateWithLifecycle()
    val onlineCatalogState by viewModel.onlineCatalogState.collectAsStateWithLifecycle()
    val recentlyPlayedOnline by viewModel.recentlyPlayedOnlineState.collectAsStateWithLifecycle()
    val diagnosticsReport by viewModel.diagnosticsReport.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(MainDestination.HOME) }
    var subRoute by remember { mutableStateOf<SubScreenRoute>(SubScreenRoute.None) }
    var isNowPlayingExpanded by rememberSaveable { mutableStateOf(false) }
    var songForPlaylistDialog by remember { mutableStateOf<Song?>(null) }
    var songForTagEditorDialog by remember { mutableStateOf<Song?>(null) }
    var introDismissedThisSession by rememberSaveable { mutableStateOf(false) }

    val audioPermissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermissionName) == PackageManager.PERMISSION_GRANTED
        )
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val audioGranted = result[audioPermissionName] == true || hasAudioPermission
        hasAudioPermission = audioGranted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = result[Manifest.permission.POST_NOTIFICATIONS] == true || hasNotificationPermission
        }
        if (audioGranted && !prefs.onboardingCompleted) {
            viewModel.completeOnboarding(hasStoragePermission = true)
        } else {
            viewModel.triggerLibraryScan(hasStoragePermission = audioGranted, showCompletionBanner = true)
        }
    }

    val safFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val docName = DocumentFile.fromTreeUri(context, treeUri)?.name ?: "Music Folder"
            viewModel.addSafFolder(treeUri.toString(), docName)
            if (!prefs.onboardingCompleted) {
                viewModel.completeOnboarding(hasStoragePermission = hasAudioPermission)
            }
        }
    }

    LaunchedEffect(hasAudioPermission) {
        if (hasAudioPermission) {
            if (!prefs.onboardingCompleted) {
                viewModel.completeOnboarding(hasStoragePermission = true)
            } else if (prefs.autoRescanOnStartup) {
                viewModel.triggerLibraryScan(hasStoragePermission = true)
            }
        }
    }

    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(3200L)
            viewModel.clearStatusBanner()
        }
    }

    if (!prefs.onboardingCompleted) {
        Box(modifier = Modifier.fillMaxSize()) {
            OnboardingScreen(
                hasAudioPermission = hasAudioPermission,
                hasNotificationPermission = hasNotificationPermission,
                onRequestPermissions = {
                    val perms = buildList {
                        add(audioPermissionName)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                    permissionsLauncher.launch(perms)
                },
                onPickSafFolder = { safFolderPickerLauncher.launch(null) },
                onContinueToApp = {
                    viewModel.completeOnboarding(hasStoragePermission = hasAudioPermission)
                }
            )

            AnimatedVisibility(
                visible = prefs.showThreeJsIntro && !introDismissedThisSession,
                exit = fadeOut()
            ) {
                ThreeJsIntroOverlay(
                    isShortTransition = prefs.hasCompletedThreeJsIntroOnce,
                    onIntroFinished = {
                        introDismissedThisSession = true
                        viewModel.markThreeJsIntroCompletedOnce()
                    }
                )
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // Floating Compact Mini-Player
                    if (playbackState.currentSong != null) {
                        CompactMiniPlayer(
                            playbackState = playbackState,
                            onOpenNowPlaying = { isNowPlayingExpanded = true },
                            onTogglePlayPause = { viewModel.playbackManager.togglePlayPause() },
                            onSkipNext = { viewModel.playbackManager.skipToNext() },
                            onToggleFavorite = {
                                playbackState.currentSong?.id?.let { viewModel.toggleFavorite(it) }
                            }
                        )
                    }

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        NavigationBarItem(
                            selected = currentTab == MainDestination.HOME && subRoute == SubScreenRoute.None,
                            onClick = {
                                subRoute = SubScreenRoute.None
                                currentTab = MainDestination.HOME
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = strings.navHome) },
                            label = { Text(strings.navHome, maxLines = 1) },
                            modifier = Modifier.testTag("nav_home")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainDestination.LIBRARY && subRoute == SubScreenRoute.None,
                            onClick = {
                                subRoute = SubScreenRoute.None
                                currentTab = MainDestination.LIBRARY
                            },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = strings.navLibrary) },
                            label = { Text(strings.navLibrary, maxLines = 1) },
                            modifier = Modifier.testTag("nav_library")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainDestination.ONLINE && subRoute == SubScreenRoute.None,
                            onClick = {
                                subRoute = SubScreenRoute.None
                                currentTab = MainDestination.ONLINE
                            },
                            icon = { Icon(Icons.Default.Cloud, contentDescription = strings.navOnline) },
                            label = { Text(strings.navOnline, maxLines = 1) },
                            modifier = Modifier.testTag("nav_online")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainDestination.EQUALIZER && subRoute == SubScreenRoute.None,
                            onClick = {
                                subRoute = SubScreenRoute.None
                                currentTab = MainDestination.EQUALIZER
                            },
                            icon = { Icon(Icons.Default.Equalizer, contentDescription = strings.navEqualizer) },
                            label = { Text(strings.navEqualizer, maxLines = 1) },
                            modifier = Modifier.testTag("nav_equalizer")
                        )
                        NavigationBarItem(
                            selected = currentTab == MainDestination.SETTINGS && subRoute == SubScreenRoute.None,
                            onClick = {
                                subRoute = SubScreenRoute.None
                                currentTab = MainDestination.SETTINGS
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = strings.navSettings) },
                            label = { Text(strings.navSettings, maxLines = 1) },
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                when (val activeSub = subRoute) {
                    is SubScreenRoute.Search -> {
                        SearchScreen(
                            searchQuery = searchQuery,
                            searchResults = searchResults,
                            searchHistory = searchHistory,
                            playbackState = playbackState,
                            onQueryChange = viewModel::updateSearchQuery,
                            onCommitQuery = viewModel::commitSearchQueryToHistory,
                            onClearHistory = viewModel::clearSearchHistory,
                            onPlaySongList = { list, idx ->
                                viewModel.playbackManager.playSongFromQueue(list, idx)
                            },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onPlayNext = viewModel.playbackManager::playNextInQueue,
                            onAddToQueue = viewModel.playbackManager::addToQueue,
                            onRequestAddToPlaylist = { songForPlaylistDialog = it },
                            onOpenAlbum = { subRoute = SubScreenRoute.AlbumDetail(it) },
                            onOpenArtist = { subRoute = SubScreenRoute.ArtistDetail(it) },
                            onOpenPlaylist = { subRoute = SubScreenRoute.CustomPlaylistDetail(it) },
                            onBack = { subRoute = SubScreenRoute.None }
                        )
                    }

                    is SubScreenRoute.AlbumDetail -> {
                        val album = albums.firstOrNull { it.id == activeSub.albumId }
                        AlbumDetailScreen(
                            album = album,
                            playbackState = playbackState,
                            onPlaySongList = { list, idx ->
                                viewModel.playbackManager.playSongFromQueue(list, idx)
                            },
                            onShufflePlay = { viewModel.playbackManager.shuffleAndPlay(it) },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onPlayNext = viewModel.playbackManager::playNextInQueue,
                            onAddToQueue = viewModel.playbackManager::addToQueue,
                            onRequestAddToPlaylist = { songForPlaylistDialog = it },
                            onEditSongTags = { songForTagEditorDialog = it },
                            onBack = { subRoute = SubScreenRoute.None }
                        )
                    }

                    is SubScreenRoute.ArtistDetail -> {
                        val artist = artists.firstOrNull { it.id == activeSub.artistId }
                        ArtistDetailScreen(
                            artist = artist,
                            playbackState = playbackState,
                            onPlaySongList = { list, idx ->
                                viewModel.playbackManager.playSongFromQueue(list, idx)
                            },
                            onShufflePlay = { viewModel.playbackManager.shuffleAndPlay(it) },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onPlayNext = viewModel.playbackManager::playNextInQueue,
                            onAddToQueue = viewModel.playbackManager::addToQueue,
                            onRequestAddToPlaylist = { songForPlaylistDialog = it },
                            onEditSongTags = { songForTagEditorDialog = it },
                            onBack = { subRoute = SubScreenRoute.None }
                        )
                    }

                    is SubScreenRoute.CustomPlaylistDetail -> {
                        val playlist = customPlaylists.firstOrNull { it.id == activeSub.playlistId }
                        PlaylistDetailScreen(
                            playlist = playlist,
                            playbackState = playbackState,
                            onPlaySongList = { list, idx ->
                                viewModel.playbackManager.playSongFromQueue(list, idx)
                            },
                            onShufflePlay = { viewModel.playbackManager.shuffleAndPlay(it) },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onPlayNext = viewModel.playbackManager::playNextInQueue,
                            onAddToQueue = viewModel.playbackManager::addToQueue,
                            onRemoveSongFromPlaylist = viewModel::removeSongFromPlaylist,
                            onReorderPlaylistSong = viewModel::reorderPlaylistSongs,
                            onRenamePlaylist = viewModel::renamePlaylist,
                            onDeletePlaylist = viewModel::deletePlaylist,
                            onExportM3u = viewModel::exportPlaylistM3u,
                            onBack = { subRoute = SubScreenRoute.None }
                        )
                    }

                    is SubScreenRoute.SmartPlaylistDetail -> {
                        val smartSongs = viewModel.getSmartPlaylistSongs(activeSub.smartType)
                        val smartPlaylist = Playlist(
                            id = -1L,
                            name = activeSub.smartType.title,
                            description = activeSub.smartType.subtitle,
                            isSmart = true,
                            smartType = activeSub.smartType,
                            songs = smartSongs
                        )
                        PlaylistDetailScreen(
                            playlist = smartPlaylist,
                            playbackState = playbackState,
                            onPlaySongList = { list, idx ->
                                viewModel.playbackManager.playSongFromQueue(list, idx)
                            },
                            onShufflePlay = { viewModel.playbackManager.shuffleAndPlay(it) },
                            onToggleFavorite = viewModel::toggleFavorite,
                            onPlayNext = viewModel.playbackManager::playNextInQueue,
                            onAddToQueue = viewModel.playbackManager::addToQueue,
                            onRemoveSongFromPlaylist = { _, _ -> },
                            onReorderPlaylistSong = { _, _, _ -> },
                            onRenamePlaylist = { _, _, _ -> },
                            onDeletePlaylist = {},
                            onExportM3u = viewModel::exportPlaylistM3u,
                            onBack = { subRoute = SubScreenRoute.None }
                        )
                    }

                    SubScreenRoute.None -> {
                        when (currentTab) {
                            MainDestination.HOME -> {
                                HomeScreen(
                                    songs = songs,
                                    recentlyPlayed = recentlyPlayed,
                                    recentlyAdded = recentlyAdded,
                                    favorites = favorites,
                                    albums = albums,
                                    artists = artists,
                                    playlists = customPlaylists,
                                    playbackState = playbackState,
                                    showSmartMixes = prefs.showHomeSmartMixes,
                                    showAlbums = prefs.showHomeAlbums,
                                    showArtists = prefs.showHomeArtists,
                                    onPlaySongList = { list, idx ->
                                        viewModel.playbackManager.playSongFromQueue(list, idx)
                                    },
                                    onShufflePlay = { viewModel.playbackManager.shuffleAndPlay(it) },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onPlayNext = viewModel.playbackManager::playNextInQueue,
                                    onAddToQueue = viewModel.playbackManager::addToQueue,
                                    onRequestAddToPlaylist = { songForPlaylistDialog = it },
                                    onOpenAlbum = { subRoute = SubScreenRoute.AlbumDetail(it) },
                                    onOpenArtist = { subRoute = SubScreenRoute.ArtistDetail(it) },
                                    onOpenPlaylist = { subRoute = SubScreenRoute.CustomPlaylistDetail(it) },
                                    onOpenSmartPlaylist = { subRoute = SubScreenRoute.SmartPlaylistDetail(it) },
                                    onOpenSearch = { subRoute = SubScreenRoute.Search },
                                    onOpenEqualizer = { currentTab = MainDestination.EQUALIZER },
                                    onRescanLibrary = {
                                        viewModel.triggerLibraryScan(
                                            hasStoragePermission = hasAudioPermission,
                                            showCompletionBanner = true
                                        )
                                    },
                                    onHideSong = viewModel::hideSong,
                                    onMoveSongToCategory = viewModel::moveSongToCategory,
                                    onRemoveSongFromCategory = viewModel::removeSongFromCategory,
                                    onDeleteSongFromDevice = viewModel::deleteSongFromDevice,
                                    onEditSongTags = { songForTagEditorDialog = it }
                                )
                            }

                            MainDestination.LIBRARY -> {
                                BackHandler(enabled = true) {
                                    currentTab = MainDestination.HOME
                                }
                                LibraryScreen(
                                    songs = songs,
                                    selectedCategory = selectedCategory,
                                    activeCategorySongs = activeCategorySongs,
                                    categoryCounts = categoryCounts,
                                    allAudioInspectorSongs = allAudioInspectorSongs,
                                    hiddenAndRemovedSongs = hiddenSongs,
                                    albums = albums,
                                    artists = artists,
                                    playlists = customPlaylists,
                                    genres = genres,
                                    folders = folders,
                                    currentSort = prefs.sortOption,
                                    playbackState = playbackState,
                                    onSelectCategory = viewModel::selectAudioCategory,
                                    onSelectSort = viewModel::setSortOption,
                                    onPlaySongList = { list, idx ->
                                        viewModel.playbackManager.playSongFromQueue(list, idx)
                                    },
                                    onShufflePlay = { viewModel.playbackManager.shuffleAndPlay(it) },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onPlayNext = viewModel.playbackManager::playNextInQueue,
                                    onAddToQueue = viewModel.playbackManager::addToQueue,
                                    onRequestAddToPlaylist = { songForPlaylistDialog = it },
                                    onHideSong = viewModel::hideSong,
                                    onHideSelectedSongs = viewModel::hideSelectedSongs,
                                    onMoveSongToCategory = viewModel::moveSongToCategory,
                                    onMoveSelectedSongsToCategory = viewModel::moveSelectedSongsToCategory,
                                    onRemoveSongFromCategory = viewModel::removeSongFromCategory,
                                    onRemoveSelectedSongsFromCategory = viewModel::removeSelectedSongsFromCategory,
                                    onRestoreSongsToCategory = viewModel::restoreSongsToCategory,
                                    onDeleteSongFromDevice = viewModel::deleteSongFromDevice,
                                    onDeleteSelectedSongsFromDevice = viewModel::deleteSelectedSongsFromDevice,
                                    onEditSongTags = { songForTagEditorDialog = it },
                                    onBulkEditTags = viewModel::bulkUpdateSongsMetadata,
                                    onOpenAlbum = { subRoute = SubScreenRoute.AlbumDetail(it) },
                                    onOpenArtist = { subRoute = SubScreenRoute.ArtistDetail(it) },
                                    onOpenPlaylist = { subRoute = SubScreenRoute.CustomPlaylistDetail(it) },
                                    onOpenSmartPlaylist = { subRoute = SubScreenRoute.SmartPlaylistDetail(it) },
                                    onCreatePlaylist = { name, desc -> viewModel.createPlaylist(name, desc) },
                                    onImportM3u = viewModel::importM3uPlaylist,
                                    onToggleFolderExclusion = viewModel::toggleFolderExclusion,
                                    onPickSafFolder = { safFolderPickerLauncher.launch(null) },
                                    onRescanLibrary = {
                                        viewModel.triggerLibraryScan(
                                            hasStoragePermission = hasAudioPermission,
                                            showCompletionBanner = true
                                        )
                                    },
                                    onOpenSearch = { subRoute = SubScreenRoute.Search }
                                )
                            }

                            MainDestination.ONLINE -> {
                                BackHandler(enabled = true) {
                                    currentTab = MainDestination.HOME
                                }
                                OnlineMusicScreen(
                                    onlineCatalogState = onlineCatalogState,
                                    recentlyPlayedOnline = recentlyPlayedOnline,
                                    playbackState = playbackState,
                                    preferences = prefs,
                                    onRefreshCatalog = viewModel::refreshOnlineDiscoveryCatalog,
                                    onSearchOnline = viewModel::searchOnlineCatalog,
                                    onPlayOnlineSongs = viewModel::playOnlineSongs,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onPlayNext = viewModel.playbackManager::playNextInQueue,
                                    onAddToQueue = viewModel.playbackManager::addToQueue,
                                    onRequestAddToPlaylist = { songForPlaylistDialog = it },
                                    onSaveProviderSettings = viewModel::saveOnlineProviderSettings
                                )
                            }

                            MainDestination.EQUALIZER -> {
                                BackHandler(enabled = true) {
                                    currentTab = MainDestination.HOME
                                }
                                EqualizerAndEffectsContent(
                                    effectsState = effectsState,
                                    onToggleEnabled = viewModel::setEqualizerEnabled,
                                    onSelectPreset = viewModel::selectEqualizerPreset,
                                    onApplyCustomPreset = viewModel::applyCustomEqualizerPreset,
                                    onSaveCustomPreset = viewModel::saveCustomEqualizerPreset,
                                    onDeleteCustomPreset = viewModel::deleteCustomEqualizerPreset,
                                    onBandLevelChange = viewModel::setEqualizerBandLevel,
                                    onPreampAndBalanceChange = viewModel::setPreampAndBalance,
                                    onReplayGainModeChange = viewModel::setReplayGainMode,
                                    onBassBoostChange = viewModel::setBassBoost,
                                    onVirtualizerChange = viewModel::setVirtualizer,
                                    onLoudnessChange = viewModel::setLoudnessNormalization,
                                    onResetDefault = viewModel::resetAudioEffects,
                                    modifier = Modifier.padding(bottom = 120.dp)
                                )
                            }

                            MainDestination.SETTINGS -> {
                                BackHandler(enabled = true) {
                                    currentTab = MainDestination.HOME
                                }
                                SettingsScreen(
                                    preferences = prefs,
                                    hiddenSongs = hiddenSongs,
                                    safFolders = safFolders,
                                    diagnosticsReport = diagnosticsReport,
                                    onSelectThemeMode = viewModel::setThemeMode,
                                    onToggleDynamicColor = viewModel::setDynamicColor,
                                    onSelectAccentColor = viewModel::setAccentColor,
                                    onSelectAppLanguage = viewModel::setAppLanguage,
                                    onToggleAnimatedVinyl = viewModel::setAnimatedVinylMode,
                                    onToggleSwipeGestures = viewModel::setSwipeArtworkChangeTrack,
                                    onToggleHapticFeedback = viewModel::setHapticFeedbackEnabled,
                                    onToggleThreeJsIntro = viewModel::setShowThreeJsIntro,
                                    onChangeHomeSections = viewModel::setHomeSectionVisibility,
                                    onToggleGroupByAlbumArtist = viewModel::setGroupByAlbumArtist,
                                    onToggleAutoRescanOnStartup = viewModel::setAutoRescanOnStartup,
                                    onToggleGapless = viewModel::setGaplessPlayback,
                                    onCrossfadeChange = viewModel::setCrossfadeDurationSec,
                                    onToggleFadeInOut = viewModel::setFadeInOutEnabled,
                                    onSkipIntervalChange = viewModel::setSkipIntervalSec,
                                    onToggleAudioNormalization = viewModel::setLoudnessNormalization,
                                    onToggleResumeLastSession = viewModel::setResumeLastSession,
                                    onMinScanDurationChange = viewModel::setMinScanDurationSec,
                                    onToggleIncludeDemoTracks = viewModel::setIncludeDemoTracks,
                                    onRescanLibrary = {
                                        viewModel.triggerLibraryScan(
                                            hasStoragePermission = hasAudioPermission,
                                            showCompletionBanner = true
                                        )
                                    },
                                    onRunDiagnostics = viewModel::runLibraryDiagnostics,
                                    onCleanUpMissingTracks = viewModel::cleanUpMissingTracks,
                                    onExportFullBackup = viewModel::exportFullBackupJson,
                                    onRestoreFullBackup = viewModel::restoreFullBackupJson,
                                    onPickSafFolder = { safFolderPickerLauncher.launch(null) },
                                    onRemoveSafFolder = viewModel::removeSafFolder,
                                    onUnhideAllSongs = viewModel::unhideAllSongs,
                                    onRestoreSongsToCategory = viewModel::restoreSongsToCategory,
                                    onOpenEqualizerScreen = { currentTab = MainDestination.EQUALIZER }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full-Screen Now Playing Overlay
        AnimatedVisibility(
            visible = isNowPlayingExpanded,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            NowPlayingScreen(
                playbackState = playbackState,
                volumeState = volumeState,
                effectsState = effectsState,
                currentLyrics = currentLyrics,
                onlineLyricsSearchState = onlineLyricsSearch,
                bookmarks = bookmarks,
                namedQueues = namedQueues,
                animatedVinylMode = prefs.animatedVinylMode,
                swipeArtworkChangeTrack = prefs.swipeArtworkChangeTrack,
                hapticFeedbackEnabled = prefs.hapticFeedbackEnabled,
                onCollapse = { isNowPlayingExpanded = false },
                onTogglePlayPause = viewModel.playbackManager::togglePlayPause,
                onSkipNext = viewModel.playbackManager::skipToNext,
                onSkipPrevious = viewModel.playbackManager::skipToPrevious,
                onSeekToMs = viewModel.playbackManager::seekToPosition,
                onSeekByDeltaSec = viewModel.playbackManager::seekByDeltaSeconds,
                onToggleShuffle = viewModel.playbackManager::toggleShuffle,
                onCycleRepeat = viewModel.playbackManager::cycleRepeatMode,
                onToggleFavorite = viewModel::toggleFavorite,
                onRatingChange = viewModel::setSongRating,
                onSetAbPointA = viewModel.playbackManager::setAbRepeatPointA,
                onSetAbPointB = viewModel.playbackManager::setAbRepeatPointB,
                onClearAbRepeat = viewModel.playbackManager::clearAbRepeat,
                onAddBookmark = viewModel::addBookmarkForCurrentSong,
                onDeleteBookmark = viewModel::deleteBookmark,
                onToggleAnimatedVinylMode = viewModel::setAnimatedVinylMode,
                onVolumeFractionChange = { viewModel.volumeController.setVolumeFraction(it) },
                onToggleMute = viewModel.volumeController::toggleMute,
                onSpeedAndPitchChange = viewModel.playbackManager::setPlaybackSpeedAndPitch,
                onSelectSleepTimer = viewModel::startSleepTimer,
                onSelectQueueIndex = { idx ->
                    viewModel.playbackManager.playSongFromQueue(playbackState.queue, idx)
                },
                onMoveQueueItem = viewModel.playbackManager::moveQueueItem,
                onRemoveQueueItem = viewModel.playbackManager::removeFromQueue,
                onClearQueue = viewModel.playbackManager::clearQueue,
                onSaveQueueAsPlaylist = viewModel::saveCurrentQueueAsPlaylist,
                onSaveNamedQueue = viewModel::saveCurrentAsNamedQueue,
                onRestoreNamedQueue = viewModel::restoreNamedQueue,
                onDeleteNamedQueue = viewModel::deleteNamedQueue,
                onRequestAddToPlaylist = { songForPlaylistDialog = it },
                onAdjustLyricsOffsetMs = viewModel::adjustCurrentLyricsOffset,
                onSaveManualLyrics = viewModel::saveManualLyricsForCurrentSong,
                onSearchOnlineLyrics = viewModel::searchOnlineLyricsForCurrentSong,
                onSelectOnlineLyricResult = viewModel::applyOnlineLyricsResult,
                onToggleEqualizerEnabled = viewModel::setEqualizerEnabled,
                onSelectEqualizerPreset = viewModel::selectEqualizerPreset,
                onApplyCustomPreset = viewModel::applyCustomEqualizerPreset,
                onSaveCustomPreset = viewModel::saveCustomEqualizerPreset,
                onDeleteCustomPreset = viewModel::deleteCustomEqualizerPreset,
                onEqualizerBandChange = viewModel::setEqualizerBandLevel,
                onPreampAndBalanceChange = viewModel::setPreampAndBalance,
                onReplayGainModeChange = viewModel::setReplayGainMode,
                onBassBoostChange = viewModel::setBassBoost,
                onVirtualizerChange = viewModel::setVirtualizer,
                onLoudnessChange = viewModel::setLoudnessNormalization,
                onResetEqualizer = viewModel::resetAudioEffects
            )
        }

        // Top Floating Status Toast Banner
        AnimatedVisibility(
            visible = statusBanner != null || playbackState.playbackError != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.clickable {
                    viewModel.clearStatusBanner()
                    viewModel.playbackManager.clearPlaybackError()
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = playbackState.playbackError ?: statusBanner.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftCyan
                    )
                }
            }
        }

        // Three.js Cinematic Intro Overlay (disposes WebGL & destroys WebView immediately on finish/skip)
        AnimatedVisibility(
            visible = prefs.showThreeJsIntro && !introDismissedThisSession,
            exit = fadeOut()
        ) {
            ThreeJsIntroOverlay(
                isShortTransition = prefs.hasCompletedThreeJsIntroOnce,
                onIntroFinished = {
                    introDismissedThisSession = true
                    viewModel.markThreeJsIntroCompletedOnce()
                }
            )
        }
    }

    if (songForPlaylistDialog != null) {
        val targetSong = songForPlaylistDialog!!
        AddToPlaylistDialog(
            song = targetSong,
            playlists = customPlaylists,
            onSelectPlaylist = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, targetSong)
            },
            onCreateNewPlaylistWithSong = { name ->
                viewModel.createPlaylist(
                    name = name,
                    description = "Created with ${targetSong.title}",
                    initialSongIds = listOf(targetSong.id)
                )
            },
            onDismiss = { songForPlaylistDialog = null }
        )
    }

    if (songForTagEditorDialog != null) {
        val targetSong = songForTagEditorDialog!!
        TagAndArtworkEditorDialog(
            song = targetSong,
            onSave = { title, artist, album, albumArtist, genre, year, trackNumber, rating, isCompilation, customArtworkUri ->
                viewModel.updateSongTagsAndArtwork(
                    songId = targetSong.id,
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    genre = genre,
                    year = year,
                    trackNumber = trackNumber,
                    rating = rating,
                    isCompilation = isCompilation,
                    customArtworkUri = customArtworkUri
                )
            },
            onDismiss = { songForTagEditorDialog = null }
        )
    }
}
