// composeApp/src/commonMain/kotlin/io/github/peningtonj/recordcollection/di/module/EventModule.kt
package io.github.peningtonj.recordcollection.di.module

import io.github.peningtonj.recordcollection.events.AlbumEventDispatcher
import io.github.peningtonj.recordcollection.events.handlers.AlbumEventHandler
import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.repository.ArtistRepository
import io.github.peningtonj.recordcollection.repository.TagRepository
import io.github.peningtonj.recordcollection.service.TagService
import kotlinx.coroutines.CoroutineScope

interface EventModule {
    fun provideTagService(
        tagRepository: TagRepository,
        albumTagRepository: AlbumTagRepository
    ): TagService
    
    fun provideAlbumEventHandlers(
        tagService: TagService,
        albumTagRepository: AlbumTagRepository,
        tagRepository: TagRepository,
        artistRepository: ArtistRepository,
        spotifyApi: SpotifyApi
    ): List<AlbumEventHandler>
    
    fun provideAlbumEventDispatcher(
        handlers: List<AlbumEventHandler>,
        scope: CoroutineScope
    ): AlbumEventDispatcher
}