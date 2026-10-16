package com.one.animetvone.data.source

import android.content.Context
import com.one.animetvone.util.ChineseConverter
import com.squareup.moshi.JsonReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private class SimpleLruCache<K, V>(private val maxSize: Int) {
    private val map = object : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
            return size > maxSize
        }
    }

    @Synchronized
    fun get(key: K): V? = map[key]

    @Synchronized
    fun put(key: K, value: V) {
        map[key] = value
    }

    @Synchronized
    fun clear() = map.clear()
}

object CoverImageFetcher {

    private const val PREF_NAME = "anime_covers"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    private val memoryCache = SimpleLruCache<String, String>(500)
    @Volatile
    private var appContext: Context? = null

    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Polite fetching: max 2 concurrent cover requests app-wide, >=500ms
    // between request starts, per-title dedup + cooldown on failure/429.
    private val fetchPermits = Semaphore(2)
    private val inFlight = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val cooldownUntil = java.util.concurrent.ConcurrentHashMap<String, Long>()
    @Volatile
    private var lastStartMs = 0L

    private val _coverUpdates = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 100)
    val coverUpdates: SharedFlow<Pair<String, String>> = _coverUpdates.asSharedFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun clearAllCache(context: Context? = null) {
        memoryCache.clear()
        cooldownUntil.clear()
        val target = context?.applicationContext ?: appContext
        target?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
    }

    /**
     * Clear cached cover URL for a specific anime title from memory and disk.
     */
    fun clearCacheForTitle(context: Context? = null, rawTitle: String) {
        val targetContext = context?.applicationContext ?: appContext
        val cleanTitle = cleanAnimeTitle(rawTitle)
        if (cleanTitle.isBlank()) return

        cooldownUntil.remove(cleanTitle)

        targetContext?.let { ctx ->
            try {
                ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(cleanTitle)
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Force clear cache and trigger online re-fetch for a specific anime title.
     */
    fun refreshCoverUrl(context: Context? = null, rawTitle: String) {
        clearCacheForTitle(context, rawTitle)
        val targetContext = context?.applicationContext ?: appContext
        val cleanTitle = cleanAnimeTitle(rawTitle)
        backgroundScope.launch {
            fetchOnlineAndSave(targetContext, cleanTitle)
        }
    }

    /**
     * Enhanced Title Cleaner:
     * Strips bracketed tags [...], 【...】, (...), <...>, subtitle group names,
     * season suffixes (第一季, 第二季, S1, S2, Season 1), resolution tags (1080p, 720p, 4k),
     * and episode range tags (e.g. [01-28]).
     */
    fun cleanAnimeTitle(title: String): String {
        var cleaned = title

        // 1. Remove bracketed text [...]
        cleaned = cleaned.replace(Regex("""\[.*?]"""), "")
        // 2. Remove parenthesized text (...)
        cleaned = cleaned.replace(Regex("""\(.*?\)"""), "")
        // 3. Remove Japanese bracketed text 【...】
        cleaned = cleaned.replace(Regex("""【.*?】"""), "")
        // 4. Remove angle bracketed text <...>
        cleaned = cleaned.replace(Regex("""<.*?>"""), "")

        // 5. Remove Chinese season / episode tags e.g. 第1季, 第一季, 第2期, 第12話, 第01集
        cleaned = cleaned.replace(Regex("""第\s*[\d一二三四五六七八九十]+\s*[季期話集]"""), "")

        // 6. Remove English Season indicators e.g. S1, S2, S01, Season 1, Season 2
        cleaned = cleaned.replace(Regex("""\bS\d{1,2}\b""", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("""\bSeason\s*\d{1,2}\b""", RegexOption.IGNORE_CASE), "")

        // 7. Remove resolution tags e.g. 1080p, 720p, 4k, 1080P
        cleaned = cleaned.replace(Regex("""\b(1080|720|480)P?\b""", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.replace(Regex("""\b4K\b""", RegexOption.IGNORE_CASE), "")

        // 8. Remove common distribution / media tags e.g. BD, WEB, TV, OVA, OAD, Movie, 重製版
        cleaned = cleaned.replace(Regex("""\b(BD|WEB|TV|OVA|OAD|Movie|重製版)\b""", RegexOption.IGNORE_CASE), "")

        // 9. Trim leftover dashes, spaces, colons
        cleaned = cleaned
            .replace(Regex("""^\s*[-:—–\s]+"""), "")
            .replace(Regex("""[-:—–\s]+$"""), "")
            .trim()

        return if (cleaned.isNotBlank()) cleaned else title.trim()
    }

    /**
     * Traditional Chinese to Simplified Chinese normalizer for anime titles.
     */
    fun toSimplifiedChinese(title: String): String {
        return ChineseConverter.toSimplifiedChinese(title)
    }

    /**
     * Extract Latin/English/Romaji portion from anime title if available.
     * e.g. "SPY×FAMILY 間諜家家酒" -> "SPY×FAMILY"
     */
    fun extractLatinTitle(title: String): String? {
        val latinMatches = Regex("""[A-Za-z0-9\s:×!/'-]{3,}""").findAll(title)
        val longest = latinMatches.maxByOrNull { it.value.trim().length }?.value?.trim()
        return if (!longest.isNullOrBlank() && longest.length >= 3 && !longest.matches(Regex("""^\d+$"""))) longest else null
    }

    /**
     * Extract base title before colons, dashes, or sub-titles.
     * e.g. "葬送的芙莉蓮：第二季" -> "葬送的芙莉蓮"
     */
    fun extractBaseTitle(title: String): String? {
        val split = title.split(":", "：", "-", "—", "–", "～")
        if (split.size > 1) {
            val base = split.first().trim()
            if (base.length >= 2) return base
        }
        return null
    }

    /**
     * Extract key noun phrase from long light novel titles by stripping common prefixes.
     * e.g. "遭到流放的轉生重騎士憑藉遊戲知識大開無雙" -> "轉生重騎士憑藉遊戲知識大開無雙"
     */
    fun extractKeywordTitle(title: String): String? {
        if (title.length < 8) return null
        var stripped = title
        val prefixes = listOf("遭到流放的", "被流放的", "關於我", "轉生變成", "被開除的", "被逐出冒險者隊伍的", "被解雇的")
        for (prefix in prefixes) {
            if (stripped.startsWith(prefix) && stripped.length - prefix.length >= 4) {
                stripped = stripped.substring(prefix.length)
            }
        }
        return if (stripped != title && stripped.length >= 4) stripped else null
    }

    /**
     * Instant 0ms cover getter.
     * Order of lookup:
     * 1. In-memory LruCache (0ms)
     * 2. SharedPreferences Disk Cache (0ms)
     * 3. PrepopulatedCovers dictionary matching (0ms)
     * 4. If none hit: Return `null` + launch async background fetch.
     */
    fun getCoverUrlFast(context: Context? = null, rawTitle: String): String? {
        val targetContext = context?.applicationContext ?: appContext
        val cleanTitle = cleanAnimeTitle(rawTitle)
        if (cleanTitle.isBlank()) return null

        // 1. In-memory LruCache (0ms)
        memoryCache.get(cleanTitle)?.let {
            if (it.isNotBlank() && !isGenericFallbackUrl(it)) return it
        }

        // 2. Disk Cache SharedPreferences "anime_covers" (0ms)
        targetContext?.let { ctx ->
            try {
                val prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                val cachedUrl = prefs.getString(cleanTitle, null)
                if (!cachedUrl.isNullOrBlank() && !isGenericFallbackUrl(cachedUrl)) {
                    memoryCache.put(cleanTitle, cachedUrl)
                    return cachedUrl
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 3. PrepopulatedCovers Dictionary matching (0ms)
        val prepopulated = PrepopulatedCovers.getPrepopulatedCover(cleanTitle)
            ?: PrepopulatedCovers.getPrepopulatedCover(toSimplifiedChinese(cleanTitle))
        if (!prepopulated.isNullOrBlank()) {
            memoryCache.put(cleanTitle, prepopulated)
            return prepopulated
        }

        // 4. Launch async background online search and return null for now.
        // Dedup + cooldown: the same missing title queues at most one fetch.
        val now = System.currentTimeMillis()
        if ((cooldownUntil[cleanTitle] ?: 0L) <= now &&
            inFlight.putIfAbsent(cleanTitle, now) == null) {
            backgroundScope.launch {
                fetchOnlineAndSave(targetContext, cleanTitle, registered = true)
            }
        }

        return null
    }

    /**
     * Multi-source Search Pipeline with fallback chain:
     * Returns online poster URL or null if not found.
     */
    suspend fun getCoverUrl(context: Context? = null, rawTitle: String): String? = withContext(Dispatchers.IO) {
        val fast = getCoverUrlFast(context, rawTitle)
        if (fast != null && !isGenericFallbackUrl(fast)) {
            return@withContext fast
        }

        val targetContext = context?.applicationContext ?: appContext
        val cleanTitle = cleanAnimeTitle(rawTitle)
        fetchOnlineAndSave(targetContext, cleanTitle)
    }

    /**
     * Clear cached cover image for a title from memory and SharedPreferences disk cache,
     * then perform a fresh online fetch and save.
     */
    suspend fun evictAndRefreshCover(context: Context? = null, rawTitle: String): String? = withContext(Dispatchers.IO) {
        val targetContext = context?.applicationContext ?: appContext
        val cleanTitle = cleanAnimeTitle(rawTitle)
        if (cleanTitle.isBlank()) return@withContext null

        memoryCache.put(cleanTitle, "")
        cooldownUntil.remove(cleanTitle)

        targetContext?.let { ctx ->
            try {
                ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .remove(cleanTitle)
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fetchOnlineAndSave(targetContext, cleanTitle)
    }

    private suspend fun fetchOnlineAndSave(context: Context?, cleanTitle: String, registered: Boolean = false): String? {
        val now = System.currentTimeMillis()
        if ((cooldownUntil[cleanTitle] ?: 0L) > now) return memoryCache.get(cleanTitle)
        if (!registered && inFlight.putIfAbsent(cleanTitle, now) != null) return memoryCache.get(cleanTitle)

        return try {
            fetchPermits.withPermit {
                // The throttle belongs inside the permit so concurrent calls
                // cannot read and update lastStartMs at the same time.
                val waitMs = 500L - (System.currentTimeMillis() - lastStartMs)
                if (waitMs > 0) delay(waitMs)
                lastStartMs = System.currentTimeMillis()
                fetchOnlineInner(context, cleanTitle)
            }
        } finally {
            inFlight.remove(cleanTitle)
        }
    }

    private suspend fun fetchOnlineInner(context: Context?, cleanTitle: String): String? {
        val targetContext = context ?: appContext
        val latinTitle = extractLatinTitle(cleanTitle)
        val baseTitle = extractBaseTitle(cleanTitle)
        val keywordTitle = extractKeywordTitle(cleanTitle)
        val simplifiedTitle = toSimplifiedChinese(cleanTitle)
        val simplifiedBase = baseTitle?.let { toSimplifiedChinese(it) }
        val simplifiedKeyword = keywordTitle?.let { toSimplifiedChinese(it) }

        val titleQueries = listOfNotNull(
            cleanTitle,
            latinTitle,
            baseTitle,
            keywordTitle,
            if (simplifiedTitle != cleanTitle) simplifiedTitle else null,
            if (simplifiedBase != null && simplifiedBase != baseTitle) simplifiedBase else null,
            if (simplifiedKeyword != null && simplifiedKeyword != keywordTitle) simplifiedKeyword else null
        ).distinct()

        var resolvedUrl: String? = null

        // Step 1: Bangumi API
        for (query in titleQueries) {
            resolvedUrl = fetchBangumiCover(query)
            if (!resolvedUrl.isNullOrBlank()) break
        }

        // Step 2: AniList GraphQL API
        if (resolvedUrl.isNullOrBlank()) {
            for (query in titleQueries) {
                resolvedUrl = fetchAniListCover(query)
                if (!resolvedUrl.isNullOrBlank()) break
            }
        }

        // Step 3: Jikan (MyAnimeList) API
        if (resolvedUrl.isNullOrBlank()) {
            for (query in titleQueries.reversed()) {
                resolvedUrl = fetchJikanCover(query)
                if (!resolvedUrl.isNullOrBlank()) break
            }
        }

        // Step 4: Bangumi Web Jsoup Search Fallback
        if (resolvedUrl.isNullOrBlank()) {
            for (query in titleQueries) {
                resolvedUrl = fetchBangumiWebSearch(query)
                if (!resolvedUrl.isNullOrBlank()) break
            }
        }

        // Do not use generic image-search results as a final fallback. Search
        // engines frequently return logos, avatars or unrelated thumbnails;
        // a deterministic text card is better than a wrong anime poster.

        // Save only trusted, usable poster URLs under all title variations.
        val trustedUrl = resolvedUrl?.takeIf { isUsableCoverUrl(it) }
        if (trustedUrl != null) {
            val cacheKeys = listOfNotNull(cleanTitle, latinTitle, baseTitle, simplifiedTitle).filter { it.isNotBlank() }.distinct()
            cacheKeys.forEach { key ->
                memoryCache.put(key, trustedUrl)
            }
            targetContext?.let { ctx ->
                try {
                    val editor = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
                    cacheKeys.forEach { key ->
                        editor.putString(key, trustedUrl)
                    }
                    editor.apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _coverUpdates.emit(Pair(cleanTitle, trustedUrl))
        } else {
            // Total miss: cool down 6h so scrolling doesn't re-hammer sources.
            cooldownUntil[cleanTitle] = System.currentTimeMillis() + 6 * 60 * 60 * 1000L
        }

        return trustedUrl
    }

    private fun isGenericFallbackUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return true
        return url.contains("unsplash.com") || url.contains("placeholder")
    }

    private fun isUsableCoverUrl(url: String?): Boolean {
        if (isGenericFallbackUrl(url)) return false
        val value = url?.trim() ?: return false
        if (!value.startsWith("https://", ignoreCase = true)) return false
        return value.contains("bgm.tv", ignoreCase = true) ||
            value.contains("anilist.co", ignoreCase = true) ||
            value.contains("myanimelist.net", ignoreCase = true) ||
            value.contains("cdn.", ignoreCase = true) ||
            value.contains("image", ignoreCase = true)
    }

    private fun fetchBangumiCover(cleanTitle: String): String? {
        return try {
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val url = "https://api.bgmapi.com/search/subject/$encodedTitle?type=2"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logD("CoverImageFetcher", "Bangumi API HTTP ${response.code} for '$cleanTitle'")
                    return null
                }
                val jsonString = response.body?.string() ?: return null
                logD("CoverImageFetcher", "Bangumi response for '$cleanTitle': ${jsonString.take(100)}")
                parseBangumiImage(jsonString)
            }
        } catch (e: Exception) {
            logD("CoverImageFetcher", "Bangumi API error for '$cleanTitle': ${e.message}")
            null
        }
    }

    private fun parseBangumiImage(jsonString: String): String? {
        if (jsonString.isBlank()) return null
        try {
            val reader = JsonReader.of(Buffer().writeUtf8(jsonString))
            reader.isLenient = true
            if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "list" && reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                        reader.beginArray()
                        while (reader.hasNext()) {
                            if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    if (reader.nextName() == "images" && reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                        var large: String? = null
                                        var common: String? = null
                                        var medium: String? = null
                                        var small: String? = null
                                        var grid: String? = null
                                        reader.beginObject()
                                        while (reader.hasNext()) {
                                            when (reader.nextName()) {
                                                "large" -> large = parseString(reader)
                                                "common" -> common = parseString(reader)
                                                "medium" -> medium = parseString(reader)
                                                "small" -> small = parseString(reader)
                                                "grid" -> grid = parseString(reader)
                                                else -> reader.skipValue()
                                            }
                                        }
                                        reader.endObject()
                                        val img = large?.ifEmpty { null }
                                            ?: common?.ifEmpty { null }
                                            ?: medium?.ifEmpty { null }
                                            ?: small?.ifEmpty { null }
                                            ?: grid?.ifEmpty { null }
                                        if (!img.isNullOrEmpty()) {
                                            return if (img.startsWith("http://")) img.replace("http://", "https://") else img
                                        }
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                reader.endObject()
                            } else {
                                reader.skipValue()
                            }
                        }
                        reader.endArray()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun fetchAniListCover(cleanTitle: String): String? {
        return try {
            val graphqlQuery = """
                {"query":"query (${'$'}search: String) { Media(search: ${'$'}search, type: ANIME) { coverImage { extraLarge large } bannerImage } }", "variables":{"search":"$cleanTitle"}}
            """.trimIndent()

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = graphqlQuery.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://graphql.anilist.co")
                .post(requestBody)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    logD("CoverImageFetcher", "AniList API HTTP ${response.code} for '$cleanTitle'")
                    return null
                }
                val jsonString = response.body?.string() ?: return null
                logD("CoverImageFetcher", "AniList response for '$cleanTitle': ${jsonString.take(100)}")
                parseAniListImage(jsonString)
            }
        } catch (e: Exception) {
            logD("CoverImageFetcher", "AniList API error for '$cleanTitle': ${e.message}")
            null
        }
    }

    private fun parseAniListImage(jsonString: String): String? {
        if (jsonString.isBlank()) return null
        try {
            val reader = JsonReader.of(Buffer().writeUtf8(jsonString))
            reader.isLenient = true
            if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "data" && reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                        reader.beginObject()
                        while (reader.hasNext()) {
                            if (reader.nextName() == "Media" && reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                var cover: String? = null
                                var banner: String? = null
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    when (reader.nextName()) {
                                        "coverImage" -> if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                            var extraLarge: String? = null
                                            var large: String? = null
                                            reader.beginObject()
                                            while (reader.hasNext()) {
                                                when (reader.nextName()) {
                                                    "extraLarge" -> extraLarge = parseString(reader)
                                                    "large" -> large = parseString(reader)
                                                    else -> reader.skipValue()
                                                }
                                            }
                                            reader.endObject()
                                            cover = extraLarge?.ifEmpty { null } ?: large?.ifEmpty { null }
                                        } else {
                                            reader.skipValue()
                                        }
                                        "bannerImage" -> banner = parseString(reader)
                                        else -> reader.skipValue()
                                    }
                                }
                                reader.endObject()
                                // Wide banner beats no image at all.
                                val img = cover?.ifEmpty { null } ?: banner?.ifEmpty { null }
                                if (!img.isNullOrEmpty()) return img
                            } else {
                                reader.skipValue()
                            }
                        }
                        reader.endObject()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private suspend fun fetchJikanCover(cleanTitle: String): String? {
        // Jikan rate limit is 3 req/s; stay far below it and back off on 429.
        delay(1000L)
        return try {
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val url = "https://api.jikan.moe/v4/anime?q=$encodedTitle&limit=1"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.code == 429) {
                    cooldownUntil[cleanTitle] = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
                    logD("CoverImageFetcher", "Jikan API rate limited (429) for '$cleanTitle', cooldown 24h")
                    return null
                }
                if (!response.isSuccessful) {
                    logD("CoverImageFetcher", "Jikan API HTTP ${response.code} for '$cleanTitle'")
                    return null
                }
                val jsonString = response.body?.string() ?: return null
                logD("CoverImageFetcher", "Jikan response for '$cleanTitle': ${jsonString.take(100)}")
                parseJikanImage(jsonString)
            }
        } catch (e: Exception) {
            logD("CoverImageFetcher", "Jikan API error for '$cleanTitle': ${e.message}")
            null
        }
    }

    private fun parseJikanImage(jsonString: String): String? {
        if (jsonString.isBlank()) return null
        try {
            val reader = JsonReader.of(Buffer().writeUtf8(jsonString))
            reader.isLenient = true
            if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "data" && reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                        reader.beginArray()
                        if (reader.hasNext() && reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                if (reader.nextName() == "images" && reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                    reader.beginObject()
                                    while (reader.hasNext()) {
                                        if (reader.nextName() == "jpg" && reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                                            var largeImageUrl: String? = null
                                            var imageUrl: String? = null
                                            reader.beginObject()
                                            while (reader.hasNext()) {
                                                when (reader.nextName()) {
                                                    "large_image_url" -> largeImageUrl = parseString(reader)
                                                    "image_url" -> imageUrl = parseString(reader)
                                                    else -> reader.skipValue()
                                                }
                                            }
                                            reader.endObject()
                                            val img = largeImageUrl?.ifEmpty { null } ?: imageUrl?.ifEmpty { null }
                                            if (!img.isNullOrEmpty()) return img
                                        } else {
                                            reader.skipValue()
                                        }
                                    }
                                    reader.endObject()
                                } else {
                                    reader.skipValue()
                                }
                            }
                            reader.endObject()
                        }
                        reader.endArray()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun fetchBangumiWebSearch(cleanTitle: String): String? {
        return try {
            val encodedTitle = URLEncoder.encode(cleanTitle, "UTF-8")
            val url = "https://bgm.tv/subject_search/$encodedTitle?cat=2"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val html = response.body?.string() ?: return null
                val doc = Jsoup.parse(html)
                val imgElem = doc.select("img[src*='bgm.tv/pic/cover/']").firstOrNull()
                val rawSrc = imgElem?.attr("src")?.ifEmpty { null } ?: imgElem?.attr("data-src")?.ifEmpty { null }
                if (!rawSrc.isNullOrBlank()) {
                    val fullUrl = if (rawSrc.startsWith("//")) "https:$rawSrc" else rawSrc
                    return fullUrl.replace("/s/", "/l/")
                        .replace("/m/", "/l/")
                        .replace("/c/", "/l/")
                        .replace("/g/", "/l/")
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseString(reader: JsonReader): String {
        return try {
            when (reader.peek()) {
                JsonReader.Token.STRING, JsonReader.Token.NUMBER -> reader.nextString().trim()
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
}
