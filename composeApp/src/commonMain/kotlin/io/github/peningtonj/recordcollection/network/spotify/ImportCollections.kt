import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import kotlinx.serialization.json.*

class PlaylistAlbumExtractor(
    private val client: HttpClient
) {
    suspend fun getPlaylists(): Result<List<Playlist>> = runCatching {
        val playlistIds = mutableSetOf<Playlist>()

        val playlistsJson = client.get("${SpotifyApi.BASE_URL}/me/playlists").body<JsonElement>()
        val playlistsArray = playlistsJson.jsonObject["items"]?.jsonArray

        playlistsArray?.mapNotNull { playlistElement ->
            val playlist = playlistElement.jsonObject
            val description = playlist["description"]?.jsonPrimitive?.contentOrNull

            if (description?.contains("albums", ignoreCase = true) == true) {
                val playlistId = playlist["id"]?.jsonPrimitive?.content
                val playlistName = playlist["name"]?.jsonPrimitive?.content
                playlistIds.add(Playlist(id = playlistId ?: "hmmm", name = playlistName ?: "oh no"))
            }
        }

        playlistIds.toList()
    }



    suspend fun extractAlbumsFromPlaylist(playlist: Playlist): List<AlbumResult> {
        val albums = mutableSetOf<AlbumResult>()
        var offset = 0
        val limit = 50

        Napier.d { "Extracting albums from playlist ${playlist.name}" }
        do {
            val url = URLBuilder("${SpotifyApi.BASE_URL}/playlists/${playlist.id}/tracks").apply {
                parameters.append("limit", limit.toString())
                parameters.append("offset", offset.toString())
            }.buildString()

            val response = client.get(url).body<JsonElement>()
            val items = response.jsonObject["items"]?.jsonArray

            Napier.d { "Found ${items?.size} items" }

            items?.forEach { item ->
                val track = item.jsonObject["track"]?.jsonObject
                val album = track?.get("album")?.jsonObject

                if (album != null) {
                    val albumId = album["id"]?.jsonPrimitive?.content ?: return@forEach
                    val albumName = album["name"]?.jsonPrimitive?.content ?: return@forEach
                    val releaseDate = album["release_date"]?.jsonPrimitive?.content ?: ""

                    val artists = album["artists"]?.jsonArray?.mapNotNull { artistElement ->
                        artistElement.jsonObject["name"]?.jsonPrimitive?.content
                    } ?: emptyList()

                    val imageUrl = album["images"]?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.content

                    albums.add(
                        AlbumResult(
                            id = albumId,
                            name = albumName,
                            artists = artists,
                            releaseDate = releaseDate,
                            imageUrl = imageUrl
                        )
                    )
                }
            }

            val nextUrl = response.jsonObject["next"]?.jsonPrimitive?.contentOrNull
            offset += limit
        } while (nextUrl != null)

        return albums.toList()
    }
}

data class AlbumResult(
    val id: String,
    val name: String,
    val artists: List<String>,
    val releaseDate: String,
    val imageUrl: String?
)

data class Playlist(
    val name: String,
    val id: String
)