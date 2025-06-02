package io.github.peningtonj.recordcollection

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform