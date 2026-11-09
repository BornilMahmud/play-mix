package com.example.feature.online

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.datastore.UserPreferences
import com.example.core.ui.AlbumArtworkBox
import com.example.core.ui.SongRowItem
import com.example.data.online.OnlineCatalogSnapshot
import com.example.domain.model.OnlineMusicAlbum
import com.example.domain.model.OnlineMusicArtist
import com.example.domain.model.OnlineMusicPlaylist
import com.example.domain.model.OnlineProviderId
import com.example.domain.model.OnlineProviderInfo
import com.example.domain.model.Song
import com.example.playback.controller.PlaybackUiState
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LocalPlayMixStrings
import com.example.ui.theme.SoftCyan

@Composable
fun OnlineMusicScreen(
    onlineCatalogState: OnlineCatalogSnapshot,
    recentlyPlayedOnline: List<Song>,
    playbackState: PlaybackUiState,
    preferences: UserPreferences,
    onRefreshCatalog: () -> Unit,
    onSearchOnline: (String) -> Unit,
    onPlayOnlineSongs: (List<Song>, Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onRequestAddToPlaylist: (Song) -> Unit,
    onSaveProviderSettings: (String, String, String, String) -> Unit
) {
    val strings = LocalPlayMixStrings.current
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    var onlineQuery by rememberSaveable { mutableStateOf("") }
    var showProviderSettingsDialog by remember { mutableStateOf(false) }
    var selectedOnlinePlaylist by remember { mutableStateOf<OnlineMusicPlaylist?>(null) }

    val sections = listOf(
        strings.onlineDiscover,
        strings.onlineTrending,
        strings.onlineNewReleases,
        "Artists & Albums",
        "Playlists",
        "Online Search",
        "Recently Streamed"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SoftCyan.copy(alpha = 0.14f),
                            ElectricViolet.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.8f, size.height * 0.06f),
                        radius = size.width * 0.85f
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.06f),
                    radius = size.width * 0.85f
                )
            }
            .testTag("online_music_screen")
    ) {
        // Android 15 Expressive Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SoftCyan.copy(alpha = 0.16f),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = SoftCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ANDROID 15 • CLOUD DISCOVERY",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftCyan,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp
                        )
                    }
                }
                Text(
                    text = strings.navOnline,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    IconButton(
                        onClick = onRefreshCatalog,
                        modifier = Modifier.testTag("online_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Online Catalog", tint = SoftCyan)
                    }
                }
                OutlinedButton(
                    onClick = { showProviderSettingsDialog = true },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("online_provider_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.onlineProviders)
                }
            }
        }

        // Provider badges strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            onlineCatalogState.providers.forEach { provider ->
                FilterChip(
                    selected = provider.isConfigured && provider.isEnabled,
                    onClick = { showProviderSettingsDialog = true },
                    shape = RoundedCornerShape(50),
                    label = {
                        Text(
                            text = if (provider.requiresApiKey && !provider.isConfigured) {
                                "${provider.id.title} (Set Key)"
                            } else {
                                provider.id.title
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (provider.isConfigured) Icons.Default.Verified else Icons.Outlined.Key,
                            contentDescription = null,
                            tint = if (provider.isConfigured) EmeraldSuccess else SoftCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        // Android 15 Expressive Capsule Search Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = onlineQuery,
                onValueChange = { onlineQuery = it },
                placeholder = { Text("Search Apple iTunes, Internet Archive, Jamendo...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SoftCyan) },
                singleLine = true,
                shape = RoundedCornerShape(50),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    focusedBorderColor = SoftCyan.copy(alpha = 0.65f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("online_search_input")
            )
            Button(
                onClick = {
                    if (onlineQuery.isNotBlank()) {
                        selectedSection = 5 // Online Search tab
                        onSearchOnline(onlineQuery)
                    }
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier.testTag("online_search_submit_button")
            ) {
                Text("Search")
            }
        }

        // Android 15 Expressive Segmented Capsule Tabs (Replaces Android 5 ScrollableTabRow)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(sections) { index, title ->
                val isSelected = selectedSection == index
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) {
                        ElectricViolet
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { selectedSection = index }
                        .border(
                            width = 1.dp,
                            brush = if (isSelected) {
                                Brush.horizontalGradient(listOf(SoftCyan, ElectricViolet))
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.08f),
                                        Color.White.copy(alpha = 0.04f)
                                    )
                                )
                            },
                            shape = RoundedCornerShape(50)
                        )
                        .testTag("online_tab_$index")
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }

        if (onlineCatalogState.isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    text = "Fetching authorized streams from enabled online music providers...",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftCyan
                )
            }
        }

        onlineCatalogState.networkMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRefreshCatalog) {
                        Text("Retry")
                    }
                }
            }
        }

        val allTrendingAndNew = (onlineCatalogState.trendingSongs + onlineCatalogState.newReleaseSongs)
            .distinctBy { it.id }

        when (selectedSection) {
            0 -> OnlineDiscoverOverviewTab(
                catalog = onlineCatalogState,
                recentlyPlayedOnline = recentlyPlayedOnline,
                playbackState = playbackState,
                onPlayOnlineSongs = onPlayOnlineSongs,
                onOpenPlaylist = { selectedOnlinePlaylist = it },
                onToggleFavorite = onToggleFavorite,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onRequestAddToPlaylist = onRequestAddToPlaylist
            )
            1 -> OnlineSongListSection(
                headerTitle = "Trending Authorized Streams",
                headerSubtitle = "Streamed directly through Play Mix Media3 ExoPlayer with EQ & Lock-screen controls",
                songs = onlineCatalogState.trendingSongs,
                playbackState = playbackState,
                onPlayOnlineSongs = onPlayOnlineSongs,
                onToggleFavorite = onToggleFavorite,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onRequestAddToPlaylist = onRequestAddToPlaylist
            )
            2 -> OnlineSongListSection(
                headerTitle = "New Releases & Electronic / Indie",
                headerSubtitle = "Authorized previews & full Creative Commons releases",
                songs = onlineCatalogState.newReleaseSongs,
                playbackState = playbackState,
                onPlayOnlineSongs = onPlayOnlineSongs,
                onToggleFavorite = onToggleFavorite,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onRequestAddToPlaylist = onRequestAddToPlaylist
            )
            3 -> OnlineArtistsAndAlbumsTab(
                artists = onlineCatalogState.popularArtists,
                albums = onlineCatalogState.featuredAlbums,
                allOnlineSongs = allTrendingAndNew,
                onPlayOnlineSongs = onPlayOnlineSongs,
                onSearchArtistOrAlbum = { q ->
                    onlineQuery = q
                    selectedSection = 5
                    onSearchOnline(q)
                }
            )
            4 -> OnlinePlaylistsTab(
                playlists = onlineCatalogState.curatedPlaylists,
                onPlayOnlineSongs = onPlayOnlineSongs,
                onOpenPlaylist = { selectedOnlinePlaylist = it }
            )
            5 -> OnlineSongListSection(
                headerTitle = if (onlineQuery.isBlank()) "Online Search Results" else "Results for \"$onlineQuery\"",
                headerSubtitle = "${onlineCatalogState.searchResults.size} authorized online streams found",
                songs = onlineCatalogState.searchResults,
                playbackState = playbackState,
                onPlayOnlineSongs = onPlayOnlineSongs,
                onToggleFavorite = onToggleFavorite,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onRequestAddToPlaylist = onRequestAddToPlaylist
            )
            6 -> OnlineSongListSection(
                headerTitle = "Recently Streamed Online Tracks",
                headerSubtitle = "Cached in your Play Mix library for instant replay & playlist mixing",
                songs = recentlyPlayedOnline,
                playbackState = playbackState,
                onPlayOnlineSongs = onPlayOnlineSongs,
                onToggleFavorite = onToggleFavorite,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onRequestAddToPlaylist = onRequestAddToPlaylist
            )
        }
    }

    selectedOnlinePlaylist?.let { playlist ->
        AlertDialog(
            onDismissRequest = { selectedOnlinePlaylist = null },
            title = { Text(playlist.title) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${playlist.providerId.title} • ${playlist.description}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftCyan
                    )
                    playlist.songs.forEachIndexed { idx, song ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPlayOnlineSongs(playlist.songs, idx)
                                    selectedOnlinePlaylist = null
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AlbumArtworkBox(
                                    artworkUri = song.albumArtUri,
                                    title = song.title,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Text(song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = SoftCyan)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onPlayOnlineSongs(playlist.songs, 0)
                        selectedOnlinePlaylist = null
                    }
                ) {
                    Text("Stream All (${playlist.songs.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedOnlinePlaylist = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (showProviderSettingsDialog) {
        OnlineProviderSettingsDialog(
            providers = onlineCatalogState.providers,
            preferences = preferences,
            onSave = { jamendo, spotify, soundcloud, enabledCsv ->
                onSaveProviderSettings(jamendo, spotify, soundcloud, enabledCsv)
                showProviderSettingsDialog = false
            },
            onDismiss = { showProviderSettingsDialog = false }
        )
    }
}

@Composable
private fun OnlineDiscoverOverviewTab(
    catalog: OnlineCatalogSnapshot,
    recentlyPlayedOnline: List<Song>,
    playbackState: PlaybackUiState,
    onPlayOnlineSongs: (List<Song>, Int) -> Unit,
    onOpenPlaylist: (OnlineMusicPlaylist) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onRequestAddToPlaylist: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Curated Playlists Banner
        if (catalog.curatedPlaylists.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Curated Online Mixes",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(catalog.curatedPlaylists, key = { it.id }) { playlist ->
                            Card(
                                modifier = Modifier
                                    .width(230.dp)
                                    .clickable { onOpenPlaylist(playlist) },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    ElectricViolet.copy(alpha = 0.38f),
                                                    DeepBlue.copy(alpha = 0.35f)
                                                )
                                            )
                                        )
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AlbumArtworkBox(
                                            artworkUri = playlist.artworkUri,
                                            title = playlist.title,
                                            modifier = Modifier.size(54.dp),
                                            cornerRadius = 14.dp
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = SoftCyan.copy(alpha = 0.2f)
                                        ) {
                                            IconButton(
                                                onClick = { onPlayOnlineSongs(playlist.songs, 0) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "Stream playlist",
                                                    tint = SoftCyan
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = playlist.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${playlist.providerId.title} • ${playlist.songs.size} tracks",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SoftCyan
                                    )
                                    Text(
                                        text = playlist.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trending Online Tracks
        if (catalog.trendingSongs.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Trending Streams", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Authorized online audio ready for instant playback",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = { onPlayOnlineSongs(catalog.trendingSongs, 0) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Play All")
                    }
                }
            }

            itemsIndexed(
                items = catalog.trendingSongs.take(8),
                key = { _, s -> "disc_trend_${s.id}" }
            ) { index, song ->
                SongRowItem(
                    song = song,
                    isCurrentSong = playbackState.currentSong?.id == song.id,
                    isPlaying = playbackState.isPlaying,
                    onClick = { onPlayOnlineSongs(catalog.trendingSongs, index) },
                    onToggleFavorite = { onToggleFavorite(song.id) },
                    onPlayNext = { onPlayNext(song) },
                    onAddToQueue = { onAddToQueue(song) },
                    onAddToPlaylist = { onRequestAddToPlaylist(song) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Recently played online
        if (recentlyPlayedOnline.isNotEmpty()) {
            item {
                Text(
                    text = "Recently Streamed Online",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            itemsIndexed(
                items = recentlyPlayedOnline.take(5),
                key = { _, s -> "recent_online_${s.id}" }
            ) { index, song ->
                SongRowItem(
                    song = song,
                    isCurrentSong = playbackState.currentSong?.id == song.id,
                    isPlaying = playbackState.isPlaying,
                    onClick = { onPlayOnlineSongs(recentlyPlayedOnline, index) },
                    onToggleFavorite = { onToggleFavorite(song.id) },
                    onPlayNext = { onPlayNext(song) },
                    onAddToQueue = { onAddToQueue(song) },
                    onAddToPlaylist = { onRequestAddToPlaylist(song) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun OnlineSongListSection(
    headerTitle: String,
    headerSubtitle: String,
    songs: List<Song>,
    playbackState: PlaybackUiState,
    onPlayOnlineSongs: (List<Song>, Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onRequestAddToPlaylist: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(headerTitle, style = MaterialTheme.typography.titleLarge)
                    Text(
                        headerSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (songs.isNotEmpty()) {
                    Button(onClick = { onPlayOnlineSongs(songs, 0) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Stream All")
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("No online tracks in this view yet", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Use the search bar above or tap Refresh to load authorized online music streams.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(songs, key = { _, s -> s.id }) { index, song ->
                SongRowItem(
                    song = song,
                    isCurrentSong = playbackState.currentSong?.id == song.id,
                    isPlaying = playbackState.isPlaying,
                    onClick = { onPlayOnlineSongs(songs, index) },
                    onToggleFavorite = { onToggleFavorite(song.id) },
                    onPlayNext = { onPlayNext(song) },
                    onAddToQueue = { onAddToQueue(song) },
                    onAddToPlaylist = { onRequestAddToPlaylist(song) }
                )
            }
        }
    }
}

@Composable
private fun OnlineArtistsAndAlbumsTab(
    artists: List<OnlineMusicArtist>,
    albums: List<OnlineMusicAlbum>,
    allOnlineSongs: List<Song>,
    onPlayOnlineSongs: (List<Song>, Int) -> Unit,
    onSearchArtistOrAlbum: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Popular Online Artists", style = MaterialTheme.typography.titleLarge)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(artists, key = { it.id }) { artist ->
                    Card(
                        modifier = Modifier
                            .width(150.dp)
                            .clickable {
                                val matching = allOnlineSongs.filter {
                                    it.artist.equals(artist.name, ignoreCase = true)
                                }
                                if (matching.isNotEmpty()) {
                                    onPlayOnlineSongs(matching, 0)
                                } else {
                                    onSearchArtistOrAlbum(artist.name)
                                }
                            },
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AlbumArtworkBox(
                                artworkUri = artist.artworkUri,
                                title = artist.name,
                                modifier = Modifier.size(84.dp),
                                cornerRadius = 42.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = artist.name,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${artist.genre} • ${artist.trackCount} tracks",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftCyan,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Featured Online Albums", style = MaterialTheme.typography.titleLarge)
        }

        items(albums, key = { it.id }) { album ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val matching = allOnlineSongs.filter {
                            it.album.equals(album.title, ignoreCase = true)
                        }
                        if (matching.isNotEmpty()) {
                            onPlayOnlineSongs(matching, 0)
                        } else {
                            onSearchArtistOrAlbum("${album.artist} ${album.title}")
                        }
                    },
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArtworkBox(
                        artworkUri = album.artworkUri,
                        title = album.title,
                        modifier = Modifier.size(58.dp),
                        cornerRadius = 12.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(album.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(
                            "${album.artist} • ${album.releaseYear}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${album.providerId.title} • ${album.trackCount} streamable tracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftCyan
                        )
                    }
                    Icon(Icons.Default.PlayArrow, contentDescription = "Stream Album", tint = SoftCyan)
                }
            }
        }
    }
}

@Composable
private fun OnlinePlaylistsTab(
    playlists: List<OnlineMusicPlaylist>,
    onPlayOnlineSongs: (List<Song>, Int) -> Unit,
    onOpenPlaylist: (OnlineMusicPlaylist) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(playlists, key = { it.id }) { playlist ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenPlaylist(playlist) },
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlbumArtworkBox(
                        artworkUri = playlist.artworkUri,
                        title = playlist.title,
                        modifier = Modifier.size(64.dp),
                        cornerRadius = 14.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(playlist.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            playlist.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${playlist.providerId.title} • ${playlist.songs.size} tracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftCyan
                        )
                    }
                    IconButton(onClick = { onPlayOnlineSongs(playlist.songs, 0) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play Playlist", tint = SoftCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineProviderSettingsDialog(
    providers: List<OnlineProviderInfo>,
    preferences: UserPreferences,
    onSave: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var jamendoClientId by remember { mutableStateOf(preferences.jamendoClientId) }
    var spotifyClientId by remember { mutableStateOf(preferences.spotifyClientId) }
    var soundCloudClientId by remember { mutableStateOf(preferences.soundCloudClientId) }

    val initialEnabled = remember(preferences.enabledOnlineProvidersCsv) {
        preferences.enabledOnlineProvidersCsv.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
    var enabledSet by remember { mutableStateOf(initialEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Online Music Providers & API Keys") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Play Mix uses a modular provider architecture. Only authorized public catalog APIs and user-configured developer connectors are queried.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                providers.forEach { provider ->
                    val checked = provider.id.name in enabledSet
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { isChecked ->
                                        enabledSet = if (isChecked) {
                                            enabledSet + provider.id.name
                                        } else {
                                            enabledSet - provider.id.name
                                        }
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(provider.id.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        provider.statusMessage,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (provider.isConfigured) SoftCyan else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = jamendoClientId,
                    onValueChange = { jamendoClientId = it },
                    label = { Text("Jamendo Client ID (api.jamendo.com)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = spotifyClientId,
                    onValueChange = { spotifyClientId = it },
                    label = { Text("Spotify Web API Client ID / Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = soundCloudClientId,
                    onValueChange = { soundCloudClientId = it },
                    label = { Text("SoundCloud API Client ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val csv = enabledSet.ifEmpty {
                        setOf(OnlineProviderId.ITUNES_AUTHORIZED.name, OnlineProviderId.INTERNET_ARCHIVE.name)
                    }.joinToString(",")
                    onSave(jamendoClientId, spotifyClientId, soundCloudClientId, csv)
                }
            ) {
                Text("Save Providers")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
