package io.github.peningtonj.recordcollection.di.module

import com.russhwolf.settings.Settings
import io.github.peningtonj.recordcollection.network.openAi.OpenAiApi
import io.github.peningtonj.recordcollection.repository.SettingsRepository
import io.github.peningtonj.recordcollection.viewmodel.SettingsViewModel
interface SettingsModule {
    fun provideSettings(): Settings
    fun provideSettingsRepository(): SettingsRepository
    fun provideSettingsViewModel(settingsRepository: SettingsRepository, openAiApi: OpenAiApi): SettingsViewModel
}

class ProductionSettingsModule(
    private val settings: Settings
) : SettingsModule {
    override fun provideSettings(): Settings = settings

    override fun provideSettingsRepository(): SettingsRepository {
        return SettingsRepository(settings)
    }

    override fun provideSettingsViewModel(
        settingsRepository: SettingsRepository,
        openAiApi: OpenAiApi
    ): SettingsViewModel {
        return SettingsViewModel(settingsRepository, openAiApi)
    }
}
