package com.example.playback.controller

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SystemVolumeState(
    val currentVolume: Int = 8,
    val maxVolume: Int = 15,
    val isMuted: Boolean = false
) {
    val volumeFraction: Float
        get() = if (maxVolume > 0) (currentVolume.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f) else 0f

    val volumePercent: Int
        get() = (volumeFraction * 100).toInt()
}

class SystemVolumeController(context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var preMuteVolume: Int = 7

    private val _volumeState = MutableStateFlow(readSystemMusicVolume())
    val volumeState: StateFlow<SystemVolumeState> = _volumeState.asStateFlow()

    private val volumeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION" ||
                intent?.action == AudioManager.ACTION_HEADSET_PLUG ||
                intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY
            ) {
                refreshVolumeState()
            }
        }
    }

    init {
        runCatching {
            val filter = IntentFilter().apply {
                addAction("android.media.VOLUME_CHANGED_ACTION")
                addAction(AudioManager.ACTION_HEADSET_PLUG)
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.registerReceiver(volumeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                appContext.registerReceiver(volumeReceiver, filter)
            }
        }
    }

    fun refreshVolumeState() {
        _volumeState.value = readSystemMusicVolume()
    }

    fun setVolumeFraction(fraction: Float, showSystemUi: Boolean = false) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = (fraction.coerceIn(0f, 1f) * max).toInt()
        setStreamVolume(target, showSystemUi)
    }

    fun setStreamVolume(index: Int, showSystemUi: Boolean = false) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val clamped = index.coerceIn(0, max)
        if (clamped > 0) {
            preMuteVolume = clamped
        }
        val flags = if (showSystemUi) AudioManager.FLAG_SHOW_UI else 0
        runCatching {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, flags)
        }
        refreshVolumeState()
    }

    fun adjustVolumeStep(directionUp: Boolean, showSystemUi: Boolean = true) {
        val dir = if (directionUp) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        val flags = if (showSystemUi) AudioManager.FLAG_SHOW_UI else 0
        runCatching {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, dir, flags)
        }
        refreshVolumeState()
    }

    fun toggleMute() {
        val current = _volumeState.value
        if (current.isMuted || current.currentVolume == 0) {
            val restore = preMuteVolume.coerceAtLeast(1)
            setStreamVolume(restore, showSystemUi = false)
        } else {
            preMuteVolume = current.currentVolume
            setStreamVolume(0, showSystemUi = false)
        }
    }

    private fun readSystemMusicVolume(): SystemVolumeState {
        return runCatching {
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val muted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.isStreamMute(AudioManager.STREAM_MUSIC) || current == 0
            } else {
                current == 0
            }
            SystemVolumeState(
                currentVolume = current,
                maxVolume = max,
                isMuted = muted
            )
        }.getOrDefault(SystemVolumeState())
    }
}
