package com.bgd.myapplication.feature.settings

import com.bgd.myapplication.core.data.SettingsRepository
import com.bgd.myapplication.core.model.ThemeMode
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun savedThemeIsExposedAndSelectionPersists() = runTest(dispatcher) {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.loading)
        viewModel.onAction(SettingsAction.SelectTheme(ThemeMode.DARK))
        assertTrue(viewModel.uiState.value.saving)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, repository.mode.value)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertFalse(viewModel.uiState.value.saving)
    }

    @Test fun failedWriteKeepsSavedThemeAndCanBeRetried() = runTest(dispatcher) {
        val repository = FakeSettingsRepository()
        val viewModel = SettingsViewModel(repository)
        advanceUntilIdle()
        repository.failWrites = true
        viewModel.onAction(SettingsAction.SelectTheme(ThemeMode.DARK))
        advanceUntilIdle()
        assertEquals(SettingsError.WRITE, viewModel.uiState.value.error)
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
        assertFalse(viewModel.uiState.value.saving)
        repository.failWrites = false
        viewModel.onAction(SettingsAction.SelectTheme(ThemeMode.DARK))
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertNull(viewModel.uiState.value.error)
    }

    @Test fun readFailureIsVisibleAndRetryResubscribes() = runTest(dispatcher) {
        val repository = FakeSettingsRepository().apply { failReads = true }
        val viewModel = SettingsViewModel(repository)
        advanceUntilIdle()
        assertEquals(SettingsError.READ, viewModel.uiState.value.error)
        viewModel.onAction(SettingsAction.SelectTheme(ThemeMode.DARK))
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, repository.mode.value)
        repository.failReads = false
        viewModel.onAction(SettingsAction.Retry)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.loading)
    }

    private class FakeSettingsRepository : SettingsRepository {
        val mode = MutableStateFlow(ThemeMode.SYSTEM)
        var failWrites = false
        var failReads = false
        override val themeMode: Flow<ThemeMode> = flow {
            if (failReads) throw IOException("Read failed")
            mode.collect { emit(it) }
        }
        override suspend fun setThemeMode(mode: ThemeMode) {
            if (failWrites) throw IOException("Write failed")
            this.mode.value = mode
        }
    }
}
