package com.one.animetvone

import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.model.Anime1EpisodeToken
import com.one.animetvone.data.model.Anime1Item
import com.one.animetvone.data.model.VideoStream
import com.one.animetvone.data.repository.AnimeRepositoryImpl
import com.one.animetvone.data.source.Anime1Service
import com.one.animetvone.ui.components.episodeCountLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * animelist.json stores episode totals as a range, not a count: "1-12",
 * "13-24", "1-25", "1-12+OVA", "連載中(41)", "劇場版". Reading only the first
 * number made every show look like a 1-episode series ("全 1 集").
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeCountStatusTest {

    private val testDispatcher = StandardTestDispatcher()

    private val rows = listOf(
        Anime1Item(101, "Range One Twelve", "1-12", "2026", "夏", ""),
        Anime1Item(102, "Range Thirteen TwentyFour", "13-24", "2026", "夏", ""),
        Anime1Item(103, "Range One TwentyFive", "1-25", "2026", "夏", ""),
        Anime1Item(104, "Range With Ova", "1-12+OVA", "2026", "夏", ""),
        Anime1Item(105, "Ongoing Show", "連載中(41)", "2026", "秋", ""),
        Anime1Item(106, "The Movie", "劇場版", "2026", "夏", ""),
        Anime1Item(107, "Plain Count", "12話", "2026", "夏", ""),
        Anime1Item(108, "Real Episodes Exceed Status", "1-12", "2026", "夏", "")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun repositoryWith(episodeCount: Int) = AnimeRepositoryImpl(
        anime1Service = object : Anime1Service() {
            override suspend fun fetchAnimeList() = rows

            override suspend fun fetchEpisodeTokens(animeId: String) =
                List(episodeCount) { index ->
                    Anime1EpisodeToken(index + 1, "第 ${index + 1} 話", "token_${index + 1}")
                }

            override suspend fun fetchDirectStream(token: String) =
                VideoStream(url = "https://v.anime1.me/x.mp4", quality = "1080p", serverName = "CDN")

            override suspend fun fetchCoverImage(title: String) = ""
        }
    )

    /**
     * The repository resolves details through the same cache the episode flow
     * fills, so fetch the episode list first (as the player does) before asking
     * for the mapped Anime.
     */
    private suspend fun countOf(repository: AnimeRepositoryImpl, id: String): Int? {
        repository.getEpisodes(id).first()
        return repository.getAnimeDetails(id).first()?.episodeCount
    }

    @Test
    fun `status ranges report the last episode, not the first number`() = runTest {
        val repository = repositoryWith(episodeCount = 0)

        assertEquals(12, countOf(repository, "101"))
        assertEquals(24, countOf(repository, "102"))
        assertEquals(25, countOf(repository, "103"))
        assertEquals(12, countOf(repository, "104"))
        assertEquals(41, countOf(repository, "105"))
        assertEquals(0, countOf(repository, "106"))
        assertEquals(12, countOf(repository, "107"))
    }

    @Test
    fun `scraped episode list raises an under-reported status count`() = runTest {
        val repository = repositoryWith(episodeCount = 14)

        // Status claims "1-12", the page really has 14 episodes.
        val episodes = repository.getEpisodes("108").first()
        assertEquals(14, episodes.size)

        assertEquals(14, countOf(repository, "108"))
    }

    @Test
    fun `label distinguishes ongoing, finished and movie statuses`() {
        assertEquals("更新至 41 集", episodeCountLabel(anime(status = "連載中(41)", count = 41)))
        assertEquals("全 12 集", episodeCountLabel(anime(status = "1-12", count = 12)))
        assertEquals("連載中", episodeCountLabel(anime(status = "連載中", count = 0)))
        assertEquals("劇場版", episodeCountLabel(anime(status = "劇場版", count = 0)))
        assertEquals("集數未定", episodeCountLabel(anime(status = "", count = 0)))
    }

    private fun anime(status: String, count: Int) = Anime(
        id = "1",
        title = "T",
        coverUrl = "",
        bannerUrl = "",
        description = "",
        rating = 0.0,
        genres = emptyList(),
        year = 2026,
        status = status,
        episodeCount = count
    )
}
