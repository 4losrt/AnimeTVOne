package com.one.animetvone.ui.favorites

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.repository.AnimeRepository
import com.one.animetvone.data.repository.AnimeRepositoryProvider
import com.one.animetvone.data.repository.FavoritesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val favoriteAnimeList: List<Anime> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class FavoritesViewModel(
    private val repository: AnimeRepository = AnimeRepositoryProvider.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    fun loadFavorites(context: Context) {
        val favIds = FavoritesManager.getFavoriteIds(context)
        _uiState.update { it.copy(favoriteIds = favIds) }

        if (favIds.isEmpty()) {
            _uiState.update { it.copy(favoriteAnimeList = emptyList(), isLoading = false) }
            return
        }

        viewModelScope.launch {
            repository.getFavoriteAnimeList(favIds)
                .onStart {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                }
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.localizedMessage ?: "Failed to load favorites"
                        )
                    }
                }
                .collect { list ->
                    _uiState.update {
                        it.copy(
                            favoriteAnimeList = list,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun removeFavorite(context: Context, animeId: String) {
        FavoritesManager.toggleFavorite(context, animeId)
        loadFavorites(context)
    }
}
