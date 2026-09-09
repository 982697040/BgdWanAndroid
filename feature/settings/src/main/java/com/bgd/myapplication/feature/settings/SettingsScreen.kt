package com.bgd.myapplication.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bgd.myapplication.core.designsystem.AppTheme
import com.bgd.myapplication.core.model.ThemeMode

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.navigate_back)) }
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.theme_title), style = MaterialTheme.typography.titleMedium)
        if (state.loading) CircularProgressIndicator()
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.SYSTEM -> R.string.theme_system
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                }
                val enabled = !state.loading && !state.saving && state.error != SettingsError.READ
                Row(
                    modifier = Modifier.fillMaxWidth().selectable(
                        selected = mode == state.themeMode,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onAction(SettingsAction.SelectTheme(mode)) },
                    ).padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = mode == state.themeMode, onClick = null, enabled = enabled)
                    Text(stringResource(label), modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
        if (state.saving) CircularProgressIndicator()
        state.error?.let { error ->
            Text(
                stringResource(if (error == SettingsError.READ) R.string.read_error else R.string.write_error),
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = {
                onAction(if (error == SettingsError.READ) SettingsAction.Retry else SettingsAction.DismissError)
            }) {
                Text(stringResource(if (error == SettingsError.READ) R.string.retry else R.string.dismiss))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    AppTheme { SettingsScreen(SettingsUiState(loading = false), {}, {}) }
}
