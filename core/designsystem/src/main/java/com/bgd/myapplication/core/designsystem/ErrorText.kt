package com.bgd.myapplication.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bgd.myapplication.core.model.AppError

@Composable
fun AppError.localizedMessage(): String = when (this) {
    AppError.Network -> stringResource(R.string.error_network)
    AppError.Timeout -> stringResource(R.string.error_timeout)
    AppError.Unauthorized -> stringResource(R.string.error_unauthorized)
    AppError.InvalidResponse -> stringResource(R.string.error_response)
    AppError.Storage -> stringResource(R.string.error_storage)
    AppError.SessionChanged -> stringResource(R.string.error_session_changed)
    is AppError.Server -> description.ifBlank { stringResource(R.string.error_server) }
    AppError.Unknown -> stringResource(R.string.error_unknown)
}
