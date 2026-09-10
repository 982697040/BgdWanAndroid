package com.bgd.myapplication

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bgd.myapplication.core.designsystem.AppTheme
import com.bgd.myapplication.core.model.Project
import com.bgd.myapplication.core.model.ProjectCategory
import com.bgd.myapplication.feature.projects.ProjectsScreen
import com.bgd.myapplication.feature.projects.ProjectsUiState
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProjectsScreenTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun projectsUseTwoColumnsAndCategoryClickSelectsItsId() {
        val selected = AtomicInteger()
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme {
                    ProjectsScreen(ProjectsUiState(
                        categories = listOf(ProjectCategory(294, "完整项目"), ProjectCategory(402, "跨平台应用")),
                        categoriesLoading = false, selectedId = 294,
                        projects = listOf(Project(1, "项目一", "", "https://wanandroid.com"), Project(2, "项目二", "", "https://wanandroid.com")),
                        endReached = true,
                    ), { selected.set(it) }, {}, {})
                }
            }
        }
        compose.onNodeWithTag("project_category_294").assertIsSelected()
        val first = compose.onNodeWithTag("project_1").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithTag("project_2").fetchSemanticsNode().boundsInRoot
        assertEquals(first.top, second.top)
        assertTrue(first.right < second.left)
        compose.onNodeWithTag("project_category_402").performClick()
        assertEquals(402, selected.get())
    }

    @Test fun failedProjectRequestOnlyRetriesWhenPressed() {
        val requests = AtomicInteger()
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                AppTheme {
                    ProjectsScreen(ProjectsUiState(categories = listOf(ProjectCategory(294, "完整项目")),
                        categoriesLoading = false, selectedId = 294, failed = true), {}, {}, { requests.incrementAndGet() })
                }
            }
        }
        compose.onNodeWithText("项目加载失败，请重试").assertIsDisplayed()
        assertEquals(0, requests.get())
        compose.onNodeWithText("重新加载").performClick()
        assertEquals(1, requests.get())
    }
}
