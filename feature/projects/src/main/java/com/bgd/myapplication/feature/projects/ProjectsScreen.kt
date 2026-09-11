package com.bgd.myapplication.feature.projects

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.bgd.myapplication.core.model.Project
import com.bgd.myapplication.core.model.AppError
import com.bgd.myapplication.core.designsystem.localizedMessage

private val Purple = Color(0xFF7042C1)

@Composable
fun ProjectsRoute(viewModel: ProjectsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProjectsScreen(state, viewModel::selectCategory, viewModel::loadCategories, viewModel::loadMore)
}

@Composable
fun ProjectsScreen(state: ProjectsUiState, onSelect: (Int) -> Unit, onRetryCategories: () -> Unit, onLoadMore: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().background(Purple).height(56.dp).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.projects_title), Modifier.weight(1f), color = Color.White, fontSize = 22.sp)
            IconButton(onClick = { Toast.makeText(context, R.string.search_pending, Toast.LENGTH_SHORT).show() }) {
                Icon(painterResource(R.drawable.ic_search), stringResource(R.string.search), tint = Color.White)
            }
        }
        when {
            state.categoriesLoading -> StatusBox { Progress() }
            state.categoriesFailed -> StatusBox { RetryMessage(R.string.categories_error, onRetryCategories, state.categoryError) }
            state.categories.isEmpty() -> StatusBox { Text(stringResource(R.string.categories_empty)) }
            else -> {
                val tabs = rememberLazyListState()
                LaunchedEffect(state.selectedId) {
                    val index = state.categories.indexOfFirst { it.id == state.selectedId }
                    if (index >= 0) tabs.animateScrollToItem(index)
                }
                LazyRow(state = tabs, modifier = Modifier.fillMaxWidth().selectableGroup(),
                    contentPadding = PaddingValues(horizontal = 8.dp)) {
                    items(state.categories, key = { it.id }) { category ->
                        val selected = category.id == state.selectedId
                        Column(Modifier.testTag("project_category_${category.id}")
                            .selectable(selected, role = Role.Tab, onClick = { onSelect(category.id) })
                            .padding(horizontal = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.height(44.dp), contentAlignment = Alignment.Center) {
                                Text(category.name, fontSize = 14.sp,
                                    color = if (selected) Purple else MaterialTheme.colorScheme.onSurface)
                            }
                            Box(Modifier.width(48.dp).height(3.dp).clip(RoundedCornerShape(2.dp))
                                .background(if (selected) Purple else Color.Transparent))
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                key(state.selectedId) {
                    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.weight(1f).testTag("projects_grid"),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(state.projects, key = { it.id }) { ProjectCard(it) }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            LaunchedEffect(state.projects.size, state.selectedId) {
                                if (!state.loading && !state.failed && !state.endReached) onLoadMore()
                            }
                            Box(Modifier.fillMaxWidth().padding(16.dp).testTag("projects_footer"), contentAlignment = Alignment.Center) {
                                when {
                                    state.loading -> Progress()
                                    state.failed -> RetryMessage(R.string.projects_error, onLoadMore, state.projectError)
                                    state.endReached -> Text(stringResource(if (state.projects.isEmpty()) R.string.projects_empty else R.string.projects_end),
                                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBox(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun Progress() { CircularProgressIndicator(Modifier.size(24.dp), color = Purple) }

@Composable
private fun RetryMessage(message: Int, retry: () -> Unit, error: AppError? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(error?.localizedMessage() ?: stringResource(message), fontSize = 14.sp)
        TextButton(onClick = retry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun ProjectCard(project: Project) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var retry by remember(project.imageUrl) { mutableIntStateOf(0) }
    Card(onClick = {
        try {
            val uri = Uri.parse(project.url)
            require(uri.scheme in listOf("https", "http") && !uri.host.isNullOrBlank())
            uriHandler.openUri(project.url)
        } catch (_: Exception) {
            Toast.makeText(context, R.string.link_error, Toast.LENGTH_SHORT).show()
        }
    }, modifier = Modifier.fillMaxWidth().testTag("project_${project.id}"), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Box(Modifier.padding(8.dp).fillMaxWidth().aspectRatio(0.55f).clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) {
            if (project.imageUrl.isBlank()) Text(stringResource(R.string.image_empty), fontSize = 12.sp)
            else key(retry) {
                SubcomposeAsyncImage(model = project.imageUrl, contentDescription = project.title,
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter,
                    loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Progress() } },
                    error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = { retry++ }) { Text(stringResource(R.string.image_retry), fontSize = 12.sp) }
                    } })
            }
        }
        Text(project.title, Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
            fontSize = 12.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
