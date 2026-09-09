package io.github.peningtonj.recordcollection.testDataFactory

import io.github.peningtonj.recordcollection.db.domain.Image
import io.github.peningtonj.recordcollection.network.spotify.model.ImageDto

object TestImageDataFactory {
    fun imageDto(
        url: String = "https://i.scdn.co/image/ab67616d00001e02ff9ca10b55ce82ae553c8228",
        height: Int = 300,
        width: Int = 300
    ) : ImageDto {
        return ImageDto(
            url,
            height,
            width
        )
    }

    fun image(
        url: String = "https://i.scdn.co/image/ab67616d00001e02ff9ca10b55ce82ae553c8228",
        height: Int = 300,
        width: Int = 300
    ) : Image {
        return Image(
            url,
            height,
            width
        )
    }
}