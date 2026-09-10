package com.bgd.myapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.semantics.SemanticsProperties
import org.junit.Assert.assertEquals

class AppNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun projectScrollSurvivesEveryBottomTab() {
        compose.onNodeWithTag("tab_projects").performClick()
        val projectCard = SemanticsMatcher("project card") { node ->
            node.config.getOrElse(SemanticsProperties.TestTag) { "" }
                .removePrefix("project_").toIntOrNull() != null
        }
        compose.waitUntil(60_000) { compose.onAllNodes(projectCard).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("projects_grid").performScrollToIndex(6)
        compose.waitForIdle()
        val before = compose.onNodeWithTag("projects_grid").fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange].value()
        listOf("home", "system", "me").forEach { tab ->
            compose.onNodeWithTag("tab_$tab").performClick()
            compose.onNodeWithTag("tab_projects").performClick()
            compose.waitForIdle()
            val after = compose.onNodeWithTag("projects_grid").fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange].value()
            assertEquals(before, after, 0.01f)
        }
    }

    @Test fun bottomNavigationStartsAtHomeAndSwitchesTabs() {
        compose.onNodeWithTag("tab_home").assertIsSelected()
        listOf("system", "projects", "me", "home").forEach { tab ->
            compose.onNodeWithTag("tab_$tab").assertIsDisplayed().performClick().assertIsSelected()
        }
    }

    @Test fun settingsReturnsToMyTab() {
        compose.onNodeWithTag("tab_me").performClick()
        compose.onNodeWithText(compose.activity.getString(
            com.bgd.myapplication.feature.home.R.string.open_settings,
        )).performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText(compose.activity.getString(
            com.bgd.myapplication.feature.settings.R.string.navigate_back,
        )).performClick()
        compose.onNodeWithTag("tab_me").assertIsSelected()
    }

    @Test fun settingsSurvivesRecreationAndBackReturnsHome() {
        val context = compose.activity
        val settings = context.getString(com.bgd.myapplication.feature.home.R.string.open_settings)
        val appearance = context.getString(com.bgd.myapplication.feature.settings.R.string.theme_title)
        val back = context.getString(com.bgd.myapplication.feature.settings.R.string.navigate_back)
        compose.onNodeWithTag("tab_me").performClick()
        compose.onNodeWithText(settings).performClick()
        compose.onNodeWithText(appearance).assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText(appearance).assertIsDisplayed()
        compose.onNodeWithText(back).performClick()
        compose.onNodeWithTag("tab_home").performClick()
        compose.onNodeWithTag("tab_home").assertIsSelected()
    }
}
