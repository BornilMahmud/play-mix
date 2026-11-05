package com.example.feature.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.core.ui.AlbumArtworkBox
import com.example.core.ui.AnimatedPlayingIndicator
import com.example.data.lyrics.LrcLibSearchResult
import com.example.domain.model.AudioEffectsState
import com.example.domain.model.CustomEqualizerPreset
import com.example.domain.model.EqualizerPreset
import com.example.domain.model.NamedQueue
import com.example.domain.model.ReplayGainMode
import com.example.domain.model.Song
import com.example.domain.model.SongLyrics
import com.example.domain.model.formatDurationMs
import com.example.feature.OnlineLyricsSearchState
import com.example.playback.controller.PlaybackUiState
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.SoftCyan

@Composable
fun QueueSheetContent(
    playbackState: PlaybackUiState,
    namedQueues: List<NamedQueue>,
    onSelectQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit,
    onSaveNamedQueue: (String) -> Unit,
    onRestoreNamedQueue: (NamedQueue) -> Unit,
    onDeleteNamedQueue: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var showNamedQueueDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Playback Queue",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${playbackState.queue.size} tracks • Track ${playbackState.currentQueueIndex + 1} active",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftCyan
                )
            }

            Row {
                IconButton(
                    onClick = { showNamedQueueDialog = true },
                    modifier = Modifier.testTag("queue_save_snapshot_btn")
                ) {
                    Icon(
                        Icons.Default.BookmarkAdded,
                        contentDescription = "Save or Restore Named Queue",
                        tint = SoftCyan
                    )
                }
                IconButton(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.testTag("queue_save_playlist_btn")
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Save queue as playlist", tint = SoftCyan)
                }
                IconButton(
                    onClick = onClearQueue,
                    modifier = Modifier.testTag("queue_clear_btn")
                ) {
                    Icon(
                        Icons.Default.ClearAll,
                        contentDescription = "Clear queue",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Multiple Named Queues / Queue History strip
        if (namedQueues.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Saved Queue Snapshots (Tap to switch queue):",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                namedQueues.forEach { nq ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onRestoreNamedQueue(nq) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${nq.name} (${nq.songIds.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = SoftCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete saved queue",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onDeleteNamedQueue(nq.id) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (playbackState.queue.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Queue is empty. Tap any track to start playing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(playbackState.queue, key = { idx, s -> "${s.id}_$idx" }) { index, song ->
                    val isCurrent = index == playbackState.currentQueueIndex
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectQueueIndex(index) }
                            .testTag("queue_item_$index"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlbumArtworkBox(
                                artworkUri = song.albumArtUri,
                                title = song.title,
                                modifier = Modifier.size(42.dp),
                                cornerRadius = 10.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isCurrent) {
                                        AnimatedPlayingIndicator(isPlaying = playbackState.isPlaying)
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isCurrent) SoftCyan else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${song.artist} • ${song.formattedDuration}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { if (index > 0) onMoveQueueItem(index, index - 1) },
                                enabled = index > 0,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = "Move up",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (index < playbackState.queue.lastIndex) {
                                        onMoveQueueItem(index, index + 1)
                                    }
                                },
                                enabled = index < playbackState.queue.lastIndex,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = "Move down",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { onRemoveQueueItem(index) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove from queue",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        var playlistName by remember { mutableStateOf("Play Mix Session") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Queue as Playlist") },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveQueueAsPlaylist(playlistName)
                        showSaveDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNamedQueueDialog) {
        var queueSnapshotName by remember { mutableStateOf("Night Drive Queue") }
        AlertDialog(
            onDismissRequest = { showNamedQueueDialog = false },
            title = { Text("Save Named Queue Snapshot") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Save the current queue order and exact playback position to switch between multiple queues anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = queueSnapshotName,
                        onValueChange = { queueSnapshotName = it },
                        label = { Text("Queue Snapshot Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveNamedQueue(queueSnapshotName)
                        showNamedQueueDialog = false
                    }
                ) {
                    Text("Save Queue Snapshot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNamedQueueDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LyricsSheetContent(
    currentSong: Song?,
    lyrics: SongLyrics?,
    currentPositionMs: Long,
    onlineSearchState: OnlineLyricsSearchState,
    onSeekToMs: (Long) -> Unit,
    onAdjustOffsetMs: (Long) -> Unit,
    onSaveManualLyrics: (String) -> Unit,
    onSearchOnlineLyrics: (String?) -> Unit,
    onSelectOnlineLyricResult: (LrcLibSearchResult) -> Unit,
    modifier: Modifier = Modifier
) {
    var showManualEditDialog by remember { mutableStateOf(false) }
    var showOnlineSearchDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val effectiveTimeMs = currentPositionMs - (lyrics?.offsetMs ?: 0L)
    val activeLineIndex = remember(lyrics, effectiveTimeMs) {
        val lines = lyrics?.lines.orEmpty()
        if (lines.isEmpty()) -1
        else lines.indexOfLast { it.timestampMs <= effectiveTimeMs }.coerceAtLeast(0)
    }

    LaunchedEffect(activeLineIndex) {
        if (activeLineIndex >= 0 && (lyrics?.isSynced == true)) {
            runCatching {
                listState.animateScrollToItem((activeLineIndex - 1).coerceAtLeast(0))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Synchronized Lyrics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (lyrics != null) {
                        "${lyrics.source.label} • Offset ${if (lyrics.offsetMs >= 0) "+${lyrics.offsetMs}" else "${lyrics.offsetMs}"}ms"
                    } else {
                        "No lyrics loaded yet"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftCyan
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = {
                        showOnlineSearchDialog = true
                        onSearchOnlineLyrics(null)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("lyrics_lrclib_search_btn")
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LRCLIB", style = MaterialTheme.typography.labelSmall)
                }

                IconButton(
                    onClick = { showManualEditDialog = true },
                    modifier = Modifier.testTag("lyrics_manual_edit_btn")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit lyrics manually")
                }
            }
        }

        // Offset adjustment controls
        if (lyrics != null && lyrics.isSynced) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sync Offset:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { onAdjustOffsetMs(-250L) }) {
                    Text("-250ms")
                }
                TextButton(onClick = { onAdjustOffsetMs(+250L) }) {
                    Text("+250ms")
                }
                Text(
                    text = "Tap any line to jump",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (lyrics == null || lyrics.lines.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No synchronized lyrics found for \"${currentSong?.title ?: "Current Track"}\"",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Search LRCLIB online or paste .LRC timestamps manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            showOnlineSearchDialog = true
                            onSearchOnlineLyrics(null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search LRCLIB")
                    }
                    OutlinedButton(onClick = { showManualEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste Lyrics")
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 24.dp)
            ) {
                itemsIndexed(lyrics.lines) { idx, line ->
                    val isActive = idx == activeLineIndex
                    val scale by animateFloatAsState(
                        targetValue = if (isActive) 1.03f else 0.96f,
                        label = "lyric_scale"
                    )
                    val lineColor by animateColorAsState(
                        targetValue = if (isActive) SoftCyan else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        label = "lyric_color"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .clickable {
                                if (lyrics.isSynced) {
                                    onSeekToMs((line.timestampMs + lyrics.offsetMs).coerceAtLeast(0L))
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                Color.Transparent
                            }
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            if (isActive && line.words.isNotEmpty()) {
                                val annotated = buildAnnotatedString {
                                    line.words.forEachIndexed { wIdx, word ->
                                        val wordPassed = effectiveTimeMs >= word.timestampMs
                                        withStyle(
                                            SpanStyle(
                                                color = if (wordPassed) SoftCyan else Color.White.copy(alpha = 0.7f),
                                                fontWeight = if (wordPassed) FontWeight.Bold else FontWeight.Medium
                                            )
                                        ) {
                                            append(word.text)
                                            if (wIdx < line.words.lastIndex) append(" ")
                                        }
                                    }
                                }
                                Text(
                                    text = annotated,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            } else {
                                Text(
                                    text = line.text,
                                    style = if (isActive) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    color = lineColor
                                )
                            }

                            if (lyrics.isSynced) {
                                Text(
                                    text = formatDurationMs(line.timestampMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showManualEditDialog) {
        var manualText by remember {
            mutableStateOf(
                lyrics?.rawContent
                    ?: "[00:00.00] Paste synchronized .LRC or plain lyrics here\n[00:05.00] Every line syncs with playback"
            )
        }
        AlertDialog(
            onDismissRequest = { showManualEditDialog = false },
            title = { Text("Edit / Paste LRC Lyrics") },
            text = {
                OutlinedTextField(
                    value = manualText,
                    onValueChange = { manualText = it },
                    label = { Text("LRC or Plain Text Lyrics") },
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveManualLyrics(manualText)
                        showManualEditDialog = false
                    }
                ) {
                    Text("Save Lyrics")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showOnlineSearchDialog) {
        var customQuery by remember {
            mutableStateOf("${currentSong?.title.orEmpty()} ${currentSong?.artist.orEmpty()}".trim())
        }
        AlertDialog(
            onDismissRequest = { showOnlineSearchDialog = false },
            title = { Text("LRCLIB Synchronized Lyrics Search") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customQuery,
                            onValueChange = { customQuery = it },
                            label = { Text("Track & Artist") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { onSearchOnlineLyrics(customQuery) }) {
                            Text("Go")
                        }
                    }

                    if (onlineSearchState.isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = SoftCyan)
                        }
                    } else if (onlineSearchState.errorMessage != null) {
                        Text(
                            text = onlineSearchState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(onlineSearchState.results) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectOnlineLyricResult(item)
                                            showOnlineSearchDialog = false
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "${item.trackName ?: "Track"} — ${item.artistName ?: "Artist"}",
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = if (!item.syncedLyrics.isNullOrBlank()) "Synced .LRC Available" else "Plain Lyrics",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SoftCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOnlineSearchDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EqualizerAndEffectsContent(
    effectsState: AudioEffectsState,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onApplyCustomPreset: (CustomEqualizerPreset) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onBandLevelChange: (Int, Int) -> Unit,
    onPreampAndBalanceChange: (Float, Float, Boolean, Boolean) -> Unit,
    onReplayGainModeChange: (ReplayGainMode) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessChange: (Boolean) -> Unit,
    onResetDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveCustomPresetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Master Enable Switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "5-Band Equalizer & DSP Engine",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${effectsState.audioOutputDeviceName} • ${effectsState.audioOutputType} • ${effectsState.hardwareSampleRateHz} Hz (Buf ${effectsState.hardwareFramesPerBuffer})",
                    style = MaterialTheme.typography.labelSmall,
                    color = SoftCyan
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onResetDefault,
                    modifier = Modifier.testTag("eq_reset_button")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = "Reset Equalizer")
                }
                Switch(
                    checked = effectsState.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("eq_enable_switch")
                )
            }
        }

        // Frequency Response Curve Visualizer Canvas
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val w = size.width
                val h = size.height
                val midY = h / 2f

                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(0f, midY),
                    end = Offset(w, midY),
                    strokeWidth = 2f
                )

                val bands = effectsState.bands
                if (bands.isNotEmpty()) {
                    val path = Path()
                    val stepX = w / (bands.size - 1).coerceAtLeast(1)
                    bands.forEachIndexed { idx, band ->
                        val norm = (band.currentLevelMb.toFloat() / band.maxLevelMb.coerceAtLeast(1)).coerceIn(-1f, 1f)
                        val x = idx * stepX
                        val y = midY - (norm * (h * 0.42f))
                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(
                            color = if (effectsState.isEnabled) SoftCyan else Color.Gray,
                            radius = 6.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(listOf(ElectricViolet, SoftCyan)),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        // Built-in & Custom Presets FlowRow
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Acoustic & Custom Presets",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedButton(
                onClick = { showSaveCustomPresetDialog = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Preset", style = MaterialTheme.typography.labelSmall)
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            EqualizerPreset.entries.forEach { preset ->
                FilterChip(
                    selected = effectsState.activePreset == preset && effectsState.activeCustomPresetName == null,
                    onClick = { onSelectPreset(preset) },
                    label = { Text(preset.displayName) },
                    modifier = Modifier.testTag("eq_preset_${preset.name.lowercase()}")
                )
            }
            effectsState.customPresets.forEach { custom ->
                FilterChip(
                    selected = effectsState.activeCustomPresetName == custom.name,
                    onClick = { onApplyCustomPreset(custom) },
                    label = { Text(custom.name) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete preset",
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onDeleteCustomPreset(custom.name) }
                        )
                    }
                )
            }
        }

        // 5-Band Frequency Sliders
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                effectsState.bands.forEach { band ->
                    val dbValue = band.currentLevelMb / 100f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = band.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftCyan,
                            modifier = Modifier.width(56.dp)
                        )
                        Slider(
                            value = band.currentLevelMb.toFloat(),
                            onValueChange = { onBandLevelChange(band.index, it.toInt()) },
                            valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = SoftCyan,
                                activeTrackColor = ElectricViolet
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%+.1f dB", dbValue),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.width(58.dp)
                        )
                    }
                }
            }
        }

        // Preamp, Left/Right Balance, Mono, ReplayGain & Clipping Prevention
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Preamp, Stereo Balance & ReplayGain", style = MaterialTheme.typography.titleSmall, color = SoftCyan)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Preamp Gain", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        String.format("%+.1f dB", effectsState.preampGainDb),
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftCyan
                    )
                }
                Slider(
                    value = effectsState.preampGainDb,
                    onValueChange = {
                        onPreampAndBalanceChange(
                            it,
                            effectsState.leftRightBalance,
                            effectsState.monoAudioEnabled,
                            effectsState.audioClippingPrevention
                        )
                    },
                    valueRange = -12f..12f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Left / Right Stereo Balance", style = MaterialTheme.typography.bodyMedium)
                    val balLabel = when {
                        effectsState.leftRightBalance < -0.05f -> String.format("L %.0f%%", -effectsState.leftRightBalance * 100)
                        effectsState.leftRightBalance > 0.05f -> String.format("R %.0f%%", effectsState.leftRightBalance * 100)
                        else -> "Center"
                    }
                    Text(balLabel, style = MaterialTheme.typography.labelMedium, color = SoftCyan)
                }
                Slider(
                    value = effectsState.leftRightBalance,
                    onValueChange = {
                        onPreampAndBalanceChange(
                            effectsState.preampGainDb,
                            it,
                            effectsState.monoAudioEnabled,
                            effectsState.audioClippingPrevention
                        )
                    },
                    valueRange = -1f..1f
                )

                Text("ReplayGain Mode", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReplayGainMode.entries.forEach { mode ->
                        FilterChip(
                            selected = effectsState.replayGainMode == mode,
                            onClick = { onReplayGainModeChange(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mono Audio Output", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Combines Left & Right stereo channels for single-earbud listening",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = effectsState.monoAudioEnabled,
                        onCheckedChange = {
                            onPreampAndBalanceChange(
                                effectsState.preampGainDb,
                                effectsState.leftRightBalance,
                                it,
                                effectsState.audioClippingPrevention
                            )
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Audio Clipping Prevention", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "Automatically limits preamp & EQ boost peaks to prevent digital distortion",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = effectsState.audioClippingPrevention,
                        onCheckedChange = {
                            onPreampAndBalanceChange(
                                effectsState.preampGainDb,
                                effectsState.leftRightBalance,
                                effectsState.monoAudioEnabled,
                                it
                            )
                        }
                    )
                }
            }
        }

        // Bass Boost, Virtualizer & Loudness Normalization
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (effectsState.isBassBoostSupported) "Bass Boost" else "Bass Boost (Software Fallback)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "${effectsState.bassBoostStrength / 10}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftCyan
                    )
                }
                Slider(
                    value = effectsState.bassBoostStrength.toFloat(),
                    onValueChange = { onBassBoostChange(it.toInt()) },
                    valueRange = 0f..1000f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (effectsState.isVirtualizerSupported) "3D Spatial Virtualizer" else "3D Virtualizer (Hardware Dependent)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "${effectsState.virtualizerStrength / 10}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftCyan
                    )
                }
                Slider(
                    value = effectsState.virtualizerStrength.toFloat(),
                    onValueChange = { onVirtualizerChange(it.toInt()) },
                    valueRange = 0f..1000f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Loudness Normalization", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Balances quiet tracks using hardware LoudnessEnhancer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = effectsState.loudnessNormalizationEnabled,
                        onCheckedChange = onLoudnessChange
                    )
                }
            }
        }
    }

    if (showSaveCustomPresetDialog) {
        var presetName by remember { mutableStateOf("My Studio EQ") }
        AlertDialog(
            onDismissRequest = { showSaveCustomPresetDialog = false },
            title = { Text("Save Custom Equalizer Preset") },
            text = {
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    label = { Text("Custom Preset Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveCustomPreset(presetName)
                        showSaveCustomPresetDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveCustomPresetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
