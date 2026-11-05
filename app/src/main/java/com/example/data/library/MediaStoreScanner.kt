package com.example.data.library

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.example.core.database.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreScanner(private val context: Context) {

    private val supportedExtensions = setOf(
        "mp3", "flac", "wav", "m4a", "m4b", "aac", "ogg", "opus", "oga", "weba", "amr", "3gp"
    )

    suspend fun scanDeviceMediaStore(
        minDurationSec: Int,
        excludedFolders: Set<String>
    ): List<SongEntity> = withContext(Dispatchers.IO) {
        // First trigger MediaScanner on any newly downloaded audio files in public directories
        val unindexedFiles = findPublicDirectoryAudioFiles(excludedFolders)
        if (unindexedFiles.isNotEmpty()) {
            runCatching {
                MediaScannerConnection.scanFile(
                    context,
                    unindexedFiles.map { it.absolutePath }.toTypedArray(),
                    null,
                    null
                )
            }
        }

        val discovered = mutableListOf<SongEntity>()
        val seenPaths = HashSet<String>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.IS_ALARM,
            MediaStore.Audio.Media.IS_RINGTONE,
            MediaStore.Audio.Media.IS_NOTIFICATION,
            MediaStore.Audio.Media.IS_PODCAST
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.IS_AUDIOBOOK)
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.Audio.Media.GENRE)
                add(MediaStore.Audio.Media.ALBUM_ARTIST)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(MediaStore.Audio.Media.IS_RECORDING)
            }
        }.toTypedArray()

        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")
        val minDurationMs = (minDurationSec.coerceAtLeast(1) * 1000L)

        runCatching {
            // Do not filter by DURATION in SQL because newly downloaded files in MediaStore
            // sometimes have DURATION = 0 or NULL until first decoded; we inspect duration in Kotlin.
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val mimeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val isMusicCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC)
                val isAlarmCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_ALARM)
                val isRingtoneCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_RINGTONE)
                val isNotificationCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_NOTIFICATION)
                val isPodcastCol = cursor.getColumnIndex(MediaStore.Audio.Media.IS_PODCAST)
                val isAudiobookCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.IS_AUDIOBOOK)
                } else -1
                val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1
                val isRecordingCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.IS_RECORDING)
                } else -1
                val genreCol = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                val albumArtistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val filePath = cursor.getString(dataCol) ?: ""
                    val relativePath = if (relativePathCol >= 0) {
                        cursor.getString(relativePathCol) ?: ""
                    } else ""
                    val parentFolder = if (filePath.isNotBlank()) {
                        File(filePath).parentFile
                    } else null
                    val folderPath = parentFolder?.absolutePath ?: "/storage/emulated/0/Music"
                    if (excludedFolders.contains(folderPath)) continue

                    val contentUriObj = ContentUris.withAppendedId(collection, mediaId)
                    val contentUri = contentUriObj.toString()

                    val isMusicFlag = isMusicCol >= 0 && cursor.getInt(isMusicCol) != 0
                    val isAlarmFlag = isAlarmCol >= 0 && cursor.getInt(isAlarmCol) != 0
                    val isRingtoneFlag = isRingtoneCol >= 0 && cursor.getInt(isRingtoneCol) != 0
                    val isNotificationFlag = isNotificationCol >= 0 && cursor.getInt(isNotificationCol) != 0
                    val isPodcastFlag = isPodcastCol >= 0 && cursor.getInt(isPodcastCol) != 0
                    val isAudiobookFlag = isAudiobookCol >= 0 && cursor.getInt(isAudiobookCol) != 0
                    val isRecordingFlag = isRecordingCol >= 0 && cursor.getInt(isRecordingCol) != 0

                    var durationMs = cursor.getLong(durationCol)
                    if (durationMs <= 0L) {
                        durationMs = resolveDurationFallbackMs(contentUriObj, filePath)
                    }
                    val isShortSystemTone = isAlarmFlag || isRingtoneFlag || isNotificationFlag
                    if (!isShortSystemTone && durationMs in 1 until minDurationMs) {
                        continue
                    }
                    if (durationMs <= 0L) {
                        durationMs = 180_000L // Fallback when container header omits duration
                    }

                    val folderName = parentFolder?.name?.takeIf { it.isNotBlank() }
                        ?: relativePath.trim('/').substringAfterLast('/').ifBlank { "Music" }
                    val fileName = if (filePath.isNotBlank()) {
                        File(filePath).name
                    } else "track_$mediaId.mp3"

                    val rawTitle = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: File(filePath).nameWithoutExtension.ifBlank { "Track $mediaId" }
                    val rawArtist = cursor.getString(artistCol)
                        ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Unknown Artist"
                    val rawAlbum = cursor.getString(albumCol)
                        ?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdCol)
                    val rawTrack = cursor.getInt(trackCol)
                    val trackNumber = if (rawTrack > 1000) rawTrack % 1000 else rawTrack.coerceAtLeast(1)
                    val year = cursor.getInt(yearCol).takeIf { it in 1900..2100 } ?: 2025
                    val fileSize = cursor.getLong(sizeCol).coerceAtLeast(0L)
                    val dateAdded = cursor.getLong(dateAddedCol) * 1000L
                    val mimeType = if (mimeCol >= 0) {
                        cursor.getString(mimeCol)?.takeIf { it.isNotBlank() } ?: "audio/mpeg"
                    } else "audio/mpeg"
                    val genre = if (genreCol >= 0) {
                        cursor.getString(genreCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Audio"
                    } else "Audio"
                    val albumArtist = if (albumArtistCol >= 0) {
                        cursor.getString(albumArtistCol)?.takeIf { it.isNotBlank() } ?: rawArtist
                    } else rawArtist

                    val classification = AudioClassificationEngine.classify(
                        AudioClassificationInput(
                            title = rawTitle,
                            artist = rawArtist,
                            album = rawAlbum,
                            genre = genre,
                            filePath = filePath,
                            relativePath = relativePath,
                            folderName = folderName,
                            fileName = fileName,
                            mimeType = mimeType,
                            durationMs = durationMs,
                            isMusicFlag = isMusicFlag,
                            isAlarmFlag = isAlarmFlag,
                            isRingtoneFlag = isRingtoneFlag,
                            isNotificationFlag = isNotificationFlag,
                            isPodcastFlag = isPodcastFlag,
                            isAudiobookFlag = isAudiobookFlag,
                            isRecordingFlag = isRecordingFlag
                        )
                    )

                    val artworkUri = if (albumId > 0) {
                        ContentUris.withAppendedId(albumArtBaseUri, albumId).toString()
                    } else null

                    if (filePath.isNotBlank()) {
                        seenPaths.add(filePath)
                    }

                    discovered.add(
                        SongEntity(
                            id = "ms_$mediaId",
                            mediaStoreId = mediaId,
                            uri = contentUri,
                            title = rawTitle,
                            artist = rawArtist,
                            album = rawAlbum,
                            albumArtist = albumArtist,
                            genre = genre,
                            durationMs = durationMs,
                            trackNumber = trackNumber,
                            year = year,
                            albumArtUri = artworkUri,
                            filePath = filePath,
                            folderName = folderName,
                            fileSize = fileSize,
                            dateAdded = if (dateAdded > 0L) dateAdded else System.currentTimeMillis(),
                            isDemoTrack = false,
                            primaryCategory = classification.primaryCategory.id,
                            originalCategory = classification.primaryCategory.id,
                            isManualClassification = false,
                            classificationReason = classification.reason,
                            mimeType = mimeType
                        )
                    )
                }
            }
        }

        // Direct fallback scan of public Download / Music / Podcasts / Alarms / Ringtones folders
        // for newly downloaded files not yet committed to Android MediaStore DB
        for (file in unindexedFiles) {
            if (file.absolutePath in seenPaths) continue
            val directEntity = extractDirectFileMetadata(file, minDurationMs)
            if (directEntity != null) {
                seenPaths.add(file.absolutePath)
                discovered.add(directEntity)
            }
        }

        discovered
    }

    private fun findPublicDirectoryAudioFiles(excludedFolders: Set<String>): List<File> {
        val result = mutableListOf<File>()
        val publicDirTypes = buildList {
            add(Environment.DIRECTORY_MUSIC)
            add(Environment.DIRECTORY_DOWNLOADS)
            add(Environment.DIRECTORY_PODCASTS)
            add(Environment.DIRECTORY_RINGTONES)
            add(Environment.DIRECTORY_ALARMS)
            add(Environment.DIRECTORY_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(Environment.DIRECTORY_AUDIOBOOKS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Environment.DIRECTORY_RECORDINGS)
            }
        }

        fun walkDir(dir: File, depth: Int) {
            if (depth > 3 || !dir.exists() || !dir.canRead()) return
            if (excludedFolders.contains(dir.absolutePath)) return
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (child.isDirectory && !child.name.startsWith(".")) {
                    walkDir(child, depth + 1)
                } else if (child.isFile && child.length() > 1024L) {
                    val ext = child.extension.lowercase()
                    if (ext in supportedExtensions) {
                        result.add(child)
                    }
                }
            }
        }

        for (type in publicDirTypes) {
            runCatching {
                val pubDir = Environment.getExternalStoragePublicDirectory(type)
                if (pubDir != null) {
                    walkDir(pubDir, 0)
                }
            }
        }
        return result
    }

    private fun resolveDurationFallbackMs(contentUri: Uri, filePath: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            try {
                retriever.setDataSource(context, contentUri)
            } catch (_: Exception) {
                if (filePath.isNotBlank() && File(filePath).exists()) {
                    retriever.setDataSource(filePath)
                }
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extractDirectFileMetadata(
        file: File,
        minDurationMs: Long
    ): SongEntity? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 180_000L).coerceAtLeast(1000L)
            if (durationMs < minDurationMs.coerceAtMost(2000L)) return null

            val folderName = file.parentFile?.name ?: "Download"
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() } ?: folderName
            val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                ?.takeIf { it.isNotBlank() } ?: artist
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.takeIf { it.isNotBlank() } ?: "Audio"
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.take(4)?.toIntOrNull() ?: 2025
            val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.toIntOrNull() ?: 1
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?.takeIf { it.isNotBlank() } ?: "audio/${file.extension.lowercase()}"

            val classification = AudioClassificationEngine.classify(
                AudioClassificationInput(
                    title = title,
                    artist = artist,
                    album = album,
                    genre = genre,
                    filePath = file.absolutePath,
                    relativePath = folderName,
                    folderName = folderName,
                    fileName = file.name,
                    mimeType = mimeType,
                    durationMs = durationMs,
                    isMusicFlag = durationMs >= 15_000L
                )
            )

            SongEntity(
                id = "file_${file.absolutePath.hashCode().toUInt()}",
                mediaStoreId = 0L,
                uri = Uri.fromFile(file).toString(),
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                genre = genre,
                durationMs = durationMs,
                trackNumber = trackNumber,
                year = year,
                albumArtUri = null,
                filePath = file.absolutePath,
                folderName = folderName,
                fileSize = file.length(),
                dateAdded = file.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis(),
                isDemoTrack = false,
                primaryCategory = classification.primaryCategory.id,
                originalCategory = classification.primaryCategory.id,
                isManualClassification = false,
                classificationReason = classification.reason,
                mimeType = mimeType
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    suspend fun scanSafTreeFolder(
        treeUriString: String,
        minDurationSec: Int
    ): List<SongEntity> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<SongEntity>()
        val treeUri = runCatching { Uri.parse(treeUriString) }.getOrNull() ?: return@withContext emptyList()
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        val rootFolderName = rootDoc.name ?: "SAF Folder"

        fun traverse(directory: DocumentFile, folderLabel: String) {
            for (file in directory.listFiles()) {
                if (file.isDirectory) {
                    traverse(file, file.name ?: folderLabel)
                } else if (file.isFile) {
                    val name = file.name ?: continue
                    val ext = name.substringAfterLast('.', "").lowercase()
                    val mime = file.type ?: ""
                    if (ext in supportedExtensions || mime.startsWith("audio/")) {
                        val song = extractSafSongMetadata(
                            file = file,
                            folderName = folderLabel,
                            minDurationMs = minDurationSec * 1000L
                        )
                        if (song != null) {
                            discovered.add(song)
                        }
                    }
                }
            }
        }

        runCatching { traverse(rootDoc, rootFolderName) }
        discovered
    }

    private fun extractSafSongMetadata(
        file: DocumentFile,
        folderName: String,
        minDurationMs: Long
    ): SongEntity? {
        val uri = file.uri
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 180_000L).coerceAtLeast(1000L)
            if (durationMs < minDurationMs.coerceAtMost(2000L)) return null

            val fileName = file.name ?: "Audio Track.mp3"
            val fileNameNoExt = fileName.substringBeforeLast('.')
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?.takeIf { it.isNotBlank() } ?: fileNameNoExt
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() } ?: folderName
            val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                ?.takeIf { it.isNotBlank() } ?: artist
            val genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                ?.takeIf { it.isNotBlank() } ?: "Local Folder"
            val year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?.take(4)?.toIntOrNull() ?: 2025
            val trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')?.toIntOrNull() ?: 1
            val mimeType = file.type ?: "audio/mpeg"
            val pathStr = uri.path ?: uri.toString()

            val classification = AudioClassificationEngine.classify(
                AudioClassificationInput(
                    title = title,
                    artist = artist,
                    album = album,
                    genre = genre,
                    filePath = pathStr,
                    relativePath = folderName,
                    folderName = folderName,
                    fileName = fileName,
                    mimeType = mimeType,
                    durationMs = durationMs,
                    isMusicFlag = durationMs >= 15_000L
                )
            )

            SongEntity(
                id = "saf_${uri.toString().hashCode().toUInt()}",
                mediaStoreId = 0L,
                uri = uri.toString(),
                title = title,
                artist = artist,
                album = album,
                albumArtist = albumArtist,
                genre = genre,
                durationMs = durationMs,
                trackNumber = trackNumber,
                year = year,
                albumArtUri = null,
                filePath = pathStr,
                folderName = folderName,
                fileSize = file.length(),
                dateAdded = file.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis(),
                isDemoTrack = false,
                primaryCategory = classification.primaryCategory.id,
                originalCategory = classification.primaryCategory.id,
                isManualClassification = false,
                classificationReason = classification.reason,
                mimeType = mimeType
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
