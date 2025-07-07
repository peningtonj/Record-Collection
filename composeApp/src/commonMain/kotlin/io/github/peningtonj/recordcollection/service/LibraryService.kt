package io.github.peningtonj.recordcollection.services

import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.ArtistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class LibraryService(
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository
) {
    fun getAllAlbumsWithGenres(): Flow<List<Album>> = combine(
        albumRepository.getAllAlbums(),
        artistRepository.getAllArtists()
    ) { albums, artists ->
        val artistGenreMap = artists.associate { it.id to it.genres }

        albums.map { album ->
            val genres = album.artists.firstOrNull()?.id?.let { artistId ->
                artistGenreMap[artistId]
            } ?: emptyList()

            album.copy(genres = genres)
        }
    }

    suspend fun syncLibraryData() {
        albumRepository.syncSavedAlbums()
        val albums = albumRepository.getAllAlbums().first()
        val artists = artistRepository.getArtistsForAlbums(albums)
        artistRepository.saveArtists(artists)
    }
}
