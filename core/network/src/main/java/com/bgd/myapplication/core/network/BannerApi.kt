package com.bgd.myapplication.core.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.http.GET

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
    suspend fun banners(): ApiResponse<List<BannerDto>>
}

@Singleton
class BannerApi @Inject constructor(factory: RetrofitFactory, private val executor: ApiExecutor) {
    private val service = factory.create("https://wanandroid.com/".toHttpUrl())
        .create(BannerService::class.java)

    suspend fun getBanners(): List<BannerDto> {
        return executor.data { service.banners() }
    }
}
