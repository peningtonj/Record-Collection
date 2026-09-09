package io.github.peningtonj.recordcollection

import kotlinx.browser.window

class WebPlatform : Platform {
    override val name: String = "Web (${window.navigator.userAgent})"
}

actual fun getPlatform(): Platform = WebPlatform()
