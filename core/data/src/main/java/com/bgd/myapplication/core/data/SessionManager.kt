package com.bgd.myapplication.core.data

import com.bgd.myapplication.core.domain.SessionRepository
import com.bgd.myapplication.core.model.SessionState
import com.bgd.myapplication.core.model.AppException
import com.bgd.myapplication.core.network.SessionStorage
import com.bgd.myapplication.core.network.hasSessionCookies
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

@Singleton
class SessionManager @Inject constructor(private val storage: SessionStorage) : SessionRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    internal val operations = Mutex()
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Restoring)
    override val state = mutableState.asStateFlow()
    init {
        scope.launch {
            storage.snapshots.collect { snapshot ->
                if (snapshot.initialized) mutableState.value = currentState()
            }
        }
    }
    private fun currentState(): SessionState {
        val snapshot = storage.snapshots.value
        val username = snapshot.username
        return when {
            !snapshot.initialized -> SessionState.Restoring
            username != null && storage.hasSessionCookies() ->
                SessionState.SignedIn(username, snapshot.collectedIds)
            else -> SessionState.SignedOut
        }
    }

    override suspend fun awaitRestored(): SessionState {
        try {
            withContext(Dispatchers.IO) { storage.initialize() }
            return currentState().also { mutableState.value = it }
        } catch (error: AppException) {
            mutableState.value = SessionState.Unavailable(error.error)
            throw error
        }
    }
}
