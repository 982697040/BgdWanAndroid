package com.bgd.myapplication

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.bgd.myapplication.core.designsystem.AppTheme
import com.bgd.myapplication.core.model.Article
import com.bgd.myapplication.feature.home.HomeScreen
import com.bgd.myapplication.feature.home.HomeUiState
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeArticlesTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun pinnedArticlesPrecedeRegularArticlesWithoutDuplicates() {
        val pinned = Article(100, "置顶标题", "https://wanandroid.com", "作者", "", "分类", false)
        val regular = pinned.copy(id = 101, title = "普通标题")
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme {
                    HomeScreen(HomeUiState(loading = false, topArticles = listOf(pinned),
                        articles = listOf(regular, pinned), articlesEndReached = true), {}, {})
                }
            }
        }
        compose.onNodeWithText("置顶").assertIsDisplayed()
        val top = compose.onNodeWithTag("article_100").fetchSemanticsNode().boundsInRoot.top
        val next = compose.onNodeWithTag("article_101").fetchSemanticsNode().boundsInRoot.top
        assertTrue(top < next)
    }

    @Test fun reachingListEndRequestsNextPage() {
        val requests = AtomicInteger()
        val articles = List(30) { Article(it, "文章 $it", "https://wanandroid.com", "作者 $it", "2026-09-10 10:00:00", "Android·开发", false) }
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme {
                    HomeScreen(HomeUiState(loading = false, articles = articles), {}, { requests.incrementAndGet() })
                }
            }
        }
        compose.onNodeWithText("文章 0").assertIsDisplayed()
        assertEquals(0, requests.get())
        compose.onNodeWithTag("home_articles").performScrollToNode(hasTestTag("article_29"))
        compose.onNodeWithText("文章 29").assertIsDisplayed()
        compose.onNodeWithTag("home_articles").performScrollToNode(hasTestTag("article_footer"))
        compose.waitUntil(5_000) { requests.get() == 1 }
    }

    @Test fun articleFailureCanRetryWithoutReloadingBanner() {
        val articleRequests = AtomicInteger()
        val bannerRequests = AtomicInteger()
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme {
                    HomeScreen(HomeUiState(loading = true, articlesFailed = true),
                        { bannerRequests.incrementAndGet() }, { articleRequests.incrementAndGet() })
                }
            }
        }
        compose.onNodeWithText("文章加载失败，请重试").assertIsDisplayed()
        compose.onNodeWithText("重新加载").performClick()
        assertEquals(1, articleRequests.get())
        assertEquals(0, bannerRequests.get())
    }
}
