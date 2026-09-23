package com.one.animetvone.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.model.AnimeFeedSection
import com.one.animetvone.data.repository.AnimeRepository
import com.one.animetvone.data.repository.AnimeRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val sections: List<AnimeFeedSection> = emptyList(),
    val featuredAnime: Anime? = null,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: AnimeRepository = AnimeRepositoryProvider.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeFeed()
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            repository.getHomeFeed()
                .onStart {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                }
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.localizedMessage ?: "Failed to load feed"
                        )
                    }
                }
                .collect { sections ->
                    val featured = sections.firstOrNull { it.id == "featured" }?.animes?.firstOrNull()
                        ?: sections.firstOrNull()?.animes?.firstOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sections = sections,
                            featuredAnime = featured,
                            errorMessage = null
                        )
                    }
                }
        }
    }
}
