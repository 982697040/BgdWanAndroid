package com.bgd.myapplication.core.domain

import com.bgd.myapplication.core.model.Article
import com.bgd.myapplication.core.model.ArticlePage
import com.bgd.myapplication.core.model.Banner
import com.bgd.myapplication.core.model.ProjectCategory
import com.bgd.myapplication.core.model.ProjectPage

interface BannerRepository { suspend fun getBanners(): List<Banner> }
interface ArticleRepository {
    suspend fun getTopArticles(): List<Article>
    suspend fun getArticles(page: Int): ArticlePage
}
interface ProjectRepository {
    suspend fun categories(): List<ProjectCategory>
    suspend fun projects(category: Int, page: Int): ProjectPage
}
