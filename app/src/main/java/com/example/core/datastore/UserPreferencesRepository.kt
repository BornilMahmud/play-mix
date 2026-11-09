package com.example.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AccentColorOption
import com.example.domain.model.AppLanguage
import com.example.domain.model.CustomEqualizerPreset
import com.example.domain.model.EqualizerPreset
import com.example.domain.model.RepeatModeState
import com.example.domain.model.ReplayGainMode
import com.example.domain.model.SortOption
import com.example.ui.theme.PlayMixThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.playMixDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "play_mix_preferences"
)

data class UserPreferences(
    val isLoadedFromDisk: Boolean = false,
    val themeMode: PlayMixThemeMode = PlayMixThemeMode.AMOLED_BLACK,
    val dynamicColor: Boolean = false,
    val accentColor: AccentColorOption = AccentColorOption.ELECTRIC_VIOLET,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val onboardingCompleted: Boolean = false,
    val showThreeJsIntro: Boolean = true,
    val hasCompletedThreeJsIntroOnce: Boolean = false,
    val lastAppOpenTimestampMs: Long = 0L,
    val sortOption: SortOption = SortOption.TITLE_ASC,
    val groupByAlbumArtist: Boolean = true,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatModeState = RepeatModeState.OFF,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val gaplessPlayback: Boolean = true,
    val crossfadeDurationSec: Int = 0,
    val fadeInOutEnabled: Boolean = true,
    val skipIntervalSec: Int = 10,
    val audioNormalization: Boolean = false,
    val replayGainMode: ReplayGainMode = ReplayGainMode.OFF,
    val preampDb: Float = 0.0f,
    val leftRightBalance: Float = 0.0f,
    val monoAudioEnabled: Boolean = false,
    val clippingPreventionEnabled: Boolean = true,
    val resumeLastSession: Boolean = true,
    val autoRescanOnStartup: Boolean = true,
    val animatedVinylMode: Boolean = false,
    val swipeArtworkChangeTrack: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val showHomeSmartMixes: Boolean = true,
    val showHomeAlbums: Boolean = true,
    val showHomeArtists: Boolean = true,
    val equalizerEnabled: Boolean = false,
    val equalizerPreset: EqualizerPreset = EqualizerPreset.FLAT,
    val bandLevelsMb: List<Int> = listOf(0, 0, 0, 0, 0),
    val customEqPresetsRaw: String = "",
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val lastSongId: String = "",
    val lastPositionMs: Long = 0L,
    val minScanDurationSec: Int = 10,
    val includeDemoTracks: Boolean = true,
    val jamendoClientId: String = "",
    val spotifyClientId: String = "",
    val soundCloudClientId: String = "",
    val enabledOnlineProvidersCsv: String = "ITUNES_AUTHORIZED,INTERNET_ARCHIVE,JAMENDO_CC"
) {
    val customEqPresets: List<CustomEqualizerPreset>
        get() = customEqPresetsRaw.split(";")
            .mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val levels = parts[1].split(",").mapNotNull { it.toIntOrNull() }
                    if (levels.size == 5) CustomEqualizerPreset(parts[0], levels) else null
                } else null
            }

    /**
     * Evaluates the strict non-skippable intro playback rules:
     * 1. First-time install: Plays 1 time when the application is installed and opened for the first time
     *    (`!hasCompletedThreeJsIntroOnce`).
     * 2. 7-day straight inactivity: Plays 1 time if the application has NOT been opened for 7 consecutive
     *    days (`nowEpochMs - lastAppOpenTimestampMs >= SEVEN_DAYS_INACTIVITY_MS`).
     * Otherwise returns `false` (must wait 7 straight days without opening the app or reinstall to view again).
     */
    fun shouldPlayIntroAtLaunch(nowEpochMs: Long): Boolean {
        if (!hasCompletedThreeJsIntroOnce) {
            return true
        }
        if (lastAppOpenTimestampMs <= 0L) {
            return false
        }
        val inactiveDurationMs = nowEpochMs - lastAppOpenTimestampMs
        return inactiveDurationMs >= SEVEN_DAYS_INACTIVITY_MS
    }

    companion object {
        const val SEVEN_DAYS_INACTIVITY_MS: Long = 7L * 24L * 60L * 60L * 1000L // 604,800,000 ms (7 straight days)
    }
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val SHOW_THREE_JS_INTRO = booleanPreferencesKey("show_three_js_intro")
        val HAS_COMPLETED_THREE_JS_INTRO_ONCE = booleanPreferencesKey("has_completed_three_js_intro_once")
        val LAST_APP_OPEN_TIMESTAMP_MS = longPreferencesKey("last_app_open_timestamp_ms")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val GROUP_BY_ALBUM_ARTIST = booleanPreferencesKey("group_by_album_artist")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val PLAYBACK_PITCH = floatPreferencesKey("playback_pitch")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val CROSSFADE_SEC = intPreferencesKey("crossfade_sec")
        val FADE_IN_OUT = booleanPreferencesKey("fade_in_out")
        val SKIP_INTERVAL_SEC = intPreferencesKey("skip_interval_sec")
        val AUDIO_NORMALIZATION = booleanPreferencesKey("audio_normalization")
        val REPLAY_GAIN_MODE = stringPreferencesKey("replay_gain_mode")
        val PREAMP_DB = floatPreferencesKey("preamp_db")
        val LR_BALANCE = floatPreferencesKey("lr_balance")
        val MONO_AUDIO = booleanPreferencesKey("mono_audio")
        val CLIPPING_PREVENTION = booleanPreferencesKey("clipping_prevention")
        val RESUME_LAST_SESSION = booleanPreferencesKey("resume_last_session")
        val AUTO_RESCAN_STARTUP = booleanPreferencesKey("auto_rescan_startup")
        val ANIMATED_VINYL_MODE = booleanPreferencesKey("animated_vinyl_mode")
        val SWIPE_ARTWORK_TRACK = booleanPreferencesKey("swipe_artwork_track")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val SHOW_HOME_SMART = booleanPreferencesKey("show_home_smart")
        val SHOW_HOME_ALBUMS = booleanPreferencesKey("show_home_albums")
        val SHOW_HOME_ARTISTS = booleanPreferencesKey("show_home_artists")
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET = stringPreferencesKey("eq_preset")
        val EQ_BAND_LEVELS = stringPreferencesKey("eq_band_levels")
        val CUSTOM_EQ_PRESETS = stringPreferencesKey("custom_eq_presets")
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
        val LAST_SONG_ID = stringPreferencesKey("last_song_id")
        val LAST_POSITION_MS = longPreferencesKey("last_position_ms")
        val MIN_SCAN_DURATION_SEC = intPreferencesKey("min_scan_duration_sec")
        val INCLUDE_DEMO_TRACKS = booleanPreferencesKey("include_demo_tracks")
        val JAMENDO_CLIENT_ID = stringPreferencesKey("jamendo_client_id")
        val SPOTIFY_CLIENT_ID = stringPreferencesKey("spotify_client_id")
        val SOUNDCLOUD_CLIENT_ID = stringPreferencesKey("soundcloud_client_id")
        val ENABLED_ONLINE_PROVIDERS = stringPreferencesKey("enabled_online_providers")
    }

    val preferencesFlow: Flow<UserPreferences> = context.playMixDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val themeMode = runCatching {
                PlayMixThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: PlayMixThemeMode.AMOLED_BLACK.name)
            }.getOrDefault(PlayMixThemeMode.AMOLED_BLACK)

            val accent = runCatching {
                AccentColorOption.valueOf(prefs[Keys.ACCENT_COLOR] ?: AccentColorOption.ELECTRIC_VIOLET.name)
            }.getOrDefault(AccentColorOption.ELECTRIC_VIOLET)

            val lang = runCatching {
                AppLanguage.valueOf(prefs[Keys.APP_LANGUAGE] ?: AppLanguage.ENGLISH.name)
            }.getOrDefault(AppLanguage.ENGLISH)

            val sortOption = runCatching {
                SortOption.valueOf(prefs[Keys.SORT_OPTION] ?: SortOption.TITLE_ASC.name)
            }.getOrDefault(SortOption.TITLE_ASC)

            val repeatMode = runCatching {
                RepeatModeState.valueOf(prefs[Keys.REPEAT_MODE] ?: RepeatModeState.OFF.name)
            }.getOrDefault(RepeatModeState.OFF)

            val replayGain = runCatching {
                ReplayGainMode.valueOf(prefs[Keys.REPLAY_GAIN_MODE] ?: ReplayGainMode.OFF.name)
            }.getOrDefault(ReplayGainMode.OFF)

            val eqPreset = runCatching {
                EqualizerPreset.valueOf(prefs[Keys.EQ_PRESET] ?: EqualizerPreset.FLAT.name)
            }.getOrDefault(EqualizerPreset.FLAT)

            val bandLevels = prefs[Keys.EQ_BAND_LEVELS]
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.takeIf { it.size == 5 }
                ?: listOf(0, 0, 0, 0, 0)

            UserPreferences(
                isLoadedFromDisk = true,
                themeMode = themeMode,
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
                accentColor = accent,
                appLanguage = lang,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                showThreeJsIntro = prefs[Keys.SHOW_THREE_JS_INTRO] ?: true,
                hasCompletedThreeJsIntroOnce = prefs[Keys.HAS_COMPLETED_THREE_JS_INTRO_ONCE] ?: false,
                lastAppOpenTimestampMs = prefs[Keys.LAST_APP_OPEN_TIMESTAMP_MS] ?: 0L,
                sortOption = sortOption,
                groupByAlbumArtist = prefs[Keys.GROUP_BY_ALBUM_ARTIST] ?: true,
                shuffleEnabled = prefs[Keys.SHUFFLE_ENABLED] ?: false,
                repeatMode = repeatMode,
                playbackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
                playbackPitch = prefs[Keys.PLAYBACK_PITCH] ?: 1.0f,
                gaplessPlayback = prefs[Keys.GAPLESS_PLAYBACK] ?: true,
                crossfadeDurationSec = prefs[Keys.CROSSFADE_SEC] ?: 0,
                fadeInOutEnabled = prefs[Keys.FADE_IN_OUT] ?: true,
                skipIntervalSec = prefs[Keys.SKIP_INTERVAL_SEC] ?: 10,
                audioNormalization = prefs[Keys.AUDIO_NORMALIZATION] ?: false,
                replayGainMode = replayGain,
                preampDb = prefs[Keys.PREAMP_DB] ?: 0.0f,
                leftRightBalance = prefs[Keys.LR_BALANCE] ?: 0.0f,
                monoAudioEnabled = prefs[Keys.MONO_AUDIO] ?: false,
                clippingPreventionEnabled = prefs[Keys.CLIPPING_PREVENTION] ?: true,
                resumeLastSession = prefs[Keys.RESUME_LAST_SESSION] ?: true,
                autoRescanOnStartup = prefs[Keys.AUTO_RESCAN_STARTUP] ?: true,
                animatedVinylMode = prefs[Keys.ANIMATED_VINYL_MODE] ?: false,
                swipeArtworkChangeTrack = prefs[Keys.SWIPE_ARTWORK_TRACK] ?: true,
                hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
                showHomeSmartMixes = prefs[Keys.SHOW_HOME_SMART] ?: true,
                showHomeAlbums = prefs[Keys.SHOW_HOME_ALBUMS] ?: true,
                showHomeArtists = prefs[Keys.SHOW_HOME_ARTISTS] ?: true,
                equalizerEnabled = prefs[Keys.EQ_ENABLED] ?: false,
                equalizerPreset = eqPreset,
                bandLevelsMb = bandLevels,
                customEqPresetsRaw = prefs[Keys.CUSTOM_EQ_PRESETS] ?: "",
                bassBoostStrength = prefs[Keys.BASS_BOOST] ?: 0,
                virtualizerStrength = prefs[Keys.VIRTUALIZER] ?: 0,
                lastSongId = prefs[Keys.LAST_SONG_ID] ?: "",
                lastPositionMs = prefs[Keys.LAST_POSITION_MS] ?: 0L,
                minScanDurationSec = prefs[Keys.MIN_SCAN_DURATION_SEC] ?: 10,
                includeDemoTracks = prefs[Keys.INCLUDE_DEMO_TRACKS] ?: true,
                jamendoClientId = prefs[Keys.JAMENDO_CLIENT_ID] ?: "",
                spotifyClientId = prefs[Keys.SPOTIFY_CLIENT_ID] ?: "",
                soundCloudClientId = prefs[Keys.SOUNDCLOUD_CLIENT_ID] ?: "",
                enabledOnlineProvidersCsv = prefs[Keys.ENABLED_ONLINE_PROVIDERS]
                    ?: "ITUNES_AUTHORIZED,INTERNET_ARCHIVE,JAMENDO_CC"
            )
        }

    suspend fun getSnapshot(): UserPreferences = preferencesFlow.first()

    suspend fun setThemeMode(mode: PlayMixThemeMode) {
        context.playMixDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.playMixDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setAccentColor(accent: AccentColorOption) {
        context.playMixDataStore.edit { it[Keys.ACCENT_COLOR] = accent.name }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.playMixDataStore.edit { it[Keys.APP_LANGUAGE] = language.name }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.playMixDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setShowThreeJsIntro(enabled: Boolean) {
        context.playMixDataStore.edit { it[Keys.SHOW_THREE_JS_INTRO] = enabled }
    }

    suspend fun markThreeJsIntroCompletedOnce(completedAtEpochMs: Long = System.currentTimeMillis()) {
        context.playMixDataStore.edit {
            it[Keys.HAS_COMPLETED_THREE_JS_INTRO_ONCE] = true
            it[Keys.LAST_APP_OPEN_TIMESTAMP_MS] = completedAtEpochMs
        }
    }

    suspend fun recordAppOpenTimestamp(openedAtEpochMs: Long = System.currentTimeMillis()) {
        context.playMixDataStore.edit {
            it[Keys.LAST_APP_OPEN_TIMESTAMP_MS] = openedAtEpochMs
        }
    }

    /**
     * Evaluates whether the non-skippable intro video should play at launch (`nowEpochMs`):
     * - Returns `true` on first install (`!hasCompletedThreeJsIntroOnce`) or if `nowEpochMs - lastAppOpenTimestampMs >= 7 days`.
     * - If `false`, immediately updates `lastAppOpenTimestampMs = nowEpochMs` so the 7-day straight inactivity window resets.
     */
    suspend fun evaluateIntroAndRecordAppLaunch(nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        val snapshot = getSnapshot()
        val shouldPlay = snapshot.shouldPlayIntroAtLaunch(nowEpochMs)
        if (!shouldPlay) {
            recordAppOpenTimestamp(nowEpochMs)
        }
        return shouldPlay
    }

    suspend fun setSortOption(option: SortOption) {
        context.playMixDataStore.edit { it[Keys.SORT_OPTION] = option.name }
    }

    suspend fun setGroupByAlbumArtist(enabled: Boolean) {
        context.playMixDataStore.edit { it[Keys.GROUP_BY_ALBUM_ARTIST] = enabled }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.playMixDataStore.edit { it[Keys.SHUFFLE_ENABLED] = enabled }
    }

    suspend fun setRepeatMode(mode: RepeatModeState) {
        context.playMixDataStore.edit { it[Keys.REPEAT_MODE] = mode.name }
    }

    suspend fun setPlaybackSpeedAndPitch(speed: Float, pitch: Float) {
        context.playMixDataStore.edit {
            it[Keys.PLAYBACK_SPEED] = speed.coerceIn(0.5f, 2.0f)
            it[Keys.PLAYBACK_PITCH] = pitch.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.playMixDataStore.edit { it[Keys.GAPLESS_PLAYBACK] = enabled }
    }

    suspend fun setCrossfadeDurationSec(seconds: Int) {
        context.playMixDataStore.edit { it[Keys.CROSSFADE_SEC] = seconds.coerceIn(0, 12) }
    }

    suspend fun setFadeInOutEnabled(enabled: Boolean) {
        context.playMixDataStore.edit { it[Keys.FADE_IN_OUT] = enabled }
    }

    suspend fun setSkipIntervalSec(seconds: Int) {
        context.playMixDataStore.edit { it[Keys.SKIP_INTERVAL_SEC] = seconds.coerceIn(5, 60) }
    }

    suspend fun setAudioNormalization(enabled: Boolean) {
        context.playMixDataStore.edit { it[Keys.AUDIO_NORMALIZATION] = enabled }
    }

    suspend fun setReplayGainMode(mode: ReplayGainMode) {
        context.playMixDataStore.edit { it[Keys.REPLAY_GAIN_MODE] = mode.name }
    }

    suspend fun setPreampAndBalance(preampDb: Float, balance: Float, mono: Boolean, clipPrevention: Boolean) {
        context.playMixDataStore.edit {
            it[Keys.PREAMP_DB] = preampDb.coerceIn(-12f, 12f)
            it[Keys.LR_BALANCE] = balance.coerceIn(-1f, 1f)
            it[Keys.MONO_AUDIO] = mono
            it[Keys.CLIPPING_PREVENTION] = clipPrevention
        }
    }

    suspend fun setResumeLastSession(enabled: Boolean) {
        context.playMixDataStore.edit { it[Keys.RESUME_LAST_SESSION] = enabled }
    }

    suspend fun setAutoRescanOnStartup(enabled: Boolean) {
        context.playMixDataStore.edit { it[Keys.AUTO_RESCAN_STARTUP] = enabled }
    }

    suspend fun setAnimatedVinylMode(enabled: Boolean) {
        context.playMixDataStore.edit { it[Keys.ANIMATED_VINYL_MODE] = enabled }
    }

    suspend fun setSwipeArtworkChangeTrack(enabled: Boolean) {
        context.playMixDataStore.edit { it[Keys.SWIPE_ARTWORK_TRACK] = enabled }
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.playMixDataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setHomeSectionVisibility(smartMixes: Boolean, albums: Boolean, artists: Boolean) {
        context.playMixDataStore.edit {
            it[Keys.SHOW_HOME_SMART] = smartMixes
            it[Keys.SHOW_HOME_ALBUMS] = albums
            it[Keys.SHOW_HOME_ARTISTS] = artists
        }
    }

    suspend fun setEqualizerState(
        enabled: Boolean,
        preset: EqualizerPreset,
        bandLevelsMb: List<Int>
    ) {
        context.playMixDataStore.edit {
            it[Keys.EQ_ENABLED] = enabled
            it[Keys.EQ_PRESET] = preset.name
            it[Keys.EQ_BAND_LEVELS] = bandLevelsMb.joinToString(",")
        }
    }

    suspend fun saveCustomEqPreset(name: String, bandLevelsMb: List<Int>) {
        val cleanName = name.replace(":", "").replace(";", "").trim().ifBlank { "My Preset" }
        val current = getSnapshot().customEqPresets.filterNot { it.name.equals(cleanName, ignoreCase = true) }
        val updated = current + CustomEqualizerPreset(cleanName, bandLevelsMb)
        val encoded = updated.joinToString(";") { "${it.name}:${it.bandLevelsMb.joinToString(",")}" }
        context.playMixDataStore.edit {
            it[Keys.CUSTOM_EQ_PRESETS] = encoded
        }
    }

    suspend fun deleteCustomEqPreset(name: String) {
        val updated = getSnapshot().customEqPresets.filterNot { it.name.equals(name, ignoreCase = true) }
        val encoded = updated.joinToString(";") { "${it.name}:${it.bandLevelsMb.joinToString(",")}" }
        context.playMixDataStore.edit {
            it[Keys.CUSTOM_EQ_PRESETS] = encoded
        }
    }

    suspend fun setBassBoostAndVirtualizer(bassStrength: Int, virtualizerStrength: Int) {
        context.playMixDataStore.edit {
            it[Keys.BASS_BOOST] = bassStrength.coerceIn(0, 1000)
            it[Keys.VIRTUALIZER] = virtualizerStrength.coerceIn(0, 1000)
        }
    }

    suspend fun saveLastPlaybackSession(songId: String, positionMs: Long) {
        context.playMixDataStore.edit {
            it[Keys.LAST_SONG_ID] = songId
            it[Keys.LAST_POSITION_MS] = positionMs.coerceAtLeast(0L)
        }
    }

    suspend fun setMinScanDurationSec(seconds: Int) {
        context.playMixDataStore.edit { it[Keys.MIN_SCAN_DURATION_SEC] = seconds.coerceIn(0, 120) }
    }

    suspend fun setIncludeDemoTracks(include: Boolean) {
        context.playMixDataStore.edit { it[Keys.INCLUDE_DEMO_TRACKS] = include }
    }

    suspend fun setOnlineProviderCredentials(
        jamendoClientId: String,
        spotifyClientId: String,
        soundCloudClientId: String,
        enabledProvidersCsv: String
    ) {
        context.playMixDataStore.edit {
            it[Keys.JAMENDO_CLIENT_ID] = jamendoClientId.trim()
            it[Keys.SPOTIFY_CLIENT_ID] = spotifyClientId.trim()
            it[Keys.SOUNDCLOUD_CLIENT_ID] = soundCloudClientId.trim()
            it[Keys.ENABLED_ONLINE_PROVIDERS] = enabledProvidersCsv
        }
    }

    suspend fun restoreFromBackupMap(values: Map<String, String>) {
        context.playMixDataStore.edit { prefs ->
            values["theme_mode"]?.let { prefs[Keys.THEME_MODE] = it }
            values["accent_color"]?.let { prefs[Keys.ACCENT_COLOR] = it }
            values["app_language"]?.let { prefs[Keys.APP_LANGUAGE] = it }
            values["sort_option"]?.let { prefs[Keys.SORT_OPTION] = it }
            values["eq_preset"]?.let { prefs[Keys.EQ_PRESET] = it }
            values["eq_band_levels"]?.let { prefs[Keys.EQ_BAND_LEVELS] = it }
            values["custom_eq_presets"]?.let { prefs[Keys.CUSTOM_EQ_PRESETS] = it }
            values["crossfade_sec"]?.toIntOrNull()?.let { prefs[Keys.CROSSFADE_SEC] = it }
            values["skip_interval_sec"]?.toIntOrNull()?.let { prefs[Keys.SKIP_INTERVAL_SEC] = it }
            values["replay_gain_mode"]?.let { prefs[Keys.REPLAY_GAIN_MODE] = it }
        }
    }
}
