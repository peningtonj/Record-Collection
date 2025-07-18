package io.github.peningtonj.recordcollection.service

import androidx.compose.ui.text.capitalize
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.db.domain.Tag
import io.github.peningtonj.recordcollection.db.domain.TagType
import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.repository.TagRepository
import kotlinx.coroutines.launch

class TagService(
    private val tagRepository: TagRepository,
    private val albumTagRepository: AlbumTagRepository,
) {
    fun generateTagsForAlbum(album: Album, artists: List<Artist>): List<Tag> {
        // Your tag generation logic here
        val tags = mutableListOf<Tag>()

        // Example: Generate tags based on release year
        val albumType = album.albumType
        tags.add(Tag(key = "Album Type", value = albumType?.name ?: "", type = TagType.METADATA))

        Napier.d { "Adding tags for album: ${album.name} with artists: ${artists.map { it.name }}" }
        artists.forEach { artist ->
            Napier.d { "Adding tags for artist: ${artist.name}" }
            Napier.d { "Genres for artist: ${artist.genres}" }
            artist.genres.forEach { genre ->
                tags.add(Tag(key = "Genre", value = genre.capitalizeWords(), type = TagType.METADATA))
            }
        }
        return tags
    }

    fun removeTagFromAlbum(albumId: String, tagId: String) {
        try {
            albumTagRepository.removeTagFromAlbum(albumId, tagId)
            Napier.d { "Removed tag $tagId from album $albumId" }
        } catch (e: Exception) {
            Napier.e(e) { "Error removing tag from album" }
        }
    }

    fun addTagToAlbum(albumId: String, tagKey: String, tagValue: String) {
        try {
            val newTag = Tag(
                key = tagKey,
                value = tagValue,
                type = TagType.USER // Adjust based on your TagType enum
            )

            // Insert the tag first
            tagRepository.insertTag(newTag)

            // Then add it to the album
            albumTagRepository.addTagToAlbum(albumId, newTag.id)

            Napier.d { "Added tag ${newTag.key}:${newTag.value} to album $albumId" }
        } catch (e: Exception) {
            Napier.e(e) { "Error adding tag to album" }
        }
    }
}



fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { firstChar -> firstChar.uppercase() } }

