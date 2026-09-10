package com.bgd.myapplication.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bgd.myapplication.core.data.BannerRepository
import com.bgd.myapplication.core.model.Banner
import com.bgd.myapplication.core.model.Article
import com.bgd.myapplication.core.data.ArticleRepository
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

    init { loadBanners(); loadArticles() }

    fun loadArticles() {
        if (articleRequest?.isActive == true || mutableState.value.articlesEndReached) return
        articleRequest = viewModelScope.launch {
            mutableState.update { it.copy(articlesLoading = true, articlesFailed = false) }
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
            } catch (_: Exception) {
                mutableState.update { it.copy(articlesLoading = false, articlesFailed = true) }
            }
        }
    }

    fun loadBanners() {
        if (request?.isActive == true) return
        request = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, failed = false) }
            try {
                val banners = repository.getBanners()
                mutableState.update { it.copy(loading = false, banners = banners) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(loading = false, failed = true) }
            }
        }
    }
}
