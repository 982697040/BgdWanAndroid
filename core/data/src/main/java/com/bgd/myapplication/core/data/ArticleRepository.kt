package com.bgd.myapplication.core.data

import android.text.Html
import com.bgd.myapplication.core.model.Article
import com.bgd.myapplication.core.model.ArticlePage
import com.bgd.myapplication.core.network.ArticleApi
import com.bgd.myapplication.core.network.ArticleDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(private val api: ArticleApi) {
    suspend fun getTopArticles(): List<Article> = mapArticles(api.getTopArticles()).distinctBy(Article::id)

    suspend fun getArticles(page: Int): ArticlePage {
        val result = api.getArticles(page)
        return ArticlePage(mapArticles(result.datas), result.over || result.datas.isEmpty())
    }

    private fun mapArticles(articles: List<ArticleDto>): List<Article> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
        return articles.map {
            Article(
                id = it.id,
                title = plainText(it.title),
                url = it.link,
                author = plainText(it.author?.takeIf(String::isNotBlank) ?: it.shareUser.orEmpty()).ifBlank { "匿名" },
                date = it.publishTime?.takeIf { time -> time > 0 }?.let { time -> dateFormat.format(Date(time)) }
                    ?: it.niceDate.orEmpty(),
                category = listOf(it.superChapterName, it.chapterName).filterNotNull()
                    .filter(String::isNotBlank).joinToString("·") { name -> plainText(name) },
                collected = it.collect == true,
            )
        }
    }
}

private fun plainText(value: String): String = Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString().trim()
