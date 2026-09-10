package com.bgd.myapplication.feature.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.bgd.myapplication.core.model.Banner
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

val HomePurple = Color(0xFF7042C1)

@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state = state, onRetry = viewModel::loadBanners, onLoadArticles = viewModel::loadArticles,
        onRetryTopArticles = viewModel::loadTopArticles)
}

@Composable
fun HomeScreen(state: HomeUiState, onRetry: () -> Unit, onLoadArticles: () -> Unit,
    modifier: Modifier = Modifier, onRetryTopArticles: () -> Unit = {},
) {
    val context = LocalContext.current
    val topIds = state.topArticles.map { it.id }.toSet()
    val regularArticles = state.articles.filterNot { it.id in topIds }
    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            Modifier.fillMaxWidth().background(HomePurple).height(56.dp).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.home_title), color = Color.White, fontSize = 22.sp,
                modifier = Modifier.weight(1f))
            IconButton(onClick = {
                Toast.makeText(context, R.string.search_not_available, Toast.LENGTH_SHORT).show()
            }) {
                Icon(painterResource(R.drawable.ic_search), stringResource(R.string.search), tint = Color.White)
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("home_articles"),
            contentPadding = PaddingValues(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "banner") {
            Box(Modifier.fillMaxWidth().padding(top = 20.dp).heightIn(min = 180.dp), contentAlignment = Alignment.Center) {
                when {
                    state.loading -> CircularProgressIndicator(Modifier.size(28.dp), color = HomePurple)
                    state.failed -> BannerMessage(R.string.banner_error, onRetry)
                    state.banners.isEmpty() -> BannerMessage(R.string.banner_empty, onRetry)
                    else -> BannerCarousel(state.banners)
                }
            }
            }
            if (state.topArticlesLoading || state.topArticlesFailed) {
                item(key = "top_status") {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        if (state.topArticlesLoading) CircularProgressIndicator(Modifier.size(24.dp), color = HomePurple)
                        else BannerMessage(R.string.top_articles_error, onRetryTopArticles)
                    }
                }
            }
            items(state.topArticles, key = { "article_${it.id}" }) { ArticleCard(it, pinned = true) }
            items(regularArticles, key = { "article_${it.id}" }) { article -> ArticleCard(article) }
            item(key = "article_footer") {
                LaunchedEffect(state.articles.size) {
                    if (!state.articlesLoading && !state.articlesFailed && !state.articlesEndReached) onLoadArticles()
                }
                Box(Modifier.fillMaxWidth().testTag("article_footer").padding(16.dp), contentAlignment = Alignment.Center) {
                    when {
                        state.articlesLoading -> CircularProgressIndicator(Modifier.size(24.dp), color = HomePurple)
                        state.articlesFailed -> BannerMessage(R.string.articles_error, onLoadArticles)
                        state.articlesEndReached -> Text(stringResource(
                            if (state.articles.isEmpty() && state.topArticles.isEmpty()) R.string.articles_empty else R.string.articles_end,
                        ), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerMessage(message: Int, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(message), color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun BannerCarousel(banners: List<Banner>) {
    // Recreate the pager only when the returned banner set changes.
    key(banners.map { it.id }) {
        val count = banners.size
        val initialPage = if (count > 1) Int.MAX_VALUE / 2 / count * count else 0
        val pager = rememberPagerState(initialPage = initialPage) { if (count > 1) Int.MAX_VALUE else 1 }
        val dragged by pager.interactionSource.collectIsDraggedAsState()
        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val lifecycle = LocalLifecycleOwner.current.lifecycle
        LaunchedEffect(pager, dragged, pressed, lifecycle) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (count > 1 && !dragged && !pressed) {
                    while (true) {
                        delay(4_000)
                        if (!pager.isScrollInProgress) {
                            pager.animateScrollToPage(if (pager.currentPage == Int.MAX_VALUE - 1) initialPage else pager.currentPage + 1)
                        }
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            HorizontalPager(
                state = pager,
                contentPadding = PaddingValues(horizontal = 36.dp),
                pageSpacing = 10.dp,
                modifier = Modifier.fillMaxWidth().testTag("home_banner"),
            ) { page ->
                val banner = banners[page % count]
                val uriHandler = LocalUriHandler.current
                val context = LocalContext.current
                var retry by remember(banner.imageUrl) { mutableIntStateOf(0) }
                Box(
                    Modifier.graphicsLayer {
                        val distance = ((pager.currentPage - page) + pager.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
                        scaleY = 1f - distance * 0.12f
                    }.fillMaxWidth().aspectRatio(1.65f).clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable(interactionSource = interaction, indication = null,
                            onClickLabel = stringResource(R.string.open_banner)) {
                            try {
                                val uri = android.net.Uri.parse(banner.url)
                                require(uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank())
                                uriHandler.openUri(banner.url)
                            } catch (_: Exception) {
                                Toast.makeText(context, R.string.link_error, Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    key(retry) {
                        SubcomposeAsyncImage(
                            model = banner.imageUrl,
                            contentDescription = banner.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), color = HomePurple)
                            } },
                            error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                TextButton(onClick = { retry++ }) { Text(stringResource(R.string.image_retry)) }
                            } },
                        )
                    }
                }
            }
            if (count > 1) {
                Row(Modifier.padding(top = 10.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(count) { index ->
                        Box(Modifier.size(5.dp).clip(CircleShape).background(
                            if (pager.currentPage % count == index) HomePurple else MaterialTheme.colorScheme.outlineVariant,
                        ))
                    }
                }
            }
        }
    }
}
