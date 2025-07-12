package io.github.peningtonj.recordcollection.di

import io.github.peningtonj.recordcollection.db.DatabaseDriver
import io.github.peningtonj.recordcollection.di.container.DependencyContainer
import io.github.peningtonj.recordcollection.di.module.impl.ProductionDatabaseModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionNetworkModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionRepositoryModule
import io.github.peningtonj.recordcollection.di.container.ModularDependencyContainer
import io.github.peningtonj.recordcollection.di.module.impl.ProductionEventModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionUseCaseModule
import io.github.peningtonj.recordcollection.network.oauth.spotify.DesktopAuthHandler
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

object DependencyContainerFactory {
    fun create(): DependencyContainer {
        // Create a basic HTTP client for auth operations
        val authClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json()
            }
        }
        
        val databaseModule = ProductionDatabaseModule(DatabaseDriver())
        val networkModule = ProductionNetworkModule()
        val repositoryModule = ProductionRepositoryModule()
        val useCaseModule = ProductionUseCaseModule()
        val eventModule = ProductionEventModule()
        
        // Create the container first
        val container = ModularDependencyContainer(
            networkModule = networkModule,
            databaseModule = databaseModule,
            repositoryModule = repositoryModule,
            useCaseModule = useCaseModule,
            authHandler = DesktopAuthHandler(authClient),
            eventModule = eventModule
        )
        
        // Now initialize the auth handler with the repository

        return container
    }
}