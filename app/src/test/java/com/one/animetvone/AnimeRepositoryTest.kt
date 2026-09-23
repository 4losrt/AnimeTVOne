package com.one.animetvone

import com.one.animetvone.data.model.Anime1EpisodeToken
import com.one.animetvone.data.model.Anime1Item
import com.one.animetvone.data.model.VideoStream
import com.one.animetvone.data.repository.AnimeRepositoryImpl
import com.one.animetvone.data.source.Anime1Service
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AnimeRepositoryImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val testService = object : Anime1Service() {
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
        repository = AnimeRepositoryImpl(anime1Service = testService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getHomeFeed returns feed sections with non-empty anime lists`() = runTest {
        val sections = repository.getHomeFeed().first()
        assertTrue(sections.isNotEmpty())
        sections.forEach { section ->
            assertTrue(section.animes.isNotEmpty())
        }
    }

    @Test
    fun `searchAnime with query returns matching results`() = runTest {
        val results = repository.searchAnime(query = "Cyber", category = null).first()
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.title.contains("Cyber", ignoreCase = true) })
    }

    @Test
    fun `searchAnime with category filter returns matching results`() = runTest {
        val results = repository.searchAnime(query = "", category = "TV").first()
        assertTrue(results.isNotEmpty())
        results.forEach { anime ->
            assertTrue(anime.genres.contains("TV"))
        }
    }

    @Test
    fun `getAnimeDetails returns valid anime for existing id`() = runTest {
        val anime = repository.getAnimeDetails("1").first()
        assertNotNull(anime)
        assertEquals("Cyber Neon: Edge Protocol", anime?.title)
    }

    @Test
    fun `getEpisodes returns episode list for valid anime id`() = runTest {
        val episodes = repository.getEpisodes("1").first()
        assertTrue(episodes.isNotEmpty())
        assertEquals("1", episodes.first().animeId)
    }

    @Test
    fun `getVideoStream returns valid video stream for episode`() = runTest {
        val stream = repository.getVideoStream("1_ep_1_token_ep1").first()
        assertNotNull(stream)
        assertTrue(stream?.url?.isNotEmpty() == true)
    }
}
