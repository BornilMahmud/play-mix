package com.example.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.ui.AlbumArtworkBox
import com.example.core.ui.SongRowItem
import com.example.domain.model.Album
import com.example.domain.model.Artist
import com.example.domain.model.AudioCategory
import com.example.domain.model.Playlist
import com.example.domain.model.SmartPlaylistType
import com.example.domain.model.Song
import com.example.playback.controller.PlaybackUiState
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FavoritePink
import com.example.ui.theme.LocalPlayMixStrings
import com.example.ui.theme.SoftCyan
import java.util.Calendar

@Composable
fun HomeScreen(
    songs: List<Song>,
    recentlyPlayed: List<Song>,
    recentlyAdded: List<Song>,
    favorites: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    playlists: List<Playlist>,
    playbackState: PlaybackUiState,
    showSmartMixes: Boolean = true,
    showAlbums: Boolean = true,
    showArtists: Boolean = true,
    onPlaySongList: (List<Song>, Int) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onRequestAddToPlaylist: (Song) -> Unit,
    onMoveSongToCategory: ((Song, AudioCategory) -> Unit)? = null,
    onRemoveSongFromCategory: ((Song) -> Unit)? = null,
    onDeleteSongFromDevice: ((Song) -> Unit)? = null,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenSmartPlaylist: (SmartPlaylistType) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onRescanLibrary: () -> Unit,
    onHideSong: (Song) -> Unit,
    onEditSongTags: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalPlayMixStrings.current
    val greeting = remember(strings) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> strings.homeGreetingMorning
            in 12..17 -> strings.homeGreetingAfternoon
            in 18..21 -> strings.homeGreetingEvening
            else -> strings.homeGreetingNight
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.labelLarge,
                        color = SoftCyan
                    )
                    Text(
                        text = "Play Mix",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier.testTag("home_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search library",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier.testTag("home_equalizer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "Open equalizer",
                            tint = SoftCyan
                        )
                    }
                    IconButton(
                        onClick = onRescanLibrary,
                        modifier = Modifier.testTag("home_rescan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan library",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Hero Quick-Play Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    ElectricViolet.copy(alpha = 0.9f),
                                    DeepBlue.copy(alpha = 0.95f),
                                    Color(0xFF091326)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "MUSIC ONLY • SCREEN-OFF READY • ${songs.size} TRACKS",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "Instant Master Mix",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "Pure Music library separated from Alarms, Voice Recordings, and Ringtones. Powered by Media3 ExoPlayer, ReplayGain, and 5-band hardware EQ.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.82f)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Button(
                                onClick = { if (songs.isNotEmpty()) onPlaySongList(songs, 0) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SoftCyan,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("hero_play_all_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.playAll, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { if (songs.isNotEmpty()) onShufflePlay(songs) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("hero_shuffle_button")
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.shuffleMix)
                            }
                        }
                    }
                }
            }
        }

        // Smart Playlists 2x2 Grid (Customizable visibility)
        if (showSmartMixes) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = strings.smartMixes,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmartMixTile(
                            title = "Favorites",
                            subtitle = "${favorites.size} tracks",
                            icon = Icons.Default.Favorite,
                            accent = FavoritePink,
                            onClick = { onOpenSmartPlaylist(SmartPlaylistType.FAVORITES) },
                            modifier = Modifier.weight(1f)
                        )
                        SmartMixTile(
                            title = "Recently Played",
                            subtitle = "${recentlyPlayed.size} tracks",
                            icon = Icons.Default.History,
                            accent = SoftCyan,
                            onClick = { onOpenSmartPlaylist(SmartPlaylistType.RECENTLY_PLAYED) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmartMixTile(
                            title = "Most Played",
                            subtitle = "${songs.count { it.playCount > 0 }} tracks",
                            icon = Icons.Default.TrendingUp,
                            accent = ElectricViolet,
                            onClick = { onOpenSmartPlaylist(SmartPlaylistType.MOST_PLAYED) },
                            modifier = Modifier.weight(1f)
                        )
                        SmartMixTile(
                            title = "Recently Added",
                            subtitle = "${recentlyAdded.size} tracks",
                            icon = Icons.Default.NewReleases,
                            accent = Color(0xFF38BDF8),
                            onClick = { onOpenSmartPlaylist(SmartPlaylistType.RECENTLY_ADDED) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Featured Albums Carousel (Customizable visibility)
        if (showAlbums && albums.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.albums,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(albums, key = { it.id }) { album ->
                            AlbumCarouselCard(
                                album = album,
                                onClick = { onOpenAlbum(album.id) }
                            )
                        }
                    }
                }
            }
        }

        // Artists Carousel (Customizable visibility)
        if (showArtists && artists.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.artists,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(artists, key = { it.id }) { artist ->
                            ArtistCarouselChip(
                                artist = artist,
                                onClick = { onOpenArtist(artist.id) }
                            )
                        }
                    }
                }
            }
        }

        // Suggested Custom Playlists
        if (playlists.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.yourPlaylists,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            Card(
                                modifier = Modifier
                                    .width(200.dp)
                                    .clickable { onOpenPlaylist(playlist.id) },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${playlist.songs.size} tracks",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SoftCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Tracks List (Music Only)
        item {
            Text(
                text = strings.quickPlayTracks,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        itemsIndexed(songs.take(12), key = { _, s -> s.id }) { index, song ->
            SongRowItem(
                song = song,
                isCurrentSong = playbackState.currentSong?.id == song.id,
                isPlaying = playbackState.isPlaying,
                onClick = { onPlaySongList(songs, index) },
                onToggleFavorite = { onToggleFavorite(song.id) },
                onPlayNext = { onPlayNext(song) },
                onAddToQueue = { onAddToQueue(song) },
                onAddToPlaylist = { onRequestAddToPlaylist(song) },
                onHideSong = { onHideSong(song) },
                onEditTags = onEditSongTags,
                onMoveToCategory = onMoveSongToCategory,
                onRemoveFromCategory = onRemoveSongFromCategory,
                onDeleteFromDevice = onDeleteSongFromDevice,
                currentCategoryLabel = AudioCategory.MUSIC.displayName,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun SmartMixTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("smart_tile_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.18f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AlbumCarouselCard(
    album: Album,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(142.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        AlbumArtworkBox(
            artworkUri = album.artworkUri,
            title = album.name,
            modifier = Modifier.size(134.dp),
            cornerRadius = 18.dp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${album.artist} • ${album.songs.size} songs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtistCarouselChip(
    artist: Artist,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtworkBox(
                artworkUri = artist.artworkUri,
                title = artist.name,
                modifier = Modifier.size(42.dp),
                cornerRadius = 21.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${artist.songs.size} tracks • ${artist.albumCount} albums",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
