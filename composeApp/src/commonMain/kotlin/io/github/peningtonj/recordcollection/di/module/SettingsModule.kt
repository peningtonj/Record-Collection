package io.github.peningtonj.recordcollection.di.module

import io.github.peningtonj.recordcollection.repository.SettingsRepository
import io.github.peningtonj.recordcollection.viewmodel.SettingsViewModel
import org.koin.dsl.module
import org.koin.core.module.dsl.singleOf

interface SettingsModule {
    fun provideSettingsRepository(): SettingsRepository
    fun provideSettingsViewModel(settingsRepository: SettingsRepository): SettingsViewModel
}

class ProductionSettingsModule : SettingsModule {
    override fun provideSettingsRepository(): SettingsRepository {
        return SettingsRepository()
    }

    override fun provideSettingsViewModel(settingsRepository: SettingsRepository): SettingsViewModel {
        return SettingsViewModel(settingsRepository)
    }
}