package io.github.peningtonj.recordcollection.di.module.impl

import io.github.peningtonj.recordcollection.di.module.EventModule
import io.github.peningtonj.recordcollection.events.AlbumEventDispatcher
import io.github.peningtonj.recordcollection.events.handlers.AlbumEventHandler
import io.github.peningtonj.recordcollection.events.handlers.AlbumProcessingHandler
import io.github.peningtonj.recordcollection.db.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.network.spotify.SpotifyApi
import io.github.peningtonj.recordcollection.repository.ArtistRepository
import io.github.peningtonj.recordcollection.repository.TagRepository
import io.github.peningtonj.recordcollection.service.TagService
import kotlinx.coroutines.CoroutineScope

class ProductionEventModule : EventModule {
    
    override fun provideTagService(): TagService {
        return TagService()
    }
    
    override fun provideAlbumEventHandlers(
        tagService: TagService,
        albumTagRepository: AlbumTagRepository,
        tagRepository: TagRepository,
        artistRepository: ArtistRepository,
        spotifyApi: SpotifyApi
    ): List<AlbumEventHandler> {
        return listOf(
            AlbumProcessingHandler(tagService, albumTagRepository, tagRepository, artistRepository, spotifyApi)
        )
    }
    
    override fun provideAlbumEventDispatcher(
        handlers: List<AlbumEventHandler>,
        scope: CoroutineScope
    ): AlbumEventDispatcher {
        return AlbumEventDispatcher(handlers, scope)
    }
}