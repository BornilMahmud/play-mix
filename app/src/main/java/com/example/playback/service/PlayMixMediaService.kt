package com.example.playback.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.example.MainActivity
import com.example.PlayMixApplication
import com.example.R
import com.example.domain.model.Song
import com.example.playback.widget.PlayMixWidgetProvider
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayMixMediaService : MediaLibraryService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "playmix_playback_channel"
        const val NOTIFICATION_ID = 1001
        private const val ROOT_ID = "playmix_root"
        private const val SONGS_ID = "playmix_songs"
        private const val FAVORITES_ID = "playmix_favorites"

        fun songToMediaItem(song: Song): MediaItem {
            val extras = Bundle().apply {
                putString("song_id", song.id)
                putString("album_artist", song.albumArtist)
                putString("genre", song.genre)
                putLong("duration_ms", song.durationMs)
                putString("file_path", song.filePath)
                putString("folder_name", song.folderName)
                putBoolean("is_favorite", song.isFavorite)
                putBoolean("is_demo", song.isDemoTrack)
            }
            val metadataBuilder = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setAlbumArtist(song.albumArtist)
                .setGenre(song.genre)
                .setTrackNumber(song.trackNumber)
                .setReleaseYear(song.year)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                .setExtras(extras)

            song.albumArtUri?.takeIf { it.isNotBlank() }?.let { artUri ->
                metadataBuilder.setArtworkUri(Uri.parse(artUri))
            }

            return MediaItem.Builder()
                .setMediaId(song.id)
                .setUri(Uri.parse(song.uri))
                .setMediaMetadata(metadataBuilder.build())
                .build()
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()

        val appContainer = (application as? PlayMixApplication)?.container

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        exoPlayer.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                appContainer?.audioEffectsController?.bindToAudioSession(audioSessionId)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val songId = mediaItem?.mediaId ?: return
                val title = mediaItem.mediaMetadata.title?.toString() ?: "Play Mix"
                val artist = mediaItem.mediaMetadata.artist?.toString() ?: "Playing"
                PlayMixWidgetProvider.notifyPlaybackChanged(
                    this@PlayMixMediaService,
                    title,
                    artist,
                    exoPlayer.isPlaying
                )
                serviceScope.launch {
                    appContainer?.libraryRepository?.recordSongPlayed(songId)
                    appContainer?.preferencesRepository?.saveLastPlaybackSession(
                        songId = songId,
                        positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                    )
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val item = exoPlayer.currentMediaItem
                val currentId = item?.mediaId ?: return
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val title = item.mediaMetadata.title?.toString() ?: "Play Mix"
                val artist = item.mediaMetadata.artist?.toString() ?: "Playing"
                PlayMixWidgetProvider.notifyPlaybackChanged(
                    this@PlayMixMediaService,
                    title,
                    artist,
                    isPlaying
                )
                serviceScope.launch {
                    appContainer?.preferencesRepository?.saveLastPlaybackSession(currentId, pos)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (exoPlayer.hasNextMediaItem()) {
                    exoPlayer.seekToNextMediaItem()
                    exoPlayer.prepare()
                }
            }
        })

        if (exoPlayer.audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
            appContainer?.audioEffectsController?.bindToAudioSession(exoPlayer.audioSessionId)
        }

        val sessionActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            sessionActivityIntent,
            pendingIntentFlags
        )

        val sessionCallback = object : MediaLibrarySession.Callback {

            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val connectionResult = super.onConnect(session, controller)
                val availablePlayerCommands = connectionResult.availablePlayerCommands
                    .buildUpon()
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .add(Player.COMMAND_SET_SHUFFLE_MODE)
                    .add(Player.COMMAND_SET_REPEAT_MODE)
                    .build()
                return MediaSession.ConnectionResult.accept(
                    connectionResult.availableSessionCommands,
                    availablePlayerCommands
                )
            }

            override fun onAddMediaItems(
                mediaSession: MediaSession,
                controller: MediaSession.ControllerInfo,
                mediaItems: MutableList<MediaItem>
            ): ListenableFuture<MutableList<MediaItem>> {
                return Futures.immediateFuture(mediaItems)
            }

            override fun onGetLibraryRoot(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<MediaItem>> {
                val rootItem = MediaItem.Builder()
                    .setMediaId(ROOT_ID)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle("Play Mix Library")
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                            .build()
                    )
                    .build()
                return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
            }

            override fun onGetChildren(
                session: MediaLibrarySession,
                browser: MediaSession.ControllerInfo,
                parentId: String,
                page: Int,
                pageSize: Int,
                params: LibraryParams?
            ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
                return when (parentId) {
                    ROOT_ID -> {
                        val tabs = ImmutableList.of(
                            MediaItem.Builder()
                                .setMediaId(SONGS_ID)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle("All Songs")
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                                        .build()
                                )
                                .build(),
                            MediaItem.Builder()
                                .setMediaId(FAVORITES_ID)
                                .setMediaMetadata(
                                    MediaMetadata.Builder()
                                        .setTitle("Favorites")
                                        .setIsBrowsable(true)
                                        .setIsPlayable(false)
                                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS)
                                        .build()
                                )
                                .build()
                        )
                        Futures.immediateFuture(LibraryResult.ofItemList(tabs, params))
                    }
                    else -> {
                        Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.of(), params))
                    }
                }
            }
        }

        mediaLibrarySession = MediaLibrarySession.Builder(this, exoPlayer, sessionCallback)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(NOTIFICATION_CHANNEL_ID)
            .setChannelName(R.string.notification_channel_name)
            .setNotificationId(NOTIFICATION_ID)
            .build()
            .apply {
                setSmallIcon(R.drawable.ic_launcher_foreground)
            }
        setMediaNotificationProvider(notificationProvider)

        player = exoPlayer

        serviceScope.launch {
            val repo = appContainer?.libraryRepository ?: return@launch
            val prefs = appContainer.preferencesRepository.getSnapshot()
            val (persistedQueue, lastState) = repo.loadPersistedQueue()
            if (persistedQueue.isNotEmpty() && exoPlayer.mediaItemCount == 0) {
                val (lastSongId, lastPosMs) = lastState
                val mediaItems = persistedQueue.map { songToMediaItem(it) }
                val startIndex = persistedQueue.indexOfFirst { it.id == lastSongId }.coerceAtLeast(0)
                exoPlayer.setMediaItems(mediaItems, startIndex, lastPosMs.coerceAtLeast(0L))
                exoPlayer.shuffleModeEnabled = prefs.shuffleEnabled
                exoPlayer.repeatMode = when (prefs.repeatMode) {
                    com.example.domain.model.RepeatModeState.OFF -> Player.REPEAT_MODE_OFF
                    com.example.domain.model.RepeatModeState.ALL -> Player.REPEAT_MODE_ALL
                    com.example.domain.model.RepeatModeState.ONE -> Player.REPEAT_MODE_ONE
                }
                exoPlayer.setPlaybackParameters(
                    androidx.media3.common.PlaybackParameters(prefs.playbackSpeed, prefs.playbackPitch)
                )
                exoPlayer.prepare()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
            val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
            }
            if (keyEvent?.action == KeyEvent.ACTION_DOWN) {
                val p = player
                if (p != null) {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (p.isPlaying) p.pause() else p.play()
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> p.play()
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> p.pause()
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            if (p.hasNextMediaItem()) p.seekToNextMediaItem() else p.seekTo(0, 0L)
                            p.play()
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            if (p.hasPreviousMediaItem()) p.seekToPreviousMediaItem() else p.seekTo(0, 0L)
                            p.play()
                        }
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val currentPlayer = player
        if (currentPlayer == null || !currentPlayer.playWhenReady || currentPlayer.mediaItemCount == 0) {
            val currentId = currentPlayer?.currentMediaItem?.mediaId.orEmpty()
            val currentPos = currentPlayer?.currentPosition ?: 0L
            val appContainer = (application as? PlayMixApplication)?.container
            serviceScope.launch {
                if (currentId.isNotBlank()) {
                    appContainer?.preferencesRepository?.saveLastPlaybackSession(currentId, currentPos)
                }
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        val appContainer = (application as? PlayMixApplication)?.container
        appContainer?.audioEffectsController?.releaseHardwareEffects()
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        player = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return
            val existing = manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.notification_channel_desc)
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
