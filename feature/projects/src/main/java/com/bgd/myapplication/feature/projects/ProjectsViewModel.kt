package com.bgd.myapplication.feature.projects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bgd.myapplication.core.data.ProjectRepository
import com.bgd.myapplication.core.model.Project
import com.bgd.myapplication.core.model.ProjectCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProjectsUiState(
    val categories: List<ProjectCategory> = emptyList(),
    val categoriesLoading: Boolean = true,
    val categoriesFailed: Boolean = false,
    val selectedId: Int? = null,
    val projects: List<Project> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
    val endReached: Boolean = false,
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val repository: ProjectRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProjectsUiState())
    val uiState = mutableState.asStateFlow()
    private var categoryRequest: Job? = null
    private var pageRequest: Job? = null
    private var generation = 0
    private var nextPage = 1

    init { loadCategories() }

    fun loadCategories() {
        if (categoryRequest?.isActive == true) return
        categoryRequest = viewModelScope.launch {
            mutableState.update { it.copy(categoriesLoading = true, categoriesFailed = false) }
            try {
                val categories = repository.categories()
                mutableState.update { it.copy(categories = categories, categoriesLoading = false) }
                val savedId = savedState.get<Int>("project_category")
                (categories.firstOrNull { it.id == savedId } ?: categories.firstOrNull())?.let { selectCategory(it.id) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(categoriesLoading = false, categoriesFailed = true) }
            }
        }
    }

    fun selectCategory(id: Int) {
        if (id == mutableState.value.selectedId || mutableState.value.categories.none { it.id == id }) return
        generation++
        pageRequest?.cancel()
        pageRequest = null
        nextPage = 1
        savedState["project_category"] = id
        mutableState.update { it.copy(selectedId = id, projects = emptyList(), loading = false, failed = false, endReached = false) }
        loadMore()
    }

    fun loadMore() {
        val category = mutableState.value.selectedId ?: return
        if (pageRequest?.isActive == true || mutableState.value.endReached) return
        val currentGeneration = generation
        pageRequest = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, failed = false) }
            try {
                val page = repository.projects(category, nextPage)
                if (currentGeneration != generation) return@launch
                nextPage++
                mutableState.update { it.copy(projects = (it.projects + page.projects).distinctBy(Project::id),
                    loading = false, endReached = page.endReached) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (currentGeneration == generation) mutableState.update { it.copy(loading = false, failed = true) }
            }
        }
    }
}
