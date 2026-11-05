package com.example.data.online

import com.example.core.database.PlayMixDao
import com.example.core.database.toEntity
import com.example.core.datastore.UserPreferences
import com.example.core.datastore.UserPreferencesRepository
import com.example.domain.model.OnlineMusicAlbum
import com.example.domain.model.OnlineMusicArtist
import com.example.domain.model.OnlineMusicPlaylist
import com.example.domain.model.OnlineProviderId
import com.example.domain.model.OnlineProviderInfo
import com.example.domain.model.Song
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
import java.util.concurrent.TimeUnit

// --- OFFICIAL ITUNES AUTHORIZED CATALOG DTOs ---
@JsonClass(generateAdapter = true)
data class ITunesSearchResponse(
    @Json(name = "resultCount") val resultCount: Int = 0,
    @Json(name = "results") val results: List<ITunesTrackDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ITunesTrackDto(
    @Json(name = "trackId") val trackId: Long? = null,
    @Json(name = "trackName") val trackName: String? = null,
    @Json(name = "artistName") val artistName: String? = null,
    @Json(name = "collectionName") val collectionName: String? = null,
    @Json(name = "previewUrl") val previewUrl: String? = null,
    @Json(name = "artworkUrl100") val artworkUrl100: String? = null,
    @Json(name = "trackTimeMillis") val trackTimeMillis: Long? = null,
    @Json(name = "primaryGenreName") val primaryGenreName: String? = null,
    @Json(name = "releaseDate") val releaseDate: String? = null,
    @Json(name = "trackNumber") val trackNumber: Int? = null
)

interface ITunesCatalogApi {
    @GET("search")
    suspend fun searchSongs(
        @Query("term") term: String,
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 25
    ): ITunesSearchResponse
}

// --- OFFICIAL JAMENDO CREATIVE COMMONS API DTOs ---
@JsonClass(generateAdapter = true)
data class JamendoTracksResponse(
    @Json(name = "results") val results: List<JamendoTrackDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class JamendoTrackDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "artist_name") val artistName: String? = null,
    @Json(name = "album_name") val albumName: String? = null,
    @Json(name = "duration") val durationSec: Int? = null,
    @Json(name = "audio") val audioStreamUrl: String? = null,
    @Json(name = "image") val imageUrl: String? = null
)

interface JamendoApi {
    @GET("v3.0/tracks")
    suspend fun searchTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("namesearch") query: String? = null,
        @Query("order") order: String = "popularity_week"
    ): JamendoTracksResponse
}

data class OnlineCatalogSnapshot(
    val providers: List<OnlineProviderInfo> = emptyList(),
    val trendingTracks: List<Song> = emptyList(),
    val newReleaseTracks: List<Song> = emptyList(),
    val popularArtists: List<OnlineMusicArtist> = emptyList(),
    val featuredAlbums: List<OnlineMusicAlbum> = emptyList(),
    val onlinePlaylists: List<OnlineMusicPlaylist> = emptyList(),
    val searchResults: List<Song> = emptyList(),
    val isLoading: Boolean = false,
    val networkMessage: String? = null
) {
    val trendingSongs: List<Song> get() = trendingTracks
    val newReleaseSongs: List<Song> get() = newReleaseTracks
    val curatedPlaylists: List<OnlineMusicPlaylist> get() = onlinePlaylists
}

