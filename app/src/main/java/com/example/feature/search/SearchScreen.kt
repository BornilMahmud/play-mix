package com.example.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.core.ui.AlbumArtworkBox
import com.example.core.ui.SongRowItem
import com.example.domain.model.Song
import com.example.feature.SearchResultsUiState
import com.example.playback.controller.PlaybackUiState
import com.example.ui.theme.SoftCyan

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    searchResults: SearchResultsUiState,
    searchHistory: List<String>,
    playbackState: PlaybackUiState,
    onQueryChange: (String) -> Unit,
    onCommitQuery: (String) -> Unit,
    onClearHistory: () -> Unit,
    onPlaySongList: (List<Song>, Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onRequestAddToPlaylist: (Song) -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("search_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Search songs, artists, albums, genres...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onCommitQuery(searchQuery) }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_input_field")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (searchQuery.isBlank()) {
            if (searchHistory.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Searches",
                        style = MaterialTheme.typography.titleMedium,
                        color = SoftCyan
                    )
                    TextButton(onClick = onClearHistory) {
                        Text("Clear All")
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    searchHistory.forEach { term ->
                        AssistChip(
                            onClick = { onQueryChange(term) },
                            label = { Text(term) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Instant Unified Library Search",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Type a song title, artist, album, playlist, or genre.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (searchResults.matchingSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Songs (${searchResults.matchingSongs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = SoftCyan
                        )
                    }
                    itemsIndexed(searchResults.matchingSongs, key = { _, s -> s.id }) { idx, song ->
                        SongRowItem(
                            song = song,
                            isCurrentSong = playbackState.currentSong?.id == song.id,
                            isPlaying = playbackState.isPlaying,
                            onClick = {
                                onCommitQuery(searchQuery)
                                onPlaySongList(searchResults.matchingSongs, idx)
                            },
                            onToggleFavorite = { onToggleFavorite(song.id) },
                            onPlayNext = { onPlayNext(song) },
                            onAddToQueue = { onAddToQueue(song) },
                            onAddToPlaylist = { onRequestAddToPlaylist(song) }
                        )
                    }
                }

                if (searchResults.matchingAlbums.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Albums (${searchResults.matchingAlbums.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = SoftCyan
                        )
                    }
                    items(searchResults.matchingAlbums, key = { it.id }) { album ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCommitQuery(searchQuery)
                                    onOpenAlbum(album.id)
                                },
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
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(album.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${album.artist} • ${album.songs.size} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (searchResults.matchingArtists.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Artists (${searchResults.matchingArtists.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = SoftCyan
                        )
                    }
                    items(searchResults.matchingArtists, key = { it.id }) { artist ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCommitQuery(searchQuery)
                                    onOpenArtist(artist.id)
                                },
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
                                    modifier = Modifier.size(44.dp),
                                    cornerRadius = 22.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(artist.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${artist.songs.size} tracks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (searchResults.matchingPlaylists.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Playlists (${searchResults.matchingPlaylists.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = SoftCyan
                        )
                    }
                    items(searchResults.matchingPlaylists, key = { it.id }) { playlist ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCommitQuery(searchQuery)
                                    onOpenPlaylist(playlist.id)
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(playlist.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${playlist.songs.size} tracks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SoftCyan
                                )
                            }
                        }
                    }
                }

                if (searchResults.matchingSongs.isEmpty() &&
                    searchResults.matchingAlbums.isEmpty() &&
                    searchResults.matchingArtists.isEmpty() &&
                    searchResults.matchingPlaylists.isEmpty()
                ) {
                    item {
                        Text(
                            text = "No results matching \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                }
            }
        }
    }
}
