package com.example.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.database.SafFolderEntity
import com.example.core.datastore.UserPreferences
import com.example.core.ui.RestoreToCategoryDialog
import com.example.domain.model.AccentColorOption
import com.example.domain.model.AppLanguage
import com.example.domain.model.AudioCategory
import com.example.domain.model.Song
import com.example.feature.LibraryDiagnosticsReport
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.LocalPlayMixStrings
import com.example.ui.theme.PlayMixThemeMode
import com.example.ui.theme.SoftCyan
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    hiddenSongs: List<Song>,
    safFolders: List<SafFolderEntity>,
    diagnosticsReport: LibraryDiagnosticsReport,
    onSelectThemeMode: (PlayMixThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onSelectAccentColor: (AccentColorOption) -> Unit,
    onSelectAppLanguage: (AppLanguage) -> Unit,
    onToggleAnimatedVinyl: (Boolean) -> Unit,
    onToggleSwipeGestures: (Boolean) -> Unit,
    onToggleHapticFeedback: (Boolean) -> Unit,
    onToggleThreeJsIntro: (Boolean) -> Unit,
    onChangeHomeSections: (Boolean, Boolean, Boolean) -> Unit,
    onToggleGroupByAlbumArtist: (Boolean) -> Unit,
    onToggleAutoRescanOnStartup: (Boolean) -> Unit,
    onToggleGapless: (Boolean) -> Unit,
    onCrossfadeChange: (Int) -> Unit,
    onToggleFadeInOut: (Boolean) -> Unit,
    onSkipIntervalChange: (Int) -> Unit,
    onToggleAudioNormalization: (Boolean) -> Unit,
    onToggleResumeLastSession: (Boolean) -> Unit,
    onMinScanDurationChange: (Int) -> Unit,
    onToggleIncludeDemoTracks: (Boolean) -> Unit,
    onRescanLibrary: () -> Unit,
    onRunDiagnostics: () -> Unit,
    onCleanUpMissingTracks: () -> Unit,
    onExportFullBackup: suspend () -> String,
    onRestoreFullBackup: (String) -> Unit,
    onPickSafFolder: () -> Unit,
    onRemoveSafFolder: (String) -> Unit,
    onUnhideAllSongs: () -> Unit,
    onRestoreSongsToCategory: (Set<String>, AudioCategory?) -> Unit = { _, _ -> },
    onOpenEqualizerScreen: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalPlayMixStrings.current
    val scope = rememberCoroutineScope()

    var exportedBackupJson by remember { mutableStateOf<String?>(null) }
    var showRestoreBackupDialog by remember { mutableStateOf(false) }
    var showRestoreCategoryDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ElectricViolet.copy(alpha = 0.16f),
                            SoftCyan.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.05f),
                        radius = size.width * 0.85f
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.05f),
                    radius = size.width * 0.85f
                )
            }
            .padding(horizontal = 16.dp)
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ElectricViolet.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "ANDROID 15 • SYSTEM & ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SoftCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = strings.settingsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Customize appearance, localization, Three.js intro, DSP audio pipeline, non-destructive classification, backup/restore, and Realme UI background playback.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. APPEARANCE, ACCENT COLOR, UX GESTURES & LOCALIZATION
        item {
            SettingsSectionCard(
                title = "Appearance, Localization & Premium UX",
                icon = Icons.Default.Palette
            ) {
                Text(
                    text = "Color Posture",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftCyan
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = listOf(
                        PlayMixThemeMode.AMOLED_BLACK to "AMOLED Black",
                        PlayMixThemeMode.DARK_CHARCOAL to "Dark Charcoal",
                        PlayMixThemeMode.LIGHT to "Light",
                        PlayMixThemeMode.SYSTEM to "System"
                    )
                    modes.forEach { (mode, label) ->
                        FilterChip(
                            selected = preferences.themeMode == mode,
                            onClick = { onSelectThemeMode(mode) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("theme_chip_${mode.name.lowercase()}")
                        )
                    }
                }

                Text(
                    text = "Custom Accent Palette",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftCyan
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AccentColorOption.entries.forEach { accent ->
                        FilterChip(
                            selected = preferences.accentColor == accent,
                            onClick = { onSelectAccentColor(accent) },
                            label = { Text(accent.label) },
                            modifier = Modifier.testTag("accent_chip_${accent.name.lowercase()}")
                        )
                    }
                }

                Text(
                    text = "${strings.languageLabel} (English / বাংলা)",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftCyan
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = preferences.appLanguage == lang,
                            onClick = { onSelectAppLanguage(lang) },
                            leadingIcon = {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text(lang.displayName) },
                            modifier = Modifier.testTag("lang_chip_${lang.code}")
                        )
                    }
                }

                SettingsToggleRow(
                    title = "Material You Dynamic Accents",
                    subtitle = "Blend Android 12+ wallpaper accents with Play Mix surfaces",
                    checked = preferences.dynamicColor,
                    onCheckedChange = onToggleDynamicColor
                )

                SettingsToggleRow(
                    title = "Three.js 3D Intro Animation on Startup",
                    subtitle = "Show lightweight WebGL 3D audio sphere & sound-wave intro (auto-disposes after launch)",
                    checked = preferences.showThreeJsIntro,
                    onCheckedChange = onToggleThreeJsIntro
                )

                SettingsToggleRow(
                    title = "Animated Spinning Vinyl Player",
                    subtitle = "Show rotating turntable disc & tonearm on Now Playing screen",
                    checked = preferences.animatedVinylMode,
                    onCheckedChange = onToggleAnimatedVinyl
                )

                SettingsToggleRow(
                    title = "Swipe Artwork to Skip Tracks",
                    subtitle = "Swipe left/right on album cover or vinyl to skip Next/Previous",
                    checked = preferences.swipeArtworkChangeTrack,
                    onCheckedChange = onToggleSwipeGestures
                )

                SettingsToggleRow(
                    title = "Tactile Haptic Feedback",
                    subtitle = "Vibrate gently on swipe gestures and primary player actions",
                    checked = preferences.hapticFeedbackEnabled,
                    onCheckedChange = onToggleHapticFeedback
                )

                Text(
                    text = "Custom Home Screen Sections",
                    style = MaterialTheme.typography.labelLarge,
                    color = SoftCyan
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = preferences.showHomeSmartMixes,
                        onClick = {
                            onChangeHomeSections(
                                !preferences.showHomeSmartMixes,
                                preferences.showHomeAlbums,
                                preferences.showHomeArtists
                            )
                        },
                        label = { Text("Smart Mixes") }
                    )
                    FilterChip(
                        selected = preferences.showHomeAlbums,
                        onClick = {
                            onChangeHomeSections(
                                preferences.showHomeSmartMixes,
                                !preferences.showHomeAlbums,
                                preferences.showHomeArtists
                            )
                        },
                        label = { Text("Featured Albums") }
                    )
                    FilterChip(
                        selected = preferences.showHomeArtists,
                        onClick = {
                            onChangeHomeSections(
                                preferences.showHomeSmartMixes,
                                preferences.showHomeAlbums,
                                !preferences.showHomeArtists
                            )
                        },
                        label = { Text("Artists Carousel") }
                    )
                }
            }
        }

        // 2. AUDIO & PLAYBACK
        item {
            SettingsSectionCard(
                title = "Audio & Playback Pipeline",
                icon = Icons.Default.Equalizer
            ) {
                Button(
                    onClick = onOpenEqualizerScreen,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_open_eq_btn")
                ) {
                    Icon(Icons.Default.Equalizer, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open 5-Band Hardware Equalizer, Preamp & ReplayGain")
                }

                SettingsToggleRow(
                    title = "Gapless Playback",
                    subtitle = "Seamless zero-silence transitions between consecutive album tracks",
                    checked = preferences.gaplessPlayback,
                    onCheckedChange = onToggleGapless
                )

                SettingsToggleRow(
                    title = "Smooth Fade-In / Fade-Out",
                    subtitle = "Gradually ramp volume when pausing, resuming, or crossfading tracks",
                    checked = preferences.fadeInOutEnabled,
                    onCheckedChange = onToggleFadeInOut
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Crossfade Duration", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (preferences.crossfadeDurationSec == 0) "Off" else "${preferences.crossfadeDurationSec}s",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftCyan
                        )
                    }
                    Slider(
                        value = preferences.crossfadeDurationSec.toFloat(),
                        onValueChange = { onCrossfadeChange(it.toInt()) },
                        valueRange = 0f..12f,
                        steps = 11
                    )
                }

                Column {
                    Text("Fast-Forward / Rewind Interval", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 30).forEach { sec ->
                            FilterChip(
                                selected = preferences.skipIntervalSec == sec,
                                onClick = { onSkipIntervalChange(sec) },
                                label = { Text("${sec}s") }
                            )
                        }
                    }
                }

                SettingsToggleRow(
                    title = "Loudness Normalization",
                    subtitle = "Normalize track volume gain using Android LoudnessEnhancer",
                    checked = preferences.audioNormalization,
                    onCheckedChange = onToggleAudioNormalization
                )

                SettingsToggleRow(
                    title = "Resume Last Session",
                    subtitle = "Automatically restore queue and playback position after app restart",
                    checked = preferences.resumeLastSession,
                    onCheckedChange = onToggleResumeLastSession
                )
            }
        }

        // 3. LIBRARY, DIAGNOSTICS & SAF FOLDERS
        item {
            SettingsSectionCard(
                title = "Music Library, Classification & Diagnostics",
                icon = Icons.Default.CreateNewFolder
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRescanLibrary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_rescan_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rescan Library")
                    }
                    OutlinedButton(
                        onClick = onPickSafFolder,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_add_saf_btn")
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Folder")
                    }
                }

                SettingsToggleRow(
                    title = "Automatic Library Rescan on Startup",
                    subtitle = "Automatically index new local audio files when Play Mix launches (preserves manual categories & removals)",
                    checked = preferences.autoRescanOnStartup,
                    onCheckedChange = onToggleAutoRescanOnStartup
                )

                SettingsToggleRow(
                    title = "Group Albums by Album Artist & Compilations",
                    subtitle = "Unify multi-artist compilation albums under 'Various Artists' or Album Artist",
                    checked = preferences.groupByAlbumArtist,
                    onCheckedChange = onToggleGroupByAlbumArtist
                )

                // Duplicate & Missing File Diagnostics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRunDiagnostics,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_run_diagnostics_btn")
                    ) {
                        Icon(Icons.Default.FindReplace, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Duplicates & Missing")
                    }
                    if (diagnosticsReport.missingFileTracks.isNotEmpty()) {
                        Button(
                            onClick = onCleanUpMissingTracks,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clean ${diagnosticsReport.missingFileTracks.size} Missing")
                        }
                    }
                }

                if (diagnosticsReport.duplicateTracks.isNotEmpty() || diagnosticsReport.missingFileTracks.isNotEmpty() || diagnosticsReport.songsWithArtworkCount > 0) {
                    Text(
                        text = "Artwork: ${diagnosticsReport.songsWithArtworkCount} track(s) have artwork (${diagnosticsReport.artworkCoversCount} cached offline). Found ${diagnosticsReport.duplicateTracks.size} duplicate(s) and ${diagnosticsReport.missingFileTracks.size} missing file(s).",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftCyan
                    )
                }

                if (safFolders.isNotEmpty()) {
                    Text(
                        text = "Selected SAF Folders (${safFolders.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = SoftCyan
                    )
                    safFolders.forEach { saf ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = saf.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onRemoveSafFolder(saf.treeUri) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove SAF folder",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Skip Short Audio Clips Under", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${preferences.minScanDurationSec} sec",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftCyan
                        )
                    }
                    Slider(
                        value = preferences.minScanDurationSec.toFloat(),
                        onValueChange = { onMinScanDurationChange(it.toInt()) },
                        valueRange = 0f..60f
                    )
                }

                SettingsToggleRow(
                    title = "Include Studio Demo Tracks & Category Samples",
                    subtitle = "Keep built-in stereo demo tracks & separated category samples in library",
                    checked = preferences.includeDemoTracks,
                    onCheckedChange = onToggleIncludeDemoTracks
                )

                if (hiddenSongs.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onUnhideAllSongs,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_restore_all_hidden_btn")
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore All (${hiddenSongs.size})")
                        }
                        Button(
                            onClick = { showRestoreCategoryDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("settings_restore_to_category_btn")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore to Category...")
                        }
                    }
                }
            }
        }

        // 4. BACKUP & RESTORE
        item {
            SettingsSectionCard(
                title = strings.backupAndRestore,
                icon = Icons.Default.Backup
            ) {
                Text(
                    text = "Export or restore a complete JSON snapshot of your Custom Playlists, Favorites, Star Ratings, Custom Equalizer Presets, and App Preferences.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                exportedBackupJson = onExportFullBackup()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_export_backup_btn")
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Backup")
                    }
                    OutlinedButton(
                        onClick = { showRestoreBackupDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_restore_backup_btn")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Backup")
                    }
                }
            }
        }

        // 5. REALME GT MASTER EDITION COMPATIBILITY & BATTERY GUIDE
        item {
            SettingsSectionCard(
                title = "Realme GT Master Edition Background Guide",
                icon = Icons.Default.BatteryChargingFull
            ) {
                Text(
                    text = "Play Mix runs a dedicated Media3 Foreground Service (foregroundServiceType=\"mediaPlayback\") with local CPU wake lock so music continues seamlessly when you lock the screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "If Realme UI aggressive battery management restricts background apps after 30+ minutes of screen-off playback, enable these Realme UI settings:",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftCyan
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "1. Open App Info → Battery Usage → Enable 'Allow background activity'.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "2. Enable 'Allow foreground activity' and 'Allow auto-launch' if using Bluetooth headset wake-up.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "3. Ensure Lock-Screen Notifications are allowed under Notifications.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Battery Settings")
                    }

                    OutlinedButton(
                        onClick = {
                            runCatching {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("App Info & Notifications")
                    }
                }
            }
        }

        // 6. HARDWARE & BLUETOOTH LIMITATIONS / ABOUT
        item {
            SettingsSectionCard(
                title = "Hardware Media Controls & About Play Mix",
                icon = Icons.Default.Headphones
            ) {
                Text(
                    text = "Play Mix v3.0.0 • Native Android Kotlin + Media3 ExoPlayer",
                    style = MaterialTheme.typography.titleSmall,
                    color = SoftCyan
                )
                Text(
                    text = "• Single headset press: Play / Pause\n• Double press / AVRCP Next: Next Track\n• Triple press / AVRCP Previous: Previous Track\n• Home-Screen Widget: Add Play Mix 4x1 Widget from your Android Launcher\n• Hardware Volume Keys: Synchronized with Android STREAM_MUSIC\n• Core Principle: Moving changes category. Removing changes visibility. Deleting removes the original file.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showRestoreCategoryDialog && hiddenSongs.isNotEmpty()) {
        RestoreToCategoryDialog(
            songs = hiddenSongs,
            onConfirmRestore = { targetCat ->
                onRestoreSongsToCategory(hiddenSongs.map { it.id }.toSet(), targetCat)
                showRestoreCategoryDialog = false
            },
            onDismiss = { showRestoreCategoryDialog = false }
        )
    }

    if (exportedBackupJson != null) {
        AlertDialog(
            onDismissRequest = { exportedBackupJson = null },
            title = { Text("Play Mix Backup JSON Snapshot") },
            text = {
                OutlinedTextField(
                    value = exportedBackupJson.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    minLines = 6,
                    maxLines = 12,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { exportedBackupJson = null }) {
                    Text("Done")
                }
            }
        )
    }

    if (showRestoreBackupDialog) {
        var jsonInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showRestoreBackupDialog = false },
            title = { Text("Restore Play Mix Backup JSON") },
            text = {
                OutlinedTextField(
                    value = jsonInput,
                    onValueChange = { jsonInput = it },
                    label = { Text("Paste exported Play Mix JSON") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (jsonInput.isNotBlank()) {
                            onRestoreFullBackup(jsonInput)
                            showRestoreBackupDialog = false
                        }
                    }
                ) {
                    Text("Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreBackupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        ElectricViolet.copy(alpha = 0.35f),
                        SoftCyan.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.06f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    ElectricViolet.copy(alpha = 0.28f),
                                    SoftCyan.copy(alpha = 0.22f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SoftCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
