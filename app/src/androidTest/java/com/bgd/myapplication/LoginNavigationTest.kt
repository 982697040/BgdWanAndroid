package com.bgd.myapplication

import androidx.activity.compose.setContent
import androidx.lifecycle.SavedStateHandle
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import com.bgd.myapplication.core.data.AccountRepository
import com.bgd.myapplication.core.data.AccountState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class LoginNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private class FakeAccount : AccountRepository {
        override val account = MutableStateFlow(AccountState())
        val operations = mutableListOf<Pair<Int, Boolean>>()
        override suspend fun login(username: String, password: String, register: Boolean, confirmation: String) {
            account.value = AccountState(username)
        }
        override suspend fun collect(id: Int, desired: Boolean) {
            operations.add(id to desired)
            account.value = account.value.copy(collectedIds = if (desired) account.value.collectedIds + id else account.value.collectedIds - id)
        }
        override suspend fun logout() { account.value = AccountState() }
    }
    @Test fun avatarLoginReturnsToMyPageWithoutCollecting() {
        val repo = FakeAccount()
        compose.activityRule.scenario.onActivity { activity ->
            val auth = AuthViewModel(repo, SavedStateHandle())
            activity.setContent { BgdApp(auth = auth) }
        }
        compose.onNodeWithTag("tab_me").performClick()
        compose.onNodeWithTag("profile_avatar").performClick()
        compose.onNodeWithTag("login_username").performTextInput("tester")
        compose.onNodeWithTag("login_password").performTextInput("test-password")
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("tab_me")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("tab_me").assertIsSelected()
        compose.onNodeWithText("tester").assertIsDisplayed()
        assertTrue(repo.operations.isEmpty())
    }
    @Test fun collectionLoginReturnsHomeAndContinuesAction() {
        val repo = FakeAccount()
        lateinit var auth: AuthViewModel
        compose.activityRule.scenario.onActivity { activity ->
            auth = AuthViewModel(repo, SavedStateHandle())
            activity.setContent { BgdApp(auth = auth) }
        }
        val collectButton = SemanticsMatcher("collection button") { node ->
            node.config.getOrElse(SemanticsProperties.TestTag) { "" }.startsWith("collect_")
        }
        compose.waitUntil(60_000) { compose.onAllNodes(collectButton).fetchSemanticsNodes().isNotEmpty() }
        val id = compose.onAllNodes(collectButton)[0].fetchSemanticsNode()
            .config[SemanticsProperties.TestTag].removePrefix("collect_").toInt()
        compose.onAllNodes(collectButton)[0].performClick()
        compose.onNodeWithTag("login_username").performTextInput("tester")
        compose.onNodeWithTag("login_password").performTextInput("test-password")
        compose.onNodeWithTag("login_submit").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("tab_home")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("tab_home").assertIsSelected()
        compose.runOnIdle { assertEquals(listOf(id to true), repo.operations); assertTrue(id in repo.account.value.collectedIds) }
    }
}
