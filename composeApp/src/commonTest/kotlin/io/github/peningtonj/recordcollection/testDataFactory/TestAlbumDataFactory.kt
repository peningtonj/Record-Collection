package io.github.peningtonj.recordcollection.testDataFactory

import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.AlbumType
import io.github.peningtonj.recordcollection.db.domain.Image
import io.github.peningtonj.recordcollection.db.domain.SimplifiedArtist
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.AlbumTypeDto
import io.github.peningtonj.recordcollection.network.spotify.model.ImageDto
import io.github.peningtonj.recordcollection.network.spotify.model.ReleaseDatePrecision
import io.github.peningtonj.recordcollection.network.spotify.model.SavedAlbumDto
import io.github.peningtonj.recordcollection.network.spotify.model.SimplifiedArtistDto
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime

object TestAlbumDataFactory {
    fun albumDto(
        id: String = "test-album-id",
        name: String = "Test Album",
        artists: List<SimplifiedArtistDto> = listOf(TestSimplifiedArtistFactory.simplifiedAristDto()),
        releaseDate: String = "2023-01-01",
        totalTracks: Int = 10,
        uri: String = "spotify:album:$id",
        albumType: AlbumTypeDto = AlbumTypeDto.ALBUM,
        images: List<ImageDto> = listOf(TestImageDataFactory.imageDto()),
        externalIds: Map<String, String> = mapOf("upc" to "123456789012"),
    ): AlbumDto = AlbumDto(
        id = id,
        name = name,
        albumType = albumType,
        artists = artists,
        totalTracks = totalTracks,
        releaseDate = releaseDate,
        releaseDatePrecision = ReleaseDatePrecision.DAY,
        uri = uri,
        externalUrls = emptyMap(),
        externalIds = externalIds,
        images = images,
        popularity = 50,
        label = "Test Label",
        copyrights = emptyList(),
        availableMarkets = listOf("AU"),
        href = "http://linkToResult.com",
        restrictions = null,
        type = "album",
        genres = emptyList(),
        tracks = null
    )

    @OptIn(ExperimentalTime::class)
    fun album(
        id: String = "test-album-id",
        name: String = "Test Album",
        artists: List<SimplifiedArtist> = listOf(TestSimplifiedArtistFactory.simplifiedArtist()),
        releaseDate: LocalDate = LocalDate(2023, 1, 1),
        totalTracks: Int = 10,
        spotifyUri: String = "spotify:album:$id",
        albumType: AlbumType = AlbumType.ALBUM,
        images: List<Image> = listOf(TestImageDataFactory.image()),
        externalIds: Map<String, String> = mapOf("upc" to "123456789012"),
        inLibrary: Boolean = true,
        addedAt: Instant = Clock.System.now(),
        releaseGroupId: String? = null,
    ): Album = Album(
        id = id,
        name = name,
        artists = artists,
        releaseDate = releaseDate,
        totalTracks = totalTracks,
        spotifyUri = spotifyUri,
        albumType = albumType,
        images = images,
        externalIds = externalIds,
        inLibrary = inLibrary,
        addedAt = addedAt,
        releaseGroupId = releaseGroupId,
        primaryArtist = artists.first().name
    )

    fun savedAlbumDto(
        album: AlbumDto = albumDto(),
        savedAt: Instant = Clock.System.now()
    ) : SavedAlbumDto {
        return SavedAlbumDto(
            addedAt = savedAt.toString(),
            album = album
        )
    }
}