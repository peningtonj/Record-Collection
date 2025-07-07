package io.github.peningtonj.recordcollection.di.container

import io.github.peningtonj.recordcollection.db.DatabaseDriver
import io.github.peningtonj.recordcollection.di.module.impl.ProductionDatabaseModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionNetworkModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionRepositoryModule
import io.github.peningtonj.recordcollection.network.oauth.spotify.AuthHandler

object DependencyContainerFactory {
    fun create(
        databaseDriver: DatabaseDriver,
        authHandler: AuthHandler
    ): DependencyContainer {
        return ModularDependencyContainer(
            networkModule = ProductionNetworkModule(),
            databaseModule = ProductionDatabaseModule(databaseDriver),
            repositoryModule = ProductionRepositoryModule(),
            authHandler = authHandler
        )
    }
}