package com.one.animetvone

import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.model.Anime1Item
import com.one.animetvone.data.repository.AnimeRepositoryImpl
import com.one.animetvone.data.source.Anime1Service
import com.one.animetvone.data.source.AnimeListDiskCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AnimeListDiskCacheTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val cacheFile = tempFolder.newFile("anime_list_cache.json")
        AnimeListDiskCache.setCacheFile(cacheFile)
        AnimeListDiskCache.clearCache()
        AnimeListDiskCache.setCacheFile(cacheFile)
    }

    @After
    fun tearDown() {
        AnimeListDiskCache.clearCache()
        Dispatchers.resetMain()
    }

    @Test
    fun `saveToDisk and loadFromDisk correctly persists anime list`() {
        val listToSave = listOf(
            Anime(
                id = "101",
                title = "Test Disk Anime 1",
                coverUrl = "https://example.com/cover1.jpg",
                bannerUrl = "https://example.com/banner1.jpg",
                description = "Test description",
                rating = 8.8,
                genres = listOf("TV", "2024年春"),
                year = 2024,
                status = "12話",
                episodeCount = 12,
                follow = true,
                progress = 5
            )
        )

        AnimeListDiskCache.saveToDisk(listToSave)

        val loadedList = AnimeListDiskCache.loadFromDisk()
        assertEquals(1, loadedList.size)
        val item = loadedList.first()
        assertEquals("101", item.id)
        assertEquals("Test Disk Anime 1", item.title)
        assertTrue(item.follow)
        assertEquals(5, item.progress)
    }

    @Test
    fun `loadFromDisk repairs episode counts stored by builds with the range bug`() {
        // Exactly what an older build wrote: "1-12" had been stored as 1 episode.
        val legacyFile = tempFolder.newFile("legacy_anime_cache.json")
        legacyFile.writeText(
            """
            [
              {"id":"201","title":"Range Show","coverUrl":"","bannerUrl":"","description":"","rating":0.0,"year":2026,"status":"1-12","episodeCount":1,"follow":false,"progress":0,"genres":[]},
              {"id":"202","title":"Ongoing Show","coverUrl":"","bannerUrl":"","description":"","rating":0.0,"year":2026,"status":"連載中(3)","episodeCount":3,"follow":false,"progress":0,"genres":[]}
            ]
            """.trimIndent()
        )

        AnimeListDiskCache.clearCache()
        AnimeListDiskCache.setCacheFile(legacyFile)

        val loaded = AnimeListDiskCache.loadFromDisk().associateBy { it.id }

        assertEquals(12, loaded.getValue("201").episodeCount)
        // Already-correct counts are left alone.
        assertEquals(3, loaded.getValue("202").episodeCount)
    }

    @Test
    fun `AnimeRepositoryImpl loads disk cache instantly and merges background sync preserving state`() = runTest {
        val initialCachedAnime = listOf(
            Anime(
                id = "1",
                title = "Cyber Neon: Edge Protocol",
                coverUrl = "https://example.com/old_cover.jpg",
                bannerUrl = "https://example.com/old_cover.jpg",
                description = "Old description",
                rating = 8.5,
                genres = listOf("TV"),
                year = 2024,
                status = "12話",
                episodeCount = 12,
                follow = true,
                progress = 3
            )
        )

        AnimeListDiskCache.saveToDisk(initialCachedAnime)

        val mockService = object : Anime1Service() {
            override suspend fun fetchAnimeList() = listOf(
                Anime1Item(1, "Cyber Neon: Edge Protocol", "12話", "2024", "春", "極影"),
                Anime1Item(2, "Shadow Arise", "連載中", "2024", "冬", "動漫國")
            )

            override suspend fun fetchCoverImage(title: String) = "https://example.com/cover.jpg"
        }

        val repository = AnimeRepositoryImpl(anime1Service = mockService, ioDispatcher = testDispatcher)

        // 1. Instant local read check
        val instantList = repository.getAllAnimeList().first()
        assertTrue(instantList.isNotEmpty())
        val initialCyber = instantList.find { it.id == "1" }
        assertEquals("Cyber Neon: Edge Protocol", initialCyber?.title)
        assertTrue(initialCyber?.follow == true)
        assertEquals(3, initialCyber?.progress)

        // 2. Wait for silent background sync
        advanceUntilIdle()

        val syncedList = repository.getAllAnimeList().first()
        assertEquals(2, syncedList.size)

        val mergedCyber = syncedList.find { it.id == "1" }
        assertEquals("Cyber Neon: Edge Protocol", mergedCyber?.title)
        assertTrue("Follow state must be preserved", mergedCyber?.follow == true)
        assertEquals("Progress must be preserved", 3, mergedCyber?.progress)

        val shadowArise = syncedList.find { it.id == "2" }
        assertEquals("Shadow Arise", shadowArise?.title)
        assertFalse(shadowArise?.follow == true)
    }
}
