package io.github.peningtonj.recordcollection.db.mapper

import io.github.peningtonj.recordcollection.db.domain.Image
import io.github.peningtonj.recordcollection.network.spotify.model.ImageDto

object ImageMapper {
    fun toDomain(entity: ImageDto): Image {
        return Image(
            url = entity.url,
            height = entity.height,
            width = entity.width
        )
    }

}