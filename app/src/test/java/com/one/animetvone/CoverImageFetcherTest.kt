package com.one.animetvone

import com.one.animetvone.data.source.CoverImageFetcher
import com.one.animetvone.data.source.PrepopulatedCovers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverImageFetcherTest {

    @Test
    fun `cleanAnimeTitle removes bracketed text and subtitle tags`() {
        val title1 = "[字幕組] 葬送的芙莉蓮 (重製版) [1080p]"
        val cleaned1 = CoverImageFetcher.cleanAnimeTitle(title1)
        assertEquals("葬送的芙莉蓮", cleaned1)

        val title2 = "【動漫國】 鬼滅之刃 第2季 [720p]"
        val cleaned2 = CoverImageFetcher.cleanAnimeTitle(title2)
        assertEquals("鬼滅之刃", cleaned2)

        val title3 = "咒術迴戰 [01-24]"
        val cleaned3 = CoverImageFetcher.cleanAnimeTitle(title3)
        assertEquals("咒術迴戰", cleaned3)
    }

    @Test
    fun `PrepopulatedCovers returns high-res poster for popular titles`() {
        val frieren = PrepopulatedCovers.getPrepopulatedCover("葬送的芙莉蓮")
        assertNotNull(frieren)
        assertTrue(frieren!!.contains("bgm.tv") || frieren.contains("anilist") || frieren.startsWith("http"))

        val demonSlayer = PrepopulatedCovers.getPrepopulatedCover("鬼滅之刃")
        assertNotNull(demonSlayer)

        val jujutsu = PrepopulatedCovers.getPrepopulatedCover("咒術迴戰")
        assertNotNull(jujutsu)

        val spyFamily = PrepopulatedCovers.getPrepopulatedCover("間諜家家酒")
        assertNotNull(spyFamily)

        val chainsaw = PrepopulatedCovers.getPrepopulatedCover("鏈鋸人")
        assertNotNull(chainsaw)

        val oshiNoKo = PrepopulatedCovers.getPrepopulatedCover("我推的孩子")
        assertNotNull(oshiNoKo)

        val slime = PrepopulatedCovers.getPrepopulatedCover("轉生史萊姆")
        assertNotNull(slime)

        val haikyuu = PrepopulatedCovers.getPrepopulatedCover("排球少年")
        assertNotNull(haikyuu)

        val kaiju = PrepopulatedCovers.getPrepopulatedCover("怪獸8號")
        assertNotNull(kaiju)
    }

    @Test
    fun `getCoverUrlFast returns valid 0ms cover immediately for prepopulated titles`() {
        val url = CoverImageFetcher.getCoverUrlFast(null, "[極影] 葬送的芙莉蓮 [1080p]")
        assertNotNull(url)
        assertTrue(url!!.startsWith("http://") || url.startsWith("https://"))
    }

    @Test
    fun `getCoverUrlFast returns null for unknown titles without unsplash fallback`() {
        val unknownUrl = CoverImageFetcher.getCoverUrlFast(null, "完全未知的隨機動畫標題 XYZ 99999")
        assertNull(unknownUrl)
    }

    @Test
    fun `getCoverUrl returns a valid non-empty cover URL string for prepopulated title`() = runBlocking {
        val url = CoverImageFetcher.getCoverUrl(null, "葬送的芙莉蓮")
        assertNotNull(url)
        assertTrue(url!!.startsWith("http://") || url.startsWith("https://"))
    }
}
