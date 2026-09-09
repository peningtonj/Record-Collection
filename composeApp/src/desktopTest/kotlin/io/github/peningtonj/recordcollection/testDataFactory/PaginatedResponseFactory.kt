package io.github.peningtonj.recordcollection.testDataFactory

import io.github.peningtonj.recordcollection.network.spotify.model.PaginatedResponse

class PaginatedResponseFactory {
    fun <T> createPaginatedResponseFromList(list: List<T>, pageSize: Int): PaginatedResponse<T> {
        return PaginatedResponse(
            href = "http://linkToResult.com",
            items = list.take(pageSize),
            total = pageSize,
            limit = pageSize,
            offset = 0,
            next = null,
            previous = null
        )
    }
}