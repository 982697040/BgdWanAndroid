package com.bgd.myapplication.core.data

import com.bgd.myapplication.core.domain.CollectionRepository
import com.bgd.myapplication.core.model.AppError
import com.bgd.myapplication.core.model.AppException
import com.bgd.myapplication.core.network.AuthApi
import com.bgd.myapplication.core.network.SessionStorage
import com.bgd.myapplication.core.network.hasSessionCookies
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class DefaultCollectionRepository @Inject constructor(
    private val api: AuthApi,
    private val storage: SessionStorage,
    private val session: SessionManager,
) : CollectionRepository {
    override suspend fun setCollected(articleId: Int, collected: Boolean) = withContext(Dispatchers.IO) {
        session.operations.withLock {
            storage.initialize()
            val snapshot = storage.snapshots.value
            if (snapshot.username == null || !storage.hasSessionCookies()) throw AppException(AppError.Unauthorized)
            if ((articleId in snapshot.collectedIds) == collected) return@withLock
            api.collect(articleId, collected)
            storage.update(snapshot.generation) {
                it.copy(collectedIds = if (collected) it.collectedIds + articleId else it.collectedIds - articleId)
            }
        }
    }
}
