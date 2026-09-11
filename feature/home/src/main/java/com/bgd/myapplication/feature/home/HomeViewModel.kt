package com.bgd.myapplication.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bgd.myapplication.core.domain.BannerRepository
import com.bgd.myapplication.core.model.Banner
import com.bgd.myapplication.core.model.Article
import com.bgd.myapplication.core.domain.ArticleRepository
import com.bgd.myapplication.core.model.AppError
import com.bgd.myapplication.core.model.AppException
import kotlinx.coroutines.flow.update
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val banners: List<Banner> = emptyList(),
    val failed: Boolean = false,
    val articles: List<Article> = emptyList(),
    val articlesLoading: Boolean = false,
    val articlesFailed: Boolean = false,
    val articlesEndReached: Boolean = false,
    val topArticles: List<Article> = emptyList(),
    val topArticlesLoading: Boolean = false,
    val topArticlesFailed: Boolean = false,
    val bannerError: AppError? = null,
    val articleError: AppError? = null,
    val topArticleError: AppError? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: BannerRepository,
    private val articleRepository: ArticleRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeUiState())
    val uiState = mutableState.asStateFlow()
    private var request: Job? = null
    private var articleRequest: Job? = null
    private var nextPage = 0
    private var topRequest: Job? = null

    init { loadBanners(); loadTopArticles(); loadArticles() }

    fun loadTopArticles() {
        if (topRequest?.isActive == true) return
        topRequest = viewModelScope.launch {
            mutableState.update { it.copy(topArticlesLoading = true, topArticlesFailed = false, topArticleError = null) }
            try {
                val articles = articleRepository.getTopArticles()
                mutableState.update { it.copy(topArticles = articles, topArticlesLoading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(topArticlesLoading = false, topArticlesFailed = true, topArticleError = (error as? AppException)?.error ?: AppError.Unknown) }
            }
        }
    }

    fun loadArticles() {
        if (articleRequest?.isActive == true || mutableState.value.articlesEndReached) return
        articleRequest = viewModelScope.launch {
            mutableState.update { it.copy(articlesLoading = true, articlesFailed = false, articleError = null) }
            try {
                val page = articleRepository.getArticles(nextPage)
                nextPage++
                mutableState.update { it.copy(
                    articles = (it.articles + page.articles).distinctBy(Article::id),
                    articlesLoading = false,
                    articlesEndReached = page.endReached,
                ) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(articlesLoading = false, articlesFailed = true, articleError = (error as? AppException)?.error ?: AppError.Unknown) }
            }
        }
    }

    fun loadBanners() {
        if (request?.isActive == true) return
        request = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, failed = false, bannerError = null) }
            try {
                val banners = repository.getBanners()
                mutableState.update { it.copy(loading = false, banners = banners) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(loading = false, failed = true, bannerError = (error as? AppException)?.error ?: AppError.Unknown) }
            }
        }
    }
}
