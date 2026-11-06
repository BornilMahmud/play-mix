package com.example.core.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.model.AudioCategory
import com.example.domain.model.Playlist
import com.example.domain.model.Song
import com.example.domain.model.formatDurationMs
import com.example.playback.controller.PlaybackUiState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FavoritePink
import com.example.ui.theme.SoftCyan

@Composable
fun AlbumArtworkBox(
    artworkUri: String?,
    title: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    targetSizePx: Int = 256
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ElectricViolet.copy(alpha = 0.85f),
                        DeepBlue.copy(alpha = 0.9f),
                        SoftCyan.copy(alpha = 0.75f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Stylized vinyl grooves fallback
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxR = size.minDimension * 0.42f
            drawCircle(
                color = Color.Black.copy(alpha = 0.28f),
                radius = maxR,
                center = center
            )
            drawCircle(
                color = SoftCyan.copy(alpha = 0.35f),
                radius = maxR * 0.45f,
                center = center
            )
        }
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(26.dp)
        )
        if (!artworkUri.isNullOrBlank()) {
            val request = remember(artworkUri, targetSizePx) {
                ImageRequest.Builder(context)
                    .data(artworkUri)
                    .size(targetSizePx)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun AnimatedPlayingIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "eq_bars")
    val bar1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse),
        label = "b1"
    )
    val bar2 by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(340), RepeatMode.Reverse),
        label = "b2"
    )
    val bar3 by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(510), RepeatMode.Reverse),
        label = "b3"
    )

    Canvas(modifier = modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val barWidth = w / 5f
        val heights = if (isPlaying) listOf(bar1, bar2, bar3) else listOf(0.35f, 0.55f, 0.4f)
        heights.forEachIndexed { idx, frac ->
            val x = (idx * 2 + 0.5f) * barWidth
            val barH = (h * frac).coerceAtLeast(4f)
            drawLine(
                color = SoftCyan,
                start = Offset(x, h),
                end = Offset(x, h - barH),
                strokeWidth = barWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    starSize: Dp = 22.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        (1..5).forEach { star ->
            Icon(
                imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Rate $star stars",
                tint = if (star <= rating) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(starSize)
                    .clickable {
                        onRatingChanged(if (rating == star) 0 else star)
                    }
            )
        }
    }
}

@Composable
fun SongRowItem(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onOpenAlbum: (() -> Unit)? = null,
    onOpenArtist: (() -> Unit)? = null,
    onHideSong: (() -> Unit)? = null,
    onEditTags: ((Song) -> Unit)? = null,
    onMoveToCategory: ((Song, AudioCategory) -> Unit)? = null,
    onRemoveFromCategory: ((Song) -> Unit)? = null,
    onDeleteFromDevice: ((Song) -> Unit)? = null,
    showCategoryBadge: Boolean = false,
    currentCategoryLabel: String = song.primaryCategory.displayName,
    isMultiSelectMode: Boolean = false,
    isSelectedInMultiSelect: Boolean = false,
    onToggleMultiSelect: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showMoveCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val containerColor = when {
        isSelectedInMultiSelect -> SoftCyan.copy(alpha = 0.22f)
        isCurrentSong -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)
        else -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
    }

    val rowBorderBrush = when {
        isSelectedInMultiSelect -> Brush.horizontalGradient(listOf(SoftCyan, ElectricViolet))
        isCurrentSong -> Brush.horizontalGradient(listOf(SoftCyan.copy(alpha = 0.7f), ElectricViolet.copy(alpha = 0.7f)))
        else -> Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.07f),
                Color.White.copy(alpha = 0.03f)
            )
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrentSong || isSelectedInMultiSelect) 1.2.dp else 0.7.dp,
                brush = rowBorderBrush,
                shape = RoundedCornerShape(18.dp)
            )
            .testTag("song_row_${song.id}")
            .clickable {
                if (isMultiSelectMode && onToggleMultiSelect != null) {
                    onToggleMultiSelect()
                } else {
                    onClick()
                }
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelectedInMultiSelect,
                    onCheckedChange = { onToggleMultiSelect?.invoke() },
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            Box(contentAlignment = Alignment.Center) {
                AlbumArtworkBox(
                    artworkUri = song.albumArtUri,
                    title = song.title,
                    modifier = Modifier.size(50.dp),
                    cornerRadius = 12.dp,
                    targetSizePx = 144
                )
                if (isCurrentSong) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedPlayingIndicator(isPlaying = isPlaying)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isCurrentSong) SoftCyan else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (showCategoryBadge) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = SoftCyan.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = song.primaryCategory.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftCyan,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else if (song.isDemoTrack) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = ElectricViolet.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "HQ",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftCyan,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else if (song.isOnlineStream) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = SoftCyan.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "STREAM",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftCyan,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${song.artist} • ${song.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (song.rating > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "★".repeat(song.rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarning
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = song.formattedDuration,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("fav_button_${song.id}")
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) FavoritePink else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("more_button_${song.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    // 1. Play
                    DropdownMenuItem(
                        text = { Text("Play") },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    // 2. Play Next
                    DropdownMenuItem(
                        text = { Text("Play Next") },
                        leadingIcon = { Icon(Icons.Default.SkipNext, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onPlayNext()
                        }
                    )
                    // 3. Add to Queue
                    DropdownMenuItem(
                        text = { Text("Add to Queue") },
                        leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddToQueue()
                        }
                    )
                    // 4. Add to Playlist
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onAddToPlaylist()
                        }
                    )
                    // 5. Move to Category (Non-Destructive Category Reassignment)
                    if (onMoveToCategory != null) {
                        DropdownMenuItem(
                            text = { Text("Move to Category...") },
                            leadingIcon = { Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = SoftCyan) },
                            onClick = {
                                menuExpanded = false
                                showMoveCategoryDialog = true
                            }
                        )
                    }
                    // 6. Remove from this Category (Non-Destructive Visibility Removal)
                    if (onRemoveFromCategory != null) {
                        DropdownMenuItem(
                            text = { Text("Remove from $currentCategoryLabel") },
                            leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onRemoveFromCategory(song)
                            }
                        )
                    }
                    // 7. Hide from Library
                    if (onHideSong != null) {
                        DropdownMenuItem(
                            text = { Text("Hide from Library") },
                            leadingIcon = { Icon(Icons.Outlined.VisibilityOff, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onHideSong()
                            }
                        )
                    }
                    // 8. Edit Metadata
                    if (onEditTags != null) {
                        DropdownMenuItem(
                            text = { Text("Edit Metadata & Artwork") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEditTags(song)
                            }
                        )
                    }
                    if (onOpenAlbum != null) {
                        DropdownMenuItem(
                            text = { Text("Go to Album") },
                            leadingIcon = { Icon(Icons.Default.Album, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenAlbum()
                            }
                        )
                    }
                    if (onOpenArtist != null) {
                        DropdownMenuItem(
                            text = { Text("Go to Artist") },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onOpenArtist()
                            }
                        )
                    }
                    // 9. View File Info
                    DropdownMenuItem(
                        text = { Text("View File & Classification Info") },
                        leadingIcon = { Icon(Icons.Outlined.Info, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            showInfoDialog = true
                        }
                    )
                    // 10. Visually Separated Destructive Action: Delete from Device
                    if (onDeleteFromDevice != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Delete from Device...",
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                showDeleteConfirmDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        TrackMetadataDialog(
            song = song,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showMoveCategoryDialog && onMoveToCategory != null) {
        MoveToCategoryDialog(
            songs = listOf(song),
            currentCategory = song.primaryCategory,
            onConfirmMove = { targetCategory ->
                onMoveToCategory(song, targetCategory)
                showMoveCategoryDialog = false
            },
            onDismiss = { showMoveCategoryDialog = false }
        )
    }

    if (showDeleteConfirmDialog && onDeleteFromDevice != null) {
        ConfirmDeleteFromDeviceDialog(
            songs = listOf(song),
            onConfirmDelete = {
                onDeleteFromDevice(song)
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}

@Composable
fun MoveToCategoryDialog(
    songs: List<Song>,
    currentCategory: AudioCategory?,
    onConfirmMove: (AudioCategory) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember {
        mutableStateOf(
            AudioCategory.entries.firstOrNull { it != currentCategory } ?: AudioCategory.MUSIC
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (songs.size == 1) "Move \"${songs.first().title}\" to Category"
                else "Move ${songs.size} Selected Items to Category"
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Non-destructive action: Moving updates the primary category in Play Mix and persists across library rescans. The physical audio file, artwork, tags, and play history are completely preserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                AudioCategory.entries.forEach { category ->
                    val isCurrent = category == currentCategory && songs.size == 1
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isCurrent) { selectedCategory = category }
                            .testTag("move_target_${category.id.lowercase()}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedCategory == category) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategory == category,
                                onClick = { if (!isCurrent) selectedCategory = category },
                                enabled = !isCurrent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isCurrent) "${category.displayName} (Current)" else category.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = category.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmMove(selectedCategory) },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier.testTag("confirm_move_category_btn")
            ) {
                Text("Move to ${selectedCategory.displayName}")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RestoreToCategoryDialog(
    songs: List<Song>,
    onConfirmRestore: (AudioCategory?) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultCat = songs.firstOrNull()?.primaryCategory ?: AudioCategory.MUSIC
    var useCustomDestination by remember { mutableStateOf(false) }
    var chosenCategory by remember { mutableStateOf(defaultCat) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (songs.size == 1) "Restore \"${songs.first().title}\""
                else "Restore ${songs.size} Removed / Hidden Items"
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choose whether to restore each item to its previous category or assign a specific destination category:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useCustomDestination = false },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !useCustomDestination,
                        onClick = { useCustomDestination = false }
                    )
                    Text(
                        text = if (songs.size == 1) {
                            "Restore to original/current category (${defaultCat.displayName})"
                        } else {
                            "Restore each item to its assigned category"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { useCustomDestination = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = useCustomDestination,
                        onClick = { useCustomDestination = true }
                    )
                    Text(
                        text = "Restore into a chosen destination category:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (useCustomDestination) {
                    AudioCategory.entries.forEach { cat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .clickable { chosenCategory = cat },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = chosenCategory == cat,
                                onClick = { chosenCategory = cat }
                            )
                            Text(cat.displayName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmRestore(if (useCustomDestination) chosenCategory else null)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                modifier = Modifier.testTag("confirm_restore_items_btn")
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmDeleteFromDeviceDialog(
    songs: List<Song>,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (songs.size == 1) "Delete File from Device?" else "Delete ${songs.size} Files from Device?",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "WARNING: This is a destructive action that will permanently remove the original audio file(s) from your device storage.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Tip: If you only want to hide this item from the current category or move it to Alarms / Recordings without deleting the file, cancel and choose 'Remove from Category' or 'Move to Category'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftCyan
                )
                Spacer(modifier = Modifier.height(4.dp))
                songs.take(5).forEach { s ->
                    val fileName = s.filePath.substringAfterLast('/').ifBlank { s.title }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "File: $fileName",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = s.filePath.ifBlank { s.uri },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (songs.size > 5) {
                    Text(
                        text = "...and ${songs.size - 5} more file(s)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_device_btn")
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete Original File(s)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CompactMiniPlayer(
    playbackState: PlaybackUiState,
    onOpenNowPlaying: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playbackState.currentSong ?: return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(
                        ElectricViolet.copy(alpha = 0.55f),
                        SoftCyan.copy(alpha = 0.55f)
                    )
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onOpenNowPlaying)
            .testTag("compact_mini_player"),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        tonalElevation = 10.dp,
        shadowElevation = 14.dp,
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ElectricViolet.copy(alpha = 0.14f),
                                Color.Transparent,
                                SoftCyan.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    AlbumArtworkBox(
                        artworkUri = song.albumArtUri,
                        title = song.title,
                        modifier = Modifier.size(48.dp),
                        cornerRadius = 14.dp,
                        targetSizePx = 144
                    )
                    if (playbackState.isPlaying) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedPlayingIndicator(isPlaying = true)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (playbackState.sleepTimerOption != com.example.domain.model.SleepTimerOption.OFF) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Sleep Timer Active",
                                tint = SoftCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "${song.artist} • ${formatDurationMs(playbackState.currentPositionMs)} / ${formatDurationMs(playbackState.durationMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftCyan.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("mini_player_favorite")
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) FavoritePink else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = SoftCyan,
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.testTag("mini_player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play or pause",
                            tint = Color(0xFF040814)
                        )
                    }
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next track",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            LinearProgressIndicator(
                progress = { playbackState.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = SoftCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
fun TrackMetadataDialog(
    song: Song,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Audio File & Classification Info", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetadataLine("Primary Category", song.primaryCategory.displayName)
                MetadataLine("Original Detected Category", song.originalCategory.displayName)
                MetadataLine(
                    "Classification Source",
                    if (song.isManualClassification) "Manual User Override (${song.classificationReason})"
                    else song.classificationReason
                )
                if (song.removedFromCategories.isNotEmpty() || song.isHidden) {
                    MetadataLine(
                        "Visibility Status",
                        song.exclusionReason.ifBlank {
                            "Removed from: ${song.removedFromCategories.joinToString { it.displayName }}"
                        }
                    )
                }
                MetadataLine("Title", song.title)
                MetadataLine("Artist", song.artist)
                MetadataLine("Album", song.album)
                MetadataLine("Album Artist", song.albumArtist)
                MetadataLine("Genre", song.genre)
                MetadataLine("MIME Type", song.mimeType)
                MetadataLine("Duration", song.formattedDuration)
                MetadataLine("Track / Year", "#${song.trackNumber} • ${song.year}")
                MetadataLine("Rating / Plays", "${song.rating} ★ • ${song.playCount} plays")
                MetadataLine("ReplayGain (Track / Album)", "${song.trackGainDb} dB / ${song.albumGainDb} dB")
                MetadataLine("Compilation Album", if (song.isCompilation) "Yes" else "No")
                MetadataLine("File Size", song.formattedFileSize)
                MetadataLine("Folder / Provider", song.onlineProviderId ?: song.folderName)
                MetadataLine("File Path / URI", song.filePath.ifBlank { song.uri })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun MetadataLine(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SoftCyan
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TagAndArtworkEditorDialog(
    song: Song,
    onSave: (
        title: String,
        artist: String,
        album: String,
        albumArtist: String,
        genre: String,
        year: Int,
        trackNumber: Int,
        rating: Int,
        isCompilation: Boolean,
        customArtworkUri: String?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var albumArtist by remember { mutableStateOf(song.albumArtist) }
    var genre by remember { mutableStateOf(song.genre) }
    var yearStr by remember { mutableStateOf(song.year.toString()) }
    var trackNumStr by remember { mutableStateOf(song.trackNumber.toString()) }
    var rating by remember { mutableIntStateOf(song.rating) }
    var isCompilation by remember { mutableStateOf(song.isCompilation) }
    var artworkUri by remember { mutableStateOf(song.albumArtUri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            artworkUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Track Tags & Artwork") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AlbumArtworkBox(
                        artworkUri = artworkUri,
                        title = title,
                        modifier = Modifier.size(68.dp),
                        cornerRadius = 14.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Artwork")
                        }
                        StarRatingBar(
                            rating = rating,
                            onRatingChanged = { rating = it }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Track Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Album") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = albumArtist,
                    onValueChange = { albumArtist = it },
                    label = { Text("Album Artist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Genre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = yearStr,
                        onValueChange = { yearStr = it },
                        label = { Text("Year") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = trackNumStr,
                        onValueChange = { trackNumStr = it },
                        label = { Text("Track #") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Compilation Album Track", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isCompilation,
                        onCheckedChange = { isCompilation = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title.ifBlank { song.title },
                        artist.ifBlank { song.artist },
                        album.ifBlank { song.album },
                        albumArtist.ifBlank { artist.ifBlank { song.albumArtist } },
                        genre.ifBlank { song.genre },
                        yearStr.toIntOrNull() ?: song.year,
                        trackNumStr.toIntOrNull() ?: song.trackNumber,
                        rating,
                        isCompilation,
                        artworkUri
                    )
                    onDismiss()
                }
            ) {
                Text("Save Tags")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BulkTagEditorDialog(
    selectedCount: Int,
    onApplyBulkUpdate: (
        artist: String?,
        album: String?,
        albumArtist: String?,
        genre: String?,
        year: Int?,
        isCompilation: Boolean?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var albumArtist by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var yearStr by remember { mutableStateOf("") }
    var setCompilation by remember { mutableStateOf(false) }
    var isCompilationValue by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Metadata Edit ($selectedCount tracks)") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Leave any field blank to keep each track's existing value.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Override Artist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Override Album") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = albumArtist,
                    onValueChange = { albumArtist = it },
                    label = { Text("Override Album Artist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Override Genre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = yearStr,
                    onValueChange = { yearStr = it },
                    label = { Text("Override Release Year") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = setCompilation,
                        onCheckedChange = { setCompilation = it }
                    )
                    Text("Update Compilation flag", modifier = Modifier.weight(1f))
                    if (setCompilation) {
                        Switch(
                            checked = isCompilationValue,
                            onCheckedChange = { isCompilationValue = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyBulkUpdate(
                        artist.takeIf { it.isNotBlank() },
                        album.takeIf { it.isNotBlank() },
                        albumArtist.takeIf { it.isNotBlank() },
                        genre.takeIf { it.isNotBlank() },
                        yearStr.toIntOrNull(),
                        if (setCompilation) isCompilationValue else null
                    )
                    onDismiss()
                }
            ) {
                Text("Apply to $selectedCount Tracks")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    song: Song,
    playlists: List<Playlist>,
    onSelectPlaylist: (Long) -> Unit,
    onCreateNewPlaylistWithSong: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add \"${song.title}\" to Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (playlists.isNotEmpty()) {
                    Text(
                        text = "Existing Playlists",
                        style = MaterialTheme.typography.labelLarge,
                        color = SoftCyan
                    )
                    playlists.forEach { playlist ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectPlaylist(playlist.id)
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(playlist.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${playlist.songs.size} tracks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Or create new playlist") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (newPlaylistName.isNotBlank()) {
                Button(
                    onClick = {
                        onCreateNewPlaylistWithSong(newPlaylistName)
                        onDismiss()
                    }
                ) {
                    Text("Create & Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
