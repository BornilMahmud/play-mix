package com.example.feature.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.ui.AlbumArtworkBox
import com.example.core.ui.BulkTagEditorDialog
import com.example.core.ui.ConfirmDeleteFromDeviceDialog
import com.example.core.ui.MoveToCategoryDialog
import com.example.core.ui.RestoreToCategoryDialog
import com.example.core.ui.SongRowItem
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.AudioCategory
import com.example.domain.model.FolderItem
import com.example.domain.model.GenreItem
import com.example.domain.model.Playlist
import com.example.domain.model.SmartPlaylistType
import com.example.domain.model.Song
import com.example.domain.model.SortOption
import com.example.domain.model.formatDurationMs
import com.example.playback.controller.PlaybackUiState
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.SoftCyan
import kotlinx.coroutines.launch

enum class LibraryTab(val title: String) {
    SONGS("Songs"),
    CATEGORIES("Audio Categories"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists"),
    GENRES("Genres"),
    FOLDERS("Folders"),
    ALL_AUDIO("All Audio"),
    REMOVED_HIDDEN("Hidden / Removed")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    songs: List<Song>,
    selectedCategory: AudioCategory,
    activeCategorySongs: List<Song>,
    categoryCounts: Map<AudioCategory, Int>,
    allAudioInspectorSongs: List<Song>,
    hiddenAndRemovedSongs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    playlists: List<Playlist>,
    genres: List<GenreItem>,
    folders: List<FolderItem>,
    currentSort: SortOption,
    playbackState: PlaybackUiState,
    onSelectCategory: (AudioCategory) -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onPlaySongList: (List<Song>, Int) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onRequestAddToPlaylist: (Song) -> Unit,
    onMoveSongToCategory: (Song, AudioCategory) -> Unit,
    onMoveSelectedSongsToCategory: (Set<String>, AudioCategory) -> Unit,
    onRemoveSongFromCategory: (Song, AudioCategory) -> Unit,
    onRemoveSelectedSongsFromCategory: (Set<String>, AudioCategory) -> Unit,
    onRestoreSongsToCategory: (Set<String>, AudioCategory?) -> Unit,
    onDeleteSongFromDevice: (Song) -> Unit,
    onDeleteSelectedSongsFromDevice: (Set<String>) -> Unit,
    onHideSong: (Song) -> Unit,
    onHideSelectedSongs: (Set<String>) -> Unit,
    onEditSongTags: (Song) -> Unit,
    onBulkEditTags: (Set<String>, String?, String?, String?, String?, Int?, Boolean?) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenSmartPlaylist: (SmartPlaylistType) -> Unit,
    onCreatePlaylist: (String, String) -> Unit,
    onImportM3u: (String, String) -> Unit,
    onToggleFolderExclusion: (FolderItem) -> Unit,
    onPickSafFolder: () -> Unit,
    onRescanLibrary: () -> Unit,
    onOpenSearch: () -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val tabs = LibraryTab.entries
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showImportM3uDialog by remember { mutableStateOf(false) }

    // Per-Category Search / Filter & Windowed Pagination State
    var categorySearchQuery by rememberSaveable { mutableStateOf("") }
    var pageLimit by rememberSaveable(selectedTabIndex, selectedCategory) { mutableIntStateOf(60) }

    // Multi-Select State
    var isMultiSelectMode by rememberSaveable { mutableStateOf(false) }
    var selectedSongIds by remember { mutableStateOf(setOf<String>()) }
    var showBulkEditDialog by remember { mutableStateOf(false) }
    var showBulkMoveDialog by remember { mutableStateOf(false) }
    var showBulkRestoreDialog by remember { mutableStateOf(false) }
    var showBulkDeleteConfirmDialog by remember { mutableStateOf(false) }
    var singleRestoreTarget by remember { mutableStateOf<Song?>(null) }

    val currentTab = tabs[selectedTabIndex]

    val baseListForActiveTab: List<Song> = remember(
        currentTab,
        songs,
        activeCategorySongs,
        allAudioInspectorSongs,
        hiddenAndRemovedSongs
    ) {
        when (currentTab) {
            LibraryTab.SONGS -> songs
            LibraryTab.CATEGORIES -> activeCategorySongs
            LibraryTab.ALL_AUDIO -> allAudioInspectorSongs
            LibraryTab.REMOVED_HIDDEN -> hiddenAndRemovedSongs
            else -> songs
        }
    }

    val filteredTabSongs: List<Song> = remember(baseListForActiveTab, categorySearchQuery) {
        val q = categorySearchQuery.trim().lowercase()
        if (q.isBlank()) {
            baseListForActiveTab
        } else {
            baseListForActiveTab.filter {
                it.title.lowercase().contains(q) ||
                    it.artist.lowercase().contains(q) ||
                    it.album.lowercase().contains(q) ||
                    it.folderName.lowercase().contains(q) ||
                    it.primaryCategory.displayName.lowercase().contains(q)
            }
        }
    }

    val pagedTabSongs: List<Song> = remember(filteredTabSongs, pageLimit) {
        filteredTabSongs.take(pageLimit)
    }

    val activeCategoryContext: AudioCategory = when (currentTab) {
        LibraryTab.CATEGORIES -> selectedCategory
        else -> AudioCategory.MUSIC
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (currentTab) {
                        LibraryTab.CATEGORIES -> "${selectedCategory.displayName} Library"
                        LibraryTab.ALL_AUDIO -> "All Audio Inspector"
                        LibraryTab.REMOVED_HIDDEN -> "Hidden & Removed Audio"
                        else -> "Music Library"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (currentTab) {
                        LibraryTab.CATEGORIES ->
                            "${activeCategorySongs.size} files in ${selectedCategory.displayName} • Strictly separated"
                        LibraryTab.ALL_AUDIO ->
                            "${allAudioInspectorSongs.size} total discovered device audio files across 8 categories"
                        LibraryTab.REMOVED_HIDDEN ->
                            "${hiddenAndRemovedSongs.size} hidden/removed items • Files preserved on device"
                        else ->
                            "${songs.size} music tracks • ${albums.size} albums • ${artists.size} artists"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        isMultiSelectMode = !isMultiSelectMode
                        if (!isMultiSelectMode) selectedSongIds = emptySet()
                    },
                    modifier = Modifier.testTag("library_multiselect_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = "Multi-select mode",
                        tint = if (isMultiSelectMode) SoftCyan else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onOpenSearch, modifier = Modifier.testTag("library_search_btn")) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = onPickSafFolder, modifier = Modifier.testTag("library_saf_btn")) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Folder via SAF", tint = SoftCyan)
                }
                IconButton(onClick = onRescanLibrary, modifier = Modifier.testTag("library_rescan_btn")) {
                    Icon(Icons.Default.Refresh, contentDescription = "Rescan")
                }
                Box {
                    IconButton(
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.testTag("library_sort_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort tracks")
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.label,
                                        color = if (option == currentSort) SoftCyan else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    sortMenuExpanded = false
                                    onSelectSort(option)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Multi-Select Action Bar (Supports Move to Category, Remove from Category, Restore, Bulk Edit, Delete)
        if (isMultiSelectMode) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${selectedSongIds.size} selected",
                        style = MaterialTheme.typography.titleSmall,
                        color = SoftCyan
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = {
                                selectedSongIds = if (selectedSongIds.size == filteredTabSongs.size) {
                                    emptySet()
                                } else {
                                    filteredTabSongs.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(
                            onClick = {
                                val selectedList = filteredTabSongs.filter { it.id in selectedSongIds }
                                if (selectedList.isNotEmpty()) {
                                    onPlaySongList(selectedList, 0)
                                    isMultiSelectMode = false
                                    selectedSongIds = emptySet()
                                }
                            },
                            enabled = selectedSongIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play selected")
                        }
                        // Move Selected to Category
                        IconButton(
                            onClick = { showBulkMoveDialog = true },
                            enabled = selectedSongIds.isNotEmpty(),
                            modifier = Modifier.testTag("library_bulk_move_category_btn")
                        ) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = "Move selected to category", tint = SoftCyan)
                        }
                        // Remove Selected from Current Category (Non-Destructive)
                        if (currentTab != LibraryTab.REMOVED_HIDDEN) {
                            IconButton(
                                onClick = {
                                    onRemoveSelectedSongsFromCategory(selectedSongIds, activeCategoryContext)
                                    isMultiSelectMode = false
                                    selectedSongIds = emptySet()
                                },
                                enabled = selectedSongIds.isNotEmpty(),
                                modifier = Modifier.testTag("library_bulk_remove_category_btn")
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove from category")
                            }
                        } else {
                            // Restore Selected
                            IconButton(
                                onClick = { showBulkRestoreDialog = true },
                                enabled = selectedSongIds.isNotEmpty(),
                                modifier = Modifier.testTag("library_bulk_restore_btn")
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = "Restore selected", tint = SoftCyan)
                            }
                        }
                        IconButton(
                            onClick = { showBulkEditDialog = true },
                            enabled = selectedSongIds.isNotEmpty(),
                            modifier = Modifier.testTag("library_bulk_edit_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Bulk edit metadata")
                        }
                        IconButton(
                            onClick = { showBulkDeleteConfirmDialog = true },
                            enabled = selectedSongIds.isNotEmpty(),
                            modifier = Modifier.testTag("library_bulk_delete_device_btn")
                        ) {
                            Icon(
                                Icons.Default.DeleteForever,
                                contentDescription = "Delete selected from device",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        IconButton(
                            onClick = {
                                isMultiSelectMode = false
                                selectedSongIds = emptySet()
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Exit multi-select")
                        }
                    }
                }
            }
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            tabs.forEachIndexed { idx, tab ->
                val tabLabel = when (tab) {
                    LibraryTab.REMOVED_HIDDEN ->
                        if (hiddenAndRemovedSongs.isNotEmpty()) "Hidden (${hiddenAndRemovedSongs.size})"
                        else "Hidden / Removed"
                    else -> tab.title
                }
                Tab(
                    selected = selectedTabIndex == idx,
                    onClick = { selectedTabIndex = idx },
                    text = { Text(tabLabel) },
                    modifier = Modifier.testTag("lib_tab_${tab.name.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (currentTab) {
                LibraryTab.SONGS -> {
                    item {
                        CategoryFilterSearchBar(
                            query = categorySearchQuery,
                            placeholder = "Filter Music tracks (${songs.size})...",
                            onQueryChange = { categorySearchQuery = it }
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { if (filteredTabSongs.isNotEmpty()) onPlaySongList(filteredTabSongs, 0) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play Music (${filteredTabSongs.size})")
                            }
                            FilledTonalButton(
                                onClick = { if (filteredTabSongs.isNotEmpty()) onShufflePlay(filteredTabSongs) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shuffle")
                            }
                        }
                    }

                    if (filteredTabSongs.isEmpty()) {
                        item {
                            EmptyCategoryCard(
                                title = "No Music Tracks Found",
                                message = "Alarms, Voice Recordings, Ringtones, and Notifications are automatically separated into their own category libraries. Use 'Audio Categories' to view or move tracks."
                            )
                        }
                    } else {
                        itemsIndexed(pagedTabSongs, key = { _, s -> s.id }) { idx, song ->
                            SongRowItem(
                                song = song,
                                isCurrentSong = playbackState.currentSong?.id == song.id,
                                isPlaying = playbackState.isPlaying,
                                onClick = { onPlaySongList(filteredTabSongs, idx) },
                                onToggleFavorite = { onToggleFavorite(song.id) },
                                onPlayNext = { onPlayNext(song) },
                                onAddToQueue = { onAddToQueue(song) },
                                onAddToPlaylist = { onRequestAddToPlaylist(song) },
                                onHideSong = { onHideSong(song) },
                                onEditTags = onEditSongTags,
                                onMoveToCategory = onMoveSongToCategory,
                                onRemoveFromCategory = { onRemoveSongFromCategory(it, AudioCategory.MUSIC) },
                                onDeleteFromDevice = onDeleteSongFromDevice,
                                currentCategoryLabel = AudioCategory.MUSIC.displayName,
                                isMultiSelectMode = isMultiSelectMode,
                                isSelectedInMultiSelect = song.id in selectedSongIds,
                                onToggleMultiSelect = {
                                    selectedSongIds = if (song.id in selectedSongIds) {
                                        selectedSongIds - song.id
                                    } else {
                                        selectedSongIds + song.id
                                    }
                                }
                            )
                        }

                        if (filteredTabSongs.size > pagedTabSongs.size) {
                            item {
                                OutlinedButton(
                                    onClick = { pageLimit += 60 },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Load More (${pagedTabSongs.size} of ${filteredTabSongs.size} shown)")
                                }
                            }
                        }
                    }
                }

                LibraryTab.CATEGORIES -> {
                    // 8 Independent Audio Category Selector Chips with Live Counts
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Select Independent Audio Category Library",
                                style = MaterialTheme.typography.labelLarge,
                                color = SoftCyan
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(AudioCategory.entries, key = { it.id }) { category ->
                                    val count = categoryCounts[category] ?: 0
                                    FilterChip(
                                        selected = selectedCategory == category,
                                        onClick = { onSelectCategory(category) },
                                        label = {
                                            Text("${category.displayName} ($count)")
                                        },
                                        modifier = Modifier.testTag("category_chip_${category.id.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${selectedCategory.displayName} (${filteredTabSongs.size} files)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftCyan
                                )
                                Text(
                                    text = "${selectedCategory.description}. Moving an item reassigns its primary category without deleting the file; removing an item hides it from ${selectedCategory.displayName} without deleting the file.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            if (filteredTabSongs.isNotEmpty()) onPlaySongList(filteredTabSongs, 0)
                                        },
                                        enabled = filteredTabSongs.isNotEmpty(),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Play ${selectedCategory.displayName}")
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            if (filteredTabSongs.isNotEmpty()) onShufflePlay(filteredTabSongs)
                                        },
                                        enabled = filteredTabSongs.isNotEmpty(),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Shuffle, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Shuffle")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        CategoryFilterSearchBar(
                            query = categorySearchQuery,
                            placeholder = "Search in ${selectedCategory.displayName}...",
                            onQueryChange = { categorySearchQuery = it }
                        )
                    }

                    if (filteredTabSongs.isEmpty()) {
                        item {
                            EmptyCategoryCard(
                                title = "No ${selectedCategory.displayName} Files",
                                message = "No visible audio files are currently classified under ${selectedCategory.displayName}. You can move any audio file here using the 3-dot menu ('Move to Category') or restore hidden items from 'Hidden / Removed'."
                            )
                        }
                    } else {
                        itemsIndexed(pagedTabSongs, key = { _, s -> s.id }) { idx, song ->
                            SongRowItem(
                                song = song,
                                isCurrentSong = playbackState.currentSong?.id == song.id,
                                isPlaying = playbackState.isPlaying,
                                onClick = { onPlaySongList(filteredTabSongs, idx) },
                                onToggleFavorite = { onToggleFavorite(song.id) },
                                onPlayNext = { onPlayNext(song) },
                                onAddToQueue = { onAddToQueue(song) },
                                onAddToPlaylist = { onRequestAddToPlaylist(song) },
                                onHideSong = { onHideSong(song) },
                                onEditTags = onEditSongTags,
                                onMoveToCategory = onMoveSongToCategory,
                                onRemoveFromCategory = { onRemoveSongFromCategory(it, selectedCategory) },
                                onDeleteFromDevice = onDeleteSongFromDevice,
                                showCategoryBadge = true,
                                currentCategoryLabel = selectedCategory.displayName,
                                isMultiSelectMode = isMultiSelectMode,
                                isSelectedInMultiSelect = song.id in selectedSongIds,
                                onToggleMultiSelect = {
                                    selectedSongIds = if (song.id in selectedSongIds) {
                                        selectedSongIds - song.id
                                    } else {
                                        selectedSongIds + song.id
                                    }
                                }
                            )
                        }

                        if (filteredTabSongs.size > pagedTabSongs.size) {
                            item {
                                OutlinedButton(
                                    onClick = { pageLimit += 60 },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Load More (${pagedTabSongs.size} of ${filteredTabSongs.size} shown)")
                                }
                            }
                        }
                    }
                }

                LibraryTab.ALBUMS -> {
                    items(albums, key = { it.id }) { album ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenAlbum(album.id) }
                                .testTag("album_card_${album.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArtworkBox(
                                    artworkUri = album.artworkUri,
                                    title = album.name,
                                    modifier = Modifier.size(56.dp),
                                    cornerRadius = 14.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = album.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${album.artist} • ${album.year}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "${album.songs.size} tracks",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SoftCyan
                                )
                            }
                        }
                    }
                }

                LibraryTab.ARTISTS -> {
                    items(artists, key = { it.id }) { artist ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenArtist(artist.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArtworkBox(
                                    artworkUri = artist.artworkUri,
                                    title = artist.name,
                                    modifier = Modifier.size(52.dp),
                                    cornerRadius = 26.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(artist.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${artist.albumCount} albums • ${artist.songs.size} songs",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                LibraryTab.PLAYLISTS -> {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showCreatePlaylistDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("create_playlist_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Playlist")
                            }
                            OutlinedButton(
                                onClick = { showImportM3uDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_m3u_btn")
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import M3U/M3U8")
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Smart Playlists",
                            style = MaterialTheme.typography.titleSmall,
                            color = SoftCyan
                        )
                    }

                    items(SmartPlaylistType.entries) { smartType ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSmartPlaylist(smartType) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(smartType.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        smartType.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text("AUTO", style = MaterialTheme.typography.labelSmall, color = SoftCyan)
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Custom Playlists (${playlists.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = SoftCyan
                        )
                    }

                    items(playlists, key = { it.id }) { playlist ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPlaylist(playlist.id) }
                                .testTag("playlist_card_${playlist.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(playlist.name, style = MaterialTheme.typography.titleMedium)
                                    if (playlist.description.isNotBlank()) {
                                        Text(
                                            playlist.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Text(
                                    "${playlist.songs.size} tracks",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SoftCyan
                                )
                            }
                        }
                    }
                }

                LibraryTab.GENRES -> {
                    items(genres, key = { it.name }) { genreItem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlaySongList(genreItem.songs, 0) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(genreItem.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${genreItem.songs.size} tracks • Tap to play genre mix",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play genre", tint = SoftCyan)
                            }
                        }
                    }
                }

                LibraryTab.FOLDERS -> {
                    item {
                        OutlinedButton(
                            onClick = onPickSafFolder,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Local Folder via Storage Access Framework")
                        }
                    }

                    items(folders, key = { it.name }) { folder ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = if (folder.isExcluded) MaterialTheme.colorScheme.error else SoftCyan
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(folder.name, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "${folder.songs.size} tracks • ${folder.path}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = !folder.isExcluded,
                                        onCheckedChange = { onToggleFolderExclusion(folder) }
                                    )
                                }

                                if (!folder.isExcluded && folder.songs.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { onPlaySongList(folder.songs, 0) }
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Play Folder (${folder.songs.size} tracks)")
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryTab.ALL_AUDIO -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "All Audio Classification Inspector (${filteredTabSongs.size} files)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftCyan
                                )
                                Text(
                                    text = "Inspect every discovered audio file across Music, Alarms, Voice Recordings, Ringtones, Notifications, Podcasts, Audiobooks, and Unclassified. Use the 3-dot menu on any item to move it to another category.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        CategoryFilterSearchBar(
                            query = categorySearchQuery,
                            placeholder = "Search across all categories...",
                            onQueryChange = { categorySearchQuery = it }
                        )
                    }

                    itemsIndexed(pagedTabSongs, key = { _, s -> s.id }) { idx, song ->
                        SongRowItem(
                            song = song,
                            isCurrentSong = playbackState.currentSong?.id == song.id,
                            isPlaying = playbackState.isPlaying,
                            onClick = { onPlaySongList(filteredTabSongs, idx) },
                            onToggleFavorite = { onToggleFavorite(song.id) },
                            onPlayNext = { onPlayNext(song) },
                            onAddToQueue = { onAddToQueue(song) },
                            onAddToPlaylist = { onRequestAddToPlaylist(song) },
                            onHideSong = { onHideSong(song) },
                            onEditTags = onEditSongTags,
                            onMoveToCategory = onMoveSongToCategory,
                            onRemoveFromCategory = { onRemoveSongFromCategory(it, it.primaryCategory) },
                            onDeleteFromDevice = onDeleteSongFromDevice,
                            showCategoryBadge = true,
                            currentCategoryLabel = song.primaryCategory.displayName,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelectedInMultiSelect = song.id in selectedSongIds,
                            onToggleMultiSelect = {
                                selectedSongIds = if (song.id in selectedSongIds) {
                                    selectedSongIds - song.id
                                } else {
                                    selectedSongIds + song.id
                                }
                            }
                        )
                    }
                }

                LibraryTab.REMOVED_HIDDEN -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Hidden & Category-Removed Audio (${hiddenAndRemovedSongs.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftCyan
                                )
                                Text(
                                    text = "Items removed from a category or hidden from the library appear here. Their original audio files, tags, artwork, and play counts are untouched on your device. Restore individual items or restore all to any chosen category.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (hiddenAndRemovedSongs.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            selectedSongIds = hiddenAndRemovedSongs.map { it.id }.toSet()
                                            showBulkRestoreDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("restore_all_removed_btn")
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Restore All (${hiddenAndRemovedSongs.size}) Items...")
                                    }
                                }
                            }
                        }
                    }

                    if (hiddenAndRemovedSongs.isEmpty()) {
                        item {
                            EmptyCategoryCard(
                                title = "No Hidden or Removed Audio",
                                message = "When you choose 'Remove from Category' or 'Hide from Library' on any audio file, it is safely hidden from that category without deleting the file, and can be restored from here at any time."
                            )
                        }
                    } else {
                        items(hiddenAndRemovedSongs, key = { it.id }) { song ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("removed_item_${song.id}"),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = song.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${song.artist} • Current: ${song.primaryCategory.displayName} (Original: ${song.originalCategory.displayName})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SoftCyan
                                            )
                                            Text(
                                                text = song.exclusionReason.ifBlank {
                                                    "Removed from: ${song.removedFromCategories.joinToString { it.displayName }}"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { singleRestoreTarget = song },
                                            modifier = Modifier.testTag("restore_btn_${song.id}")
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (singleRestoreTarget != null) {
        val target = singleRestoreTarget!!
        RestoreToCategoryDialog(
            songs = listOf(target),
            onConfirmRestore = { destCat ->
                onRestoreSongsToCategory(setOf(target.id), destCat)
                singleRestoreTarget = null
            },
            onDismiss = { singleRestoreTarget = null }
        )
    }

    if (showBulkMoveDialog) {
        val targetSongs = filteredTabSongs.filter { it.id in selectedSongIds }
        MoveToCategoryDialog(
            songs = targetSongs,
            currentCategory = if (currentTab == LibraryTab.CATEGORIES) selectedCategory else null,
            onConfirmMove = { targetCat ->
                onMoveSelectedSongsToCategory(selectedSongIds, targetCat)
                showBulkMoveDialog = false
                isMultiSelectMode = false
                selectedSongIds = emptySet()
            },
            onDismiss = { showBulkMoveDialog = false }
        )
    }

    if (showBulkRestoreDialog) {
        val targetSongs = hiddenAndRemovedSongs.filter { it.id in selectedSongIds }
        RestoreToCategoryDialog(
            songs = targetSongs,
            onConfirmRestore = { destCat ->
                onRestoreSongsToCategory(selectedSongIds, destCat)
                showBulkRestoreDialog = false
                isMultiSelectMode = false
                selectedSongIds = emptySet()
            },
            onDismiss = { showBulkRestoreDialog = false }
        )
    }

    if (showBulkDeleteConfirmDialog) {
        val targetSongs = filteredTabSongs.filter { it.id in selectedSongIds }
        ConfirmDeleteFromDeviceDialog(
            songs = targetSongs,
            onConfirmDelete = {
                onDeleteSelectedSongsFromDevice(selectedSongIds)
                showBulkDeleteConfirmDialog = false
                isMultiSelectMode = false
                selectedSongIds = emptySet()
            },
            onDismiss = { showBulkDeleteConfirmDialog = false }
        )
    }

    if (showBulkEditDialog) {
        BulkTagEditorDialog(
            selectedCount = selectedSongIds.size,
            onApplyBulkUpdate = { artist, album, albumArtist, genre, year, isComp ->
                onBulkEditTags(selectedSongIds, artist, album, albumArtist, genre, year, isComp)
                isMultiSelectMode = false
                selectedSongIds = emptySet()
            },
            onDismiss = { showBulkEditDialog = false }
        )
    }

    if (showCreatePlaylistDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create New Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_playlist_name_input")
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreatePlaylist(name, desc)
                            showCreatePlaylistDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_playlist_btn")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportM3uDialog) {
        var name by remember { mutableStateOf("Imported Mix") }
        var m3uText by remember {
            mutableStateOf("#EXTM3U\n#EXTENC:UTF-8\n#EXTINF:36,Nova Pulse - Electric Horizon\n01_electric_horizon.wav")
        }
        AlertDialog(
            onDismissRequest = { showImportM3uDialog = false },
            title = { Text("Import M3U / M3U8 Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Playlist Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = m3uText,
                        onValueChange = { m3uText = it },
                        label = { Text("Paste #EXTM3U / M3U8 Content") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onImportM3u(name, m3uText)
                        showImportM3uDialog = false
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportM3uDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CategoryFilterSearchBar(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear filter")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("category_filter_search_input")
    )
}

@Composable
private fun EmptyCategoryCard(
    title: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SoftCyan
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AlbumDetailScreen(
    album: Album?,
    playbackState: PlaybackUiState,
    onPlaySongList: (List<Song>, Int) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onRequestAddToPlaylist: (Song) -> Unit,
    onEditSongTags: ((Song) -> Unit)? = null,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    if (album == null) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Album not found.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtworkBox(
                    artworkUri = album.artworkUri,
                    title = album.name,
                    modifier = Modifier.size(120.dp),
                    cornerRadius = 22.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = album.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = SoftCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${album.year} • ${album.songs.size} tracks • ${formatDurationMs(album.totalDurationMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onPlaySongList(album.songs, 0) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play Album")
                }
                FilledTonalButton(
                    onClick = { onShufflePlay(album.songs) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle")
                }
            }
        }

        itemsIndexed(album.songs, key = { _, s -> s.id }) { idx, song ->
            SongRowItem(
                song = song,
                isCurrentSong = playbackState.currentSong?.id == song.id,
                isPlaying = playbackState.isPlaying,
                onClick = { onPlaySongList(album.songs, idx) },
                onToggleFavorite = { onToggleFavorite(song.id) },
                onPlayNext = { onPlayNext(song) },
                onAddToQueue = { onAddToQueue(song) },
                onAddToPlaylist = { onRequestAddToPlaylist(song) },
                onEditTags = onEditSongTags
            )
        }
    }
}

@Composable
fun ArtistDetailScreen(
    artist: Artist?,
    playbackState: PlaybackUiState,
    onPlaySongList: (List<Song>, Int) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onRequestAddToPlaylist: (Song) -> Unit,
    onEditSongTags: ((Song) -> Unit)? = null,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    if (artist == null) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Artist not found.")
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AlbumArtworkBox(
                    artworkUri = artist.artworkUri,
                    title = artist.name,
                    modifier = Modifier.size(96.dp),
                    cornerRadius = 48.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = artist.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${artist.albumCount} albums • ${artist.songs.size} tracks",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onPlaySongList(artist.songs, 0) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play Artist")
                }
                FilledTonalButton(
                    onClick = { onShufflePlay(artist.songs) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle")
                }
            }
        }

        itemsIndexed(artist.songs, key = { _, s -> s.id }) { idx, song ->
            SongRowItem(
                song = song,
                isCurrentSong = playbackState.currentSong?.id == song.id,
                isPlaying = playbackState.isPlaying,
                onClick = { onPlaySongList(artist.songs, idx) },
                onToggleFavorite = { onToggleFavorite(song.id) },
                onPlayNext = { onPlayNext(song) },
                onAddToQueue = { onAddToQueue(song) },
                onAddToPlaylist = { onRequestAddToPlaylist(song) },
                onEditTags = onEditSongTags
            )
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist?,
    playbackState: PlaybackUiState,
    onPlaySongList: (List<Song>, Int) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onRemoveSongFromPlaylist: (Long, String) -> Unit,
    onReorderPlaylistSong: (Long, Int, Int) -> Unit,
    onRenamePlaylist: (Long, String, String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onExportM3u: suspend (Playlist) -> String,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    if (playlist == null) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Playlist not found.")
        }
        return
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    var exportedM3uContent by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                if (!playlist.isSmart) {
                    Row {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    exportedM3uContent = onExportM3u(playlist)
                                }
                            }
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export M3U8", tint = SoftCyan)
                        }
                        IconButton(onClick = { showRenameDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename playlist")
                        }
                        IconButton(
                            onClick = {
                                onDeletePlaylist(playlist.id)
                                onBack()
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete playlist",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Text(
                text = playlist.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            if (playlist.description.isNotBlank()) {
                Text(
                    text = playlist.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${playlist.songs.size} tracks",
                style = MaterialTheme.typography.labelMedium,
                color = SoftCyan
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { if (playlist.songs.isNotEmpty()) onPlaySongList(playlist.songs, 0) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play All")
                }
                FilledTonalButton(
                    onClick = { if (playlist.songs.isNotEmpty()) onShufflePlay(playlist.songs) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Shuffle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Shuffle")
                }
            }
        }

        if (playlist.songs.isEmpty()) {
            item {
                Text(
                    text = "No tracks in this playlist yet. Add songs using the 3-dot menu on any track.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        } else {
            itemsIndexed(playlist.songs, key = { idx, s -> "${s.id}_$idx" }) { idx, song ->
                Column {
                    SongRowItem(
                        song = song,
                        isCurrentSong = playbackState.currentSong?.id == song.id,
                        isPlaying = playbackState.isPlaying,
                        onClick = { onPlaySongList(playlist.songs, idx) },
                        onToggleFavorite = { onToggleFavorite(song.id) },
                        onPlayNext = { onPlayNext(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onAddToPlaylist = {}
                    )
                    if (!playlist.isSmart) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (idx > 0) onReorderPlaylistSong(playlist.id, idx, idx - 1) },
                                enabled = idx > 0
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = "Move track up",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (idx < playlist.songs.lastIndex) {
                                        onReorderPlaylistSong(playlist.id, idx, idx + 1)
                                    }
                                },
                                enabled = idx < playlist.songs.lastIndex
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = "Move track down",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            TextButton(
                                onClick = { onRemoveSongFromPlaylist(playlist.id, song.id) }
                            ) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(playlist.name) }
        var newDesc by remember { mutableStateOf(playlist.description) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Edit Playlist") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it },
                        label = { Text("Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRenamePlaylist(playlist.id, newName, newDesc)
                        showRenameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (exportedM3uContent != null) {
        AlertDialog(
            onDismissRequest = { exportedM3uContent = null },
            title = { Text("Exported M3U8 Playlist") },
            text = {
                OutlinedTextField(
                    value = exportedM3uContent.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { exportedM3uContent = null }) {
                    Text("Done")
                }
            }
        )
    }
}
