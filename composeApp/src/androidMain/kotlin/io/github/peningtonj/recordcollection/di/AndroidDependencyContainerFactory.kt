package io.github.peningtonj.recordcollection.di

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.peningtonj.recordcollection.di.container.DependencyContainer
import io.github.peningtonj.recordcollection.di.container.ModularDependencyContainer
import io.github.peningtonj.recordcollection.di.module.ProductionSettingsModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionEventModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionFirebaseModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionNetworkModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionRepositoryModule
import io.github.peningtonj.recordcollection.di.module.impl.ProductionUseCaseModule
import io.github.peningtonj.recordcollection.network.oauth.spotify.AndroidAuthHandler

object AndroidDependencyContainerFactory {
    fun create(context: Context): DependencyContainer {
        val sharedPrefs = context.getSharedPreferences(
            "record_collection_prefs",
            Context.MODE_PRIVATE
        )
        val settings = SharedPreferencesSettings(sharedPrefs)

        return ModularDependencyContainer(
            networkModule = ProductionNetworkModule(),
            repositoryModule = ProductionRepositoryModule(),
            authHandler = AndroidAuthHandler(context.applicationContext),
            useCaseModule = ProductionUseCaseModule(),
            eventModule = ProductionEventModule(),
            settingsModule = ProductionSettingsModule(settings),
            firebaseModule = ProductionFirebaseModule()
        )
    }
}

