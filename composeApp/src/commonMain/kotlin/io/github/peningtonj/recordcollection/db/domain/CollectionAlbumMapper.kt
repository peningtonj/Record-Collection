package io.github.peningtonj.recordcollection.db.domain

import io.github.peningtonj.recordcollection.db.Albums
import io.github.peningtonj.recordcollection.db.Collection_albums
import io.github.peningtonj.recordcollection.db.SelectAlbumsInCollection
import io.github.peningtonj.recordcollection.db.mapper.AlbumMapper
import kotlinx.datetime.Instant

object CollectionAlbumMapper {
    fun toAlbumWithPosition(joinResult: SelectAlbumsInCollection): CollectionAlbum {
        return CollectionAlbum(
            id = 0, // or get from collection_albums if needed
            collectionId = 0, // You'll need to pass this in or get it from context
            album = AlbumMapper.toDomain(joinResult.toAlbumEntity()),
            position = joinResult.position.toInt(),
            addedAt = Instant.fromEpochSeconds(joinResult.c_added_at)
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
            updated_at = this.updated_at
        )
    }


}