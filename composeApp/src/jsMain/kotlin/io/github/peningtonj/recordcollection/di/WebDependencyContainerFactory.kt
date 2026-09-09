package io.github.peningtonj.recordcollection.di

import com.russhwolf.settings.StorageSettings
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.db.FirebaseDriver
import io.github.peningtonj.recordcollection.db.ensureAnonymousAuth
import io.github.peningtonj.recordcollection.di.container.DependencyContainer
import io.github.peningtonj.recordcollection.di.container.ModularDependencyContainer
import io.github.peningtonj.recordcollection.di.module.ProductionSettingsModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionEventModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionFirebaseModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionNetworkModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionRepositoryModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionUseCaseModule
import io.github.peningtonj.recordcollection.network.httpClientEngine
import io.github.peningtonj.recordcollection.network.oauth.spotify.WebAuthHandler
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

object WebDependencyContainerFactory {

    /**
     * There's no `runBlocking` on JS, so anonymous auth is kicked off in the background by
     * [ensureAnonymousAuthAsync] instead — the Login screen doesn't touch Firestore, and
     * it completes well before the user finishes the Spotify redirect.
     */
    fun create(): DependencyContainer {
        FirebaseDriver().initializeFirebase()

        val authClient = HttpClient(httpClientEngine) {
            install(ContentNegotiation) { json() }
        }

        return ModularDependencyContainer(
            networkModule = ProductionNetworkModule(),
            repositoryModule = ProductionRepositoryModule(),
            authHandler = WebAuthHandler(authClient),
            useCaseModule = ProductionUseCaseModule(),
            eventModule = ProductionEventModule(),
            settingsModule = ProductionSettingsModule(StorageSettings()),
            firebaseModule = ProductionFirebaseModule(),
        )
    }

    suspend fun ensureAnonymousAuthAsync() {
        runCatching { ensureAnonymousAuth() }
            .onFailure { Napier.e("Anonymous Firebase auth failed", it) }
    }
}
