package com.one.animetvone.data.source

import com.one.animetvone.data.model.Anime1EpisodeToken
import com.one.animetvone.data.model.Anime1Item
import com.squareup.moshi.JsonReader
import okio.Buffer
import org.jsoup.Jsoup

object Anime1Parser {

    val DAYS_OF_WEEK = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")

    /**
     * Parse animelist.json array using Moshi JsonReader (JVM & Android runtime compatible):
     * item[0] = id (Int)
     * item[1] = title (String)
     * item[2] = episodeCount/status (String)
     * item[3] = year (String, e.g. "2026")
     * item[4] = season (String, single char e.g. "秋", may be blank)
     * item[5] = subtitle/fansub group (String, may be blank)
     * Filter out item[0] <= 0 (adult/invalid entries).
     */
    fun parseAnimeListJson(jsonString: String): List<Anime1Item> {
        val result = mutableListOf<Anime1Item>()
        if (jsonString.isBlank()) return result

        try {
            val reader = JsonReader.of(Buffer().writeUtf8(jsonString))
            reader.isLenient = true
            if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                reader.beginArray()
                while (reader.hasNext()) {
                    if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                        reader.beginArray()
                        var id = -1
                        var title = ""
                        var epStatus = ""
                        var year = ""
                        var season = ""
                        var subtitleGroup = ""
                        var index = 0

                        while (reader.hasNext()) {
                            when (index) {
                                0 -> id = parseIdValue(reader)
                                1 -> title = parseNextString(reader)
                                2 -> epStatus = parseNextString(reader)
                                3 -> year = parseNextString(reader)
                                4 -> season = parseNextString(reader)
                                5 -> subtitleGroup = parseNextString(reader)
                                else -> reader.skipValue()
                            }
                            index++
                        }
                        reader.endArray()

                        if (id > 0 && title.isNotBlank()) {
                            result.add(
                                Anime1Item(
                                    id = id,
                                    title = title,
                                    episodeCountOrStatus = epStatus,
                                    year = year,
                                    season = season,
                                    subtitleGroup = subtitleGroup
                                )
                            )
                        }
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    private fun parseIdValue(reader: JsonReader): Int {
        return try {
            when (reader.peek()) {
                JsonReader.Token.NUMBER -> reader.nextInt()
                JsonReader.Token.STRING -> reader.nextString().toIntOrNull() ?: -1
                else -> {
                    reader.skipValue()
                    -1
                }
            }
        } catch (e: Exception) {
            reader.skipValue()
            -1
        }
    }

    private fun parseNextString(reader: JsonReader): String {
        return try {
            when (reader.peek()) {
                JsonReader.Token.STRING, JsonReader.Token.NUMBER -> reader.nextString().trim()
                JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
                JsonReader.Token.NULL -> {
                    reader.nextNull<Unit>()
                    ""
                }
                else -> {
                    reader.skipValue()
                    ""
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Parse Episode list & Video Tokens from anime1.me HTML using Jsoup.
     * Locates `<video>` tags or `data-apireq` elements and extracts attribute value (`token`).
     */
    fun parseEpisodesFromHtml(html: String): List<Anime1EpisodeToken> {
        val episodes = mutableListOf<Anime1EpisodeToken>()
        if (html.isBlank()) return episodes

        try {
            val doc = Jsoup.parse(html)
            val articles = doc.select("article")

            var fallbackIndex = 1

            if (articles.isNotEmpty()) {
                for (article in articles) {
                    val videoElem = article.select("video[data-apireq], [data-apireq]").first()
                        ?: if (article.hasAttr("data-apireq")) article else null

                    val token = videoElem?.attr("data-apireq")?.trim() ?: continue
                    if (token.isEmpty()) continue

                    val rawTitle = article.select(".entry-title, h1, h2, h3").text().ifEmpty {
                        article.select("a").text()
                    }.trim()

                    val detailUrl = article.select(".entry-title a, h1 a, h2 a").attr("href").ifEmpty { null }

                    val epNum = extractEpisodeNumber(rawTitle) ?: fallbackIndex

                    val displayTitle = if (rawTitle.isNotEmpty()) {
                        rawTitle
                    } else {
                        "第 $epNum 集"
                    }

                    episodes.add(
                        Anime1EpisodeToken(
                            episodeNumber = epNum,
                            title = displayTitle,
                            token = token,
                            detailUrl = detailUrl
                        )
                    )
                    fallbackIndex++
                }

                val lastArticleTitle = articles.lastOrNull()?.select(".entry-title, h1, h2, h3")?.text()?.trim() ?: ""
                val firstEpNum = episodes.firstOrNull()?.episodeNumber
                val lastEpNum = episodes.lastOrNull()?.episodeNumber

                val isReversed = lastArticleTitle.endsWith("[01]") ||
                        lastArticleTitle.contains("[01]") ||
                        (firstEpNum != null && lastEpNum != null && firstEpNum > lastEpNum)

                val orderedEpisodes = if (isReversed) {
                    episodes.reversed()
                } else {
                    episodes
                }

                return orderedEpisodes.distinctBy { it.token }.sortedBy { it.episodeNumber }
            } else {
                val videoElements = doc.select("video[data-apireq], [data-apireq]")
                for (video in videoElements) {
                    val token = video.attr("data-apireq").trim()
                    if (token.isEmpty()) continue

                    episodes.add(
                        Anime1EpisodeToken(
                            episodeNumber = fallbackIndex,
                            title = "第 $fallbackIndex 集",
                            token = token
                        )
                    )
                    fallbackIndex++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return episodes.distinctBy { it.token }.sortedBy { it.episodeNumber }
    }

    /**
     * Parse Schedule from anime1.me seasonal 新番 page (primary) or homepage (fallback).
     * Method mirrors HQAnime/AnimeOne + Predidit/oneAnime: the weekly table's
     * first table holds one tbody whose data rows each carry 7 cells;
     * column position alone decides the weekday (anime1 puts Sunday first),
     * so no header matching is required. Header matching is kept only as a
     * secondary pass. Cells without links are accepted via title matching.
     * Adult entries (id <= 0) are always excluded.
     * DAYS_OF_WEEK is Monday-first: col 0 (Sun) -> 6, col 1 (Mon) -> 0, ...
     */
    fun parseScheduleFromHtml(html: String, allAnimeList: List<Anime1Item>): Map<String, List<Anime1Item>> {
        val resultMap = LinkedHashMap<String, MutableList<Anime1Item>>()
        DAYS_OF_WEEK.forEach { resultMap[it] = mutableListOf() }

        val safeList = allAnimeList.filter { it.id > 0 }
        if (safeList.isEmpty()) return resultMap

        if (html.isNotBlank()) {
            try {
                val doc = Jsoup.parse(html)
                // Primary: header matching (handles tables whose columns start
                // on any weekday, e.g. Monday-first tables).
                parseScheduleByHeaders(doc, safeList, resultMap)

                // Secondary: positional columns (anime1.me seasonal tables put
                // Sunday first and may carry no usable headers), only when the
                // header pass found nothing.
                if (resultMap.values.all { it.isEmpty() }) {
                    val firstTable = doc.select("table").firstOrNull()
                    if (firstTable != null) {
                        val bodyRows = firstTable.select("tbody tr")
                        val rows = if (bodyRows.isNotEmpty()) bodyRows else firstTable.select("tr")
                        for (row in rows) {
                            val cells = row.select("td")
                            if (cells.size <= 1) continue
                            cells.forEachIndexed { colIndex, cell ->
                                if (colIndex > 6) return@forEachIndexed
                                val dayName = DAYS_OF_WEEK[(colIndex + 6) % 7]
                                collectCellItems(cell, safeList, resultMap, dayName)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // No fabricated padding: days without a real match stay empty so the UI
        // can show an honest "no schedule" state instead of invented entries.
        return resultMap
    }

    fun addScheduleItem(
        resultMap: LinkedHashMap<String, MutableList<Anime1Item>>,
        dayName: String,
        item: Anime1Item?
    ) {
        if (item == null || item.id <= 0) return
        if (resultMap[dayName]?.none { it.id == item.id } == true) {
            resultMap[dayName]?.add(item)
        }
    }

    /** Shared cell handling: ?cat= links first, plain-text cells via title match. */
    val SINGLE_CHAR_DAY_MAP = mapOf(
        "日" to "星期日",
        "一" to "星期一",
        "二" to "星期二",
        "三" to "星期三",
        "四" to "星期四",
        "五" to "星期五",
        "六" to "星期六",
        "天" to "星期日"
    )

    fun matchDayHeader(headerText: String): String? {
        val trimmed = headerText.trim()
        if (trimmed.isEmpty()) return null
        DAYS_OF_WEEK.find { trimmed.contains(it) }?.let { return it }
        if (trimmed.length <= 3) {
            SINGLE_CHAR_DAY_MAP[trimmed]?.let { return it }
            for ((ch, day) in SINGLE_CHAR_DAY_MAP) {
                if (trimmed.contains(ch)) return day
            }
        }
        return null
    }

    fun findItemByTitle(allAnimeList: List<Anime1Item>, title: String): Anime1Item? {
        if (title.isBlank()) return null
        allAnimeList.find { it.title.equals(title, ignoreCase = true) }?.let { return it }
        allAnimeList.find { it.title.contains(title, ignoreCase = true) }?.let { return it }
        allAnimeList.find { title.contains(it.title, ignoreCase = true) }?.let { return it }
        return null
    }

    fun extractCatFromUrl(url: String): Int? {
        val match = CAT_PATTERN.find(url)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }
    fun collectCellItems(
        cell: org.jsoup.nodes.Element,
        safeList: List<Anime1Item>,
        resultMap: LinkedHashMap<String, MutableList<Anime1Item>>,
        dayName: String
    ) {
        val links = cell.select("a[href]")
        for (link in links) {
            val catId = extractCatFromUrl(link.attr("href"))
            val matched = if (catId != null) {
                safeList.find { it.id == catId }
            } else {
                findItemByTitle(safeList, link.text().trim())
            }
            addScheduleItem(resultMap, dayName, matched)
        }
        if (links.isEmpty()) {
            val textTitle = cell.text().trim()
            if (textTitle.isNotBlank()) {
                addScheduleItem(resultMap, dayName, findItemByTitle(safeList, textTitle))
            }
        }
    }

    /** Secondary pass: match th/td headers to weekdays (legacy behavior). */
    fun parseScheduleByHeaders(
        doc: org.jsoup.nodes.Document,
        safeList: List<Anime1Item>,
        resultMap: LinkedHashMap<String, MutableList<Anime1Item>>
    ) {
        for (table in doc.select("table")) {
            val rows = table.select("tr")
            if (rows.isEmpty()) continue
            val headers = rows.first()?.select("th, td")?.map { it.text().trim() } ?: emptyList()
            val dayIndexes = mutableMapOf<Int, String>()
            headers.forEachIndexed { index, headerText ->
                val matchedDay = matchDayHeader(headerText)
                if (matchedDay != null) dayIndexes[index] = matchedDay
            }
            if (dayIndexes.isEmpty()) continue
            for (i in 1 until rows.size) {
                val cells = rows[i].select("td, th")
                // Tables may carry a leading row-label column (e.g. seasonal
                // pages: 8 headers but 7 data cells). Align from the left.
                val offset = (headers.size - cells.size).coerceIn(0, 2)
                cells.forEachIndexed { colIndex, cell ->
                    val dayName = dayIndexes[colIndex + offset] ?: return@forEachIndexed
                    collectCellItems(cell, safeList, resultMap, dayName)
                }
            }
            if (resultMap.values.any { it.isNotEmpty() }) return
        }
    }


    /**
     * Parse pagination next page URL from HTML if present.
     */
    fun parseNextPageUrl(html: String): String? {
        if (html.isBlank()) return null
        try {
            val doc = Jsoup.parse(html)
            val nextLink = doc.select(".nav-previous a, .nav-links a.next, a.next, .pagination a.next").attr("href").trim()
            if (nextLink.isNotEmpty() && (nextLink.startsWith("http://") || nextLink.startsWith("https://") || nextLink.startsWith("/"))) {
                return nextLink
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Parse stream URL from v.anime1.me API JSON response using Moshi JsonReader.
     * Response JSON: `{ "s": [ { "src": "//v.anime1.me/..." } ] }`
     */
    fun parseDirectStreamUrl(jsonString: String): String? {
        if (jsonString.isBlank()) return null
        try {
            val reader = JsonReader.of(Buffer().writeUtf8(jsonString))
            reader.isLenient = true
            var srcUrl: String? = null

            if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    if (name == "s" && reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    val innerName = reader.nextName()
                                    if (innerName == "src") {
                                        srcUrl = parseNextString(reader)
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                reader.endObject()
                            } else if (reader.peek() == JsonReader.Token.STRING) {
                                srcUrl = parseNextString(reader)
                            } else {
                                reader.skipValue()
                            }
                        }
                        reader.endArray()
                    } else if (name == "src") {
                        srcUrl = parseNextString(reader)
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }

            if (!srcUrl.isNullOrBlank()) {
                val trimmed = srcUrl.trim()
                return when {
                    trimmed.startsWith("//") -> "https:$trimmed"
                    trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                    trimmed.startsWith("/") -> "https://v.anime1.me$trimmed"
                    else -> "https://$trimmed"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Filter Set-Cookie headers for keys matching 'e', 'p', 'h', or starting with '_ga'.
     * Format into a clean cookie string: "e=...; p=...; h=...; _ga=..." with trailing semicolons stripped.
     */
    fun videoCookieC(setCookieHeaders: List<String>): String {
        if (setCookieHeaders.isEmpty()) return ""
        val cookieMap = mutableMapOf<String, String>()

        for (header in setCookieHeaders) {
            val parts = header.split(";")
            for (part in parts) {
                val trimmed = part.trim()
                val equalsIndex = trimmed.indexOf('=')
                if (equalsIndex > 0) {
                    val key = trimmed.substring(0, equalsIndex).trim()
                    val value = trimmed.substring(equalsIndex + 1).trim()
                    if (key == "e" || key == "p" || key == "h" || key.startsWith("_ga")) {
                        cookieMap[key] = value
                    }
                }
            }
        }

        if (cookieMap.isEmpty()) return ""

        val formatted = cookieMap.entries.joinToString("; ") { "${it.key}=${it.value}" }
        return formatted.trimEnd(';', ' ')
    }

    fun extractEpisodeNumber(title: String): Int? {
        if (title.isBlank()) return null

        val bracketMatch = EP_BRACKET_PATTERN.find(title)
        if (bracketMatch != null) {
            bracketMatch.groupValues[1].toIntOrNull()?.let { return it }
        }

        val chMatch = EP_HUA_PATTERN.find(title)
        if (chMatch != null) {
            chMatch.groupValues[1].toIntOrNull()?.let { return it }
        }

        val epMatch = EP_EN_PATTERN.find(title)
        if (epMatch != null) {
            epMatch.groupValues[1].toIntOrNull()?.let { return it }
        }

        val numMatch = EP_NUMBER_PATTERN.find(title)
        if (numMatch != null) {
            numMatch.groupValues[1].toIntOrNull()?.let { return it }
        }

        return null
    }

    // Hoisted: these run per article/link while parsing episode lists and the
    // weekly schedule table, so they must not be recompiled on every call.
    private val CAT_PATTERN = Regex("""cat=(\d+)""")
    private val EP_BRACKET_PATTERN = Regex("""\[(\d+)(?:\.\d+)?]""")
    private val EP_HUA_PATTERN = Regex("""第\s*(\d+)\s*[話集]""")
    private val EP_EN_PATTERN = Regex("""(?:EP|Ep|ep)\.?\s*(\d+)""")
    private val EP_NUMBER_PATTERN = Regex("""\b(\d+)\b""")
}
