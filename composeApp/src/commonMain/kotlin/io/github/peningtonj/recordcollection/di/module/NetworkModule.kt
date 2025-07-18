
package io.github.peningtonj.recordcollection.di.module

import io.github.peningtonj.recordcollection.network.miscApi.MiscApi
import io.github.peningtonj.recordcollection.network.openAi.OpenAiApi
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.repository.SpotifyAuthRepository
import io.ktor.client.HttpClient

interface NetworkModule {
    fun provideHttpClient(): HttpClient
    fun provideSpotifyApi(authRepository: SpotifyAuthRepository): SpotifyApi
    fun provideOpenAiApi(): OpenAiApi
    fun provideMiscApi(): MiscApi
    fun close()
}