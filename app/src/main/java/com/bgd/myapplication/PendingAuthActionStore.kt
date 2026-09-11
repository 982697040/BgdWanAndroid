package com.bgd.myapplication

import androidx.lifecycle.SavedStateHandle
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed interface PendingAuthAction {
    @Serializable
    data class CollectArticle(val articleId: Int, val collected: Boolean) : PendingAuthAction
}

@Serializable
data class LoginRequest(val id: String, val action: PendingAuthAction? = null)

/** A single versioned record prevents partially restored id/desired/navigation combinations. */
class PendingAuthActionStore(private val saved: SavedStateHandle) {
    private val mutableRequest = MutableStateFlow(saved.get<String>(KEY)?.let {
        runCatching { Json.decodeFromString<LoginRequest>(it) }.getOrNull()
    })
    val request = mutableRequest.asStateFlow()

    fun open(action: PendingAuthAction? = null) = set(LoginRequest(UUID.randomUUID().toString(), action))
    fun clear() = set(null)
    fun consume(id: String): LoginRequest? {
        val current = mutableRequest.value?.takeIf { it.id == id } ?: return null
        set(null)
        return current
    }
    private fun set(value: LoginRequest?) {
        saved[KEY] = value?.let { Json.encodeToString(it) }
        mutableRequest.value = value
    }
    private companion object { const val KEY = "pending_auth_request_v1" }
}
