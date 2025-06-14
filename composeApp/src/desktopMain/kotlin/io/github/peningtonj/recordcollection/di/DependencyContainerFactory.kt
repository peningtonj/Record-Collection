package io.github.peningtonj.recordcollection.di

import io.github.peningtonj.recordcollection.db.DatabaseDriver
import io.github.peningtonj.recordcollection.network.oauth.spotify.DesktopAuthHandler

object DependencyContainerFactory {
    fun create(): DependencyContainer {
        return BaseDependencyContainer(
            databaseDriver = DatabaseDriver(),
            authHandler = DesktopAuthHandler()
        )
    }
}
