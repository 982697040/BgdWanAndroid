package com.bgd.myapplication.core.data

import com.bgd.myapplication.core.domain.AuthenticationRepository
import com.bgd.myapplication.core.model.AppError
import com.bgd.myapplication.core.model.AppException
import com.bgd.myapplication.core.network.AuthApi
import com.bgd.myapplication.core.network.SessionStorage
import com.bgd.myapplication.core.network.hasSessionCookies
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class DefaultAuthenticationRepository @Inject constructor(
    private val api: AuthApi,
    private val storage: SessionStorage,
    private val session: SessionManager,
) : AuthenticationRepository {
    override suspend fun login(username: String, password: String) = withContext(Dispatchers.IO) {
        session.operations.withLock {
            val generation = storage.reset()
            var committed = false
            try {
                val user = api.login(username, password)
                if (!storage.hasSessionCookies()) throw AppException(AppError.InvalidResponse)
                storage.update(generation) { it.copy(username = user.username, collectedIds = user.collectIds) }
                committed = true
            } finally {
                if (!committed) withContext(NonCancellable) { storage.invalidate(generation) }
            }
        }
    }

    override suspend fun register(username: String, password: String, confirmation: String) = withContext(Dispatchers.IO) {
        session.operations.withLock {
            val generation = storage.reset()
            try { api.register(username, password, confirmation) }
            finally { withContext(NonCancellable) { storage.invalidate(generation) } }
        }
    }

    override suspend fun logout() = withContext(Dispatchers.IO) {
        session.operations.withLock {
            try { api.logout() }
            finally { withContext(NonCancellable) { storage.reset() } }
        }
    }
}
