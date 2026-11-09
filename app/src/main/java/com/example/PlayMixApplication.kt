package com.example

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.core.database.PlayMixDatabase
import com.example.core.datastore.UserPreferencesRepository
import com.example.data.library.MediaStoreScanner
import com.example.data.library.MusicLibraryRepository
import com.example.data.library.OfflineArtworkDetector
import com.example.data.lyrics.LyricsRepository
import com.example.data.online.OnlineMusicRepository
import com.example.playback.controller.AudioEffectsController
import com.example.playback.controller.PlaybackConnectionManager
import com.example.playback.controller.SystemVolumeController

class PlayMixAppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: PlayMixDatabase by lazy {
        PlayMixDatabase.getInstance(appContext)
    }

    val preferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(appContext)
    }

    val offlineArtworkDetector: OfflineArtworkDetector by lazy {
        OfflineArtworkDetector(appContext)
    }

    val mediaStoreScanner: MediaStoreScanner by lazy {
        MediaStoreScanner(appContext, offlineArtworkDetector)
    }

    val libraryRepository: MusicLibraryRepository by lazy {
        MusicLibraryRepository(
            context = appContext,
            dao = database.playMixDao(),
            scanner = mediaStoreScanner,
            prefsRepo = preferencesRepository,
            artworkDetector = offlineArtworkDetector
        )
    }

    val lyricsRepository: LyricsRepository by lazy {
        LyricsRepository(database.playMixDao())
    }

    val onlineMusicRepository: OnlineMusicRepository by lazy {
        OnlineMusicRepository(
            dao = database.playMixDao(),
            prefsRepo = preferencesRepository
        )
    }

    val audioEffectsController: AudioEffectsController by lazy {
        AudioEffectsController(appContext, preferencesRepository)
    }

    val systemVolumeController: SystemVolumeController by lazy {
        SystemVolumeController(appContext)
    }

    val playbackManager: PlaybackConnectionManager by lazy {
        PlaybackConnectionManager(
            context = appContext,
            libraryRepository = libraryRepository,
            prefsRepository = preferencesRepository,
            audioEffectsController = audioEffectsController
        )
    }
}

class PlayMixApplication : Application(), ImageLoaderFactory {
    lateinit var container: PlayMixAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = PlayMixAppContainer(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("playmix_artwork_cache"))
                    .maxSizeBytes(64L * 1024L * 1024L) // 64 MB cap for lightweight storage footprint
                    .build()
            }
            .crossfade(150)
            .respectCacheHeaders(false)
            .build()
    }
}
