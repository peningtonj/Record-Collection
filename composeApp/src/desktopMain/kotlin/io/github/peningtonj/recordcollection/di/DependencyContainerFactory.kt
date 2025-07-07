package io.github.peningtonj.recordcollection.di

import io.github.peningtonj.recordcollection.db.DatabaseDriver
import io.github.peningtonj.recordcollection.di.container.DependencyContainer
import io.github.peningtonj.recordcollection.di.container.DependencyContainerFactory
import io.github.peningtonj.recordcollection.network.oauth.spotify.DesktopAuthHandler

object DependencyContainerFactory {
    fun create(): DependencyContainer {
        return DependencyContainerFactory.create(
            databaseDriver = DatabaseDriver(),
            authHandler = DesktopAuthHandler()
        )
    }
}