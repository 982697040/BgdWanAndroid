package com.bgd.myapplication.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bgd.myapplication.core.data.BannerRepository
import com.bgd.myapplication.core.model.Banner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val banners: List<Banner> = emptyList(),
    val failed: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: BannerRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeUiState())
    val uiState = mutableState.asStateFlow()
    private var request: Job? = null

    init { loadBanners() }

    fun loadBanners() {
        if (request?.isActive == true) return
        request = viewModelScope.launch {
            mutableState.value = HomeUiState()
            try {
                mutableState.value = HomeUiState(loading = false, banners = repository.getBanners())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = HomeUiState(loading = false, failed = true)
            }
        }
    }
}
