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
                Anime1Item(1, "Cyber Neon: Edge Protocol", "12話", "2024", "春", "極影"),
                Anime1Item(2, "Shadow Arise", "連載中", "2024", "冬", "動漫國")
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

        // Season chips are derived from the combined "YYYY年X" genre tag built
        // from the real anime1 wire format (separate year + season fields).
        viewModel.onSearchQueryChanged("")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.availableSeasons.contains("2024年春"))
        assertTrue(viewModel.uiState.value.availableSeasons.contains("2024年冬"))

        viewModel.onSeasonSelected("2024年春")
        advanceUntilIdle()
        state = viewModel.uiState.value
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

    @Test
    fun `PlayerViewModel keeps playback speed and resize mode across episodes`() = runTest {
        val repository = AnimeRepositoryImpl(anime1Service = testService)
        val viewModel = PlayerViewModel(repository)

        viewModel.loadEpisode("1_ep_1_token_ep1")
        advanceUntilIdle()

        assertEquals(1.0f, viewModel.uiState.value.playbackSpeed, 0.001f)

        viewModel.setPlaybackSpeed(1.5f)
        viewModel.setResizeMode(3)
        assertEquals(1.5f, viewModel.uiState.value.playbackSpeed, 0.001f)
        assertEquals(3, viewModel.uiState.value.resizeMode)

        viewModel.playNextEpisode()
        advanceUntilIdle()

        // Speed and aspect ratio are user preferences and must survive an
        // episode change (they used to reset because they were local state).
        assertEquals("1_ep_2_token_ep2", viewModel.uiState.value.episodeId)
        assertEquals(1.5f, viewModel.uiState.value.playbackSpeed, 0.001f)
        assertEquals(3, viewModel.uiState.value.resizeMode)
    }

    @Test
    fun `PlayerViewModel drops stale episodes when switching anime`() = runTest {
        val repository = AnimeRepositoryImpl(anime1Service = testService)
        val viewModel = PlayerViewModel(repository)

        viewModel.loadEpisode("1_ep_1_token_ep1")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.episodes.isNotEmpty())

        // Switching to a different anime must immediately clear the previous
        // episode list so next/previous cannot operate on stale data.
        viewModel.loadEpisode("2_ep_1_sometoken")
        assertTrue(viewModel.uiState.value.episodes.isEmpty())
        assertFalse(viewModel.hasNextEpisode())
        advanceUntilIdle()
    }

    @Test
    fun `PlayerViewModel toggles the episode list and tracks retries`() = runTest {
        val repository = AnimeRepositoryImpl(anime1Service = testService)
        val viewModel = PlayerViewModel(repository)

        viewModel.loadEpisode("1_ep_1_token_ep1")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEpisodeListVisible)
        viewModel.toggleEpisodeList()
        assertTrue(viewModel.uiState.value.isEpisodeListVisible)
        viewModel.setEpisodeListVisible(false)
        assertFalse(viewModel.uiState.value.isEpisodeListVisible)

        assertEquals(0, viewModel.uiState.value.retryCount)
        viewModel.retryEpisode()
        assertEquals(1, viewModel.uiState.value.retryCount)
        advanceUntilIdle()
    }
}
