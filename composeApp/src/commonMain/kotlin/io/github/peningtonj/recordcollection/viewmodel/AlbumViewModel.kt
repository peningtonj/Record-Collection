package io.github.peningtonj.recordcollection.viewmodel

import androidx.lifecycle.ViewModel
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.domain.Album
import io.github.peningtonj.recordcollection.db.domain.Tag
import io.github.peningtonj.recordcollection.db.domain.TagType
import io.github.peningtonj.recordcollection.repository.AlbumTagRepository
import io.github.peningtonj.recordcollection.repository.AlbumRepository
import io.github.peningtonj.recordcollection.repository.CollectionAlbumRepository
import io.github.peningtonj.recordcollection.repository.OnAddToCollection
import io.github.peningtonj.recordcollection.repository.RatingRepository
import io.github.peningtonj.recordcollection.repository.SettingsRepository
import io.github.peningtonj.recordcollection.repository.TagRepository
import io.github.peningtonj.recordcollection.service.TagService
import io.github.peningtonj.recordcollection.usecase.GetAlbumDetailUseCase
import io.github.peningtonj.recordcollection.ui.models.AlbumDetailUiState
import io.github.peningtonj.recordcollection.usecase.ReleaseGroupUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

class AlbumViewModel (
    private val albumRepository: AlbumRepository,
    private val ratingRepository: RatingRepository,
    private val collectionAlbumRepository: CollectionAlbumRepository,
    private val tagService: TagService,
    private val releaseGroupUseCase: ReleaseGroupUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _releaseGroupStatus = MutableStateFlow(ReleaseGroupStatus.Idle)
    val releaseGroupStatus = _releaseGroupStatus.asStateFlow()

    fun setRating(albumId: String, rating: Int) = launchSafely("setRating($albumId)") {
        ratingRepository.addRating(albumId, rating)
    }

    fun addTagToAlbum(albumId: String, tagKey: String, tagValue: String) = launchSafely("addTagToAlbum($albumId)") {
        tagService.addTagToAlbum(albumId, tagKey, tagValue)
    }

    fun removeTagFromAlbum(albumId: String, tagId: String) = launchSafely("removeTagFromAlbum($albumId)") {
        tagService.removeTagFromAlbum(albumId, tagId)
    }

    fun updateReleaseGroup(album: Album) = launchSafely(
        operation = "updateReleaseGroup(${album.id})",
        onError = { _releaseGroupStatus.value = ReleaseGroupStatus.Idle },
    ) {
        _releaseGroupStatus.value = ReleaseGroupStatus.Updating
        val release = releaseGroupUseCase.getReleaseFromAlbum(album)
        val releaseGroupId = release?.releaseGroup?.id

        if (releaseGroupId == null) {
            Napier.d { "Release group ID is null" }
            _releaseGroupStatus.value = ReleaseGroupStatus.Idle
            return@launchSafely
        }

        albumRepository.updateReleaseGroupId(album.id, release.releaseGroup.id)
        delay(1000L) // Wait a second because of rate limiting
        val releases = releaseGroupUseCase.getReleases(releaseGroupId)
        val albums = releaseGroupUseCase.searchAlbumsFromReleaseGroup(
            releases.distinctBy {
                Pair(it.title, it.disambiguation)
            },
            album.primaryArtist
        )
        Napier.d { "Setting the release group $releaseGroupId to ${albums.joinToString(", ") {it.name}}" }
        releaseGroupUseCase.updateAlbums(releaseGroupId, albums)
        _releaseGroupStatus.value = ReleaseGroupStatus.Idle
    }

    fun addAlbumToCollection(album: Album, collectionName: String, addToLibraryOverrideValue: Boolean? = null) =
        launchSafely("addAlbumToCollection(${album.id} -> $collectionName)") {
            val settings = settingsRepository.settings.first()
            val appDefault = settings.defaultOnAddToCollection
            val addToLibrary = addToLibraryOverrideValue ?: settings.collectionAddToLibrary.getOrDefault(collectionName, OnAddToCollection.DEFAULT).value ?: appDefault

            Napier.d { "Adding album ${album.id} to collection $collectionName" }
            val existingAlbum = albumRepository.getAlbumByNameAndArtistIfPresent(album.name, album.primaryArtist).first()
            if (existingAlbum == null) {
                albumRepository.saveAlbum(album, addToLibrary)
                collectionAlbumRepository.addAlbumToCollection(collectionName, album.id)

            } else {
                if (addToLibraryOverrideValue ?: false) {
                    albumRepository.addAlbumToLibrary(album.id)
                }

                collectionAlbumRepository.addAlbumToCollection(collectionName, existingAlbum.id)
            }
        }

    fun removeAlbumFromCollection(album: Album, collectionName: String) =
        launchSafely("removeAlbumFromCollection(${album.id})") {
            collectionAlbumRepository.removeAlbumFromCollection(collectionName, album.id)
        }

}

sealed interface AlbumScreenUiState {
    data object Loading : AlbumScreenUiState
    data class Error(val message: String) : AlbumScreenUiState
    data class Success(
        val albumDetail: AlbumDetailUiState,
    ) : AlbumScreenUiState
}

enum class ReleaseGroupStatus {
    Idle,
    Updating,
}
