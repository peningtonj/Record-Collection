package io.github.peningtonj.recordcollection.service

import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.repository.AlbumCollectionRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository

class CollectionsService(
    private val collectionAlbumRepository : CollectionAlbumRepository,
    private val albumCollectionRepository: AlbumCollectionRepository
) {
    fun createCollectionFromAlbums(albums: List<Album>, name: String) {
        albumCollectionRepository.createCollection(name)
        albums.forEach { album ->
            collectionAlbumRepository.addAlbumToCollection(
                collectionName = name,
                albumId = album.id
            )
        }
    }
}