package io.github.peningtonj.recordcollection.events.handlers

import io.github.peningtonj.recordcollection.events.AlbumEvent
import io.github.peningtonj.recordcollection.service.TagService
import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.repository.ArtistRepository
import io.github.peningtonj.recordcollection.repository.TagRepository
import kotlinx.coroutines.flow.first

class AlbumProcessingHandler(
    private val tagService: TagService,
    private val albumTagRepository: AlbumTagRepository,
    private val tagRepository: TagRepository,
    private val artistRepository: ArtistRepository,
    private val spotifyApi: SpotifyApi
) : AlbumEventHandler {

    override suspend fun handle(event: AlbumEvent) {
        when (event) {
            is AlbumEvent.AlbumAdded -> {
                processAlbumComplete(event.album, event.replaceArtist)
            }
            is AlbumEvent.AlbumUpdated -> {
                processAlbumComplete(event.album)
            }
            is AlbumEvent.AlbumDeleted -> {
                // Could implement cleanup logic here if needed
            }
        }
    }

    private suspend fun processAlbumComplete(album: Album, replaceArtist : Boolean = false) {
        try {
            Napier.d("Processing album: ${album.name}")

            // Step 1: Process Artists
            val artists = processArtistsForAlbum(album, replaceArtist)

            // Step 2: Process Tags (using the artist data)
            processAlbumTags(album, artists)

            Napier.d("Completed processing album: ${album.name}")

        } catch (e: Exception) {
            Napier.e("Error processing album ${album.name}", e)
        }
    }

    private suspend fun processArtistsForAlbum(album: Album, replace: Boolean = false): List<Artist> {
        val processedArtists = mutableListOf<Artist>()

        try {
            Napier.d("Processing artists for album: ${album.name}")

            album.artists.forEach { artistDto ->
                val existingArtist = artistRepository.getArtistById(artistDto.id).first()

                if (existingArtist == null || replace) {
                    // Fetch full artist data from Spotify API
                    val artist = artistRepository.fetchArtistWithEnhancedGenres(artistDto.id)
                    artistRepository.saveArtist(artist.getOrThrow())
                    // Get the saved artist
                    val savedArtist = artistRepository.getArtistById(artistDto.id).first()
                    savedArtist?.let { processedArtists.add(it) }
                    Napier.d("Saved artist: ${savedArtist?.name}")

                } else {
                    processedArtists.add(existingArtist)
                    Napier.d("Artist ${artistDto.name} already exists in database")
                }
            }

        } catch (e: Exception) {
            Napier.e("Error processing artists for album ${album.name}", e)
        }

        return processedArtists
    }
    private fun processAlbumTags(album: Album, artists: List<Artist>) {
        try {
            Napier.d("Generating tags for album: ${album.name}")
            val tags = tagService.generateTagsForAlbum(album, artists)

            tags.forEach { tag ->
                albumTagRepository.addTagToAlbum(album.id, tag.id)
                tagRepository.insertTag(tag)
            }
            Napier.d("Processed ${tags.size} tags for album: ${album.name}")
        } catch (e: Exception) {
            Napier.e("Failed to process tags for album ${album.id}", e)
        }
    }
}
