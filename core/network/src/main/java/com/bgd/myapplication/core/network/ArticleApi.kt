package com.bgd.myapplication.core.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.http.GET
import retrofit2.http.Path

@Serializable
data class ArticlePageDto(val datas: List<ArticleDto> = emptyList(), val over: Boolean = false)

@Serializable
data class ArticleDto(
    val id: Int,
    val title: String,
    val link: String,
    val author: String? = null,
    val shareUser: String? = null,
    val publishTime: Long? = null,
    val niceDate: String? = null,
    val superChapterName: String? = null,
    val chapterName: String? = null,
    val collect: Boolean? = null,
)

internal interface ArticleService {
    @GET("article/top/json")
    suspend fun topArticles(): ApiResponse<List<ArticleDto>>
    @GET("article/list/{page}/json")
    suspend fun articles(@Path("page") page: Int): ApiResponse<ArticlePageDto>
}

@Singleton
class ArticleApi @Inject constructor(factory: RetrofitFactory, private val executor: ApiExecutor) {
    suspend fun getTopArticles(): List<ArticleDto> {
        return executor.data { service.topArticles() }
    }
    private val service = factory.create("https://wanandroid.com/".toHttpUrl()).create(ArticleService::class.java)
    suspend fun getArticles(page: Int): ArticlePageDto {
        return executor.data { service.articles(page) }
    }
}
