package com.bgd.myapplication

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.fillMaxWidth
import com.bgd.myapplication.feature.home.HomePurple
import com.bgd.myapplication.feature.projects.ProjectsRoute
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.bgd.myapplication.core.model.SessionState
import com.bgd.myapplication.core.designsystem.localizedMessage
import com.bgd.myapplication.feature.auth.LoginRoute
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.bgd.myapplication.core.designsystem.AppTheme
import com.bgd.myapplication.core.model.ThemeMode
import com.bgd.myapplication.feature.home.HomeRoute
import com.bgd.myapplication.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

@Serializable
data object Home : NavKey

@Serializable
data object Settings : NavKey

@Serializable
data class Login(val requestId: String) : NavKey

@Serializable
data object Me : NavKey

@Serializable
data object System : NavKey

@Serializable
data object Projects : NavKey

private enum class MainTab(val key: NavKey, val label: Int, val icon: Int) {
    HOME(Home, R.string.tab_home, R.drawable.ic_tab_home),
    SYSTEM(System, R.string.tab_system, R.drawable.ic_tab_system),
    PROJECTS(Projects, R.string.tab_projects, R.drawable.ic_tab_projects),
    ME(Me, R.string.tab_me, R.drawable.ic_tab_me),
}
@Composable
fun BgdApp(viewModel: AppViewModel = hiltViewModel(), auth: AuthViewModel = hiltViewModel()) {
    val session by auth.session.collectAsStateWithLifecycle()
    val account = session as? SessionState.SignedIn
    val authState by auth.uiState.collectAsStateWithLifecycle()
    val loginRequest by auth.loginRequest.collectAsStateWithLifecycle()
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val dark = when (theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val backStack = rememberNavBackStack(Home)
    LaunchedEffect(loginRequest?.id) {
        val top = backStack.lastOrNull()
        if (top is Login && top.requestId != loginRequest?.id) backStack.removeAt(backStack.lastIndex)
        loginRequest?.let { if (backStack.lastOrNull() !is Login) backStack.add(Login(it.id)) }
    }
    val snackbars = remember { SnackbarHostState() }
    val noticeText = when (val notice = authState.notice?.content) {
        AccountNotice.Collected -> stringResource(R.string.collected_success)
        AccountNotice.Uncollected -> stringResource(R.string.uncollected_success)
        AccountNotice.SignedOut -> stringResource(R.string.logout_success)
        is AccountNotice.Failure -> notice.error.localizedMessage()
        null -> null
    }
    LaunchedEffect(authState.notice?.id) {
        val notice = authState.notice ?: return@LaunchedEffect
        noticeText?.let { snackbars.showSnackbar(it) }
        auth.acknowledgeNotice(notice.id)
    }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    val tabStateHolder = rememberSaveableStateHolder()
    val hasPurpleHeader = backStack.lastOrNull() == Home &&
        selectedTab in listOf(MainTab.HOME, MainTab.PROJECTS)
    BackHandler(enabled = backStack.lastOrNull() == Home && selectedTab != MainTab.HOME) {
        selectedTab = MainTab.HOME
    }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark && !hasPurpleHeader
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val onBack: () -> Unit = {
        if (backStack.lastOrNull() is Login) auth.cancelLogin()
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
    AppTheme(themeMode = theme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbars) },
            topBar = {
                if (hasPurpleHeader) Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(HomePurple))
            },
            bottomBar = {
                if (backStack.lastOrNull() == Home) {
                    NavigationBar {
                        MainTab.entries.forEach { tab ->
                            NavigationBarItem(
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                                label = { Text(stringResource(tab.label)) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavDisplay(
                modifier = Modifier.padding(padding),
                backStack = backStack,
                onBack = onBack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<Home> {
                        // All tabs share the main entry's ViewModel lifetime, but save UI state separately.
                        tabStateHolder.SaveableStateProvider(selectedTab.name) {
                            when (selectedTab) {
                                MainTab.HOME -> HomeRoute(collectedIds = account?.collectedIds.orEmpty(), collecting = authState.busy, onCollect = auth::collect)
                                MainTab.SYSTEM -> TabScreen(R.string.tab_system)
                                MainTab.PROJECTS -> ProjectsRoute()
                                MainTab.ME -> TabScreen(R.string.tab_me) {
                                    androidx.compose.material3.IconButton(onClick = auth::openLogin, enabled = !authState.busy && account == null,
                                        modifier = Modifier.testTag("profile_avatar")) {
                                        Icon(painterResource(R.drawable.ic_tab_me), contentDescription = stringResource(R.string.avatar))
                                    }
                                    Text(account?.username ?: stringResource(if (session is SessionState.Restoring) R.string.session_restoring else R.string.tap_avatar_login))
                                    if (session is SessionState.Unavailable) Button(onClick = auth::restoreSession, enabled = !authState.busy) { Text(stringResource(R.string.session_retry)) }
                                    if (account != null) Button(onClick = auth::logout, enabled = !authState.busy) { Text(stringResource(R.string.logout)) }
                                    Button(onClick = { backStack.add(Settings) }) {
                                        Text(stringResource(com.bgd.myapplication.feature.home.R.string.open_settings))
                                    }
                                }
                            }
                        }
                    }
                    entry<Settings> { SettingsRoute(onBack = onBack) }
                    entry<Login> { key -> LoginRoute(onBack, { auth.loginCompleted(key.requestId) }) }
                },
            )
        }
    }
}

@Composable
private fun TabScreen(title: Int, content: @Composable () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium)
        content()
    }
}
