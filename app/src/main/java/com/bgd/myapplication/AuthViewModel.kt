package com.bgd.myapplication

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bgd.myapplication.core.domain.AuthenticationRepository
import com.bgd.myapplication.core.domain.CollectArticleUseCase
import com.bgd.myapplication.core.domain.SessionRepository
import com.bgd.myapplication.core.model.AppError
import com.bgd.myapplication.core.model.AppException
import com.bgd.myapplication.core.model.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AccountNotice {
    data object Collected : AccountNotice
    data object Uncollected : AccountNotice
    data object SignedOut : AccountNotice
    data class Failure(val error: AppError) : AccountNotice
}
data class Notice(val id: Long, val content: AccountNotice)
data class AuthUiState(val busy: Boolean = false, val notice: Notice? = null)

/** Coordinates navigation requests and authenticated actions; form submission belongs to LoginViewModel. */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sessions: SessionRepository,
    private val authentication: AuthenticationRepository,
    private val collectArticle: CollectArticleUseCase,
    saved: SavedStateHandle,
) : ViewModel() {
    private val pending = PendingAuthActionStore(saved)
    val loginRequest = pending.request
    val session = sessions.state
    private val mutableState = MutableStateFlow(AuthUiState())
    val uiState = mutableState.asStateFlow()
    private var noticeId = 0L

    init { restoreSession() }

    fun restoreSession() {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { sessions.awaitRestored() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: AppException) { notify(AccountNotice.Failure(error.error)) }
            finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    fun openLogin() {
        if (mutableState.value.busy || loginRequest.value != null) return
        if (session.value is SessionState.SignedIn) return
        pending.open()
    }

    fun cancelLogin() { pending.clear() }

    fun collect(id: Int, desired: Boolean) {
        if (id <= 0 || mutableState.value.busy || loginRequest.value != null) return
        execute(PendingAuthAction.CollectArticle(id, desired))
    }

    fun loginCompleted(requestId: String) {
        val request = pending.consume(requestId) ?: return
        request.action?.let(::execute)
    }

    private fun execute(action: PendingAuthAction) {
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                when (action) {
                    is PendingAuthAction.CollectArticle -> {
                        collectArticle(action.articleId, action.collected)
                        notify(if (action.collected) AccountNotice.Collected else AccountNotice.Uncollected)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: AppException) {
                if (error.error == AppError.Unauthorized) pending.open(action)
                else notify(AccountNotice.Failure(error.error))
            } catch (_: Exception) { notify(AccountNotice.Failure(AppError.Unknown)) }
            finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    fun logout() {
        if (mutableState.value.busy || loginRequest.value != null) return
        pending.clear()
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { authentication.logout(); notify(AccountNotice.SignedOut) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: AppException) { notify(AccountNotice.Failure(error.error)) }
            catch (_: Exception) { notify(AccountNotice.Failure(AppError.Unknown)) }
            finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    fun acknowledgeNotice(id: Long) {
        mutableState.update { if (it.notice?.id == id) it.copy(notice = null) else it }
    }
    private fun notify(content: AccountNotice) {
        mutableState.update { it.copy(notice = Notice(++noticeId, content)) }
    }
}
