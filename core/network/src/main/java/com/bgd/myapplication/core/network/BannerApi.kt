package com.bgd.myapplication.core.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.http.GET

@OptIn(InternalSerializationApi::class)
@Serializable
data class BannerResponse(
    val data: List<BannerDto>? = null,
    val errorCode: Int,
    val errorMsg: String = "",
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class BannerDto(
    val id: Int,
    val title: String = "",
    val imagePath: String,
    val url: String,
    val isVisible: Int = 1,
)

internal interface BannerService {
    @GET("banner/json")
    suspend fun banners(): BannerResponse
}

@Singleton
class BannerApi @Inject constructor(factory: RetrofitFactory) {
    private val service = factory.create("https://wanandroid.com/".toHttpUrl())
        .create(BannerService::class.java)

    suspend fun getBanners(): List<BannerDto> {
        val response = service.banners()
        check(response.errorCode == 0) { response.errorMsg.ifBlank { "Banner request failed" } }
        return response.data.orEmpty()
    }
}
