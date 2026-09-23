package com.one.animetvone.ui.animelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.repository.AnimeRepository
import com.one.animetvone.data.repository.AnimeRepositoryProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil

data class AnimeListUiState(
    val allAnimeList: List<Anime> = emptyList(),
    val filteredAnimeList: List<Anime> = emptyList(),
    val currentPageItems: List<Anime> = emptyList(),
    val searchQuery: String = "",
    val selectedSeason: String = "全部",
    val availableSeasons: List<String> = listOf("全部"),
    val currentPage: Int = 1,
    val itemsPerPage: Int = 24,
    val totalPages: Int = 1,
    val totalItemsCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AnimeListViewModel(
    private val repository: AnimeRepository = AnimeRepositoryProvider.instance,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimeListUiState())
    val uiState: StateFlow<AnimeListUiState> = _uiState.asStateFlow()

    private var filterAndPaginationJob: Job? = null
    private var pageSwitchJob: Job? = null
    @Volatile
    private var allAnimeCache: List<Anime> = emptyList()

    init {
        loadAnimeList()
    }

    fun loadAnimeList() {
        viewModelScope.launch(ioDispatcher) {
            repository.getAllAnimeList()
                .onStart {
                    _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                }
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.localizedMessage ?: "Failed to load anime list"
                        )
                    }
                }
                .collect { list ->
                    // Cover-only updates keep the same size: refresh items in
                    // place WITHOUT resetting the page (else browsing jumps
                    // to page 1 on every arriving cover). Structural changes
                    // (fresh sync / filter source swap) reset to page 1.
                    val structural = allAnimeCache.isEmpty() || allAnimeCache.size != list.size
                    allAnimeCache = list
                    // Season chips: prefer parsed "YYYY年X" tags and sort newest-first
                    // so the row covers up to the current year (e.g. 2026) instead of
                    // freezing at whatever the cached sample list contained.
                    val seasonPattern = Regex("""(\d{4})年([春夏秋冬])""")
                    val seasons = listOf("全部") + list.mapNotNull { anime ->
                        anime.genres.firstNotNullOfOrNull { genre ->
                            seasonPattern.find(genre)?.value
                        } ?: anime.genres.find { it.contains("年") || it.contains("季") }
                    }.distinct().sortedWith { a, b ->
                        val ma = seasonPattern.find(a)
                        val mb = seasonPattern.find(b)
                        if (ma != null && mb != null) {
                            val yearCmp = mb.groupValues[1].compareTo(ma.groupValues[1])
                            if (yearCmp != 0) yearCmp else {
                                val order = mapOf("冬" to 0, "秋" to 1, "夏" to 2, "春" to 3)
                                (order[mb.groupValues[2]] ?: 9).compareTo(order[ma.groupValues[2]] ?: 9)
                            }
                        } else if (ma != null) -1
                        else if (mb != null) 1
                        else a.compareTo(b)
                    }.take(20)

                    _uiState.update {
                        it.copy(
                            allAnimeList = list,
                            availableSeasons = seasons,
                            isLoading = false
                        )
                    }
                    applyFiltersAndPagination(resetPage = structural)
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        if (_uiState.value.searchQuery == query) return
        _uiState.update { it.copy(searchQuery = query) }
        applyFiltersAndPagination(resetPage = true)
    }

    fun onSeasonSelected(season: String) {
        if (_uiState.value.selectedSeason == season) return
        _uiState.update { it.copy(selectedSeason = season) }
        applyFiltersAndPagination(resetPage = true)
    }

    fun goToPage(page: Int) {
        val state = _uiState.value
        val targetPage = page.coerceIn(1, maxOf(1, state.totalPages))
        if (targetPage == state.currentPage && state.currentPageItems.isNotEmpty()) return

        pageSwitchJob?.cancel()
        pageSwitchJob = viewModelScope.launch(ioDispatcher) {
            val currentState = _uiState.value
            val filtered = currentState.filteredAnimeList
            val itemsPerPage = currentState.itemsPerPage

            val pageItems = preResolvePageItems(filtered, targetPage, itemsPerPage)

            _uiState.update {
                it.copy(
                    currentPage = targetPage,
                    currentPageItems = pageItems
                )
            }
        }
    }

    fun nextPage() {
        goToPage(_uiState.value.currentPage + 1)
    }

    fun previousPage() {
        goToPage(_uiState.value.currentPage - 1)
    }

    private fun applyFiltersAndPagination(resetPage: Boolean) {
        filterAndPaginationJob?.cancel()
        filterAndPaginationJob = viewModelScope.launch(ioDispatcher) {
            val state = _uiState.value
            val query = state.searchQuery.trim()
            val season = state.selectedSeason
            val sourceList = allAnimeCache.ifEmpty { state.allAnimeList }

            val filtered = if (query.isBlank() && season == "全部") {
                sourceList
            } else {
                sourceList.filter { anime ->
                    val matchesQuery = if (query.isBlank()) true else {
                        anime.title.contains(query, ignoreCase = true) ||
                                anime.description.contains(query, ignoreCase = true) ||
                                anime.genres.any { it.contains(query, ignoreCase = true) }
                    }

                    val matchesSeason = if (season == "全部") true else {
                        anime.genres.contains(season) || anime.description.contains(season)
                    }

                    matchesQuery && matchesSeason
                }
            }

            val itemsPerPage = state.itemsPerPage
            val totalPages = maxOf(1, ceil(filtered.size / itemsPerPage.toDouble()).toInt())
            val targetPage = if (resetPage) 1 else state.currentPage.coerceIn(1, totalPages)

            val pageItems = preResolvePageItems(filtered, targetPage, itemsPerPage)

            _uiState.update {
                it.copy(
                    filteredAnimeList = filtered,
                    totalItemsCount = filtered.size,
                    totalPages = totalPages,
                    currentPage = targetPage,
                    currentPageItems = pageItems
                )
            }
        }
    }

    private fun preResolvePageItems(
        filteredList: List<Anime>,
        page: Int,
        itemsPerPage: Int
    ): List<Anime> {
        val startIndex = (page - 1) * itemsPerPage
        if (startIndex >= filteredList.size || filteredList.isEmpty()) {
            return emptyList()
        }

        val endIndex = minOf(startIndex + itemsPerPage, filteredList.size)
        return filteredList.subList(startIndex, endIndex)
    }
}
