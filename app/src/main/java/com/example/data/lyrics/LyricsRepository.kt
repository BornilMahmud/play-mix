package com.example.data.lyrics

import com.example.core.database.LyricsEntity
import com.example.core.database.PlayMixDao
import com.example.domain.model.LyricLine
import com.example.domain.model.LyricSource
import com.example.domain.model.LyricWord
import com.example.domain.model.Song
import com.example.domain.model.SongLyrics
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class LrcLibSearchResult(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "trackName") val trackName: String? = null,
    @Json(name = "artistName") val artistName: String? = null,
    @Json(name = "albumName") val albumName: String? = null,
    @Json(name = "duration") val duration: Double? = null,
    @Json(name = "syncedLyrics") val syncedLyrics: String? = null,
    @Json(name = "plainLyrics") val plainLyrics: String? = null
)

interface LrcLibApiService {
    @GET("api/search")
    suspend fun searchLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String
    ): List<LrcLibSearchResult>

    @GET("api/search")
    suspend fun searchByQuery(
        @Query("q") query: String
    ): List<LrcLibSearchResult>
}

class LyricsRepository(private val dao: PlayMixDao) {

    private val lrcLibApi: LrcLibApiService by lazy {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "PlayMixMusicPlayer/1.0.0 (Android)")
                    .build()
                chain.proceed(req)
            }
            .build()
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LrcLibApiService::class.java)
    }

    fun observeLyricsForSong(song: Song): Flow<SongLyrics?> {
        return dao.observeLyricsForSong(song.id).map { entity ->
            if (entity != null) {
                parseLrcToDomain(
                    songId = song.id,
                    rawContent = entity.rawContent,
                    offsetMs = entity.offsetMs,
                    source = runCatching { LyricSource.valueOf(entity.sourceName) }.getOrDefault(LyricSource.DEMO)
                )
            } else {
                loadLocalOrEmbeddedFallback(song)
            }
        }
    }

    suspend fun ensureLocalLyricsLoaded(song: Song) = withContext(Dispatchers.IO) {
        val cached = dao.getLyricsForSong(song.id)
        if (cached != null) return@withContext

        // 1. Check sidecar .lrc file next to local audio file
        if (song.filePath.isNotBlank() && !song.filePath.startsWith("content://")) {
            val audioFile = File(song.filePath)
            val lrcFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
            if (lrcFile.exists() && lrcFile.canRead()) {
                val raw = runCatching { lrcFile.readText() }.getOrNull()
                if (!raw.isNullOrBlank()) {
                    dao.upsertLyrics(
                        LyricsEntity(
                            songId = song.id,
                            rawContent = raw,
                            isSynced = containsLrcTimestamps(raw),
                            offsetMs = 0L,
                            sourceName = LyricSource.LOCAL_FILE.name
                        )
                    )
                    return@withContext
                }
            }
        }

        // 2. Check embedded lyrics tag
        if (!song.embeddedLyrics.isNullOrBlank()) {
            dao.upsertLyrics(
                LyricsEntity(
                    songId = song.id,
                    rawContent = song.embeddedLyrics,
                    isSynced = containsLrcTimestamps(song.embeddedLyrics),
                    offsetMs = 0L,
                    sourceName = if (song.isDemoTrack) LyricSource.DEMO.name else LyricSource.EMBEDDED.name
                )
            )
        }
    }

    suspend fun searchOnlineLyrics(song: Song, customQuery: String? = null): Result<List<LrcLibSearchResult>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val results = if (!customQuery.isNullOrBlank()) {
                    lrcLibApi.searchByQuery(customQuery.trim())
                } else {
                    val primary = lrcLibApi.searchLyrics(song.title, song.artist)
                    if (primary.isNotEmpty()) primary else lrcLibApi.searchByQuery("${song.title} ${song.artist}")
                }
                results.filter { !it.syncedLyrics.isNullOrBlank() || !it.plainLyrics.isNullOrBlank() }
            }
        }

    suspend fun applyLrcLibResult(songId: String, result: LrcLibSearchResult) = withContext(Dispatchers.IO) {
        val content = result.syncedLyrics?.takeIf { it.isNotBlank() }
            ?: result.plainLyrics?.takeIf { it.isNotBlank() }
            ?: return@withContext
        dao.upsertLyrics(
            LyricsEntity(
                songId = songId,
                rawContent = content,
                isSynced = !result.syncedLyrics.isNullOrBlank(),
                offsetMs = 0L,
                sourceName = LyricSource.LRCLIB.name
            )
        )
    }

    suspend fun saveManualLyrics(songId: String, rawLrcOrPlain: String) = withContext(Dispatchers.IO) {
        val trimmed = rawLrcOrPlain.trim()
        if (trimmed.isBlank()) return@withContext
        dao.upsertLyrics(
            LyricsEntity(
                songId = songId,
                rawContent = trimmed,
                isSynced = containsLrcTimestamps(trimmed),
                offsetMs = 0L,
                sourceName = LyricSource.MANUAL.name
            )
        )
    }

    suspend fun adjustLyricsOffset(songId: String, newOffsetMs: Long) = withContext(Dispatchers.IO) {
        dao.updateLyricsOffset(songId, newOffsetMs.coerceIn(-10_000L, 10_000L))
    }

    private fun loadLocalOrEmbeddedFallback(song: Song): SongLyrics? {
        val raw = song.embeddedLyrics?.takeIf { it.isNotBlank() } ?: return null
        return parseLrcToDomain(
            songId = song.id,
            rawContent = raw,
            offsetMs = 0L,
            source = if (song.isDemoTrack) LyricSource.DEMO else LyricSource.EMBEDDED
        )
    }

    companion object {
        private val lineTimestampRegex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?]""")
        private val wordTimestampRegex = Regex("""<(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?>([^\s<]+)""")

        fun containsLrcTimestamps(raw: String): Boolean {
            return lineTimestampRegex.containsMatchIn(raw)
        }

        fun parseLrcToDomain(
            songId: String,
            rawContent: String,
            offsetMs: Long,
            source: LyricSource
        ): SongLyrics {
            val parsedLines = mutableListOf<LyricLine>()
            val isSynced = containsLrcTimestamps(rawContent)

            if (isSynced) {
                rawContent.lines().forEach { rawLine ->
                    val trimmed = rawLine.trim()
                    val match = lineTimestampRegex.find(trimmed)
                    if (match != null) {
                        val lineTimeMs = parseMinutesSecondsMillis(
                            minStr = match.groupValues[1],
                            secStr = match.groupValues[2],
                            fracStr = match.groupValues[3]
                        )
                        val afterBracket = trimmed.substring(match.range.last + 1).trim()
                        val words = mutableListOf<LyricWord>()
                        wordTimestampRegex.findAll(afterBracket).forEach { wMatch ->
                            val wordTimeMs = parseMinutesSecondsMillis(
                                minStr = wMatch.groupValues[1],
                                secStr = wMatch.groupValues[2],
                                fracStr = wMatch.groupValues[3]
                            )
                            words.add(LyricWord(timestampMs = wordTimeMs, text = wMatch.groupValues[4]))
                        }

                        val cleanText = afterBracket
                            .replace(Regex("""<\d{1,2}:\d{2}(?:\.\d{1,3})?>"""), "")
                            .replace(Regex("""\s+"""), " ")
                            .trim()

                        if (cleanText.isNotBlank()) {
                            parsedLines.add(
                                LyricLine(
                                    timestampMs = lineTimeMs,
                                    text = cleanText,
                                    words = words
                                )
                            )
                        }
                    }
                }
                parsedLines.sortBy { it.timestampMs }
            } else {
                rawContent.lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .forEachIndexed { idx, text ->
                        parsedLines.add(
                            LyricLine(
                                timestampMs = idx * 4000L,
                                text = text,
                                words = emptyList()
                            )
                        )
                    }
            }

            return SongLyrics(
                songId = songId,
                rawContent = rawContent,
                isSynced = isSynced,
                lines = parsedLines,
                offsetMs = offsetMs,
                source = source
            )
        }

        private fun parseMinutesSecondsMillis(minStr: String, secStr: String, fracStr: String): Long {
            val minutes = minStr.toLongOrNull() ?: 0L
            val seconds = secStr.toLongOrNull() ?: 0L
            val millis = when (fracStr.length) {
                1 -> (fracStr.toLongOrNull() ?: 0L) * 100L
                2 -> (fracStr.toLongOrNull() ?: 0L) * 10L
                3 -> fracStr.toLongOrNull() ?: 0L
                else -> 0L
            }
            return (minutes * 60_000L) + (seconds * 1000L) + millis
        }
    }
}