class OnlineMusicRepository(
    private val dao: PlayMixDao,
    private val prefsRepo: UserPreferencesRepository
) {
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "PlayMixMusicPlayer/2.0.0 (Android)")
                    .build()
                chain.proceed(req)
            }
            .build()
    }

    private val iTunesApi: ITunesCatalogApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ITunesCatalogApi::class.java)
    }

    private val jamendoApi: JamendoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.jamendo.com/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(JamendoApi::class.java)
    }

    val recentlyPlayedOnlineFlow: Flow<List<Song>> = dao.observeAllSongs().map { entities ->
        entities
            .filter { it.onlineProviderId != null && it.lastPlayedAt > 0L }
            .sortedByDescending { it.lastPlayedAt }
            .map { it.toDomain() }
    }

    fun getInternetArchiveCuratedStreams(): List<Song> = listOf(
        Song(
            id = "ia_open_synth_01",
            uri = "https://archive.org/download/Mythium/JLS_ATI.mp3",
            title = "All This Is (Open Archive)",
            artist = "Joe L.'s Studio",
            album = "Mythium Open Audio",
            albumArtist = "Joe L.'s Studio",
            genre = "Ambient Rock",
            durationMs = 148_000L,
            year = 2024,
            folderName = "Internet Archive Open Audio",
            onlineProviderId = OnlineProviderId.INTERNET_ARCHIVE.name
        ),
        Song(
            id = "ia_open_synth_02",
            uri = "https://archive.org/download/Mythium/BSFM_TF.mp3",
            title = "The Forsaken (Open Archive)",
            artist = "Broad Stand",
            album = "Mythium Open Audio",
            albumArtist = "Broad Stand",
            genre = "Instrumental",
            durationMs = 212_000L,
            year = 2024,
            folderName = "Internet Archive Open Audio",
            onlineProviderId = OnlineProviderId.INTERNET_ARCHIVE.name
        ),
        Song(
            id = "ia_open_synth_03",
            uri = "https://archive.org/download/Mythium/BSAT_ATKM.mp3",
            title = "All The King's Men (Open Archive)",
            artist = "Broad Stand",
            album = "Mythium Open Audio",
            albumArtist = "Broad Stand",
            genre = "Acoustic Indie",
            durationMs = 195_000L,
            year = 2024,
            folderName = "Internet Archive Open Audio",
            onlineProviderId = OnlineProviderId.INTERNET_ARCHIVE.name
        )
    )

    fun buildProviderList(prefs: UserPreferences): List<OnlineProviderInfo> {
        val enabledSet = prefs.enabledOnlineProvidersCsv
            .split(",")
            .map { it.trim() }
            .toSet()

        return listOf(
            OnlineProviderInfo(
                id = OnlineProviderId.ITUNES_AUTHORIZED,
                name = "Apple iTunes Authorized Previews",
                description = "Official public Apple iTunes Search API providing metadata, high-resolution artwork, and authorized 30-second AAC preview streams.",
                endpointBaseUrl = "https://itunes.apple.com/search",
                requiresCredentials = false,
                subscriptionRequirement = "Free Public API (No account or API key required)",
                isEnabled = enabledSet.contains(OnlineProviderId.ITUNES_AUTHORIZED.name),
                hasValidCredentials = true,
                credentialSetupInstructions = "Ready out of the box using Apple's public search & preview endpoint."
            ),
            OnlineProviderInfo(
                id = OnlineProviderId.INTERNET_ARCHIVE,
                name = "Internet Archive Open Audio",
                description = "Authorized public-domain & Creative Commons studio recordings and live concerts hosted on archive.org.",
                endpointBaseUrl = "https://archive.org/download/",
                requiresCredentials = false,
                subscriptionRequirement = "Free Open-Access Audio Library (No API key required)",
                isEnabled = enabledSet.contains(OnlineProviderId.INTERNET_ARCHIVE.name),
                hasValidCredentials = true,
                credentialSetupInstructions = "Ready out of the box for open-licensed archive.org streams."
            ),
            OnlineProviderInfo(
                id = OnlineProviderId.JAMENDO_CC,
                name = "Jamendo Independent Music API",
                description = "Full-length independent artist tracks licensed under Creative Commons via Jamendo REST API v3.0.",
                endpointBaseUrl = "https://api.jamendo.com/v3.0/tracks",
                requiresCredentials = true,
                subscriptionRequirement = "Free Jamendo Developer Client ID required",
                isEnabled = enabledSet.contains(OnlineProviderId.JAMENDO_CC.name),
                hasValidCredentials = prefs.jamendoClientId.isNotBlank(),
                credentialSetupInstructions = "Register a free app at https://devportal.jamendo.com to obtain your 8-character Client ID and enter it in Provider Settings."
            ),
            OnlineProviderInfo(
                id = OnlineProviderId.SPOTIFY_WEB_API,
                name = "Spotify Web API Connector",
                description = "Official Spotify Web API metadata & authorized catalog integration.",
                endpointBaseUrl = "https://api.spotify.com/v1/",
                requiresCredentials = true,
                subscriptionRequirement = "Spotify Developer Client ID & Premium OAuth Token required",
                isEnabled = enabledSet.contains(OnlineProviderId.SPOTIFY_WEB_API.name),
                hasValidCredentials = prefs.spotifyClientId.isNotBlank(),
                credentialSetupInstructions = "Create an application at https://developer.spotify.com/dashboard and configure your Spotify Client ID."
            ),
            OnlineProviderInfo(
                id = OnlineProviderId.SOUNDCLOUD_API,
                name = "SoundCloud Official API",
                description = "Authorized SoundCloud developer API for independent creator streams.",
                endpointBaseUrl = "https://api.soundcloud.com/",
                requiresCredentials = true,
                subscriptionRequirement = "SoundCloud OAuth 2.1 Client ID required",
                isEnabled = enabledSet.contains(OnlineProviderId.SOUNDCLOUD_API.name),
                hasValidCredentials = prefs.soundCloudClientId.isNotBlank(),
                credentialSetupInstructions = "Register at https://soundcloud.com/you/apps and provide your SoundCloud Client ID."
            )
        )
    }

    suspend fun fetchOnlineDiscoveryCatalog(): OnlineCatalogSnapshot = withContext(Dispatchers.IO) {
        val prefs = prefsRepo.getSnapshot()
        val providers = buildProviderList(prefs)
        val enabledIds = providers.filter { it.isReadyForStreaming }.map { it.id }.toSet()

        if (enabledIds.isEmpty()) {
            return@withContext OnlineCatalogSnapshot(
                providers = providers,
                networkMessage = "No active online providers enabled. Enable a provider or configure credentials in Provider Settings."
            )
        }

        val trendingList = mutableListOf<Song>()
        val newReleasesList = mutableListOf<Song>()
        var errorNote: String? = null

        if (enabledIds.contains(OnlineProviderId.INTERNET_ARCHIVE)) {
            trendingList.addAll(getInternetArchiveCuratedStreams())
        }

        // 1. Query Jamendo if configured and enabled
        if (enabledIds.contains(OnlineProviderId.JAMENDO_CC) && prefs.jamendoClientId.isNotBlank()) {
            runCatching {
                val resp = jamendoApi.searchTracks(clientId = prefs.jamendoClientId, limit = 12)
                trendingList.addAll(resp.results.mapNotNull { it.toDomainSong() })
            }.onFailure {
                errorNote = "Jamendo API warning: verify Client ID or network."
            }
        }

        // 2. Query official Apple iTunes Authorized Previews if enabled
        if (enabledIds.contains(OnlineProviderId.ITUNES_AUTHORIZED)) {
            runCatching {
                val trendingResp = iTunesApi.searchSongs(term = "electronic synthwave ambient", limit = 16)
                trendingList.addAll(trendingResp.results.mapNotNull { it.toDomainSong() })

                val newResp = iTunesApi.searchSongs(term = "indie pop electronic 2025", limit = 14)
                newReleasesList.addAll(newResp.results.mapNotNull { it.toDomainSong() })
            }.onFailure { err ->
                errorNote = "Online catalog currently unreachable (${err.localizedMessage ?: "Offline"}). Open Archive & local music playback remain available."
            }
        }

        val distinctTrending = trendingList.distinctBy { it.id }
        val distinctNew = newReleasesList.distinctBy { it.id }.ifEmpty { distinctTrending.reversed() }

        val artists = (distinctTrending + distinctNew)
            .groupBy { it.artist }
            .values
            .take(10)
            .map { group ->
                val first = group.first()
                OnlineMusicArtist(
                    id = "online_artist_${first.artist.hashCode()}",
                    name = first.artist,
                    genre = first.genre,
                    artworkUrl = first.albumArtUri,
                    providerId = OnlineProviderId.ITUNES_AUTHORIZED,
                    trackCount = group.size
                )
            }

        val albums = (distinctTrending + distinctNew)
            .groupBy { it.album }
            .values
            .take(10)
            .map { group ->
                val first = group.first()
                OnlineMusicAlbum(
                    id = "online_album_${first.album.hashCode()}",
                    title = first.album,
                    artist = first.artist,
                    artworkUrl = first.albumArtUri,
                    providerId = OnlineProviderId.ITUNES_AUTHORIZED,
                    releaseYear = first.year,
                    trackCount = group.size
                )
            }

        val playlists = if (distinctTrending.isNotEmpty()) {
            listOf(
                OnlineMusicPlaylist(
                    id = "online_pl_synth",
                    title = "Global Electronic & Synth Discovery",
                    subtitle = "Authorized streaming previews via Media3 ExoPlayer",
                    providerId = OnlineProviderId.ITUNES_AUTHORIZED,
                    tracks = distinctTrending.take(8)
                ),
                OnlineMusicPlaylist(
                    id = "online_pl_fresh",
                    title = "Fresh Indie & Open Archive Radar",
                    subtitle = "Curated new releases across connected providers",
                    providerId = OnlineProviderId.INTERNET_ARCHIVE,
                    tracks = distinctNew.take(8)
                )
            )
        } else emptyList()

        OnlineCatalogSnapshot(
            providers = providers,
            trendingTracks = distinctTrending,
            newReleaseTracks = distinctNew,
            popularArtists = artists,
            featuredAlbums = albums,
            onlinePlaylists = playlists,
            isLoading = false,
            networkMessage = errorNote
        )
    }

    suspend fun searchOnlineProviders(query: String): Result<List<Song>> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isBlank()) return@withContext Result.success(emptyList())

        val prefs = prefsRepo.getSnapshot()
        val providers = buildProviderList(prefs)
        val enabledIds = providers.filter { it.isReadyForStreaming }.map { it.id }.toSet()

        if (enabledIds.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("No online providers are currently active. Enable Apple iTunes Previews, Internet Archive, or configure Jamendo credentials.")
            )
        }

        runCatching {
            val combined = mutableListOf<Song>()
            if (enabledIds.contains(OnlineProviderId.INTERNET_ARCHIVE)) {
                val qLower = clean.lowercase()
                combined.addAll(
                    getInternetArchiveCuratedStreams().filter {
                        it.title.lowercase().contains(qLower) ||
                            it.artist.lowercase().contains(qLower) ||
                            it.genre.lowercase().contains(qLower)
                    }
                )
            }
            if (enabledIds.contains(OnlineProviderId.JAMENDO_CC) && prefs.jamendoClientId.isNotBlank()) {
                runCatching {
                    val jamendoRes = jamendoApi.searchTracks(
                        clientId = prefs.jamendoClientId,
                        query = clean,
                        limit = 15
                    )
                    combined.addAll(jamendoRes.results.mapNotNull { it.toDomainSong() })
                }
            }
            if (enabledIds.contains(OnlineProviderId.ITUNES_AUTHORIZED)) {
                val iTunesRes = iTunesApi.searchSongs(term = clean, limit = 25)
                combined.addAll(iTunesRes.results.mapNotNull { it.toDomainSong() })
            }
            combined.distinctBy { it.id }
        }
    }

    suspend fun cacheOnlineSongForPlayback(song: Song) = withContext(Dispatchers.IO) {
        val existing = dao.getSongById(song.id)
        if (existing == null) {
            dao.upsertSong(song.copy(isHidden = true, lastPlayedAt = System.currentTimeMillis()).toEntity())
        } else {
            dao.incrementPlayCount(song.id, System.currentTimeMillis())
        }
    }

    private fun ITunesTrackDto.toDomainSong(): Song? {
        val streamUrl = previewUrl?.takeIf { it.startsWith("http") } ?: return null
        val idVal = trackId ?: streamUrl.hashCode().toLong()
        val highResArt = artworkUrl100?.replace("100x100bb", "600x600bb") ?: artworkUrl100
        val yearVal = releaseDate?.take(4)?.toIntOrNull() ?: 2025
        return Song(
            id = "itunes_$idVal",
            mediaStoreId = 0L,
            uri = streamUrl,
            title = trackName?.takeIf { it.isNotBlank() } ?: "Untitled Stream",
            artist = artistName?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
            album = collectionName?.takeIf { it.isNotBlank() } ?: "Single",
            albumArtist = artistName ?: "Unknown Artist",
            genre = primaryGenreName ?: "Online Stream",
            durationMs = 30_000L, // Authorized 30s preview stream
            trackNumber = trackNumber ?: 1,
            year = yearVal,
            albumArtUri = highResArt,
            filePath = streamUrl,
            folderName = "Apple iTunes Authorized Stream",
            fileSize = 0L,
            isDemoTrack = false,
            onlineProviderId = OnlineProviderId.ITUNES_AUTHORIZED.name
        )
    }

    private fun JamendoTrackDto.toDomainSong(): Song? {
        val streamUrl = audioStreamUrl?.takeIf { it.startsWith("http") } ?: return null
        val trackId = id ?: streamUrl.hashCode().toString()
        return Song(
            id = "jamendo_$trackId",
            mediaStoreId = 0L,
            uri = streamUrl,
            title = name?.takeIf { it.isNotBlank() } ?: "Jamendo Track",
            artist = artistName?.takeIf { it.isNotBlank() } ?: "Independent Artist",
            album = albumName?.takeIf { it.isNotBlank() } ?: "Jamendo CC",
            albumArtist = artistName ?: "Independent Artist",
            genre = "Creative Commons",
            durationMs = ((durationSec ?: 180) * 1000L).coerceAtLeast(1000L),
            trackNumber = 1,
            year = 2025,
            albumArtUri = imageUrl,
            filePath = streamUrl,
            folderName = "Jamendo CC Stream",
            fileSize = 0L,
            isDemoTrack = false,
            onlineProviderId = OnlineProviderId.JAMENDO_CC.name
        )
    }
}
