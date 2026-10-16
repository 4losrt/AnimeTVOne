package com.one.animetvone.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.model.Episode
import com.one.animetvone.data.model.VideoStream
import com.one.animetvone.data.repository.AnimeRepository
import com.one.animetvone.data.repository.AnimeRepositoryProvider
import com.one.animetvone.data.repository.FavoritesManager
import com.one.animetvone.data.source.logD
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerUiState(
    val episodeId: String? = null,
    val currentEpisode: Episode? = null,
    val currentAnime: Anime? = null,
    val episodes: List<Episode> = emptyList(),
    val videoStream: VideoStream? = null,
    val isPlaying: Boolean = false,
    val areControlsVisible: Boolean = true,
    val playbackPosition: Long = 0L,
    val duration: Long = 0L,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isFavorite: Boolean = false
)

class PlayerViewModel(
    private val repository: AnimeRepository = AnimeRepositoryProvider.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            FavoritesManager.favoritesState.collect { favSet ->
                val currentAnimeId = _uiState.value.currentAnime?.id
                if (currentAnimeId != null) {
                    _uiState.update { it.copy(isFavorite = favSet.contains(currentAnimeId)) }
                }
            }
        }
    }

    fun loadEpisode(episodeId: String) {
        logD("Anime1Video", "PlayerViewModel loading episode: $episodeId")
        if (_uiState.value.episodeId == episodeId && _uiState.value.isLoading) return

        // Resolve the episode number up front so the player header can show a
        // meaningful "第 N 集" label immediately, instead of waiting for the
        // episode list (which downloads lazily and may race with the stream).
        val animeId = episodeId.split("_ep_").firstOrNull() ?: ""
        val epNumber = episodeId.split("_ep_").getOrNull(1)?.split("_")?.firstOrNull()?.toIntOrNull() ?: 0
        val placeholderTitle = if (epNumber > 0) "第 $epNumber 集" else ""

        // Preserve a previously-resolved currentEpisode only when it matches;
        // otherwise drop stale title/number inherited from the last episode
        // (otherwise the header shows the previous episode while the new one
        //  loads).
        _uiState.update { state ->
            val keep = state.currentEpisode?.takeIf { it.id == episodeId }
            state.copy(
                episodeId = episodeId,
                isLoading = true,
                errorMessage = null,
                playbackPosition = 0L,
                currentAnime = if (state.episodeId != episodeId) null else state.currentAnime,
                currentEpisode = keep ?: Episode(
                    id = episodeId,
                    animeId = animeId,
                    episodeNumber = epNumber,
                    title = placeholderTitle,
                    thumbnailUrl = "",
                    duration = "24:00",
                    streamUrl = ""
                )
            )
        }

        viewModelScope.launch {
            repository.getVideoStream(episodeId)
                .onStart {
                    _uiState.update { it.copy(isLoading = true) }
                }
                .catch { exception ->
                    logD("Anime1Video", "Error resolving video stream: ${exception.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.localizedMessage ?: "影片載入失敗，請重試"
                        )
                    }
                }
                .collect { stream ->
                    if (stream != null) {
                        logD("Anime1Video", "Stream resolved: ${stream.url}")
                        logD("Anime1Video", "Stream headers: ${stream.headers}")
                    } else {
                        logD("Anime1Video", "Stream resolution returned null")
                    }
                    _uiState.update {
                        it.copy(
                            videoStream = stream,
                            isLoading = false,
                            errorMessage = if (stream == null) "影片載入失敗，請重試" else null
                        )
                    }
                }
        }

        // Resolve parent anime and full episode list for next/prev support
        viewModelScope.launch {
            val animeId = episodeId.split("_ep_").firstOrNull() ?: return@launch
            repository.getAnimeDetails(animeId).collect { anime ->
                _uiState.update { state ->
                    val isFav = anime?.let { FavoritesManager.favoritesState.value.contains(it.id) } ?: false
                    state.copy(
                        currentAnime = anime,
                        isFavorite = isFav
                    )
                }
            }
        }

        viewModelScope.launch {
            val animeId = episodeId.split("_ep_").firstOrNull() ?: return@launch
            repository.getEpisodes(animeId).collect { episodes ->
                val ep = episodes.find { it.id == episodeId }
                    ?: episodes.find { e ->
                        val num = episodeId.split("_ep_").getOrNull(1)?.split("_")?.firstOrNull()?.toIntOrNull()
                        num != null && e.episodeNumber == num
                    }
                _uiState.update {
                    it.copy(
                        episodes = episodes,
                        currentEpisode = ep ?: it.currentEpisode
                    )
                }
            }
        }
    }

    fun checkFavorite(context: Context) {
        val animeId = _uiState.value.currentAnime?.id ?: return
        val isFav = FavoritesManager.isFavorite(context, animeId)
        _uiState.update { it.copy(isFavorite = isFav) }
    }

    fun toggleFavorite(context: Context) {
        val animeId = _uiState.value.currentAnime?.id ?: return
        val newFav = FavoritesManager.toggleFavorite(context, animeId)
        _uiState.update { it.copy(isFavorite = newFav) }
    }

    fun hasNextEpisode(): Boolean {
        val state = _uiState.value
        val currentIndex = state.episodes.indexOfFirst { it.id == state.episodeId }
        return currentIndex != -1 && currentIndex < state.episodes.size - 1
    }

    fun hasPreviousEpisode(): Boolean {
        val state = _uiState.value
        val currentIndex = state.episodes.indexOfFirst { it.id == state.episodeId }
        return currentIndex > 0
    }

    fun playNextEpisode() {
        val state = _uiState.value
        if (state.isLoading || state.videoStream == null) return
        val currentIndex = state.episodes.indexOfFirst { it.id == state.episodeId }
        if (currentIndex != -1 && currentIndex < state.episodes.size - 1) {
            val nextEp = state.episodes[currentIndex + 1]
            loadEpisode(nextEp.id)
        }
    }

    fun playPreviousEpisode() {
        val state = _uiState.value
        if (state.isLoading || state.videoStream == null) return
        val currentIndex = state.episodes.indexOfFirst { it.id == state.episodeId }
        if (currentIndex > 0) {
            val prevEp = state.episodes[currentIndex - 1]
            loadEpisode(prevEp.id)
        }
    }

    fun setPlaying(isPlaying: Boolean) {
        _uiState.update { it.copy(isPlaying = isPlaying) }
    }

    fun setControlsVisible(visible: Boolean) {
        _uiState.update { it.copy(areControlsVisible = visible) }
    }

    fun toggleControlsVisibility() {
        _uiState.update { it.copy(areControlsVisible = !it.areControlsVisible) }
    }

    fun updatePlaybackPosition(position: Long, duration: Long) {
        _uiState.update {
            it.copy(
                playbackPosition = position,
                duration = duration
            )
        }
    }

    fun retryEpisode() {
        val id = _uiState.value.episodeId ?: return
        repository.evictVideoStream(id)
        loadEpisode(id)
    }

    fun setPlaybackError(message: String?) {
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = message
            )
        }
    }
}
