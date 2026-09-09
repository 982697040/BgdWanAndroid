package com.bgd.myapplication

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.fillMaxWidth
import com.bgd.myapplication.feature.home.HomePurple
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
import androidx.compose.runtime.getValue
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
fun BgdApp(viewModel: AppViewModel = hiltViewModel()) {
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val dark = when (theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val backStack = rememberNavBackStack(Home)
    val isHome = backStack.lastOrNull() == Home
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark && !isHome
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    val onBack: () -> Unit = {
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }
    AppTheme(themeMode = theme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (isHome) Box(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars).background(HomePurple))
            },
            bottomBar = {
                if (backStack.lastOrNull() != Settings) {
                    NavigationBar {
                        MainTab.entries.forEach { tab ->
                            NavigationBarItem(
                                modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
                                selected = backStack.lastOrNull() == tab.key,
                                onClick = {
                                    if (backStack.lastOrNull() != tab.key) {
                                        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                                        if (tab.key != Home) backStack.add(tab.key)
                                    }
                                },
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
                        HomeRoute()
                    }
                    entry<Settings> { SettingsRoute(onBack = onBack) }
                    entry<System> { TabScreen(R.string.tab_system) }
                    entry<Projects> { TabScreen(R.string.tab_projects) }
                    entry<Me> {
                        TabScreen(R.string.tab_me) {
                            Button(onClick = { backStack.add(Settings) }) {
                                Text(stringResource(com.bgd.myapplication.feature.home.R.string.open_settings))
                            }
                        }
                    }
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
