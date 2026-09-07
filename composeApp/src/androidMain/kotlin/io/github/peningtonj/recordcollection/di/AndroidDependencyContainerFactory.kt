package io.github.peningtonj.recordcollection.di

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.peningtonj.recordcollection.db.FirebaseDriver
import io.github.peningtonj.recordcollection.db.ensureAnonymousAuth
import io.github.peningtonj.recordcollection.di.container.DependencyContainer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
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
        FirebaseDriver().initializeFirebase()

        // Firestore rules require request.auth != null — sign in anonymously first.
        // One-time bootstrap blocking call; timeout guards against a network hang.
        runBlocking {
            withTimeoutOrNull(15_000) { ensureAnonymousAuth() }
        }

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

