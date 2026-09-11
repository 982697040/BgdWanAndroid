package com.bgd.myapplication.feature.auth

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bgd.myapplication.core.designsystem.localizedMessage

@Composable
fun LoginRoute(onBack: () -> Unit, onSuccess: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler { viewModel.cancel(); onBack() }
    LaunchedEffect(state.completed) { if (state.completed) onSuccess() }
    LoginScreen(state, { viewModel.cancel(); onBack() }, viewModel::submit)
}

@Composable
fun LoginScreen(state: LoginUiState, onBack: () -> Unit, onSubmit: (String, String, String, Boolean) -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    // Credentials never enter SavedStateHandle, navigation arguments or persistence.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.registrationCompleted) { if (state.registrationCompleted) register = false }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        Text(stringResource(if (register) R.string.register else R.string.login), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(username, { username = it }, Modifier.fillMaxWidth().testTag("login_username"),
            label = { Text(stringResource(R.string.username)) }, singleLine = true, enabled = !state.busy)
        OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth().testTag("login_password"),
            label = { Text(stringResource(R.string.password)) }, singleLine = true, enabled = !state.busy,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        if (register) OutlinedTextField(confirmation, { confirmation = it }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.confirm_password)) }, singleLine = true, enabled = !state.busy,
            visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
        state.credentialError?.let {
            Text(stringResource(if (it == CredentialError.REQUIRED) R.string.credentials_required else R.string.password_mismatch),
                color = MaterialTheme.colorScheme.error)
        }
        state.error?.let { Text(it.localizedMessage(), color = MaterialTheme.colorScheme.error) }
        if (state.registrationCompleted && !state.completed) Text(stringResource(R.string.registered))
        Button(onClick = { onSubmit(username, password, confirmation, register) }, enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().testTag("login_submit")) {
            if (state.busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text(stringResource(if (register) R.string.register_and_login else R.string.login))
        }
        TextButton(onClick = { register = !register; confirmation = "" }, enabled = !state.busy && !state.registrationCompleted) {
            Text(stringResource(if (register) R.string.go_login else R.string.go_register))
        }
    }
}
