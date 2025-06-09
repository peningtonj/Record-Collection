package io.github.peningtonj.recordcollection.network.oauth

actual class SpotifyAuthHandler {
    actual suspend fun authenticate(): String {
        // Will implement using Chrome Custom Tabs
        return "token"
    }
}