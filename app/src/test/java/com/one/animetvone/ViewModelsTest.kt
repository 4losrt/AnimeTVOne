package com.one.animetvone

import com.one.animetvone.data.model.Anime1EpisodeToken
import com.one.animetvone.data.model.Anime1Item
import com.one.animetvone.data.model.VideoStream
import com.one.animetvone.data.repository.AnimeRepositoryImpl
import com.one.animetvone.data.source.Anime1Service
import com.one.animetvone.ui.animelist.AnimeListViewModel
import com.one.animetvone.ui.detail.DetailViewModel
import com.one.animetvone.ui.home.HomeViewModel
import com.one.animetvone.ui.player.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testService: Anime1Service

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        testService = object : Anime1Service() {
            override suspend fun fetchAnimeList() = listOf(
                Anime1Item(1, "Cyber Neon: Edge Protocol", "12話", "2024年春", "極影", "TV"),
                Anime1Item(2, "Shadow Arise", "連載中", "2024年冬", "動漫國", "TV")
            )

            override suspend fun fetchEpisodeTokens(animeId: String) = listOf(
                Anime1EpisodeToken(1, "第 1 話", "token_ep1"),
                Anime1EpisodeToken(2, "第 2 話", "token_ep2")
            )

            override suspend fun fetchDirectStream(token: String) = VideoStream(
                url = "https://v.anime1.me/video/sample.mp4",
                quality = "1080p HD",
                serverName = "Anime1 CDN Direct"
            )

            override suspend fun fetchCoverImage(title: String) = "https://images.unsplash.com/photo-1578632767115-351597cf2477"
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `HomeViewModel loads home feed successfully`() = runTest {
        val repository = AnimeRepositoryImpl(anime1Service = testService)
        val viewModel = HomeViewModel(repository)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.sections.isNotEmpty())
        assertNotNull(state.featuredAnime)
    }

    @Test
    fun `AnimeListViewModel handles search query, filtering and pagination`() = runTest {
        val repository = AnimeRepositoryImpl(anime1Service = testService)
        val viewModel = AnimeListViewModel(repository, testDispatcher)

        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.filteredAnimeList.size)
        assertEquals(1, state.currentPage)

        viewModel.onSearchQueryChanged("Cyber")
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals("Cyber", state.searchQuery)
        assertEquals(1, state.filteredAnimeList.size)
        assertEquals("Cyber Neon: Edge Protocol", state.filteredAnimeList.first().title)
    }

    @Test
    fun `DetailViewModel loads anime details and episodes`() = runTest {
        val repository = AnimeRepositoryImpl(anime1Service = testService)
        val viewModel = DetailViewModel(repository)

        viewModel.loadAnimeDetails("1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.anime)
        assertEquals("1", state.anime?.id)
        assertTrue(state.episodes.isNotEmpty())
    }

    @Test
    fun `PlayerViewModel handles episode loading and navigation`() = runTest {
        val repository = AnimeRepositoryImpl(anime1Service = testService)
        val viewModel = PlayerViewModel(repository)

        viewModel.loadEpisode("1_ep_1_token_ep1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.videoStream)
        assertEquals("1_ep_1_token_ep1", state.episodeId)
        assertTrue(viewModel.hasNextEpisode())
        assertFalse(viewModel.hasPreviousEpisode())

        viewModel.playNextEpisode()
        advanceUntilIdle()

        val nextState = viewModel.uiState.value
        assertEquals("1_ep_2_token_ep2", nextState.episodeId)
        assertTrue(viewModel.hasPreviousEpisode())
    }
}
