package com.example.playback.controller

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import com.example.core.datastore.UserPreferencesRepository
import com.example.domain.model.AudioEffectsState
import com.example.domain.model.CustomEqualizerPreset
import com.example.domain.model.EqualizerBandInfo
import com.example.domain.model.EqualizerPreset
import com.example.domain.model.ReplayGainMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class AudioEffectsController(
    context: Context,
    private val prefsRepo: UserPreferencesRepository
) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var boundSessionId: Int = 0
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _state = MutableStateFlow(AudioEffectsState())
    val state: StateFlow<AudioEffectsState> = _state.asStateFlow()

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshOutputDeviceInfo()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshOutputDeviceInfo()
        }
    }

    init {
        runCatching { audioManager.registerAudioDeviceCallback(deviceCallback, null) }
        detectHardwareEffectDescriptors()
        refreshOutputDeviceInfo()
        scope.launch {
            prefsRepo.preferencesFlow.collectLatest { prefs ->
                _state.update { current ->
                    current.copy(
                        isEnabled = prefs.equalizerEnabled,
                        activePreset = prefs.equalizerPreset,
                        customPresets = prefs.customEqPresets,
                        preampDb = prefs.preampDb,
                        leftRightBalance = prefs.leftRightBalance,
                        monoAudioEnabled = prefs.monoAudioEnabled,
                        replayGainMode = prefs.replayGainMode,
                        clippingPreventionEnabled = prefs.clippingPreventionEnabled,
                        bassBoostStrength = prefs.bassBoostStrength,
                        virtualizerStrength = prefs.virtualizerStrength,
                        loudnessNormalizationEnabled = prefs.audioNormalization,
                        bands = current.bands.mapIndexed { idx, band ->
                            band.copy(currentLevelMb = prefs.bandLevelsMb.getOrElse(idx) { 0 })
                        }
                    )
                }
            }
        }
    }

    private fun detectHardwareEffectDescriptors() {
        runCatching {
            val descriptors = AudioEffect.queryEffects().orEmpty()
            val hasEq = descriptors.any { it.type == AudioEffect.EFFECT_TYPE_EQUALIZER }
            val hasBb = descriptors.any { it.type == AudioEffect.EFFECT_TYPE_BASS_BOOST }
            val hasVirt = descriptors.any { it.type == AudioEffect.EFFECT_TYPE_VIRTUALIZER }
            _state.update {
                it.copy(
                    isSupported = hasEq || true,
                    bassBoostSupported = hasBb || true,
                    virtualizerSupported = hasVirt || true
                )
            }
        }
    }

    fun bindToAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == boundSessionId) {
            refreshOutputDeviceInfo()
            return
        }
        releaseHardwareEffects()
        boundSessionId = audioSessionId

        val currentState = _state.value
        var hardwareBands: List<EqualizerBandInfo> = currentState.bands

        runCatching {
            val eq = Equalizer(0, audioSessionId)
            eq.enabled = currentState.isEnabled
            val numBands = eq.numberOfBands.toInt().coerceAtMost(5)
            val range = eq.bandLevelRange
            val minMb = range.getOrElse(0) { (-1500).toShort() }.toInt()
            val maxMb = range.getOrElse(1) { 1500.toShort() }.toInt()

            val mapped = (0 until numBands).map { i ->
                val bandShort = i.toShort()
                val freqMilliHz = eq.getCenterFreq(bandShort)
                val freqHz = (freqMilliHz / 1000).coerceAtLeast(30)
                val desiredMb = currentState.bands.getOrNull(i)?.currentLevelMb?.coerceIn(minMb, maxMb) ?: 0
                if (currentState.isEnabled) {
                    eq.setBandLevel(bandShort, desiredMb.toShort())
                }
                EqualizerBandInfo(
                    index = i,
                    centerFreqHz = freqHz,
                    currentLevelMb = desiredMb,
                    minLevelMb = minMb,
                    maxLevelMb = maxMb
                )
            }
            if (mapped.isNotEmpty()) {
                hardwareBands = mapped
            }
            equalizer = eq
        }

        runCatching {
            val bb = BassBoost(0, audioSessionId)
            if (bb.strengthSupported) {
                bb.enabled = currentState.isEnabled && currentState.bassBoostStrength > 0
                bb.setStrength(currentState.bassBoostStrength.coerceIn(0, 1000).toShort())
            }
            bassBoost = bb
        }

        runCatching {
            val virt = Virtualizer(0, audioSessionId)
            if (virt.strengthSupported) {
                virt.enabled = currentState.isEnabled && currentState.virtualizerStrength > 0
                virt.setStrength(currentState.virtualizerStrength.coerceIn(0, 1000).toShort())
            }
            virtualizer = virt
        }

        runCatching {
            val enhancer = LoudnessEnhancer(audioSessionId)
            enhancer.enabled = currentState.loudnessNormalizationEnabled || currentState.preampDb != 0f
            val preampMb = (currentState.preampDb * 100f).toInt()
            val normMb = if (currentState.loudnessNormalizationEnabled) 450 else 0
            val totalGainMb = if (currentState.clippingPreventionEnabled) {
                (preampMb + normMb).coerceIn(-1200, 600)
            } else {
                (preampMb + normMb).coerceIn(-1200, 1200)
            }
            enhancer.setTargetGain(totalGainMb)
            loudnessEnhancer = enhancer
        }

        _state.update {
            it.copy(
                bands = hardwareBands,
                audioSessionId = audioSessionId
            )
        }
        refreshOutputDeviceInfo()
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        runCatching { equalizer?.enabled = enabled }
        runCatching {
            bassBoost?.enabled = enabled && _state.value.bassBoostStrength > 0
            virtualizer?.enabled = enabled && _state.value.virtualizerStrength > 0
        }
        _state.update { it.copy(isEnabled = enabled) }
        persistCurrentEqualizerState()
    }

    fun selectPreset(preset: EqualizerPreset) {
        val updatedBands = _state.value.bands.mapIndexed { idx, band ->
            val gainNorm = preset.bandGainsPercent.getOrElse(idx) { 0f }
            val levelMb = (gainNorm * band.maxLevelMb).roundToInt().coerceIn(band.minLevelMb, band.maxLevelMb)
            runCatching {
                if (_state.value.isEnabled) {
                    equalizer?.setBandLevel(idx.toShort(), levelMb.toShort())
                }
            }
            band.copy(currentLevelMb = levelMb)
        }
        _state.update {
            it.copy(
                isEnabled = true,
                activePreset = preset,
                activeCustomPresetName = null,
                bands = updatedBands
            )
        }
        runCatching { equalizer?.enabled = true }
        persistCurrentEqualizerState()
    }

    fun applyCustomPreset(customPreset: CustomEqualizerPreset) {
        val updatedBands = _state.value.bands.mapIndexed { idx, band ->
            val levelMb = customPreset.bandLevelsMb.getOrElse(idx) { 0 }.coerceIn(band.minLevelMb, band.maxLevelMb)
            runCatching {
                equalizer?.enabled = true
                equalizer?.setBandLevel(idx.toShort(), levelMb.toShort())
            }
            band.copy(currentLevelMb = levelMb)
        }
        _state.update {
            it.copy(
                isEnabled = true,
                activePreset = EqualizerPreset.CUSTOM,
                activeCustomPresetName = customPreset.name,
                bands = updatedBands
            )
        }
        persistCurrentEqualizerState()
    }

    fun saveCurrentAsCustomPreset(name: String) {
        val levels = _state.value.bands.map { it.currentLevelMb }
        scope.launch {
            prefsRepo.saveCustomEqPreset(name, levels)
        }
    }

    fun deleteCustomPreset(name: String) {
        scope.launch {
            prefsRepo.deleteCustomEqPreset(name)
        }
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val currentBands = _state.value.bands.toMutableList()
        if (bandIndex !in currentBands.indices) return
        val band = currentBands[bandIndex]
        val clamped = levelMb.coerceIn(band.minLevelMb, band.maxLevelMb)
        currentBands[bandIndex] = band.copy(currentLevelMb = clamped)

        runCatching {
            equalizer?.enabled = true
            equalizer?.setBandLevel(bandIndex.toShort(), clamped.toShort())
        }

        _state.update {
            it.copy(
                isEnabled = true,
                activePreset = EqualizerPreset.CUSTOM,
                activeCustomPresetName = null,
                bands = currentBands
            )
        }
        persistCurrentEqualizerState()
    }

    fun setPreampAndSpatialBalance(
        preampDb: Float = _state.value.preampDb,
        leftRightBalance: Float = _state.value.leftRightBalance,
        monoAudio: Boolean = _state.value.monoAudioEnabled,
        clippingPrevention: Boolean = _state.value.clippingPreventionEnabled
    ) {
        val clampedPreamp = preampDb.coerceIn(-12f, 12f)
        val clampedBalance = leftRightBalance.coerceIn(-1f, 1f)
        runCatching {
            val enhancer = loudnessEnhancer
            if (enhancer != null) {
                enhancer.enabled = _state.value.loudnessNormalizationEnabled || clampedPreamp != 0f
                val preampMb = (clampedPreamp * 100f).toInt()
                val normMb = if (_state.value.loudnessNormalizationEnabled) 450 else 0
                val targetMb = if (clippingPrevention) {
                    (preampMb + normMb).coerceIn(-1200, 600)
                } else {
                    (preampMb + normMb).coerceIn(-1200, 1200)
                }
                enhancer.setTargetGain(targetMb)
            }
        }
        _state.update {
            it.copy(
                preampDb = clampedPreamp,
                leftRightBalance = clampedBalance,
                monoAudioEnabled = monoAudio,
                clippingPreventionEnabled = clippingPrevention
            )
        }
        scope.launch {
            prefsRepo.setPreampAndBalance(clampedPreamp, clampedBalance, monoAudio, clippingPrevention)
        }
    }

    fun setReplayGainMode(mode: ReplayGainMode) {
        _state.update { it.copy(replayGainMode = mode) }
        scope.launch { prefsRepo.setReplayGainMode(mode) }
    }

    fun setBassBoostStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            bassBoost?.enabled = _state.value.isEnabled && clamped > 0
            bassBoost?.setStrength(clamped.toShort())
        }
        _state.update { it.copy(bassBoostStrength = clamped) }
        scope.launch {
            prefsRepo.setBassBoostAndVirtualizer(clamped, _state.value.virtualizerStrength)
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        runCatching {
            virtualizer?.enabled = _state.value.isEnabled && clamped > 0
            virtualizer?.setStrength(clamped.toShort())
        }
        _state.update { it.copy(virtualizerStrength = clamped) }
        scope.launch {
            prefsRepo.setBassBoostAndVirtualizer(_state.value.bassBoostStrength, clamped)
        }
    }

    fun setLoudnessNormalization(enabled: Boolean, gainMb: Int = 450) {
        val clampedGain = if (enabled) gainMb.coerceIn(0, 1000) else 0
        runCatching {
            loudnessEnhancer?.enabled = enabled || _state.value.preampDb != 0f
            loudnessEnhancer?.setTargetGain(clampedGain + (_state.value.preampDb * 100f).toInt())
        }
        _state.update {
            it.copy(
                loudnessNormalizationEnabled = enabled,
                loudnessGainMb = clampedGain
            )
        }
        scope.launch {
            prefsRepo.setAudioNormalization(enabled)
        }
    }

    fun resetToDefault() {
        selectPreset(EqualizerPreset.FLAT)
        setBassBoostStrength(0)
        setVirtualizerStrength(0)
        setLoudnessNormalization(false)
        setPreampAndSpatialBalance(0f, 0f, monoAudio = false, clippingPrevention = true)
        setReplayGainMode(ReplayGainMode.OFF)
    }

    fun refreshOutputDeviceInfo() {
        runCatching {
            val sampleRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)
                ?.toIntOrNull() ?: 48000
            val bufferFrames = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)
                ?.toIntOrNull() ?: 256

            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val external = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_USB_DEVICE
            } ?: devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

            val (name, typeLabel) = when (external?.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                    val prod = external.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth Audio"
                    prod to "Bluetooth A2DP / AVRCP (${sampleRate}Hz)"
                }
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> {
                    "Wired Headphones" to "3.5mm / Analog Headset (${sampleRate}Hz)"
                }
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE -> {
                    val prod = external.productName?.toString()?.takeIf { it.isNotBlank() } ?: "USB DAC"
                    prod to "Hi-Res USB Audio (${sampleRate}Hz)"
                }
                else -> "Realme / Device Stereo Speaker" to "Internal Audio Output (${sampleRate}Hz • ${bufferFrames}f)"
            }
            _state.update {
                it.copy(
                    audioOutputDeviceName = name,
                    audioOutputType = typeLabel,
                    hardwareSampleRateHz = sampleRate,
                    hardwareBufferFrames = bufferFrames
                )
            }
        }
    }

    private fun persistCurrentEqualizerState() {
        val snapshot = _state.value
        scope.launch {
            prefsRepo.setEqualizerState(
                enabled = snapshot.isEnabled,
                preset = snapshot.activePreset,
                bandLevelsMb = snapshot.bands.map { it.currentLevelMb }
            )
        }
    }

    fun releaseHardwareEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        boundSessionId = 0
    }
}
