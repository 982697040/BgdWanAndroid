package com.bgd.myapplication.core.model

data class Article(
    val id: Int,
    val title: String,
    val url: String,
    val author: String,
    val date: String,
    val category: String,
    val collected: Boolean,
)

data class ArticlePage(val articles: List<Article>, val endReached: Boolean)
