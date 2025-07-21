package io.github.peningtonj.recordcollection.util

import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Artist
import io.github.peningtonj.recordcollection.db.domain.SearchResult
import io.github.peningtonj.recordcollection.db.domain.Track
import io.github.peningtonj.recordcollection.network.spotify.model.*
import kotlin.math.max
import kotlin.math.min

object SearchRankingUtils {

    // Main ranking function that combines multiple scoring methods
    fun rankSearchResults(
        results: SearchResult,
        query: String
    ): RankedSearchResults {
        val normalizedQuery = query.lowercase().trim()

        val rankedAlbums = results.albums?.map { album ->
            RankedAlbum(album, calculateAlbumRelevance(album, normalizedQuery))
        }?.sortedByDescending { it.relevanceScore } ?: emptyList()

        val rankedArtists = results.artists?.map { artist ->
            RankedArtist(artist, calculateArtistRelevance(artist, normalizedQuery))
        }?.sortedByDescending { it.relevanceScore } ?: emptyList()

        val rankedTracks = results.tracks?.map { track ->
            RankedTrack(track, calculateTrackRelevance(track, normalizedQuery))
        }?.sortedByDescending { it.relevanceScore } ?: emptyList()

        return RankedSearchResults(rankedAlbums, rankedArtists, rankedTracks)
    }

    private fun calculateAlbumRelevance(album: Album, query: String): Double {
        var score = 0.0
        val albumName = album.name.lowercase()
        val artistNames = album.artists.map { it.name.lowercase() }

        // Exact match gets highest score
        if (albumName == query) score += 100.0

        // Starts with query gets high score
        if (albumName.startsWith(query)) score += 80.0

        // Contains query gets medium score
        if (albumName.contains(query)) score += 50.0

        // Artist name matches
        artistNames.forEach { artistName ->
            if (artistName == query) score += 90.0
            else if (artistName.startsWith(query)) score += 70.0
            else if (artistName.contains(query)) score += 40.0
        }

        // Fuzzy matching for typos/partial matches
        score += fuzzyMatch(albumName, query) * 30.0
        artistNames.forEach { artistName ->
            score += fuzzyMatch(artistName, query) * 25.0
        }

        return score
    }

    private fun calculateArtistRelevance(artist: Artist, query: String): Double {
        var score = 0.0
        val artistName = artist.name.lowercase()

        // Exact match gets highest score
        if (artistName == query) score += 100.0

        // Starts with query gets high score
        if (artistName.startsWith(query)) score += 80.0

        // Contains query gets medium score
        if (artistName.contains(query)) score += 50.0

        // Fuzzy matching
        score += fuzzyMatch(artistName, query) * 30.0

        // Boost popular artists (if popularity data available)
        // artist.popularity?.let { score += it * 0.1 }

        return score
    }

    private fun calculateTrackRelevance(track: Track, query: String): Double {
        var score = 0.0
        val trackName = track.name.lowercase()
        val albumName = track.album?.name?.lowercase() ?: ""
        val artistNames = track.artists.map { it.name.lowercase() }

        // Track name matches
        if (trackName == query) score += 100.0
        else if (trackName.startsWith(query)) score += 80.0
        else if (trackName.contains(query)) score += 50.0

        // Album name matches
        if (albumName == query) score += 70.0
        else if (albumName.startsWith(query)) score += 50.0
        else if (albumName.contains(query)) score += 30.0

        // Artist name matches
        artistNames.forEach { artistName ->
            if (artistName == query) score += 90.0
            else if (artistName.startsWith(query)) score += 70.0
            else if (artistName.contains(query)) score += 40.0
        }

        // Fuzzy matching
        score += fuzzyMatch(trackName, query) * 30.0
        score += fuzzyMatch(albumName, query) * 20.0
        artistNames.forEach { artistName ->
            score += fuzzyMatch(artistName, query) * 25.0
        }

        // Boost popular tracks (if popularity data available)
        // track.popularity?.let { score += it * 0.1 }

        return score
    }

    // Simple fuzzy matching using Levenshtein distance
    private fun fuzzyMatch(text: String, query: String): Double {
        if (text.isEmpty() || query.isEmpty()) return 0.0

        val distance = levenshteinDistance(text, query)
        val maxLength = max(text.length, query.length)

        // Convert distance to similarity score (0-1)
        return max(0.0, 1.0 - (distance.toDouble() / maxLength))
    }

    // Levenshtein distance algorithm
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[s1.length][s2.length]
    }

    // Advanced: Word-based matching for multi-word queries
    fun calculateWordBasedRelevance(text: String, query: String): Double {
        val textWords = text.lowercase().split("\\s+".toRegex())
        val queryWords = query.lowercase().split("\\s+".toRegex())

        if (queryWords.isEmpty()) return 0.0

        var totalScore = 0.0
        var matchedWords = 0

        queryWords.forEach { queryWord ->
            var bestWordScore = 0.0

            textWords.forEach { textWord ->
                val wordScore = when {
                    textWord == queryWord -> 1.0
                    textWord.startsWith(queryWord) -> 0.8
                    textWord.contains(queryWord) -> 0.6
                    else -> fuzzyMatch(textWord, queryWord) * 0.4
                }
                bestWordScore = max(bestWordScore, wordScore)
            }

            if (bestWordScore > 0.3) { // Threshold for considering a word "matched"
                totalScore += bestWordScore
                matchedWords++
            }
        }

        // Penalty for not matching all query words
        val completenessBonus = matchedWords.toDouble() / queryWords.size
        return (totalScore / queryWords.size) * completenessBonus
    }
}

// Data classes for ranked results
data class RankedSearchResults(
    val albums: List<RankedAlbum>,
    val artists: List<RankedArtist>,
    val tracks: List<RankedTrack>
)

data class RankedAlbum(
    val album: Album,
    val relevanceScore: Double
)

data class RankedArtist(
    val artist: Artist,
    val relevanceScore: Double
)

data class RankedTrack(
    val track: Track,
    val relevanceScore: Double
)

// Extension function for easy use
fun SearchResult.rankByRelevance(query: String): RankedSearchResults {
    return SearchRankingUtils.rankSearchResults(this, query)
}