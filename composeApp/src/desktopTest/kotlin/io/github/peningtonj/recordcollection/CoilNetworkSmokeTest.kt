package io.github.peningtonj.recordcollection

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Smoke test for the coil3 image pipeline used by every `AsyncImage` in the UI
 * (`coil-compose` + `coil-network-okhttp`). Guards the TECH_DEBT 1.10 version bump
 * that moved `coil-network-okhttp` from `3.0.0-alpha07` to `3.2.0` so it matches
 * `coil-core`. Fetches a real Spotify album-art URL and asserts it decodes.
 *
 * Requires network; if the CDN is unreachable the test is skipped rather than failed.
 */
class CoilNetworkSmokeTest {

    private val albumArtUrl =
        "https://i.scdn.co/image/ab67616d0000b273e8b066f70c206551210d902b"

    @Test
    fun `coil fetches and decodes a remote album-art image`() = runBlocking {
        val ctx = PlatformContext.INSTANCE
        val loader = ImageLoader.Builder(ctx).build()

        val result = loader.execute(ImageRequest.Builder(ctx).data(albumArtUrl).build())

        if (result is ErrorResult) {
            val msg = result.throwable.message ?: ""
            val networkDown = result.throwable is java.io.IOException ||
                msg.contains("Unable to resolve host") ||
                msg.contains("timeout", ignoreCase = true)
            if (networkDown) {
                println("CoilNetworkSmokeTest skipped — CDN unreachable: $msg")
                return@runBlocking
            }
            throw AssertionError("coil failed to load $albumArtUrl", result.throwable)
        }

        assertTrue(result is SuccessResult, "expected SuccessResult, got ${result::class.simpleName}")
        val image = (result as SuccessResult).image
        assertTrue(
            image.width > 0 && image.height > 0,
            "decoded image has no size: ${image.width}x${image.height}",
        )
        println("CoilNetworkSmokeTest: decoded ${image.width}x${image.height} from $albumArtUrl")
    }
}
