package com.bgd.myapplication.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bgd.myapplication.core.domain.AuthenticationRepository
import com.bgd.myapplication.core.model.AppError
import com.bgd.myapplication.core.model.AppException
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CredentialError { REQUIRED, MISMATCH }
data class LoginUiState(
    val busy: Boolean = false,
    val error: AppError? = null,
    val credentialError: CredentialError? = null,
    val completed: Boolean = false,
    val registrationCompleted: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authentication: AuthenticationRepository,
    private val saved: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LoginUiState(
        completed = saved["login_completed"] ?: false,
        registrationCompleted = saved["registration_completed"] ?: false,
    ))
    val state = mutableState.asStateFlow()
    private var job: Job? = null

    fun submit(username: String, password: String, confirmation: String, register: Boolean) {
        if (mutableState.value.busy || mutableState.value.completed) return
        val validation = when {
            username.isBlank() || password.isBlank() -> CredentialError.REQUIRED
            register && password != confirmation -> CredentialError.MISMATCH
            else -> null
        }
        if (validation != null) {
            mutableState.value = mutableState.value.copy(credentialError = validation, error = null)
            return
        }
        mutableState.value = mutableState.value.copy(busy = true, error = null, credentialError = null)
        job = viewModelScope.launch {
            try {
                if (register && !mutableState.value.registrationCompleted) {
                    authentication.register(username.trim(), password, confirmation)
                    saved["registration_completed"] = true
                    mutableState.value = mutableState.value.copy(registrationCompleted = true)
                }
                authentication.login(username.trim(), password)
                saved["login_completed"] = true
                mutableState.value = mutableState.value.copy(busy = false, completed = true)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: AppException) { mutableState.value = mutableState.value.copy(busy = false, error = error.error) }
            catch (_: Exception) { mutableState.value = mutableState.value.copy(busy = false, error = AppError.Unknown) }
            finally { mutableState.value = mutableState.value.copy(busy = false) }
        }
    }

    fun cancel() { job?.cancel() }
}
