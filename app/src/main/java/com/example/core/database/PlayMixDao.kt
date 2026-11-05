package com.example.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayMixDao {

    // --- SONGS ---
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE primaryCategory = :categoryId ORDER BY title COLLATE NOCASE ASC")
    fun observeSongsByCategory(categoryId: String): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs")
    suspend fun getAllSongsSnapshot(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongs(songs: List<SongEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSong(song: SongEntity)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun setFavorite(songId: String, isFavorite: Boolean)

    @Query("UPDATE songs SET rating = :rating WHERE id = :songId")
    suspend fun setRating(songId: String, rating: Int)

    @Query("UPDATE songs SET isHidden = :isHidden, exclusionReason = :reason, categoryUpdatedAt = :updatedAt WHERE id = :songId")
    suspend fun setHidden(
        songId: String,
        isHidden: Boolean,
        reason: String = "",
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE songs SET isHidden = 0, removedFromCategoriesCsv = '', exclusionReason = ''")
    suspend fun unhideAllSongs()

    @Query(
        """
        UPDATE songs
        SET primaryCategory = :targetCategoryId,
            isManualClassification = 1,
            classificationReason = :reason,
            removedFromCategoriesCsv = :updatedRemovedCsv,
            exclusionReason = '',
            isHidden = 0,
            categoryUpdatedAt = :updatedAt
        WHERE id = :songId
        """
    )
    suspend fun updateSongCategory(
        songId: String,
        targetCategoryId: String,
        reason: String,
        updatedRemovedCsv: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query(
        """
        UPDATE songs
        SET removedFromCategoriesCsv = :updatedRemovedCsv,
            exclusionReason = :reason,
            categoryUpdatedAt = :updatedAt
        WHERE id = :songId
        """
    )
    suspend fun updateSongCategoryVisibility(
        songId: String,
        updatedRemovedCsv: String,
        reason: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE songs SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :songId")
    suspend fun incrementPlayCount(songId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSongById(songId: String)

    // --- PLAYLISTS ---
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists")
    suspend fun getPlaylistsSnapshot(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist_songs ORDER BY position ASC")
    fun observeAllPlaylistRefs(): Flow<List<PlaylistSongCrossRef>>

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistSongRefs(playlistId: Long): List<PlaylistSongCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :newName, description = :description WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, newName: String, description: String = "")

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongRef(ref: PlaylistSongCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongRefs(refs: List<PlaylistSongCrossRef>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)

    @Query("DELETE FROM playlist_songs WHERE songId = :songId")
    suspend fun removeSongFromAllPlaylists(songId: String)

    // --- LYRICS CACHE ---
    @Query("SELECT * FROM lyrics_cache WHERE songId = :songId LIMIT 1")
    fun observeLyricsForSong(songId: String): Flow<LyricsEntity?>

    @Query("SELECT * FROM lyrics_cache WHERE songId = :songId LIMIT 1")
    suspend fun getLyricsForSong(songId: String): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLyrics(lyrics: LyricsEntity)

    @Query("UPDATE lyrics_cache SET offsetMs = :offsetMs WHERE songId = :songId")
    suspend fun updateLyricsOffset(songId: String, offsetMs: Long)

    // --- PLAYBACK HISTORY ---
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT 100")
    fun observePlaybackHistory(): Flow<List<PlaybackHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaybackHistory(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history")
    suspend fun clearPlaybackHistory()

    // --- PERSISTED QUEUE ---
    @Query("SELECT * FROM persisted_queue ORDER BY queueIndex ASC")
    suspend fun getPersistedQueue(): List<QueueItemEntity>

    @Query("DELETE FROM persisted_queue")
    suspend fun clearPersistedQueue()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersistedQueue(items: List<QueueItemEntity>)

    @Transaction
    suspend fun replacePersistedQueue(items: List<QueueItemEntity>) {
        clearPersistedQueue()
        insertPersistedQueue(items)
    }

    // --- BOOKMARKS ---
    @Query("SELECT * FROM bookmarks ORDER BY positionMs ASC")
    fun observeAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: Long)

    // --- NAMED QUEUES / QUEUE HISTORY ---
    @Query("SELECT * FROM named_queues ORDER BY updatedAt DESC")
    fun observeNamedQueues(): Flow<List<NamedQueueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNamedQueue(namedQueue: NamedQueueEntity): Long

    @Query("DELETE FROM named_queues WHERE id = :queueId")
    suspend fun deleteNamedQueue(queueId: Long)

    // --- SEARCH HISTORY ---
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 15")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(item: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE query = :query")
    suspend fun deleteSearchQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // --- EXCLUDED & SAF FOLDERS ---
    @Query("SELECT * FROM excluded_folders")
    fun observeExcludedFolders(): Flow<List<ExcludedFolderEntity>>

    @Query("SELECT * FROM excluded_folders")
    suspend fun getExcludedFoldersSnapshot(): List<ExcludedFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun excludeFolder(folder: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE folderPath = :folderPath")
    suspend fun includeFolder(folderPath: String)

    @Query("SELECT * FROM saf_folders ORDER BY addedAt DESC")
    fun observeSafFolders(): Flow<List<SafFolderEntity>>

    @Query("SELECT * FROM saf_folders")
    suspend fun getSafFoldersSnapshot(): List<SafFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSafFolder(folder: SafFolderEntity)

    @Query("DELETE FROM saf_folders WHERE treeUri = :treeUri")
    suspend fun removeSafFolder(treeUri: String)
}
