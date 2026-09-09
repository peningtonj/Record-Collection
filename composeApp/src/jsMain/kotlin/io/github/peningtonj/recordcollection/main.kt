@file:OptIn(
    org.jetbrains.compose.resources.ExperimentalResourceApi::class,
    coil3.annotation.ExperimentalCoilApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package io.github.peningtonj.recordcollection

import androidx.compose.ui.window.ComposeViewport
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.di.WebDependencyContainerFactory
import io.github.peningtonj.recordcollection.navigation.WebNavigator
import io.ktor.client.HttpClient
import kotlinx.browser.document
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

fun main() {
    Napier.base(DebugAntilog())

    val dependencies = WebDependencyContainerFactory.create()
    val navigator = WebNavigator()

    // No runBlocking on JS — sign in anonymously in the background; the Login screen
    // doesn't hit Firestore and this finishes long before the Spotify redirect completes.
    MainScope().launch { WebDependencyContainerFactory.ensureAnonymousAuthAsync() }

    ComposeViewport(document.body!!) {
        setSingletonImageLoaderFactory { context ->
            ImageLoader.Builder(context)
                .components { add(KtorNetworkFetcherFactory(httpClient = { HttpClient() })) }
                .build()
        }
        App(dependencies, navigator)
    }
}
