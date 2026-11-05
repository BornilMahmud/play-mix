package com.example.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.ui.AlbumArtworkBox
import com.example.core.ui.StarRatingBar
import com.example.core.ui.TrackMetadataDialog
import com.example.data.lyrics.LrcLibSearchResult
import com.example.domain.model.AudioEffectsState
import com.example.domain.model.CustomEqualizerPreset
import com.example.domain.model.EqualizerPreset
import com.example.domain.model.NamedQueue
import com.example.domain.model.PlaybackBookmark
import com.example.domain.model.RepeatModeState
import com.example.domain.model.ReplayGainMode
import com.example.domain.model.SleepTimerOption
import com.example.domain.model.Song
import com.example.domain.model.SongLyrics
import com.example.domain.model.formatDurationMs
import com.example.feature.OnlineLyricsSearchState
import com.example.playback.controller.PlaybackUiState
import com.example.playback.controller.SystemVolumeState
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FavoritePink
import com.example.ui.theme.SoftCyan

private enum class ActivePlayerSheet {
    NONE,
    QUEUE,
    LYRICS,
    EQUALIZER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playbackState: PlaybackUiState,
    volumeState: SystemVolumeState,
    effectsState: AudioEffectsState,
    currentLyrics: SongLyrics?,
    onlineLyricsSearchState: OnlineLyricsSearchState,
    bookmarks: List<PlaybackBookmark>,
    namedQueues: List<NamedQueue>,
    animatedVinylMode: Boolean,
    swipeArtworkChangeTrack: Boolean,
    hapticFeedbackEnabled: Boolean,
    onCollapse: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekToMs: (Long) -> Unit,
    onSeekByDeltaSec: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onRatingChange: (String, Int) -> Unit,
    onSetAbPointA: () -> Unit,
    onSetAbPointB: () -> Unit,
    onClearAbRepeat: () -> Unit,
    onAddBookmark: (String) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    onToggleAnimatedVinylMode: (Boolean) -> Unit,
    onVolumeFractionChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onSpeedAndPitchChange: (Float, Float) -> Unit,
    onSelectSleepTimer: (SleepTimerOption, Int) -> Unit,
    onSelectQueueIndex: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onRemoveQueueItem: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit,
    onSaveNamedQueue: (String) -> Unit,
    onRestoreNamedQueue: (NamedQueue) -> Unit,
    onDeleteNamedQueue: (Long) -> Unit,
    onRequestAddToPlaylist: (Song) -> Unit,
    onAdjustLyricsOffsetMs: (Long) -> Unit,
    onSaveManualLyrics: (String) -> Unit,
    onSearchOnlineLyrics: (String?) -> Unit,
    onSelectOnlineLyricResult: (LrcLibSearchResult) -> Unit,
    onToggleEqualizerEnabled: (Boolean) -> Unit,
    onSelectEqualizerPreset: (EqualizerPreset) -> Unit,
    onApplyCustomPreset: (CustomEqualizerPreset) -> Unit,
    onSaveCustomPreset: (String) -> Unit,
    onDeleteCustomPreset: (String) -> Unit,
    onEqualizerBandChange: (Int, Int) -> Unit,
    onPreampAndBalanceChange: (Float, Float, Boolean, Boolean) -> Unit,
    onReplayGainModeChange: (ReplayGainMode) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessChange: (Boolean) -> Unit,
    onResetEqualizer: () -> Unit
) {
    BackHandler(onBack = onCollapse)

    val song = playbackState.currentSong
    val haptics = LocalHapticFeedback.current
    var activeSheet by remember { mutableStateOf(ActivePlayerSheet.NONE) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showMetadataDialog by remember { mutableStateOf(false) }
    var showBookmarkDialog by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }

    val artworkScale by animateFloatAsState(
        targetValue = if (playbackState.isPlaying) 1.0f else 0.91f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "art_scale"
    )

    val currentSongBookmarks = remember(bookmarks, song?.id) {
        bookmarks.filter { it.songId == song?.id }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ElectricViolet.copy(alpha = 0.28f),
                        DeepBlue.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("now_playing_screen")
    ) {
        val isLandscape = maxWidth > 640.dp

        if (song == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No track currently active", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onCollapse) {
                    Text("Browse Music Library")
                }
            }
            return@BoxWithConstraints
        }

        val artworkOrVinylComposable: @Composable (Modifier) -> Unit = { boxMod ->
            var dragAccumulated by remember { mutableFloatStateOf(0f) }
            Box(
                modifier = boxMod.pointerInput(swipeArtworkChangeTrack, hapticFeedbackEnabled) {
                    if (swipeArtworkChangeTrack) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccumulated = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                dragAccumulated += dragAmount
                            },
                            onDragEnd = {
                                if (dragAccumulated < -95f) {
                                    if (hapticFeedbackEnabled) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    onSkipNext()
                                } else if (dragAccumulated > 95f) {
                                    if (hapticFeedbackEnabled) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    onSkipPrevious()
                                }
                                dragAccumulated = 0f
                            }
                        )
                    }
                },
                contentAlignment = Alignment.Center
            ) {
                if (animatedVinylMode) {
                    AnimatedVinylTurntable(
                        artworkUri = song.albumArtUri,
                        title = song.title,
                        isPlaying = playbackState.isPlaying,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AlbumArtworkBox(
                        artworkUri = song.albumArtUri,
                        title = song.title,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(artworkScale),
                        cornerRadius = 28.dp
                    )
                }
            }
        }

        val controlsColumnComposable: @Composable (Modifier) -> Unit = { colMod ->
            Column(
                modifier = colMod,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Song Title, Artist, Rating & Favorite Heart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${song.artist} • ${song.genre}",
                            style = MaterialTheme.typography.titleMedium,
                            color = SoftCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StarRatingBar(
                            rating = song.rating,
                            onRatingChanged = { newRating -> onRatingChange(song.id, newRating) },
                            starSize = 18.dp
                        )
                    }

                    IconButton(
                        onClick = { onToggleFavorite(song.id) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_favorite_btn")
                    ) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Toggle Favorite",
                            tint = if (song.isFavorite) FavoritePink else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrubbing Seek Bar + Timestamps
                val safeDuration = playbackState.durationMs.coerceAtLeast(1L).toFloat()
                Slider(
                    value = playbackState.currentPositionMs.toFloat().coerceIn(0f, safeDuration),
                    onValueChange = { onSeekToMs(it.toLong()) },
                    valueRange = 0f..safeDuration,
                    colors = SliderDefaults.colors(
                        thumbColor = SoftCyan,
                        activeTrackColor = ElectricViolet,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("now_playing_seek_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDurationMs(playbackState.currentPositionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = buildString {
                            if (playbackState.playbackSpeed != 1.0f) append("${playbackState.playbackSpeed}x • ")
                            append("EQ: ${effectsState.activeCustomPresetName ?: effectsState.activePreset.displayName}")
                            if (effectsState.replayGainMode != ReplayGainMode.OFF) {
                                append(" • RG")
                            }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftCyan,
                        modifier = Modifier.clickable { showSpeedDialog = true }
                    )
                    Text(
                        text = formatDurationMs(playbackState.durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // A-B Repeat & Bookmarks Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = playbackState.abRepeatStartMs != null,
                        onClick = onSetAbPointA,
                        label = {
                            Text(
                                playbackState.abRepeatStartMs?.let { "A: ${formatDurationMs(it)}" } ?: "Set A"
                            )
                        },
                        modifier = Modifier.testTag("ab_repeat_set_a_btn")
                    )
                    FilterChip(
                        selected = playbackState.abRepeatEndMs != null,
                        onClick = onSetAbPointB,
                        label = {
                            Text(
                                playbackState.abRepeatEndMs?.let { "B: ${formatDurationMs(it)}" } ?: "Set B"
                            )
                        },
                        modifier = Modifier.testTag("ab_repeat_set_b_btn")
                    )
                    if (playbackState.abRepeatStartMs != null || playbackState.abRepeatEndMs != null) {
                        TextButton(onClick = onClearAbRepeat) {
                            Text("Clear A-B")
                        }
                    }

                    FilterChip(
                        selected = currentSongBookmarks.isNotEmpty(),
                        onClick = { showBookmarkDialog = true },
                        leadingIcon = {
                            Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Bookmarks (${currentSongBookmarks.size})") },
                        modifier = Modifier.testTag("now_playing_bookmarks_btn")
                    )

                    FilterChip(
                        selected = animatedVinylMode,
                        onClick = { onToggleAnimatedVinylMode(!animatedVinylMode) },
                        leadingIcon = {
                            Icon(Icons.Default.Album, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text(if (animatedVinylMode) "Vinyl On" else "Vinyl Off") },
                        modifier = Modifier.testTag("now_playing_vinyl_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Primary Transport Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_shuffle_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Toggle Shuffle",
                            tint = if (playbackState.shuffleEnabled) SoftCyan else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("now_playing_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Track",
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(
                        onClick = { onSeekByDeltaSec(-playbackState.skipIntervalSec) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind ${playbackState.skipIntervalSec}s",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = ElectricViolet,
                        shadowElevation = 12.dp,
                        modifier = Modifier.size(74.dp)
                    ) {
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("now_playing_play_pause_btn")
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play or Pause",
                                tint = Color.White,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { onSeekByDeltaSec(playbackState.skipIntervalSec) },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Fast forward ${playbackState.skipIntervalSec}s",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier
                            .size(54.dp)
                            .testTag("now_playing_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Track",
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(
                        onClick = onCycleRepeat,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("now_playing_repeat_btn")
                    ) {
                        Icon(
                            imageVector = if (playbackState.repeatMode == RepeatModeState.ONE) {
                                Icons.Default.RepeatOne
                            } else {
                                Icons.Default.Repeat
                            },
                            contentDescription = "Repeat Mode",
                            tint = if (playbackState.repeatMode != RepeatModeState.OFF) {
                                SoftCyan
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hardware-Linked System Media Volume Bar
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("now_playing_mute_btn")
                        ) {
                            Icon(
                                imageVector = if (volumeState.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Mute or unmute volume",
                                tint = if (volumeState.isMuted) MaterialTheme.colorScheme.error else SoftCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Slider(
                            value = volumeState.volumeFraction,
                            onValueChange = onVolumeFractionChange,
                            valueRange = 0f..1f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("now_playing_volume_slider")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "${volumeState.volumePercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Pills: Queue, Synchronized Lyrics, Equalizer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { activeSheet = ActivePlayerSheet.QUEUE },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_queue_sheet_btn")
                    ) {
                        Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Queue (${playbackState.queue.size})", maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = { activeSheet = ActivePlayerSheet.LYRICS },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_lyrics_sheet_btn")
                    ) {
                        Icon(Icons.Default.Lyrics, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Lyrics", maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = { activeSheet = ActivePlayerSheet.EQUALIZER },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_eq_sheet_btn")
                    ) {
                        Icon(Icons.Default.Equalizer, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Audio EQ", maxLines = 1)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Collapse, Playing From Info, Sleep Timer & More Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.testTag("now_playing_collapse_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize player",
                        modifier = Modifier.size(30.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (song.isOnlineStream) "ONLINE STREAM" else "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftCyan
                    )
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier.testTag("now_playing_sleep_timer_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (playbackState.sleepTimerOption != SleepTimerOption.OFF) SoftCyan else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { moreMenuExpanded = true },
                            modifier = Modifier.testTag("now_playing_more_btn")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = { moreMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Add to Playlist") },
                                leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    onRequestAddToPlaylist(song)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Playback Bookmarks (${currentSongBookmarks.size})") },
                                leadingIcon = { Icon(Icons.Default.BookmarkAdd, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    showBookmarkDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Playback Speed & Pitch (${playbackState.playbackSpeed}x)") },
                                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    showSpeedDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sleep Timer") },
                                leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                                onClick = {
                                    moreMenuExpanded = false
                                    showSleepTimerDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Track Metadata") },
                                onClick = {
                                    moreMenuExpanded = false
                                    showMetadataDialog = true
                                }
                            )
                        }
                    }
                }
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    artworkOrVinylComposable(
                        Modifier
                            .fillMaxHeight(0.86f)
                            .aspectRatio(1f)
                    )
                    controlsColumnComposable(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    artworkOrVinylComposable(
                        Modifier
                            .fillMaxWidth(0.82f)
                            .aspectRatio(1f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    controlsColumnComposable(Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Expandable Modal Bottom Sheets for Queue, Lyrics, and Equalizer
    if (activeSheet != ActivePlayerSheet.NONE) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { activeSheet = ActivePlayerSheet.NONE },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            when (activeSheet) {
                ActivePlayerSheet.QUEUE -> {
                    QueueSheetContent(
                        playbackState = playbackState,
                        namedQueues = namedQueues,
                        onSelectQueueIndex = onSelectQueueIndex,
                        onMoveQueueItem = onMoveQueueItem,
                        onRemoveQueueItem = onRemoveQueueItem,
                        onClearQueue = onClearQueue,
                        onSaveQueueAsPlaylist = onSaveQueueAsPlaylist,
                        onSaveNamedQueue = onSaveNamedQueue,
                        onRestoreNamedQueue = onRestoreNamedQueue,
                        onDeleteNamedQueue = onDeleteNamedQueue
                    )
                }
                ActivePlayerSheet.LYRICS -> {
                    LyricsSheetContent(
                        currentSong = song,
                        lyrics = currentLyrics,
                        currentPositionMs = playbackState.currentPositionMs,
                        onlineSearchState = onlineLyricsSearchState,
                        onSeekToMs = onSeekToMs,
                        onAdjustOffsetMs = onAdjustLyricsOffsetMs,
                        onSaveManualLyrics = onSaveManualLyrics,
                        onSearchOnlineLyrics = onSearchOnlineLyrics,
                        onSelectOnlineLyricResult = onSelectOnlineLyricResult
                    )
                }
                ActivePlayerSheet.EQUALIZER -> {
                    EqualizerAndEffectsContent(
                        effectsState = effectsState,
                        onToggleEnabled = onToggleEqualizerEnabled,
                        onSelectPreset = onSelectEqualizerPreset,
                        onApplyCustomPreset = onApplyCustomPreset,
                        onSaveCustomPreset = onSaveCustomPreset,
                        onDeleteCustomPreset = onDeleteCustomPreset,
                        onBandLevelChange = onEqualizerBandChange,
                        onPreampAndBalanceChange = onPreampAndBalanceChange,
                        onReplayGainModeChange = onReplayGainModeChange,
                        onBassBoostChange = onBassBoostChange,
                        onVirtualizerChange = onVirtualizerChange,
                        onLoudnessChange = onLoudnessChange,
                        onResetDefault = onResetEqualizer
                    )
                }
                ActivePlayerSheet.NONE -> {}
            }
        }
    }

    // Bookmarks Dialog
    if (showBookmarkDialog) {
        var bookmarkLabel by remember { mutableStateOf("Chorus Drop") }
        AlertDialog(
            onDismissRequest = { showBookmarkDialog = false },
            title = { Text("Playback Bookmarks") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bookmarkLabel,
                            onValueChange = { bookmarkLabel = it },
                            label = { Text("Bookmark at ${formatDurationMs(playbackState.currentPositionMs)}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                onAddBookmark(bookmarkLabel)
                                bookmarkLabel = "Marker"
                            }
                        ) {
                            Text("Add")
                        }
                    }

                    if (currentSongBookmarks.isEmpty()) {
                        Text(
                            "No bookmarks saved for this track yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        currentSongBookmarks.forEach { bm ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSeekToMs(bm.positionMs)
                                        showBookmarkDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(bm.label, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            formatDurationMs(bm.positionMs),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SoftCyan
                                        )
                                    }
                                    IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete bookmark",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBookmarkDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Playback Speed & Pitch Dialog
    if (showSpeedDialog) {
        var speed by remember { mutableFloatStateOf(playbackState.playbackSpeed) }
        var pitch by remember { mutableFloatStateOf(playbackState.playbackPitch) }
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed & Pitch") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = String.format("Speed: %.2fx", speed),
                        style = MaterialTheme.typography.titleSmall,
                        color = SoftCyan
                    )
                    Slider(
                        value = speed,
                        onValueChange = { speed = (it * 20).toInt() / 20f },
                        valueRange = 0.5f..2.0f
                    )

                    Text(
                        text = String.format("Pitch: %.2fx", pitch),
                        style = MaterialTheme.typography.titleSmall,
                        color = SoftCyan
                    )
                    Slider(
                        value = pitch,
                        onValueChange = { pitch = (it * 20).toInt() / 20f },
                        valueRange = 0.5f..2.0f
                    )

                    TextButton(
                        onClick = {
                            speed = 1.0f
                            pitch = 1.0f
                        }
                    ) {
                        Text("Reset to 1.0x Normal")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSpeedAndPitchChange(speed, pitch)
                        showSpeedDialog = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        var customMin by remember { mutableStateOf("25") }
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SleepTimerOption.entries.forEach { option ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (playbackState.sleepTimerOption == option) {
                                ElectricViolet.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectSleepTimer(option, customMin.toIntOrNull() ?: 25)
                                    showSleepTimerDialog = false
                                }
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = customMin,
                        onValueChange = { customMin = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Or Custom Duration (Minutes)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = customMin.toIntOrNull()?.coerceIn(1, 360) ?: 25
                        onSelectSleepTimer(SleepTimerOption.MIN_15, mins)
                        showSleepTimerDialog = false
                    }
                ) {
                    Text("Start Custom (${customMin.ifBlank { "25" }}m)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showMetadataDialog && song != null) {
        TrackMetadataDialog(
            song = song,
            onDismiss = { showMetadataDialog = false }
        )
    }
}

@Composable
private fun AnimatedVinylTurntable(
    artworkUri: String?,
    title: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinyl_angle"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.94f)
                .rotate(if (isPlaying) spinAngle else 0f)
                .clip(CircleShape)
                .background(Color(0xFF101118)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxR = size.minDimension / 2f
                for (i in 1..6) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = maxR * (0.48f + i * 0.075f),
                        center = center,
                        style = Stroke(width = 2f)
                    )
                }
                drawCircle(
                    color = SoftCyan.copy(alpha = 0.25f),
                    radius = maxR * 0.44f,
                    center = center,
                    style = Stroke(width = 3f)
                )
            }

            AlbumArtworkBox(
                artworkUri = artworkUri,
                title = title,
                modifier = Modifier.fillMaxSize(0.42f),
                cornerRadius = 200.dp
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            )
        }

        // Stylus Tonearm overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val start = Offset(size.width * 0.86f, size.height * 0.12f)
            val pivot = if (isPlaying) {
                Offset(size.width * 0.64f, size.height * 0.44f)
            } else {
                Offset(size.width * 0.82f, size.height * 0.48f)
            }
            drawCircle(color = SoftCyan, radius = 10.dp.toPx(), center = start)
            drawLine(
                color = Color.LightGray,
                start = start,
                end = pivot,
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
