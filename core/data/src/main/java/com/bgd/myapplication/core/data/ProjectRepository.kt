package com.bgd.myapplication.core.data

import android.text.Html
import com.bgd.myapplication.core.model.Project
import com.bgd.myapplication.core.model.ProjectCategory
import com.bgd.myapplication.core.model.ProjectPage
import com.bgd.myapplication.core.network.ProjectApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(private val api: ProjectApi) : com.bgd.myapplication.core.domain.ProjectRepository {
    override suspend fun categories(): List<ProjectCategory> = api.categories()
        .distinctBy { it.id }.map { ProjectCategory(it.id, decode(it.name)) }

    override suspend fun projects(category: Int, page: Int): ProjectPage {
        val response = api.projects(category, page)
        return ProjectPage(response.datas.map {
            Project(it.id, decode(it.title), it.envelopePic.orEmpty(), it.link)
        }, response.over || response.datas.isEmpty())
    }
}

private fun decode(value: String) = Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString().trim()
