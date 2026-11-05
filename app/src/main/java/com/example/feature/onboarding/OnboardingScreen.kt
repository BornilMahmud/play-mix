package com.example.feature.onboarding

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.SoftCyan
import com.example.ui.theme.SuccessEmerald

@Composable
fun OnboardingScreen(
    hasAudioPermission: Boolean,
    hasNotificationPermission: Boolean,
    onRequestPermissions: () -> Unit,
    onPickSafFolder: () -> Unit,
    onContinueToApp: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onContinueToApp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("onboarding_start_listening"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasAudioPermission) "Open Music Library" else "Launch Play Mix",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Brand Emblem
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(ElectricViolet, DeepBlue, SoftCyan)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "Play Mix Emblem",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "PLAY MIX",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Audiophile Native Android Music Player",
                style = MaterialTheme.typography.titleSmall,
                color = SoftCyan
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Permission & Setup Status Card (Placed near top so it is immediately accessible)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Device Permissions & Access",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    PermissionStatusRow(
                        label = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            "Audio Library Access (READ_MEDIA_AUDIO)"
                        } else {
                            "Storage Audio Access (READ_EXTERNAL_STORAGE)"
                        },
                        granted = hasAudioPermission
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionStatusRow(
                            label = "Media Notification Controls (POST_NOTIFICATIONS)",
                            granted = hasNotificationPermission
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (hasAudioPermission) {
                                    onContinueToApp()
                                } else {
                                    onRequestPermissions()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_grant_permissions")
                        ) {
                            Text(if (hasAudioPermission) "Permissions Granted ✓" else "Grant Access")
                        }

                        OutlinedButton(
                            onClick = onPickSafFolder,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("onboarding_pick_folder")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pick Folder")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OnboardingFeatureCard(
                icon = Icons.Default.Lock,
                title = "Uninterrupted Screen-Off & Lock-Screen Playback",
                description = "Powered by a dedicated Media3 Foreground Service with wake-lock management so music never drops when you lock your Realme GT Master Edition."
            )

            Spacer(modifier = Modifier.height(10.dp))

            OnboardingFeatureCard(
                icon = Icons.Default.Headphones,
                title = "Bluetooth AVRCP, Headset & 5-Band Hardware EQ",
                description = "Full support for Bluetooth headphones, wired headset buttons, Bass Boost, Virtualizer, Crossfade, Sleep Timer, and synced LRC/LRCLIB lyrics."
            )

            Spacer(modifier = Modifier.height(10.dp))

            OnboardingFeatureCard(
                icon = Icons.Default.LibraryMusic,
                title = "Local MediaStore + Download Folder + SAF Folders",
                description = "Automatically discovers MP3, FLAC, WAV, M4A, AAC, OGG, and Opus tracks across Music and Download folders, plus includes 6 multi-harmonic studio demo tracks."
            )
        }
    }
}

@Composable
private fun OnboardingFeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = ElectricViolet.copy(alpha = 0.2f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SoftCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    granted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (granted) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (granted) SuccessEmerald else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
