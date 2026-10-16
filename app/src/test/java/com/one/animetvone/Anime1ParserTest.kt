package com.one.animetvone

import com.one.animetvone.data.model.Anime1Item
import com.one.animetvone.data.source.Anime1Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Anime1ParserTest {

    @Test
    fun `parseAnimeListJson filters out invalid IDs and parses items correctly`() {
        // Real anime1.me wire format: [id, title, episodeCountOrStatus, year, season, subtitleGroup]
        val json = """
            [
                [101, "Cyber Neon: Edge Protocol", "12話", "2024", "春", "極影字幕組"],
                [0, "Adult / Invalid Entry", "1話", "", "", ""],
                [-1, "Negative ID Entry", "0話", "", "", ""],
                [202, "Shadow Arise", "連載中", "2024", "冬", "動漫國"]
            ]
        """.trimIndent()

        val items = Anime1Parser.parseAnimeListJson(json)

        assertEquals(2, items.size)
        assertEquals(101, items[0].id)
        assertEquals("Cyber Neon: Edge Protocol", items[0].title)
        assertEquals("12話", items[0].episodeCountOrStatus)
        assertEquals("2024", items[0].year)
        assertEquals("春", items[0].season)
        assertEquals("極影字幕組", items[0].subtitleGroup)

        assertEquals(202, items[1].id)
        assertEquals("Shadow Arise", items[1].title)
    }

    @Test
    fun `parseEpisodesFromHtml extracts video tokens and episode titles`() {
        val html = """
            <html>
                <body>
                    <article>
                        <h2 class="entry-title"><a href="https://anime1.me/1234">Cyber Neon [01]</a></h2>
                        <video data-apireq="sample_token_ep1_abc123"></video>
                    </article>
                    <article>
                        <h2 class="entry-title"><a href="https://anime1.me/1235">Cyber Neon [02]</a></h2>
                        <video data-apireq="sample_token_ep2_xyz789"></video>
                    </article>
                </body>
            </html>
        """.trimIndent()

        val episodes = Anime1Parser.parseEpisodesFromHtml(html)

        assertEquals(2, episodes.size)
        assertEquals(1, episodes[0].episodeNumber)
        assertEquals("sample_token_ep1_abc123", episodes[0].token)
        assertEquals("Cyber Neon [01]", episodes[0].title)

        assertEquals(2, episodes[1].episodeNumber)
        assertEquals("sample_token_ep2_xyz789", episodes[1].token)
    }

    @Test
    fun `parseDirectStreamUrl parses stream src from API JSON`() {
        val json = """
            {
                "s": [
                    { "src": "//v.anime1.me/video/sample_1080p.mp4?e=12345" }
                ]
            }
        """.trimIndent()

        val streamUrl = Anime1Parser.parseDirectStreamUrl(json)

        assertNotNull(streamUrl)
        assertEquals("https://v.anime1.me/video/sample_1080p.mp4?e=12345", streamUrl)
    }

    @Test
    fun `videoCookieC filters matching keys e, p, h, _ga and formats cleanly`() {
        val setCookieHeaders = listOf(
            "e=1711200000; path=/; domain=.anime1.me; HttpOnly",
            "h=abcd1234efgh; path=/; domain=.anime1.me",
            "p=xyz987; path=/",
            "_ga=GA1.1.12345; path=/",
            "unrelated_cookie=ignore_me; path=/"
        )

        val cookieHeader = Anime1Parser.videoCookieC(setCookieHeaders)

        assertTrue(cookieHeader.contains("e=1711200000"))
        assertTrue(cookieHeader.contains("h=abcd1234efgh"))
        assertTrue(cookieHeader.contains("p=xyz987"))
        assertTrue(cookieHeader.contains("_ga=GA1.1.12345"))
        assertTrue(!cookieHeader.contains("unrelated_cookie"))
        assertTrue(!cookieHeader.endsWith(";"))
    }

    @Test
    fun `parseEpisodesFromHtml reverses episode order when entry-title ends with 01`() {
        val html = """
            <html>
                <body>
                    <article>
                        <h2 class="entry-title"><a href="https://anime1.me/1235">Cyber Neon [02]</a></h2>
                        <video data-apireq="token_ep2"></video>
                    </article>
                    <article>
                        <h2 class="entry-title"><a href="https://anime1.me/1234">Cyber Neon [01]</a></h2>
                        <video data-apireq="token_ep1"></video>
                    </article>
                </body>
            </html>
        """.trimIndent()

        val episodes = Anime1Parser.parseEpisodesFromHtml(html)

        assertEquals(2, episodes.size)
        assertEquals(1, episodes[0].episodeNumber)
        assertEquals("token_ep1", episodes[0].token)
        assertEquals(2, episodes[1].episodeNumber)
        assertEquals("token_ep2", episodes[1].token)
    }

    @Test
    fun `parseScheduleFromHtml parses days of week schedule table`() {
        val html = """
            <html>
                <body>
                    <table>
                        <tr>
                            <th>星期一</th>
                            <th>星期二</th>
                        </tr>
                        <tr>
                            <td><a href="https://anime1.me/?cat=101">Cyber Neon</a></td>
                            <td><a href="https://anime1.me/?cat=202">Shadow Arise</a></td>
                        </tr>
                    </table>
                </body>
            </html>
        """.trimIndent()

        val allItems = listOf(
            Anime1Item(101, "Cyber Neon", "12話", "2024", "春", "極影"),
            Anime1Item(202, "Shadow Arise", "連載", "2024", "冬", "動漫國")
        )

        val scheduleMap = Anime1Parser.parseScheduleFromHtml(html, allItems)

        assertNotNull(scheduleMap["星期一"])
        assertTrue(scheduleMap["星期一"]!!.any { it.id == 101 })
        assertNotNull(scheduleMap["星期二"])
        assertTrue(scheduleMap["星期二"]!!.any { it.id == 202 })
    }

    @Test
    fun `parseScheduleFromHtml handles seasonal table with label column`() {
        // Mirrors the real anime1.me seasonal page: 8 headers (row label +
        // 日..六) but only 7 data cells per row, mixing linked and plain-text cells.
        val html = """
            <html>
                <body>
                    <table id="twentysixsummer">
                        <tr>
                            <th>2026年夏季</th>
                            <th>日</th><th>一</th><th>二</th><th>三</th><th>四</th><th>五</th><th>六</th>
                        </tr>
                        <tr>
                            <td><a href="https://anime1.me/?cat=301">Sun Anime</a></td>
                            <td>Mon Anime</td>
                            <td><a href="https://anime1.me/?cat=303">Wed Anime</a></td>
                            <td></td>
                            <td></td>
                            <td></td>
                            <td></td>
                        </tr>
                    </table>
                </body>
            </html>
        """.trimIndent()

        val allItems = listOf(
            Anime1Item(301, "Sun Anime", "連載", "2026", "夏", "字幕組"),
            Anime1Item(302, "Mon Anime", "連載", "2026", "夏", "字幕組"),
            Anime1Item(303, "Wed Anime", "連載", "2026", "夏", "字幕組")
        )

        val scheduleMap = Anime1Parser.parseScheduleFromHtml(html, allItems)

        // The header row carries a leading label column, so data cell i aligns to
        // the (i+1)-th header: cell0 -> 日, cell1 -> 一, cell2 -> 二.
        assertTrue(scheduleMap["星期日"]!!.any { it.id == 301 })
        assertTrue(scheduleMap["星期一"]!!.any { it.id == 302 })
        assertTrue(scheduleMap["星期二"]!!.any { it.id == 303 })
        // Days with no real match must stay empty: the parser never fabricates
        // entries to fill the week (that used to put 303 into 星期三 via id % 7).
        assertTrue(scheduleMap["星期三"]!!.isEmpty())
        assertTrue(scheduleMap["星期四"]!!.isEmpty())
        assertTrue(scheduleMap["星期五"]!!.isEmpty())
        assertTrue(scheduleMap["星期六"]!!.isEmpty())
    }

    @Test
    fun `parseScheduleFromHtml handles real seasonal page with colspan title row in thead`() {
        // Mirrors the EXACT real anime1.me seasonal page markup
        // (https://anime1.me/2026年夏季新番):
        //   <thead>
        //     <tr><th colspan="7">2026年夏季(7-9月)新番</th></tr>   <- no day names!
        //     <tr><th>日</th><th>一</th>..<th>六</th></tr>          <- single-char headers
        //   </thead>
        //   <tbody><tr> 7 cells ... </tr></tbody>
        // The first row carrying NO day names is what used to make the header
        // pass skip the only table on the page and fall back to sample data.
        val html = """
            <html>
                <body>
                    <div style="overflow:auto;">
                        <table style="font-size: 12px;" id="twentysixsummer">
                            <thead>
                                <tr><th style="text-align: center;" colspan="7">2026年夏季(7-9月)新番</th></tr>
                                <tr><th>日</th><th>一</th><th>二</th><th>三</th><th>四</th><th>五</th><th>六</th></tr>
                            </thead>
                            <tbody class="row-hover">
                                <tr>
                                    <td><a href="/?cat=1956">Let’s Go 怪奇組</a></td>
                                    <td>北斗之拳 拳王軍雜兵們的輓歌</td>
                                    <td><a href="/?cat=1940">感謝對戰。</a></td>
                                    <td><a href="/?cat=1957">女主角？聖女？</a></td>
                                    <td><a href="/?cat=1914">令和妖神斑小姐</a></td>
                                    <td><a href="/?cat=1915">畫完這個再去死</a></td>
                                    <td><a href="/?cat=1921">無職轉生III</a></td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </body>
            </html>
        """.trimIndent()

        val allItems = listOf(
            Anime1Item(1956, "Let’s Go 怪奇組", "連載中(3)", "2026", "夏", "TV"),
            Anime1Item(1958, "北斗之拳 拳王軍雜兵們的輓歌", "連載中(5)", "2026", "夏", "TV"),
            Anime1Item(1940, "感謝對戰。～大小姐才不玩格鬥遊戲～", "連載中(12)", "2026", "夏", "TV"),
            Anime1Item(1957, "女主角？聖女？都不對，我是雜役女僕（自豪）！", "連載中(12)", "2026", "夏", "TV"),
            Anime1Item(1914, "令和妖神斑小姐", "連載中(12)", "2026", "夏", "TV"),
            Anime1Item(1915, "畫完這個再去死", "連載中(12)", "2026", "夏", "TV"),
            Anime1Item(1921, "無職轉生III～到了異世界就拿出真本事～ (第三季)", "連載中(12)", "2026", "夏", "TV")
        )

        val scheduleMap = Anime1Parser.parseScheduleFromHtml(html, allItems)

        // Sunday-first table: col 0 -> 星期日, col 1 -> 星期一, ... col 6 -> 星期六
        assertTrue("Linked Sunday cell", scheduleMap["星期日"]!!.any { it.id == 1956 })
        assertTrue("Plain-text Monday cell", scheduleMap["星期一"]!!.any { it.title.contains("北斗之拳") })
        assertTrue("Tuesday cell", scheduleMap["星期二"]!!.any { it.id == 1940 })
        assertTrue("Wednesday cell", scheduleMap["星期三"]!!.any { it.id == 1957 })
        assertTrue("Thursday cell", scheduleMap["星期四"]!!.any { it.id == 1914 })
        assertTrue("Friday cell", scheduleMap["星期五"]!!.any { it.id == 1915 })
        assertTrue("Saturday cell", scheduleMap["星期六"]!!.any { it.id == 1921 })
    }
}
