package com.bgd.myapplication.core.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
data class ProjectCategoryDto(val id: Int, val name: String)

@Serializable
data class ProjectDto(val id: Int, val title: String, val link: String, val envelopePic: String? = null)

@Serializable
data class ProjectPageDto(val datas: List<ProjectDto> = emptyList(), val over: Boolean = false)

internal interface ProjectService {
    @GET("project/tree/json")
    suspend fun categories(): ApiResponse<List<ProjectCategoryDto>>

    @GET("project/list/{page}/json")
    suspend fun projects(@Path("page") page: Int, @Query("cid") category: Int): ApiResponse<ProjectPageDto>
}

@Singleton
class ProjectApi @Inject constructor(factory: RetrofitFactory, private val executor: ApiExecutor) {
    private val service = factory.create("https://wanandroid.com/".toHttpUrl()).create(ProjectService::class.java)
    suspend fun categories(): List<ProjectCategoryDto> = executor.data { service.categories() }
    suspend fun projects(category: Int, page: Int): ProjectPageDto = executor.data { service.projects(page, category) }
}
