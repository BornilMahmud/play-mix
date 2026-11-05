package com.example.playback.controller

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.core.datastore.UserPreferencesRepository
import com.example.data.library.MusicLibraryRepository
import com.example.domain.model.RepeatModeState
import com.example.domain.model.ReplayGainMode
import com.example.domain.model.SleepTimerOption
import com.example.domain.model.Song
import com.example.playback.service.PlayMixMediaService
import com.example.playback.widget.PlayMixWidgetProvider
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.pow

data class PlaybackUiState(
    val currentSong: Song? = null,
    val queue: List<Song> = emptyList(),
    val currentQueueIndex: Int = 0,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeState = RepeatModeState.OFF,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val crossfadeDurationSec: Int = 0,
    val fadeInOutEnabled: Boolean = true,
    val gaplessPlayback: Boolean = true,
    val skipIntervalSec: Int = 10,
    val abRepeatStartMs: Long? = null,
    val abRepeatEndMs: Long? = null,
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF,
    val sleepTimerOption: SleepTimerOption = SleepTimerOption.OFF,
    val sleepTimerRemainingMs: Long = 0L,
    val stopAtEndOfCurrentTrack: Boolean = false,
    val playbackError: String? = null
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) {
            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

class PlaybackConnectionManager(
    context: Context,
    private val libraryRepository: MusicLibraryRepository,
    private val prefsRepository: UserPreferencesRepository,
    private val audioEffectsController: AudioEffectsController
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var progressTickerJob: Job? = null
    private var sleepTimerJob: Job? = null

    private var allSongsSnapshotById: Map<String, Song> = emptyMap()

    private val _playbackState = MutableStateFlow(PlaybackUiState())
    val playbackState: StateFlow<PlaybackUiState> = _playbackState.asStateFlow()

    init {
        observePreferencesAndLibrary()
        connectToService()
        startProgressTicker()
    }

    private fun observePreferencesAndLibrary() {
        scope.launch {
            libraryRepository.allSongsIncludingHiddenFlow.collectLatest { songs ->
                allSongsSnapshotById = songs.associateBy { it.id }
                val currentId = _playbackState.value.currentSong?.id
                val updatedCurrent = currentId?.let { allSongsSnapshotById[it] } ?: _playbackState.value.currentSong
                val updatedQueue = _playbackState.value.queue.map { qSong ->
                    allSongsSnapshotById[qSong.id] ?: qSong
                }
                _playbackState.update {
                    it.copy(
                        currentSong = updatedCurrent,
                        queue = updatedQueue
                    )
                }
            }
        }

        scope.launch {
            prefsRepository.preferencesFlow.collectLatest { prefs ->
                _playbackState.update {
                    it.copy(
                        shuffleEnabled = prefs.shuffleEnabled,
                        repeatMode = prefs.repeatMode,
                        playbackSpeed = prefs.playbackSpeed,
                        playbackPitch = prefs.playbackPitch,
                        crossfadeDurationSec = prefs.crossfadeDurationSec,
                        fadeInOutEnabled = prefs.fadeInOutEnabled,
                        gaplessPlayback = prefs.gaplessPlayback,
                        skipIntervalSec = prefs.skipIntervalSec,
                        replayGainMode = prefs.replayGainMode
                    )
                }
            }
        }
    }

    fun connectToService() {
        if (mediaController != null || controllerFuture != null) return
        val sessionToken = SessionToken(
            appContext,
            ComponentName(appContext, PlayMixMediaService::class.java)
        )
        val future = MediaController.Builder(appContext, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val controller = future.get()
                    mediaController = controller
                    attachControllerListener(controller)
                    syncStateFromController(controller)
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    private fun attachControllerListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (_playbackState.value.stopAtEndOfCurrentTrack &&
                    reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                ) {
                    controller.pause()
                    cancelSleepTimer()
                }
                // Clear A-B repeat when transitioning to a different song
                clearAbRepeat()
                syncStateFromController(controller)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                syncStateFromController(controller)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                syncStateFromController(controller)
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playbackState.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
                scope.launch { prefsRepository.setShuffleEnabled(shuffleModeEnabled) }
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                val mapped = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatModeState.ONE
                    Player.REPEAT_MODE_ALL -> RepeatModeState.ALL
                    else -> RepeatModeState.OFF
                }
                _playbackState.update { it.copy(repeatMode = mapped) }
                scope.launch { prefsRepository.setRepeatMode(mapped) }
            }

            override fun onPlayerError(error: PlaybackException) {
                _playbackState.update {
                    it.copy(playbackError = "Playback issue (${error.errorCodeName}). Advancing if queue has next track.")
                }
            }
        })
    }

    private fun syncStateFromController(controller: MediaController) {
        val count = controller.mediaItemCount
        if (count == 0) {
            scope.launch {
                val (persistedQueue, lastPair) = libraryRepository.loadPersistedQueue()
                if (persistedQueue.isNotEmpty() && controller.mediaItemCount == 0) {
                    val (lastSongId, lastPosMs) = lastPair
                    val startIdx = persistedQueue.indexOfFirst { it.id == lastSongId }.coerceAtLeast(0)
                    val items = persistedQueue.map { PlayMixMediaService.songToMediaItem(it) }
                    controller.setMediaItems(items, startIdx, lastPosMs)
                    controller.prepare()
                    val activeSong = persistedQueue.getOrNull(startIdx)
                    _playbackState.update {
                        it.copy(
                            queue = persistedQueue,
                            currentSong = activeSong,
                            currentQueueIndex = startIdx,
                            currentPositionMs = lastPosMs,
                            durationMs = activeSong?.durationMs ?: 0L
                        )
                    }
                    if (activeSong != null) {
                        PlayMixWidgetProvider.notifyPlaybackChanged(
                            appContext,
                            activeSong.title,
                            activeSong.artist,
                            false
                        )
                    }
                }
            }
            return
        }

        val currentQueue = (0 until count).mapNotNull { idx ->
            val item = controller.getMediaItemAt(idx)
            allSongsSnapshotById[item.mediaId] ?: mediaItemToFallbackSong(item)
        }
        val currentIdx = controller.currentMediaItemIndex.coerceIn(0, (currentQueue.size - 1).coerceAtLeast(0))
        val currentSong = currentQueue.getOrNull(currentIdx)
        val duration = controller.duration.takeIf { it > 0L } ?: currentSong?.durationMs ?: 0L
        val position = controller.currentPosition.coerceIn(0L, duration.coerceAtLeast(1L))

        _playbackState.update {
            it.copy(
                currentSong = currentSong,
                queue = currentQueue,
                currentQueueIndex = currentIdx,
                isPlaying = controller.isPlaying,
                currentPositionMs = position,
                durationMs = duration,
                playbackError = null
            )
        }

        if (currentSong != null) {
            PlayMixWidgetProvider.notifyPlaybackChanged(
                appContext,
                currentSong.title,
                currentSong.artist,
                controller.isPlaying
            )
        }
    }

    private fun startProgressTicker() {
        progressTickerJob?.cancel()
        progressTickerJob = scope.launch {
            var persistTick = 0
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.mediaItemCount > 0) {
                    val state = _playbackState.value
                    val currentSong = state.currentSong
                    val duration = controller.duration.takeIf { it > 0L } ?: currentSong?.durationMs ?: 0L
                    val position = controller.currentPosition.coerceAtLeast(0L)

                    // 1. A-B Repeat loop check
                    val abStart = state.abRepeatStartMs
                    val abEnd = state.abRepeatEndMs
                    if (abStart != null && abEnd != null && abEnd > abStart && position >= abEnd) {
                        controller.seekTo(abStart)
                    }

                    // 2. Compute ReplayGain + Crossfade + Clipping Prevention volume envelope
                    val rgMultiplier = when (state.replayGainMode) {
                        ReplayGainMode.OFF -> 1.0f
                        ReplayGainMode.TRACK -> {
                            val db = currentSong?.trackGainDb ?: 0f
                            10f.pow(db / 20f).coerceIn(0.4f, 1.0f)
                        }
                        ReplayGainMode.ALBUM -> {
                            val db = currentSong?.albumGainDb ?: 0f
                            10f.pow(db / 20f).coerceIn(0.4f, 1.0f)
                        }
                    }

                    val crossfadeMs = state.crossfadeDurationSec * 1000L
                    val fadeEnvelope = if (crossfadeMs > 0L && duration > crossfadeMs * 2 && controller.isPlaying) {
                        val remaining = duration - position
                        when {
                            position < crossfadeMs -> (position.toFloat() / crossfadeMs.toFloat()).coerceIn(0.25f, 1.0f)
                            remaining in 1..crossfadeMs -> (remaining.toFloat() / crossfadeMs.toFloat()).coerceIn(0.2f, 1.0f)
                            else -> 1.0f
                        }
                    } else 1.0f

                    val finalVol = (rgMultiplier * fadeEnvelope).coerceIn(0.1f, 1.0f)
                    if (controller.volume != finalVol) {
                        controller.volume = finalVol
                    }

                    _playbackState.update {
                        it.copy(
                            isPlaying = controller.isPlaying,
                            currentPositionMs = position,
                            durationMs = duration
                        )
                    }

                    persistTick++
                    if (controller.isPlaying && persistTick % 20 == 0 && currentSong != null) {
                        prefsRepository.saveLastPlaybackSession(currentSong.id, position)
                    }
                }
                delay(200L)
            }
        }
    }

    // --- A-B REPEAT ---

    fun setAbRepeatPointA() {
        val pos = _playbackState.value.currentPositionMs
        _playbackState.update {
            it.copy(
                abRepeatStartMs = pos,
                abRepeatEndMs = if (it.abRepeatEndMs != null && it.abRepeatEndMs <= pos) null else it.abRepeatEndMs
            )
        }
    }

    fun setAbRepeatPointB() {
        val pos = _playbackState.value.currentPositionMs
        val start = _playbackState.value.abRepeatStartMs ?: 0L
        if (pos > start + 500L) {
            _playbackState.update {
                it.copy(
                    abRepeatStartMs = start,
                    abRepeatEndMs = pos
                )
            }
        }
    }

    fun clearAbRepeat() {
        _playbackState.update {
            it.copy(abRepeatStartMs = null, abRepeatEndMs = null)
        }
    }

    // --- TRANSPORT & QUEUE ACTIONS ---

    fun playSongFromQueue(songs: List<Song>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        if (songs.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, songs.lastIndex)
        val selectedSong = songs[safeIndex]

        _playbackState.update {
            it.copy(
                currentSong = selectedSong,
                queue = songs,
                currentQueueIndex = safeIndex,
                isPlaying = true,
                currentPositionMs = startPositionMs,
                durationMs = selectedSong.durationMs,
                abRepeatStartMs = null,
                abRepeatEndMs = null,
                playbackError = null
            )
        }

        val controller = mediaController
        if (controller != null) {
            val mediaItems = songs.map { PlayMixMediaService.songToMediaItem(it) }
            controller.setMediaItems(mediaItems, safeIndex, startPositionMs)
            controller.prepare()
            controller.play()
        }

        PlayMixWidgetProvider.notifyPlaybackChanged(
            appContext,
            selectedSong.title,
            selectedSong.artist,
            true
        )

        scope.launch {
            libraryRepository.recordSongPlayed(selectedSong.id)
            libraryRepository.savePersistedQueue(songs, selectedSong.id, startPositionMs)
        }
    }

    fun shuffleAndPlay(songs: List<Song>) {
        if (songs.isEmpty()) return
        val shuffled = songs.shuffled()
        setShuffleEnabled(true)
        playSongFromQueue(shuffled, 0)
    }

    fun togglePlayPause() {
        val controller = mediaController
        val currentState = _playbackState.value
        if (controller != null) {
            if (controller.mediaItemCount == 0 && currentState.queue.isNotEmpty()) {
                playSongFromQueue(currentState.queue, currentState.currentQueueIndex)
                return
            }
            if (controller.isPlaying) {
                if (currentState.fadeInOutEnabled) {
                    scope.launch {
                        controller.volume = 0.5f
                        delay(80L)
                        controller.pause()
                        controller.volume = 1.0f
                    }
                } else {
                    controller.pause()
                }
                _playbackState.update { it.copy(isPlaying = false) }
            } else {
                if (currentState.fadeInOutEnabled) {
                    controller.volume = 0.35f
                    controller.play()
                    scope.launch {
                        delay(120L)
                        controller.volume = 1.0f
                    }
                } else {
                    controller.play()
                }
                _playbackState.update { it.copy(isPlaying = true) }
            }
        } else {
            _playbackState.update { it.copy(isPlaying = !it.isPlaying) }
        }
    }

    fun skipToNext() {
        val controller = mediaController
        val state = _playbackState.value
        if (state.queue.isEmpty()) return
        val nextIndex = if (state.currentQueueIndex + 1 < state.queue.size) {
            state.currentQueueIndex + 1
        } else {
            0
        }
        val nextSong = state.queue[nextIndex]
        _playbackState.update {
            it.copy(
                currentQueueIndex = nextIndex,
                currentSong = nextSong,
                currentPositionMs = 0L,
                durationMs = nextSong.durationMs,
                abRepeatStartMs = null,
                abRepeatEndMs = null
            )
        }
        if (controller != null && controller.mediaItemCount > 0) {
            if (controller.hasNextMediaItem()) {
                controller.seekToNextMediaItem()
            } else {
                controller.seekTo(0, 0L)
            }
            controller.play()
        }
        scope.launch {
            libraryRepository.recordSongPlayed(nextSong.id)
            libraryRepository.savePersistedQueue(state.queue, nextSong.id, 0L)
        }
    }

    fun skipToPrevious() {
        val controller = mediaController
        val state = _playbackState.value
        if (state.queue.isEmpty()) return
        if (state.currentPositionMs > 4000L) {
            seekToPosition(0L)
            return
        }
        val prevIndex = if (state.currentQueueIndex - 1 >= 0) {
            state.currentQueueIndex - 1
        } else {
            state.queue.lastIndex
        }
        val prevSong = state.queue[prevIndex]
        _playbackState.update {
            it.copy(
                currentQueueIndex = prevIndex,
                currentSong = prevSong,
                currentPositionMs = 0L,
                durationMs = prevSong.durationMs,
                abRepeatStartMs = null,
                abRepeatEndMs = null
            )
        }
        if (controller != null && controller.mediaItemCount > 0) {
            if (controller.hasPreviousMediaItem()) {
                controller.seekToPreviousMediaItem()
            } else {
                controller.seekTo(prevIndex, 0L)
            }
            controller.play()
        }
        scope.launch {
            libraryRepository.recordSongPlayed(prevSong.id)
            libraryRepository.savePersistedQueue(state.queue, prevSong.id, 0L)
        }
    }

    fun seekToPosition(positionMs: Long) {
        val duration = _playbackState.value.durationMs.coerceAtLeast(1L)
        val clamped = positionMs.coerceIn(0L, duration)
        mediaController?.seekTo(clamped)
        _playbackState.update { it.copy(currentPositionMs = clamped) }
        val currentId = _playbackState.value.currentSong?.id ?: return
        scope.launch { prefsRepository.saveLastPlaybackSession(currentId, clamped) }
    }

    fun seekByDeltaSeconds(deltaSec: Int) {
        val target = _playbackState.value.currentPositionMs + (deltaSec * 1000L)
        seekToPosition(target)
    }

    fun setShuffleEnabled(enabled: Boolean) {
        mediaController?.shuffleModeEnabled = enabled
        _playbackState.update { it.copy(shuffleEnabled = enabled) }
        scope.launch { prefsRepository.setShuffleEnabled(enabled) }
    }

    fun toggleShuffle() {
        setShuffleEnabled(!_playbackState.value.shuffleEnabled)
    }

    fun cycleRepeatMode() {
        val next = when (_playbackState.value.repeatMode) {
            RepeatModeState.OFF -> RepeatModeState.ALL
            RepeatModeState.ALL -> RepeatModeState.ONE
            RepeatModeState.ONE -> RepeatModeState.OFF
        }
        val playerMode = when (next) {
            RepeatModeState.OFF -> Player.REPEAT_MODE_OFF
            RepeatModeState.ALL -> Player.REPEAT_MODE_ALL
            RepeatModeState.ONE -> Player.REPEAT_MODE_ONE
        }
        mediaController?.repeatMode = playerMode
        _playbackState.update { it.copy(repeatMode = next) }
        scope.launch { prefsRepository.setRepeatMode(next) }
    }

    fun playNextInQueue(song: Song) {
        val currentQueue = _playbackState.value.queue.toMutableList()
        if (currentQueue.isEmpty()) {
            playSongFromQueue(listOf(song), 0)
            return
        }
        val insertIdx = (_playbackState.value.currentQueueIndex + 1).coerceAtMost(currentQueue.size)
        currentQueue.add(insertIdx, song)
        mediaController?.addMediaItem(insertIdx, PlayMixMediaService.songToMediaItem(song))
        _playbackState.update { it.copy(queue = currentQueue) }
        persistQueueSnapshot()
    }

    fun addToQueue(song: Song) {
        val currentQueue = _playbackState.value.queue.toMutableList()
        if (currentQueue.isEmpty()) {
            playSongFromQueue(listOf(song), 0)
            return
        }
        currentQueue.add(song)
        mediaController?.addMediaItem(PlayMixMediaService.songToMediaItem(song))
        _playbackState.update { it.copy(queue = currentQueue) }
        persistQueueSnapshot()
    }

    fun removeFromQueue(index: Int) {
        val currentQueue = _playbackState.value.queue.toMutableList()
        if (index !in currentQueue.indices) return
        currentQueue.removeAt(index)
        mediaController?.removeMediaItem(index)
        val newCurrentIdx = when {
            currentQueue.isEmpty() -> 0
            index < _playbackState.value.currentQueueIndex -> (_playbackState.value.currentQueueIndex - 1).coerceAtLeast(0)
            else -> _playbackState.value.currentQueueIndex.coerceAtMost(currentQueue.lastIndex)
        }
        _playbackState.update {
            it.copy(
                queue = currentQueue,
                currentQueueIndex = newCurrentIdx,
                currentSong = currentQueue.getOrNull(newCurrentIdx)
            )
        }
        persistQueueSnapshot()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val currentQueue = _playbackState.value.queue.toMutableList()
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices || fromIndex == toIndex) return
        val moved = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, moved)
        mediaController?.moveMediaItem(fromIndex, toIndex)

        val currentSongId = _playbackState.value.currentSong?.id
        val newCurrentIdx = currentQueue.indexOfFirst { it.id == currentSongId }.coerceAtLeast(0)
        _playbackState.update {
            it.copy(
                queue = currentQueue,
                currentQueueIndex = newCurrentIdx
            )
        }
        persistQueueSnapshot()
    }

    fun clearQueue() {
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _playbackState.update {
            it.copy(
                queue = emptyList(),
                currentSong = null,
                currentQueueIndex = 0,
                isPlaying = false,
                currentPositionMs = 0L,
                durationMs = 0L,
                abRepeatStartMs = null,
                abRepeatEndMs = null
            )
        }
        persistQueueSnapshot()
    }

    fun setPlaybackSpeedAndPitch(speed: Float, pitch: Float) {
        val s = speed.coerceIn(0.5f, 2.0f)
        val p = pitch.coerceIn(0.5f, 2.0f)
        mediaController?.setPlaybackParameters(PlaybackParameters(s, p))
        _playbackState.update {
            it.copy(playbackSpeed = s, playbackPitch = p)
        }
        scope.launch {
            prefsRepository.setPlaybackSpeedAndPitch(s, p)
        }
    }

    // --- SLEEP TIMER ---

    fun startSleepTimer(option: SleepTimerOption, customMinutes: Int = 20) {
        sleepTimerJob?.cancel()
        when (option) {
            SleepTimerOption.OFF -> {
                _playbackState.update {
                    it.copy(
                        sleepTimerOption = SleepTimerOption.OFF,
                        sleepTimerRemainingMs = 0L,
                        stopAtEndOfCurrentTrack = false
                    )
                }
            }
            SleepTimerOption.END_OF_TRACK -> {
                _playbackState.update {
                    it.copy(
                        sleepTimerOption = SleepTimerOption.END_OF_TRACK,
                        sleepTimerRemainingMs = (it.durationMs - it.currentPositionMs).coerceAtLeast(0L),
                        stopAtEndOfCurrentTrack = true
                    )
                }
            }
            else -> {
                val minutes = if (option.minutes > 0) option.minutes else customMinutes.coerceAtLeast(1)
                val totalMs = minutes * 60_000L
                _playbackState.update {
                    it.copy(
                        sleepTimerOption = option,
                        sleepTimerRemainingMs = totalMs,
                        stopAtEndOfCurrentTrack = false
                    )
                }
                sleepTimerJob = scope.launch {
                    var remaining = totalMs
                    while (remaining > 0L && isActive) {
                        delay(1000L)
                        remaining = (remaining - 1000L).coerceAtLeast(0L)
                        _playbackState.update { it.copy(sleepTimerRemainingMs = remaining) }
                    }
                    if (remaining <= 0L) {
                        mediaController?.pause()
                        _playbackState.update {
                            it.copy(
                                isPlaying = false,
                                sleepTimerOption = SleepTimerOption.OFF,
                                sleepTimerRemainingMs = 0L
                            )
                        }
                    }
                }
            }
        }
    }

    fun cancelSleepTimer() {
        startSleepTimer(SleepTimerOption.OFF)
    }

    fun clearPlaybackError() {
        _playbackState.update { it.copy(playbackError = null) }
    }

    private fun persistQueueSnapshot() {
        val state = _playbackState.value
        scope.launch {
            libraryRepository.savePersistedQueue(
                songs = state.queue,
                currentSongId = state.currentSong?.id.orEmpty(),
                currentPositionMs = state.currentPositionMs
            )
        }
    }

    private fun mediaItemToFallbackSong(item: MediaItem): Song {
        val meta = item.mediaMetadata
        val extras = meta.extras
        return Song(
            id = item.mediaId.ifBlank { "unknown" },
            uri = item.localConfiguration?.uri?.toString().orEmpty(),
            title = meta.title?.toString() ?: "Unknown Track",
            artist = meta.artist?.toString() ?: "Unknown Artist",
            album = meta.albumTitle?.toString() ?: "Unknown Album",
            albumArtist = meta.albumArtist?.toString() ?: "Unknown Artist",
            genre = meta.genre?.toString() ?: "Music",
            durationMs = extras?.getLong("duration_ms") ?: 180_000L,
            albumArtUri = meta.artworkUri?.toString(),
            filePath = extras?.getString("file_path").orEmpty(),
            folderName = extras?.getString("folder_name") ?: "Music",
            isFavorite = extras?.getBoolean("is_favorite") ?: false,
            isDemoTrack = extras?.getBoolean("is_demo") ?: false
        )
    }
}
