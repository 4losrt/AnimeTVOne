package com.one.animetvone.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.model.Episode
import com.one.animetvone.data.model.HistoryItem
import com.one.animetvone.data.repository.AnimeRepository
import com.one.animetvone.data.repository.AnimeRepositoryProvider
import com.one.animetvone.data.repository.FavoritesManager
import com.one.animetvone.data.source.CoverImageFetcher
import com.one.animetvone.data.source.HistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DetailUiState(
    val anime: Anime? = null,
    val episodes: List<Episode> = emptyList(),
    val historyItems: List<HistoryItem> = emptyList(),
    val isFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class DetailViewModel(
    private val repository: AnimeRepository = AnimeRepositoryProvider.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    fun loadAnimeDetails(animeId: String, context: Context? = null) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        if (context != null) {
            val isFav = FavoritesManager.isFavorite(context, animeId)
            _uiState.update { it.copy(isFavorite = isFav) }
        }

        viewModelScope.launch {
            repository.getAnimeDetails(animeId)
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.localizedMessage ?: "Failed to load details"
                        )
                    }
                }
                .collect { anime ->
                    _uiState.update {
                        it.copy(
                            anime = anime,
                            isLoading = false
                        )
                    }
                }
        }

        viewModelScope.launch {
            repository.getEpisodes(animeId)
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.localizedMessage ?: "Failed to load episodes"
                        )
                    }
                }
                .collect { episodes ->
                    _uiState.update {
                        it.copy(episodes = episodes)
                    }
                }
        }

        // Playback history is read off the main thread: it parses a small JSON
        // document and must never run during composition.
        if (context != null) {
            viewModelScope.launch {
                val history = withContext(Dispatchers.IO) {
                    HistoryManager.getHistoryList(context)
                }
                _uiState.update { it.copy(historyItems = history) }
            }
        }
    }

    fun toggleFavorite(context: Context) {
        val animeId = _uiState.value.anime?.id ?: return
        val newFavState = FavoritesManager.toggleFavorite(context, animeId)
        _uiState.update { it.copy(isFavorite = newFavState) }
    }

    fun refreshCoverImage(context: Context) {
        val anime = _uiState.value.anime ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val newCover = CoverImageFetcher.evictAndRefreshCover(context, anime.title)
            if (!newCover.isNullOrBlank()) {
                val updated = anime.copy(coverUrl = newCover, bannerUrl = newCover)
                _uiState.update { it.copy(anime = updated, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
