package io.github.peningtonj.recordcollection.service

import androidx.compose.ui.text.capitalize
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.db.domain.Tag
import io.github.peningtonj.recordcollection.db.domain.TagType

class TagService {
    fun generateTagsForAlbum(album: Album, artists: List<Artist>): List<Tag> {
        // Your tag generation logic here
        val tags = mutableListOf<Tag>()

        // Example: Generate tags based on release year
        val albumType = album.albumType
        tags.add(Tag(key = "Album Type", value = albumType.name, type = TagType.METADATA))

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
}

fun String.capitalizeWords(): String = split(" ").joinToString(" ") { it.replaceFirstChar { firstChar -> firstChar.uppercase() } }

