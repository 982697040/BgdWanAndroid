package com.bgd.myapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class AppNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun settingsSurvivesRecreationAndBackReturnsHome() {
        val context = compose.activity
        val settings = context.getString(com.bgd.myapplication.feature.home.R.string.open_settings)
        val appearance = context.getString(com.bgd.myapplication.feature.settings.R.string.theme_title)
        val back = context.getString(com.bgd.myapplication.feature.settings.R.string.navigate_back)
        val home = context.getString(com.bgd.myapplication.feature.home.R.string.home_title)
        compose.onNodeWithText(settings).performClick()
        compose.onNodeWithText(appearance).assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText(appearance).assertIsDisplayed()
        compose.onNodeWithText(back).performClick()
        compose.onNodeWithText(home).assertIsDisplayed()
    }
}
