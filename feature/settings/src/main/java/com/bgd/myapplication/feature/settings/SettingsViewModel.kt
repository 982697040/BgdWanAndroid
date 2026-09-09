package com.bgd.myapplication.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bgd.myapplication.core.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.io.IOException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()
    private var observeJob: Job? = null

    init { observeSettings() }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SelectTheme -> {
                if (_uiState.value.saving || _uiState.value.loading || _uiState.value.error == SettingsError.READ) return
                _uiState.update { it.copy(saving = true, error = null) }
                viewModelScope.launch {
                    try {
                        repository.setThemeMode(action.mode)
                    } catch (_: IOException) {
                        _uiState.update { it.copy(error = SettingsError.WRITE) }
                    } finally {
                        _uiState.update { it.copy(saving = false) }
                    }
                }
            }
            SettingsAction.Retry -> observeSettings()
            SettingsAction.DismissError -> {
                if (_uiState.value.error == SettingsError.WRITE) {
                    _uiState.update { it.copy(error = null) }
                }
            }
        }
    }

    private fun observeSettings() {
        observeJob?.cancel()
        _uiState.update { it.copy(loading = true, error = null) }
        observeJob = viewModelScope.launch {
            try {
                repository.themeMode.collect { mode ->
                    _uiState.update { it.copy(themeMode = mode, loading = false) }
                }
            } catch (_: IOException) {
                _uiState.update { it.copy(loading = false, error = SettingsError.READ) }
            }
        }
    }
}
