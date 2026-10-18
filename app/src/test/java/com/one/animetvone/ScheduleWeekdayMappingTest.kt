package com.one.animetvone

import com.one.animetvone.data.model.Anime1Item
import com.one.animetvone.data.source.Anime1Parser
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The weekly (每週新番表) page is a 7-column table that starts on SUNDAY.
 * A wrong column offset silently files every show under the wrong weekday, so
 * these tests pin the mapping to a real captured page
 * (`src/test/resources/schedule/2026_summer.html`).
 */
class ScheduleWeekdayMappingTest {

    private fun fixtureHtml(): String =
        javaClass.classLoader!!.getResource("schedule/2026_summer.html")!!.readText()

    private fun dataRows(html: String): List<Element> {
        val table = Jsoup.parse(html).select("table").first()!!
        return table.select("tr").filter { it.select("td").size == 7 }
    }

    /** Catalogue containing every anime the page links to, as animelist.json would. */
    private fun catalogue(rows: List<Element>): List<Anime1Item> {
        val items = LinkedHashMap<Int, Anime1Item>()
        rows.forEach { row ->
            row.select("td").forEach { cell ->
                cell.select("a[href]").forEach { link ->
                    val id = Anime1Parser.extractCatFromUrl(link.attr("href")) ?: return@forEach
                    items.putIfAbsent(
                        id,
                        Anime1Item(
                            id = id,
                            title = cell.text().trim(),
                            episodeCountOrStatus = "1-12",
                            year = "2026",
                            season = "夏",
                            subtitleGroup = ""
                        )
                    )
                }
            }
        }
        return items.values.toList()
    }

    private fun expectedDayForColumn(colIndex: Int): String =
        Anime1Parser.DAYS_OF_WEEK[(colIndex + 6) % 7]

    @Test
    fun `sunday-first columns map to the matching weekday`() {
        val html = fixtureHtml()
        val rows = dataRows(html)
        val catalogue = catalogue(rows)
        assertTrue("fixture should link many anime", catalogue.size > 30)

        val schedule = Anime1Parser.parseScheduleFromHtml(html, catalogue)

        // Every weekday column carries entries (the page has shows on all 7 days).
        Anime1Parser.DAYS_OF_WEEK.forEach { day ->
            assertTrue("$day has no entries", schedule[day]!!.isNotEmpty())
        }

        // col 0 is 星期日, col 1 星期一, ... col 6 星期六 — never shifted.
        assertEquals("星期日", expectedDayForColumn(0))
        assertEquals("星期一", expectedDayForColumn(1))
        assertEquals("星期六", expectedDayForColumn(6))

        rows.forEach { row ->
            row.select("td").forEachIndexed { colIndex, cell ->
                if (colIndex > 6) return@forEachIndexed
                val expectedDay = expectedDayForColumn(colIndex)
                val idsInCell = cell.select("a[href]")
                    .mapNotNull { Anime1Parser.extractCatFromUrl(it.attr("href")) }
                idsInCell.forEach { id ->
                    assertTrue(
                        "anime $id belongs to $expectedDay",
                        schedule[expectedDay]!!.any { it.id == id }
                    )
                    Anime1Parser.DAYS_OF_WEEK
                        .filter { it != expectedDay }
                        .forEach { other ->
                            assertTrue(
                                "anime $id must not be filed under $other",
                                schedule[other]!!.none { it.id == id }
                            )
                        }
                }
            }
        }
    }

    @Test
    fun `header and title rows do not fabricate entries`() {
        val html = fixtureHtml()
        val schedule = Anime1Parser.parseScheduleFromHtml(html, catalogue(dataRows(html)))

        val bogus = setOf("日", "一", "二", "三", "四", "五", "六", "2026年夏季(7-9月)新番")
        schedule.forEach { (day, items) ->
            items.forEach { item ->
                assertTrue("$day contains a header row title: ${item.title}", item.title !in bogus)
            }
        }
    }

    @Test
    fun `plain text cells absent from the catalogue are skipped, not guessed`() {
        val html = fixtureHtml()
        val catalogue = catalogue(dataRows(html))
        val catalogueTitles = catalogue.map { it.title }.toSet()

        // Titles announced on the page but not yet in animelist.json (no link).
        val notInCatalogue = listOf(
            "北斗之拳 拳王軍雜兵們的輓歌",
            "鎧真傳",
            "ONE PIECE HEROINES"
        )
        notInCatalogue.forEach { title ->
            assertTrue("precondition: $title is not in the catalogue", title !in catalogueTitles)
        }

        val schedule = Anime1Parser.parseScheduleFromHtml(html, catalogue)
        val listed = schedule.values.flatten().map { it.title }.toSet()
        notInCatalogue.forEach { title ->
            assertTrue("fabricated entry for $title", title !in listed)
        }
    }
}
