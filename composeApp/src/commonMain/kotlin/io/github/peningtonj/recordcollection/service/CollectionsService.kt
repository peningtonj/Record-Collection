package io.github.peningtonj.recordcollection.service

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.filter.AlbumFilter
import io.github.peningtonj.recordcollection.repository.AlbumCollectionRepository
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository

class CollectionsService(
    private val collectionAlbumRepository : CollectionAlbumRepository,
    private val albumCollectionRepository: AlbumCollectionRepository,
    private val albumRepository: AlbumRepository,
    ) {

    fun createCollectionFromFilter(filter: AlbumFilter, name: String) {
        albumCollectionRepository.saveFilterCollection(name, filter)
    }
    fun createCollectionFromAlbums(albums: List<Album>, name: String) {
        albumCollectionRepository.createCollection(name)
        albums.forEach { album ->
            collectionAlbumRepository.addAlbumToCollection(
                collectionName = name,
                albumId = album.id
            )
        }
    }

    suspend fun import() {
        val playlists = albumRepository.import()
        playlists.forEach { (playlist, albumIds) ->
            Napier.d { "Importing playlist: ${playlist.name}"}
            val albums = albumRepository.fetchMultipleAlbums(albumIds.map { it.id })
            albumCollectionRepository.createCollection(playlist.name)
            albums.onSuccess { response ->
                response.forEach { album ->
                    collectionAlbumRepository.addAlbumToCollection(
                        collectionName = playlist.name,
                        albumId = album.id
                    )
                }
            }
        }
    }
}