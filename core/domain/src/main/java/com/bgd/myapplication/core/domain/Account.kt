package com.bgd.myapplication.core.domain

import com.bgd.myapplication.core.model.AppError
import com.bgd.myapplication.core.model.AppException
import com.bgd.myapplication.core.model.SessionState
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val state: StateFlow<SessionState>
    suspend fun awaitRestored(): SessionState
}

interface AuthenticationRepository {
    suspend fun login(username: String, password: String)
    suspend fun register(username: String, password: String, confirmation: String)
    suspend fun logout()
}

interface CollectionRepository {
    suspend fun setCollected(articleId: Int, collected: Boolean)
}

class CollectArticleUseCase @Inject constructor(
    private val session: SessionRepository,
    private val collections: CollectionRepository,
) {
    suspend operator fun invoke(articleId: Int, collected: Boolean) {
        require(articleId > 0)
        if (session.awaitRestored() !is SessionState.SignedIn) throw AppException(AppError.Unauthorized)
        collections.setCollected(articleId, collected)
    }
}
