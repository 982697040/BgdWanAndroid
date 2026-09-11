package com.bgd.myapplication.core.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class LoginUserDto(val username: String, val collectIds: Set<Int> = emptySet())

internal interface AuthService {
    @FormUrlEncoded @POST("user/login")
    suspend fun login(@Field("username") username: String, @Field("password") password: String): ApiResponse<LoginUserDto>
    @FormUrlEncoded @POST("user/register")
    suspend fun register(@Field("username") username: String, @Field("password") password: String,
        @Field("repassword") confirmation: String): ApiResponse<JsonElement>
    @GET("user/logout/json") suspend fun logout(): ApiResponse<JsonElement>
    @POST("lg/collect/{id}/json") suspend fun collect(@Path("id") id: Int): ApiResponse<JsonElement>
    @POST("lg/uncollect_originId/{id}/json") suspend fun uncollect(@Path("id") id: Int): ApiResponse<JsonElement>
}

@Singleton
class AuthApi @Inject constructor(factory: RetrofitFactory, private val executor: ApiExecutor) {
    private val service = factory.create("https://wanandroid.com/".toHttpUrl()).create(AuthService::class.java)
    suspend fun login(username: String, password: String): LoginUserDto = executor.data { service.login(username, password) }
    suspend fun register(username: String, password: String, confirmation: String) =
        executor.action { service.register(username, password, confirmation) }
    suspend fun logout() = executor.action { service.logout() }
    suspend fun collect(id: Int, desired: Boolean) = executor.action {
        if (desired) service.collect(id) else service.uncollect(id)
    }
}
