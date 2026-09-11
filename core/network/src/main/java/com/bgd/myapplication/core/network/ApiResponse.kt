package com.bgd.myapplication.core.network

import com.bgd.myapplication.core.model.AppError
import com.bgd.myapplication.core.model.AppException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

@Serializable
internal data class ApiResponse<T>(val data: T? = null, val errorCode: Int, val errorMsg: String = "")

/** One validation path for all endpoints, including public endpoints returning -1001. */
@Singleton
class ApiExecutor @Inject constructor(private val storage: SessionStorage) {
    internal suspend fun <T : Any> data(request: suspend () -> ApiResponse<T>): T = execute {
        checkNotNullResponse(checked(request).data)
    }

    internal suspend fun action(request: suspend () -> ApiResponse<kotlinx.serialization.json.JsonElement>) {
        execute { checked(request); Unit }
    }

    private suspend fun <T> checked(request: suspend () -> ApiResponse<T>): ApiResponse<T> {
        // Initialization happens off the main thread even for the first anonymous request.
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { storage.initialize() }
        val generation = storage.snapshots.value.generation
        val result = try { request() } catch (error: HttpException) {
            if (error.code() == 401) {
                if (storage.snapshots.value.generation != generation) throw AppException(AppError.SessionChanged)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { storage.invalidate(generation) }
                throw AppException(AppError.Unauthorized, error)
            }
            throw error
        }
        if (storage.snapshots.value.generation != generation) throw AppException(AppError.SessionChanged)
        if (result.errorCode == -1001) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { storage.invalidate(generation) }
            throw AppException(AppError.Unauthorized)
        }
        if (result.errorCode != 0) throw AppException(AppError.Server(result.errorCode, result.errorMsg))
        return result
    }

    private fun <T : Any> checkNotNullResponse(data: T?): T = data ?: throw AppException(AppError.InvalidResponse)

    private suspend fun <T> execute(block: suspend () -> T): T = try { block() }
    catch (cancelled: CancellationException) { throw cancelled }
    catch (known: AppException) { throw known }
    catch (error: SocketTimeoutException) { throw AppException(AppError.Timeout, error) }
    catch (error: SerializationException) { throw AppException(AppError.InvalidResponse, error) }
    catch (error: HttpException) { throw AppException(AppError.Server(error.code(), ""), error) }
    catch (error: IOException) { throw (error.cause as? AppException ?: AppException(AppError.Network, error)) }
}
