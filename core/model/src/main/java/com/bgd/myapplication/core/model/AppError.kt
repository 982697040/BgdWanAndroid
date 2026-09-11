package com.bgd.myapplication.core.model

sealed interface AppError {
    data object Network : AppError
    data object Timeout : AppError
    data object Unauthorized : AppError
    data object InvalidResponse : AppError
    data object Storage : AppError
    data object SessionChanged : AppError
    data class Server(val code: Int, val description: String) : AppError
    data object Unknown : AppError
}

class AppException(val error: AppError, cause: Throwable? = null) : Exception(null, cause)

sealed interface SessionState {
    data object Restoring : SessionState
    data object SignedOut : SessionState
    data class Unavailable(val error: AppError) : SessionState
    data class SignedIn(val username: String, val collectedIds: Set<Int>) : SessionState
}
