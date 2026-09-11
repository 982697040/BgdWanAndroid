package com.bgd.myapplication.core.data

import com.bgd.myapplication.core.datastore.PreferencesDataSource
import com.bgd.myapplication.core.model.ThemeMode
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

typealias SettingsRepository = com.bgd.myapplication.core.domain.SettingsRepository

class OfflineSettingsRepository @Inject constructor(
    private val preferences: PreferencesDataSource,
) : SettingsRepository {
    override val themeMode = preferences.themeMode
    override suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: OfflineSettingsRepository): SettingsRepository
}
