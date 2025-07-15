package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.Albums
import io.github.peningtonj.recordcollection.db.GetCollectionsForAlbum
import io.github.peningtonj.recordcollection.db.SelectAlbumsInCollection
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.db.domain.AlbumCollectionInfo
import io.github.peningtonj.recordcollection.db.domain.CollectionAlbum
import kotlinx.datetime.Instant

object CollectionAlbumMapper {
    fun toAlbumWithPosition(joinResult: SelectAlbumsInCollection): CollectionAlbum {
        return CollectionAlbum(
            collectionName = joinResult.name, // You'll need to pass this in or get it from context
            album = AlbumMapper.toDomain(joinResult.toAlbumEntity()),
            position = joinResult.position.toInt(),
            addedAt = Instant.Companion.fromEpochSeconds(joinResult.c_added_at)
        )
    }

    fun toDomain(result: GetCollectionsForAlbum): AlbumCollectionInfo {
        return AlbumCollectionInfo(
            collection = AlbumCollection(
                name = result.name,
                description = result.description,
                createdAt = Instant.fromEpochSeconds(result.created_at),
                updatedAt = Instant.fromEpochSeconds(result.updated_at)
            ),
            position = result.position.toInt(),
            addedAt = Instant.fromEpochSeconds(result.added_at)
        )
    }
    private fun SelectAlbumsInCollection.toAlbumEntity(): Albums {
        return Albums(
            id = this.id,
            name = this.name,
            primary_artist = this.primary_artist,
            artists = this.artists,
            release_date = this.release_date,
            total_tracks = this.total_tracks,
            spotify_uri = this.spotify_uri,
            added_at = this.added_at,
            album_type = this.album_type,
            images = this.images,
            updated_at = this.updated_at,
            in_library = this.in_library
        )
    }


}