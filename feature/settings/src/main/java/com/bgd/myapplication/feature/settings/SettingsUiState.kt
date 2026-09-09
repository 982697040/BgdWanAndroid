package com.bgd.myapplication.feature.settings

import com.bgd.myapplication.core.model.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: SettingsError? = null,
)

enum class SettingsError { READ, WRITE }

sealed interface SettingsAction {
    data class SelectTheme(val mode: ThemeMode) : SettingsAction
    data object Retry : SettingsAction
    data object DismissError : SettingsAction
}
