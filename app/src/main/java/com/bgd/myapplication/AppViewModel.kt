package com.bgd.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bgd.myapplication.core.data.SettingsRepository
import com.bgd.myapplication.core.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import java.io.IOException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay

@HiltViewModel
class AppViewModel @Inject constructor(repository: SettingsRepository) : ViewModel() {
    // Keep the app usable on a transient disk error; Settings exposes the error and retry action.
    val themeMode = repository.themeMode
        .retryWhen { cause, _ ->
            if (cause is IOException) { delay(2_000); true } else false
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
}
