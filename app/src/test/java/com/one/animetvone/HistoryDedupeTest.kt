package com.one.animetvone

import com.one.animetvone.data.model.HistoryItem
import com.one.animetvone.data.source.HistoryManager
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The same episode used to end up in history more than once because it can be
 * addressed by two id shapes: "<anime>_ep_3" (fallback list) and
 * "<anime>_ep_3_<token>" (scraped list). History is now keyed on the episode
 * number, and old duplicates are collapsed on read.
 */
class HistoryDedupeTest {

    private fun item(
        animeId: String,
        episodeId: String,
        timestamp: Long,
        positionMs: Long = 1_000L,
        episodeNumber: Int = 0,
        episodeTitle: String = ""
    ) = HistoryItem(
        animeId = animeId,
        animeTitle = "Anime $animeId",
        coverUrl = "",
        episodeId = episodeId,
        episodeTitle = episodeTitle.ifEmpty { episodeId },
        playbackPositionMs = positionMs,
        durationMs = 24 * 60 * 1000L,
        timestamp = timestamp,
        episodeNumber = episodeNumber
    )

    @Test
    fun `episode number is recovered from both id shapes`() {
        assertEquals(3, HistoryManager.episodeNumberFromId("2003_ep_3"))
        assertEquals(12, HistoryManager.episodeNumberFromId("2003_ep_12_%7B%22t%22%3A%22x%22%7D"))
        assertEquals(0, HistoryManager.episodeNumberFromId("no-marker-here"))
    }

    @Test
    fun `same anime and episode under two id shapes collapses to one entry`() {
        val items = listOf(
            item("2003", "2003_ep_3_%7B%22a%22%7D", timestamp = 900L, episodeNumber = 3),
            item("2003", "2003_ep_3", timestamp = 500L)
        )

        val result = HistoryManager.dedupeByEpisode(items)

        assertEquals(1, result.size)
        // The newest record wins.
        assertEquals("2003_ep_3_%7B%22a%22%7D", result[0].episodeId)
        assertEquals(900L, result[0].timestamp)
    }

    @Test
    fun `different episodes of the same anime stay independent`() {
        val items = listOf(
            item("2003", "2003_ep_3_abc", timestamp = 900L, episodeNumber = 3),
            item("2003", "2003_ep_4_def", timestamp = 800L, episodeNumber = 4),
            item("2004", "2004_ep_3_ghi", timestamp = 700L, episodeNumber = 3)
        )

        val result = HistoryManager.dedupeByEpisode(items)

        assertEquals(3, result.size)
        assertEquals(listOf(3, 4, 3), result.map { it.episodeNumber })
    }

    @Test
    fun `legacy records without an episode number still dedupe by parsed number`() {
        val items = listOf(
            item("77", "77_ep_9_newtoken", timestamp = 900L),
            item("77", "77_ep_9_oldtoken", timestamp = 100L)
        )

        val result = HistoryManager.dedupeByEpisode(items)

        assertEquals(1, result.size)
        assertEquals("77_ep_9_newtoken", result[0].episodeId)
    }

    @Test
    fun `records without a usable episode number are kept as-is`() {
        val items = listOf(
            item("88", "88_weird", timestamp = 900L),
            item("88", "88_other", timestamp = 100L)
        )

        val result = HistoryManager.dedupeByEpisode(items)

        assertEquals(2, result.size)
    }
}
