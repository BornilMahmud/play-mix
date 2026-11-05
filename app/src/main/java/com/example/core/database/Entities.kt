package com.example.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AudioCategory
import com.example.domain.model.LyricSource
import com.example.domain.model.NamedQueue
import com.example.domain.model.PlaybackBookmark
import com.example.domain.model.Song

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["primaryCategory"]),
        Index(value = ["isHidden"])
    ]
)
data class SongEntity(
    @PrimaryKey val id: String,
    val mediaStoreId: Long = 0L,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val durationMs: Long,
    val trackNumber: Int,
    val year: Int,
    val albumArtUri: String?,
    val filePath: String,
    val folderName: String,
    val fileSize: Long,
    val dateAdded: Long,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val isDemoTrack: Boolean = false,
    val embeddedLyrics: String? = null,
    val rating: Int = 0,
    val isCompilation: Boolean = false,
    val trackGainDb: Float = -1.2f,
    val albumGainDb: Float = -1.0f,
    val onlineProviderId: String? = null,
    val primaryCategory: String = AudioCategory.MUSIC.id,
    val originalCategory: String = AudioCategory.MUSIC.id,
    val isManualClassification: Boolean = false,
    val classificationReason: String = "MediaStore IS_MUSIC flag",
    val removedFromCategoriesCsv: String = "",
    val exclusionReason: String = "",
    val categoryUpdatedAt: Long = 0L,
    val mimeType: String = "audio/wav"
) {
    fun toDomain(): Song = Song(
        id = id,
        mediaStoreId = mediaStoreId,
        uri = uri,
        title = title,
        artist = artist,
        album = album,
        albumArtist = albumArtist,
        genre = genre,
        durationMs = durationMs,
        trackNumber = trackNumber,
        year = year,
        albumArtUri = albumArtUri,
        filePath = filePath,
        folderName = folderName,
        fileSize = fileSize,
        dateAdded = dateAdded,
        isFavorite = isFavorite,
        isHidden = isHidden,
        playCount = playCount,
        lastPlayedAt = lastPlayedAt,
        isDemoTrack = isDemoTrack,
        embeddedLyrics = embeddedLyrics,
        rating = rating,
        isCompilation = isCompilation,
        trackGainDb = trackGainDb,
        albumGainDb = albumGainDb,
        onlineProviderId = onlineProviderId,
        primaryCategory = AudioCategory.fromId(primaryCategory),
        originalCategory = AudioCategory.fromId(originalCategory),
        isManualClassification = isManualClassification,
        classificationReason = classificationReason,
        removedFromCategories = AudioCategory.parseSetFromCsv(removedFromCategoriesCsv),
        exclusionReason = exclusionReason,
        categoryUpdatedAt = categoryUpdatedAt,
        mimeType = mimeType
    )
}

fun Song.toEntity(): SongEntity = SongEntity(
    id = id,
    mediaStoreId = mediaStoreId,
    uri = uri,
    title = title,
    artist = artist,
    album = album,
    albumArtist = albumArtist,
    genre = genre,
    durationMs = durationMs,
    trackNumber = trackNumber,
    year = year,
    albumArtUri = albumArtUri,
    filePath = filePath,
    folderName = folderName,
    fileSize = fileSize,
    dateAdded = dateAdded,
    isFavorite = isFavorite,
    isHidden = isHidden,
    playCount = playCount,
    lastPlayedAt = lastPlayedAt,
    isDemoTrack = isDemoTrack,
    embeddedLyrics = embeddedLyrics,
    rating = rating,
    isCompilation = isCompilation,
    trackGainDb = trackGainDb,
    albumGainDb = albumGainDb,
    onlineProviderId = onlineProviderId,
    primaryCategory = primaryCategory.id,
    originalCategory = originalCategory.id,
    isManualClassification = isManualClassification,
    classificationReason = classificationReason,
    removedFromCategoriesCsv = AudioCategory.toCsv(removedFromCategories),
    exclusionReason = exclusionReason,
    categoryUpdatedAt = categoryUpdatedAt,
    mimeType = mimeType
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: String,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "lyrics_cache")
data class LyricsEntity(
    @PrimaryKey val songId: String,
    val rawContent: String,
    val isSynced: Boolean,
    val offsetMs: Long = 0L,
    val sourceName: String = LyricSource.DEMO.name,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val historyId: Long = 0L,
    val songId: String,
    val playedAt: Long = System.currentTimeMillis(),
    val listenedDurationMs: Long = 0L
)

@Entity(tableName = "persisted_queue")
data class QueueItemEntity(
    @PrimaryKey val queueIndex: Int,
    val songId: String
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey val folderPath: String,
    val excludedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saf_folders")
data class SafFolderEntity(
    @PrimaryKey val treeUri: String,
    val displayName: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks", indices = [Index("songId")])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val songId: String,
    val positionMs: Long,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = PlaybackBookmark(
        id = id,
        songId = songId,
        positionMs = positionMs,
        label = label,
        createdAt = createdAt
    )
}

@Entity(tableName = "named_queues")
data class NamedQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val songIdsCsv: String,
    val currentIndex: Int = 0,
    val currentPositionMs: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = NamedQueue(
        id = id,
        name = name,
        songIds = songIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
        currentIndex = currentIndex,
        currentPositionMs = currentPositionMs,
        updatedAt = updatedAt
    )
}
