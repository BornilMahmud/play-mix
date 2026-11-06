package com.example.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.SuccessEmerald
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
            .drawBehind {
                // Pro Max Ambient Studio Glow Backdrop
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricViolet.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.08f),
                        radius = size.minDimension * 0.72f
                    ),
                    radius = size.minDimension * 0.72f,
                    center = Offset(size.width * 0.88f, size.height * 0.08f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SoftCyan.copy(alpha = 0.11f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.12f, size.height * 0.34f),
                        radius = size.minDimension * 0.65f
                    ),
                    radius = size.minDimension * 0.65f,
                    center = Offset(size.width * 0.12f, size.height * 0.34f)
                )
            }
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Pro Max Top Editorial Header Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 16.dp, top = 12.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(SuccessEmerald, CircleShape)
                        )
                        Text(
                            text = greeting.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.8.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = SoftCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Play Mix",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Glassmorphic Action Pill Cluster
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
                    modifier = Modifier.border(
                        width = 1.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                ElectricViolet.copy(alpha = 0.38f),
                                SoftCyan.copy(alpha = 0.38f)
                            )
                        ),
                        shape = RoundedCornerShape(50)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
        }

        // 2. Asymmetric Pro Max Hero Showcase Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                SoftCyan.copy(alpha = 0.55f),
                                ElectricViolet.copy(alpha = 0.55f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF23134E),
                                    DeepBlue.copy(alpha = 0.96f),
                                    Color(0xFF070C18)
                                )
                            )
                        )
                ) {
                    // Decorative asymmetric vinyl & acoustic wave rings in top-right background
                    Canvas(
                        modifier = Modifier
                            .size(190.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 38.dp, y = (-26).dp)
                    ) {
                        val center = Offset(size.width * 0.58f, size.height * 0.44f)
                        for (ring in 1..4) {
                            drawCircle(
                                color = if (ring % 2 == 0) SoftCyan.copy(alpha = 0.14f) else ElectricViolet.copy(alpha = 0.18f),
                                radius = size.minDimension * (0.18f * ring),
                                center = center,
                                style = Stroke(width = 2.5f)
                            )
                        }
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(SoftCyan.copy(alpha = 0.30f), Color.Transparent),
                                center = center,
                                radius = size.minDimension * 0.25f
                            ),
                            radius = size.minDimension * 0.25f,
                            center = center
                        )
                    }

                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.42f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.border(
                                    width = 0.8.dp,
                                    color = SoftCyan.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(50)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = SoftCyan,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "MUSIC ONLY • AUTO-SYNC • ${songs.size} TRACKS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 0.8.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = SoftCyan,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Instant Master Mix",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Text(
                            text = "Pure Music library strictly isolated from Alarms & Recordings. Newly downloaded tracks in Download or Music folders appear automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.84f)
                        )

                        // Live Library Telemetry Strip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HeroMetricBadge(label = "SONGS", value = "${songs.size}")
                            HeroMetricBadge(label = "ALBUMS", value = "${albums.size}")
                            HeroMetricBadge(label = "ARTISTS", value = "${artists.size}")
                            HeroMetricBadge(label = "DSP EQ", value = "5-BAND")
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Button(
                                onClick = { if (songs.isNotEmpty()) onPlaySongList(songs, 0) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SoftCyan,
                                    contentColor = Color(0xFF040814)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("hero_play_all_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.playAll, fontWeight = FontWeight.ExtraBold)
                            }

                            FilledTonalButton(
                                onClick = { if (songs.isNotEmpty()) onShufflePlay(songs) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("hero_shuffle_button")
                            ) {
                                Icon(Icons.Default.Shuffle, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.shuffleMix, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Pro Max 2x2 Bento Grid for Smart Playlists
        if (showSmartMixes) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionTitleHeader(
                        title = strings.smartMixes,
                        badgeText = "CURATED"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SmartMixTile(
                            title = "Favorites",
                            subtitle = "${favorites.size} loved tracks",
                            icon = Icons.Default.Favorite,
                            accent = FavoritePink,
                            onClick = { onOpenSmartPlaylist(SmartPlaylistType.FAVORITES) },
                            modifier = Modifier.weight(1f)
                        )
                        SmartMixTile(
                            title = "Recently Played",
                            subtitle = "${recentlyPlayed.size} in history",
                            icon = Icons.Default.History,
                            accent = SoftCyan,
                            onClick = { onOpenSmartPlaylist(SmartPlaylistType.RECENTLY_PLAYED) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SmartMixTile(
                            title = "Most Played",
                            subtitle = "${songs.count { it.playCount > 0 }} top rotations",
                            icon = Icons.Default.TrendingUp,
                            accent = ElectricViolet,
                            onClick = { onOpenSmartPlaylist(SmartPlaylistType.MOST_PLAYED) },
                            modifier = Modifier.weight(1f)
                        )
                        SmartMixTile(
                            title = "Recently Added",
                            subtitle = "${recentlyAdded.size} new downloads",
                            icon = Icons.Default.NewReleases,
                            accent = Color(0xFF38BDF8),
                            onClick = { onOpenSmartPlaylist(SmartPlaylistType.RECENTLY_ADDED) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 4. Featured Albums Carousel with Vinyl-Sleeve Peek
        if (showAlbums && albums.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitleHeader(
                        title = strings.albums,
                        badgeText = "${albums.size} RELEASES",
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

        // 5. Artists Carousel
        if (showArtists && artists.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitleHeader(
                        title = strings.artists,
                        badgeText = "${artists.size} ARTISTS",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
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

        // 6. Custom Playlists Carousel
        if (playlists.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitleHeader(
                        title = strings.yourPlaylists,
                        badgeText = "${playlists.size} MIXES",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            Card(
                                modifier = Modifier
                                    .width(210.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            listOf(
                                                ElectricViolet.copy(alpha = 0.42f),
                                                SoftCyan.copy(alpha = 0.22f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { onOpenPlaylist(playlist.id) },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = playlist.description.ifBlank { "Custom Play Mix Session" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = SoftCyan.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "${playlist.songs.size} tracks",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = SoftCyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Quick Play Tracks List (Music Only)
        item {
            SectionTitleHeader(
                title = strings.quickPlayTracks,
                badgeText = "${songs.size} SONGS",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        itemsIndexed(songs.take(15), key = { _, s -> s.id }) { index, song ->
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
private fun HeroMetricBadge(
    label: String,
    value: String
) {
    Surface(
        color = Color.Black.copy(alpha = 0.34f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.border(
            width = 0.6.dp,
            color = Color.White.copy(alpha = 0.14f),
            shape = RoundedCornerShape(12.dp)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = SoftCyan.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SectionTitleHeader(
    title: String,
    badgeText: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(50),
            modifier = Modifier.border(
                width = 0.8.dp,
                color = SoftCyan.copy(alpha = 0.25f),
                shape = RoundedCornerShape(50)
            )
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                fontWeight = FontWeight.SemiBold,
                color = SoftCyan,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.48f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .testTag("smart_tile_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accent.copy(alpha = 0.22f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumCarouselCard(
    album: Album,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 0.8.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AlbumArtworkBox(
                    artworkUri = album.artworkUri,
                    title = album.name,
                    modifier = Modifier.size(136.dp),
                    cornerRadius = 16.dp
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "${album.songs.size} TRK",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = SoftCyan,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = album.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = album.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArtistCarouselChip(
    artist: Artist,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        ElectricViolet.copy(alpha = 0.35f),
                        SoftCyan.copy(alpha = 0.25f)
                    )
                ),
                shape = RoundedCornerShape(50)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArtworkBox(
                artworkUri = artist.artworkUri,
                title = artist.name,
                modifier = Modifier.size(44.dp),
                cornerRadius = 22.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${artist.songs.size} tracks • ${artist.albumCount} albums",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftCyan.copy(alpha = 0.85f)
                )
            }
        }
    }
}
