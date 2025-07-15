package io.github.peningtonj.recordcollection.service

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.filter.AlbumFilter
import io.github.peningtonj.recordcollection.db.domain.filter.DateRange
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.ArtistRepository
import io.github.peningtonj.recordcollection.repository.RatingRepository
import io.github.peningtonj.recordcollection.ui.models.AlbumDisplayData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class LibraryService(
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val ratingRepository: RatingRepository
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
        getAllAlbumsEnriched().map()
     { albums ->
        filterAlbums(albums, filter)
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

    suspend fun syncLibraryData() {
        Napier.d("Starting library sync")

        albumRepository.syncSavedAlbums()

        Napier.d("Library sync completed")
    }

}

data class LibraryStats(
    val totalAlbums: Int,
    val uniqueArtists: Int,
    val genreDistribution: Map<String, Int>,
    val decadeDistribution: Map<String, Int>
)
