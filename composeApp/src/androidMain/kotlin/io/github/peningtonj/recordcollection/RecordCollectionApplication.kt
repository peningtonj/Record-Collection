package io.github.peningtonj.recordcollection

import android.app.Application
import io.github.peningtonj.recordcollection.di.AndroidDependencyContainerFactory
import io.github.peningtonj.recordcollection.di.container.DependencyContainer

/**
 * Process-scoped application class.
 *
 * Holding the [DependencyContainer] here (rather than in MainActivity) means
 * that both MainActivity and PlaybackMonitorService can access the same
 * container and its shared session state via
 * `applicationContext as RecordCollectionApplication`.
 */
class RecordCollectionApplication : Application() {

    lateinit var dependencyContainer: DependencyContainer
        private set

    override fun onCreate() {
        super.onCreate()
        dependencyContainer = AndroidDependencyContainerFactory.create(this)
    }
}


