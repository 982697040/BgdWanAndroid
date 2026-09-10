package com.bgd.myapplication.core.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

@OptIn(InternalSerializationApi::class)
@Serializable
data class ProjectCategoryDto(val id: Int, val name: String)

@OptIn(InternalSerializationApi::class)
@Serializable
data class ProjectDto(val id: Int, val title: String, val link: String, val envelopePic: String? = null)

@OptIn(InternalSerializationApi::class)
@Serializable
data class ProjectPageDto(val datas: List<ProjectDto> = emptyList(), val over: Boolean = false)

@OptIn(InternalSerializationApi::class)
@Serializable
data class ProjectResponse<T>(val data: T? = null, val errorCode: Int, val errorMsg: String = "")

internal interface ProjectService {
    @GET("project/tree/json")
    suspend fun categories(): ProjectResponse<List<ProjectCategoryDto>>

    @GET("project/list/{page}/json")
    suspend fun projects(@Path("page") page: Int, @Query("cid") category: Int): ProjectResponse<ProjectPageDto>
}

@Singleton
class ProjectApi @Inject constructor(factory: RetrofitFactory) {
    private val service = factory.create("https://wanandroid.com/".toHttpUrl()).create(ProjectService::class.java)
    suspend fun categories(): List<ProjectCategoryDto> = service.categories().checkedData()
    suspend fun projects(category: Int, page: Int): ProjectPageDto = service.projects(page, category).checkedData()
}

private fun <T : Any> ProjectResponse<T>.checkedData(): T {
    check(errorCode == 0) { errorMsg.ifBlank { "Project request failed" } }
    return checkNotNull(data) { "Missing project response data" }
}
