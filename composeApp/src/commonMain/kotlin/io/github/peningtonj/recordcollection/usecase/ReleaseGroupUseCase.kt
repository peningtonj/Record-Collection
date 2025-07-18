package io.github.peningtonj.recordcollection.usecase

import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.network.miscApi.model.Release
import io.github.peningtonj.recordcollection.network.spotify.model.SearchType
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.SearchRepository
import kotlinx.coroutines.delay

class ReleaseGroupUseCase(
    private val albumRepository: AlbumRepository,
    private val searchRepository: SearchRepository,
) {

    suspend fun getReleaseFromAlbum(album: Album): Release? {
        Napier.d { "Fetching release group id for  ${album.name}" }
        return albumRepository.fetchReleaseGroupId(album).getOrNull()?.releases?.first()
    }
    suspend fun getReleases(releaseGroupId: String): List<Release> {
            Napier.d { "Fetched release group $releaseGroupId" }
            albumRepository.fetchReleaseGroup(releaseGroupId).onSuccess { releaseGroup ->
                Napier.d { "Fetched release group with ${releaseGroup.size} albums" }
                return releaseGroup
            }
        return emptyList()
    }

    suspend fun searchAlbumsFromReleaseGroup(releases: List<Release>, artist: String): List<Album> {
        Napier.d { "Searching releases: ${releases.joinToString("\n") {"${it.title} ${it.disambiguation}"}}"}
        return releases.distinctBy { release ->
            Pair(release.title, release.disambiguation)
        }.mapNotNull { release ->
            Napier.d { "Searching Album ${release.title} ${release.disambiguation} $artist" }
            val results = searchRepository.searchSpotify(
                "${release.title} ${release.disambiguation} $artist",
                listOf(SearchType.ALBUM)
            ).albums.orEmpty()
            Napier.d { "Results: ${results.take(5).joinToString(", ") { it.name }}" }
            Napier.d { "First result: ${results.first().name} | ${results.first().artists.first().name}" }

            val exactMatch = results.filter {
                (it.name.lowercase() == (release.title.lowercase()) ||
                it.name.lowercase() == "${release.title.lowercase()} (${release.disambiguation?.lowercase()})") &&
                it.artists.map { artist -> artist.name }.contains(artist)
            }

            if (exactMatch.isNotEmpty()){
                val result = exactMatch.first()
                Napier.d { "Found exact match ${result.name}" }
                result
            } else {
                val result = results.firstOrNull {
                    it.name.contains(release.title) &&
                    it.artists.map { it.name }.contains(artist)
                }
                Napier.d { "Found other match ${result?.name}" }
                result
            }
        }
    }

    fun updateAlbums(releaseGroupId: String, albums: List<Album>) {
        albums.forEach { album ->
            Napier.d { "Updating Album ${album.name}" }
            if (albumRepository.albumExists(album.id)) {
                albumRepository.updateReleaseGroupId(album.id, releaseGroupId)
            } else {
                albumRepository.saveAlbum(album.copy(releaseGroupId=releaseGroupId))
            }
        }
    }
}

