package io.github.peningtonj.recordcollection.service

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.db.domain.filter.AlbumFilter
import io.github.peningtonj.recordcollection.db.domain.filter.DateRange
import io.github.peningtonj.recordcollection.db.mapper.AlbumMapper
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.ArtistRepository
import io.github.peningtonj.recordcollection.repository.ProfileRepository
import io.github.peningtonj.recordcollection.repository.RatingRepository
import io.github.peningtonj.recordcollection.repository.SettingsRepository
import io.github.peningtonj.recordcollection.repository.TrackRepository
import io.github.peningtonj.recordcollection.repository.SortOrder
import io.github.peningtonj.recordcollection.ui.models.AlbumDisplayData
import io.github.peningtonj.recordcollection.viewmodel.LibraryDifferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class LibraryService(
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val ratingRepository: RatingRepository,
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val trackRepository: TrackRepository
) {
    // Core data operations
    fun getAllAlbumsEnriched(): Flow<List<AlbumDisplayData>> = combine(
        albumRepository.getAllAlbumsInLibrary(),
        artistRepository.getAllArtists(),
        ratingRepository.getAllRatings()
    ) { albums, artists, ratings ->
        val artistGenreMap = artists.associate { it.id to it.genres }
        val ratingsMap = ratings.associate { it.albumId to it.rating }
        enrichAlbumsWithGenres(albums, artistGenreMap).map {
            AlbumDisplayData(it, 0, ratingsMap[it.id] ?: 0)
        }
    }


    fun getFilteredAlbums(filter: AlbumFilter): Flow<List<AlbumDisplayData>> =
        getAllAlbumsEnriched().map { albums ->
            val filtered = filterAlbums(albums, filter)

            when (settingsRepository.settings.value.defaultSortOrder) {
                SortOrder.ARTIST_NAME -> filtered.sortedBy { it.album.primaryArtist.lowercase() }
                SortOrder.ALBUM_NAME -> filtered.sortedBy { it.album.name.lowercase() }
                SortOrder.RELEASE_DATE -> filtered.sortedByDescending { it.album.releaseDate }
                SortOrder.DATE_ADDED -> filtered.sortedByDescending { it.album.addedAt }
                SortOrder.RATING -> filtered.sortedByDescending { it.rating }
            }
        }

    // Library statistics
    fun getLibraryStats(): Flow<LibraryStats> = combine(
        albumRepository.getLibraryCount(),
        albumRepository.getAllAlbums(),
        artistRepository.getAllGenres()
    ) { count, albums, genres ->
        LibraryStats(
            totalAlbums = count.toInt(),
            uniqueArtists = albums.map { it.primaryArtist }.distinct().size,
            genreDistribution = genres.groupingBy { it }.eachCount(),
            decadeDistribution = calculateDecadeDistribution(albums)
        )
    }

    // Helper functions
    private fun enrichAlbumsWithGenres(
        albums: List<Album>,
        artistGenreMap: Map<String, List<String>>
    ): List<Album> = albums.map { album ->
        val genres = album.artists.firstOrNull()?.id?.let { artistId ->
            artistGenreMap[artistId]
        } ?: emptyList()
        album.copy(genres = genres)
    }

    private fun filterAlbums(albums: List<AlbumDisplayData>, albumFilter: AlbumFilter): List<AlbumDisplayData> {
        Napier.d("Filtering ${albums.size} albums with filter: $albumFilter")

        return albums.filter { album ->
            matchesDateRange(album.album, albumFilter.releaseDateRange) &&
                    matchesTags(album.album, albumFilter.tags) &&
                    matchesRating(album, albumFilter.minRating)
        }
    }

    private fun matchesDateRange(album: Album, dateRange: DateRange?): Boolean {
        if (dateRange == null) return true

        return when {
            dateRange.start != null && album.releaseDate <= dateRange.start -> false
            dateRange.end != null && album.releaseDate >= dateRange.end -> false
            else -> true
        }
    }

    private fun matchesTags(album: Album, tags: Map<String, List<String>>): Boolean {
        return tags.all { (category, values) ->
            when (category) {
                "Artist" -> matchesArtist(album, values)
                "Genre" -> matchesGenre(album, values)
                else -> true
            }
        }
    }

    private fun matchesRating(album: AlbumDisplayData, minRating: Int?): Boolean {
        if (minRating == null) return true

        return album.rating >= minRating
    }

    private fun matchesArtist(album: Album, artists: List<String>): Boolean {
        val albumArtists = album.artists.map { it.name.lowercase() }
        return artists.any { selectedArtist ->
            albumArtists.any { it.contains(selectedArtist.lowercase()) }
        }
    }

    private fun matchesGenre(album: Album, genres: List<String>): Boolean {
        val albumGenres = album.genres.map { it.lowercase() }
        return genres.any { selectedGenre ->
            albumGenres.any { it.contains(selectedGenre.lowercase()) }
        }
    }

    private fun calculateDecadeDistribution(albums: List<Album>): Map<String, Int> {
        return albums
            .groupBy { album -> "${(album.releaseDate.year / 10) * 10}s" }
            .mapValues { it.value.size }
    }

    suspend fun getLibraryDifferences(): LibraryDifferences {
        Napier.d("Starting library sync")

        // Get album differences (existing code)
        val userSavedAlbums = profileRepository.fetchUserSavedAlbums().map { AlbumMapper.toDomain(it.album) }
        val localLibrary = albumRepository.getAllAlbumsInLibrary().first()

        val localDuplicates = identifyDuplicates(localLibrary)
        val spotifyDuplicates = identifyDuplicates(userSavedAlbums)

        val cleanedLocal = localLibrary - localDuplicates
        val cleanedUserSavedAlbums = userSavedAlbums - spotifyDuplicates

        val spotifyIds = cleanedUserSavedAlbums.mapTo(HashSet()) { it.id }
        val inBoth = cleanedLocal.count { it.id in spotifyIds }


        return LibraryDifferences(
            localCount = cleanedLocal.size,
            spotifyCount = cleanedUserSavedAlbums.size,
            onlyInLocal = cleanedLocal.size - inBoth,
            onlyInSpotify = cleanedUserSavedAlbums.size - inBoth,
            inBoth = inBoth,
            userSavedAlbums = cleanedUserSavedAlbums,
            localLibrary = cleanedLocal,
            localDuplicates = localDuplicates,
            userSavedAlbumsDuplicates = spotifyDuplicates,
        )
    }

    fun identifyDuplicates(albums: List<Album>) : List<Album> {
        return albums
            .groupBy { it.name to it.primaryArtist }
            .filterValues { it.size > 1 }
            .values
            .flatMap { duplicateGroup ->
                duplicateGroup
                    .sortedByDescending { it.addedAt }
                    .drop(1)
            }

    }
    suspend fun batchArtistsThenSaveLocalAlbums(albums: List<Album>) {
        val artists = albums.map { it.artists.first() }.distinct()
        artistRepository.fetchArtistsWithEnhancedGenres(artists.map { it.id }, true)
        albums.forEach { album ->
            albumRepository.saveAlbumIfNotPresent(album)
            albumRepository.addAlbumToLibrary(album.id)
        }
    }

    suspend fun applySync(differences: LibraryDifferences, action: SyncAction, removeDuplicates: Boolean = true) {
        // Remove duplicates if requested
        if (removeDuplicates) {
            Napier.d { "Removing duplicates" }
            Napier.d { "Removing ${differences.localDuplicates.size} from local" }
            differences.localDuplicates.forEach { album ->
                albumRepository.removeAlbumFromLibrary(album.id)
            }
            Napier.d { "Removing ${differences.userSavedAlbumsDuplicates.size} from spotify" }
            profileRepository.removeAlbumsFromSpotifyLibrary(differences.userSavedAlbumsDuplicates)
        }

        // Pre-compute sets for efficient lookups
        val localIds = differences.localLibrary.mapTo(HashSet()) { it.id }
        val spotifyIds = differences.userSavedAlbums.mapTo(HashSet()) { it.id }

        // Pre-compute album groups for reuse
        val localOnlyAlbums = differences.localLibrary.filter { it.id !in spotifyIds }
        val spotifyOnlyAlbums = differences.userSavedAlbums.filter { it.id !in localIds }

        when (action) {
            SyncAction.Combine -> {
                // Add Spotify-only albums to local library
                Napier.d { "Saving spotify only albums: ${spotifyOnlyAlbums.size}" }
                batchArtistsThenSaveLocalAlbums(spotifyOnlyAlbums)

                // Add local-only albums to Spotify library
                profileRepository.addAlbumsToSpotifyLibrary(localOnlyAlbums)
            }

            SyncAction.Intersection -> {
                Napier.d { "Removing albums that are only in local library: ${localOnlyAlbums.size}" }
                localOnlyAlbums.forEach { album ->
                    albumRepository.removeAlbumFromLibrary(album.id)
                }

                // Remove albums that are only in Spotify library
                profileRepository.removeAlbumsFromSpotifyLibrary(spotifyOnlyAlbums)
            }

            SyncAction.UseLocal -> {
                profileRepository.removeAlbumsFromSpotifyLibrary(spotifyOnlyAlbums)
                profileRepository.addAlbumsToSpotifyLibrary(localOnlyAlbums)
                
                // Don't sync saved tracks in this case as we're using local only
            }

            SyncAction.UseSpotify -> {
                Napier.d { "Removing albums that are only in local library: ${localOnlyAlbums.size}" }
                localOnlyAlbums.forEach { album ->
                    albumRepository.removeAlbumFromLibrary(album.id)
                }

                Napier.d { "Saving spotify only albums: ${spotifyOnlyAlbums.size}" }
                batchArtistsThenSaveLocalAlbums(spotifyOnlyAlbums)

            }
        }
    }

    fun addAlbumToLibrary(album: Album) {
        albumRepository.addAlbumToLibrary(album.id)
    }

    fun removeAlbumFromLibrary(album: Album) {
        albumRepository.removeAlbumFromLibrary(album.id)
    }

    suspend fun updateLibraryTracksFromSpotify() {
        val remoteSavedTracks = trackRepository.fetchLibraryTracks()
        val localSavedTracks = trackRepository.getSavedTracks().first()

        val onlyLocal = localSavedTracks - remoteSavedTracks
        val onlyRemote = remoteSavedTracks - localSavedTracks

        Napier.d { "Updating library tracks from Spotify: ${onlyLocal.size} local, ${onlyRemote.size} remote" }

        onlyLocal.forEach { track ->
            trackRepository.removeTrackFromLibrary(track.id)
        }

        onlyRemote.forEach { track ->
            trackRepository.addTrackToLibrary(track.id)
        }
    }

    suspend fun saveTrackLocalAndRemote(trackId: String) {
        trackRepository.saveTracksRemote(listOf(trackId))
        trackRepository.addTrackToLibrary(trackId)
    }
    suspend fun removeTrackLocalAndRemote(trackId: String) {
        trackRepository.removeTracksRemote(listOf(trackId))
        trackRepository.removeTrackFromLibrary(trackId)
    }

    suspend fun addAllSongsFromAlbumToSavedSongs(album: Album) {
        val tracks = trackRepository.getTracksForAlbum(album.id).first()
        Napier.d { "Adding ${tracks.size} tracks to saved songs" }
        trackRepository.saveTracksRemote(
            tracks.map { it.id }
        )
        trackRepository.saveTracksLocalAndRemote(
            tracks.map { it.id }
        )

    }

}

data class LibraryStats(
    val totalAlbums: Int,
    val uniqueArtists: Int,
    val genreDistribution: Map<String, Int>,
    val decadeDistribution: Map<String, Int>
)


sealed class SyncAction {
    object Combine : SyncAction()
    object UseSpotify : SyncAction()
    object UseLocal : SyncAction()
    object Intersection : SyncAction()
}