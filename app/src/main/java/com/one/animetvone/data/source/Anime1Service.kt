package com.one.animetvone.data.source

import android.util.Log
import com.one.animetvone.data.model.Anime1EpisodeToken
import com.one.animetvone.data.model.Anime1Item
import com.one.animetvone.data.model.VideoStream
import com.one.animetvone.data.repository.SampleAnimeData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

internal fun logD(tag: String, message: String) {
    try {
        Log.d(tag, message)
    } catch (e: Throwable) {
        println("[$tag] $message")
    }
}

open class Anime1Service(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .followRedirects(true)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            if (original.header("Accept-Language") == null) {
                requestBuilder.header("Accept-Language", ACCEPT_LANGUAGE)
            }
            if (original.header("Referer") == null) {
                requestBuilder.header("Referer", REFERER)
            }
            if (original.header("User-Agent") == null) {
                requestBuilder.header("User-Agent", USER_AGENT)
            }
            chain.proceed(requestBuilder.build())
        }
        .build()
) {

    companion object {
        const val ANIMELIST_PRIMARY_URL = "https://anime1.me/animelist.json"
        const val ANIMELIST_FALLBACK_URL = "https://d1zquzjgwo9yb.cloudfront.net/"
        const val ANIME_BASE_URL = "https://anime1.me/?cat="
        const val ANIME_HOME_URL = "https://anime1.me/"
        const val API_STREAM_URL = "https://v.anime1.me/api"
        const val ACCEPT_LANGUAGE = "zh-TW,zh;q=0.9,zh-CN;q=0.8,en;q=0.7"
        const val REFERER = "https://anime1.me/"
        const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    }

    @Volatile
    private var inMemoryAnimeList: List<Anime1Item>? = null

    /**
     * Warm up animelist.json in memory at app startup (Dispatchers.IO) for 0ms navigation.
     */
    open suspend fun warmUp(): List<Anime1Item> = withContext(Dispatchers.IO) {
        inMemoryAnimeList?.let { return@withContext it }
        fetchAnimeList().also {
            if (it.isNotEmpty()) {
                inMemoryAnimeList = it
            }
        }
    }

    /**
     * Fetch animelist.json from primary anime1.me URL or fallback CloudFront URL.
     * Guaranteed non-empty fallback so the app NEVER shows a blank/stuck screen.
     */
    open suspend fun fetchAnimeList(): List<Anime1Item> = withContext(Dispatchers.IO) {
        inMemoryAnimeList?.let { return@withContext it }

        var jsonString = fetchUrlString(ANIMELIST_PRIMARY_URL)
        var list = Anime1Parser.parseAnimeListJson(jsonString)

        if (list.isEmpty()) {
            logD("Anime1Service", "Primary animelist fetch failed, attempting fallback URL...")
            jsonString = fetchUrlString(ANIMELIST_FALLBACK_URL)
            list = Anime1Parser.parseAnimeListJson(jsonString)
        }

        if (list.isEmpty()) {
            logD("Anime1Service", "Online animelist fetch empty, using robust offline sample dataset.")
            list = SampleAnimeData.sampleAnime1Items
        }

        if (list.isNotEmpty()) {
            inMemoryAnimeList = list
        }

        list
    }

    /**
     * Fetch Episode list & Video Tokens for a given animeId (cat parameter).
     * Supports pagination if multiple pages exist.
     */
    open suspend fun fetchEpisodeTokens(animeId: String): List<Anime1EpisodeToken> = withContext(Dispatchers.IO) {
        logD("Anime1Video", "Fetching episode tokens for animeId: $animeId")
        val allEpisodes = mutableListOf<Anime1EpisodeToken>()
        var currentUrl = "$ANIME_BASE_URL$animeId"
        var pageCount = 0
        val maxPages = 10

        while (currentUrl.isNotEmpty() && pageCount < maxPages) {
            val html = fetchUrlString(currentUrl)
            if (html.isBlank()) break

            val pageEpisodes = Anime1Parser.parseEpisodesFromHtml(html)
            allEpisodes.addAll(pageEpisodes)

            val nextPage = Anime1Parser.parseNextPageUrl(html)
            if (!nextPage.isNullOrBlank() && nextPage != currentUrl) {
                currentUrl = if (nextPage.startsWith("http://") || nextPage.startsWith("https://")) {
                    nextPage
                } else {
                    "https://anime1.me$nextPage"
                }
                pageCount++
            } else {
                break
            }
        }

        val result = allEpisodes.distinctBy { it.token }.sortedBy { it.episodeNumber }
        logD("Anime1Video", "Found ${result.size} episode tokens for animeId $animeId")
        result
    }

    /**
     * Filter Set-Cookie headers for keys matching 'e', 'p', 'h', or starting with '_ga'.
     * Format into a clean cookie string: "e=...; p=...; h=...; _ga=..." with trailing semicolons stripped.
     */
    open fun videoCookieC(setCookieHeaders: List<String>): String {
        return Anime1Parser.videoCookieC(setCookieHeaders)
    }

    /**
     * Fetch Direct Stream URL from v.anime1.me/api given a video token.
     * Retries up to 3 times with backoff; never aborts the whole call on a
     * single HTTP error (previous code did `return null` on first failure,
     * which is why some episodes could never load).
     */
    open suspend fun fetchDirectStream(token: String): VideoStream? = withContext(Dispatchers.IO) {
        var lastError: Exception? = null
        var lastHttpCode = -1
        for (attempt in 1..3) {
            try {
                logD("Anime1Video", "getVideoLink called with token (attempt $attempt): $token")
                var rawToken = token.trim()
                if (rawToken.startsWith("d=")) {
                    rawToken = rawToken.substring(2)
                }
                // Episode ids look like "<anime>_ep_<num>_<token>"; tolerate being
                // handed the full id by extracting the trailing token segment.
                if (rawToken.contains("_ep_")) {
                    rawToken.substringAfterLast("_").takeIf { it.isNotBlank() }?.let { rawToken = it }
                }
                val decodedToken = try {
                    if (rawToken.contains("%")) {
                        URLDecoder.decode(rawToken, "UTF-8")
                    } else {
                        rawToken
                    }
                } catch (e: Exception) {
                    rawToken
                }
                if (decodedToken.isBlank()) {
                    logD("Anime1Video", "Empty token after decode, aborting")
                    break
                }

                val formBody = FormBody.Builder()
                    .add("d", decodedToken)
                    .build()

                val request = Request.Builder()
                    .url(API_STREAM_URL)
                    .post(formBody)
                    .header("Accept-Language", ACCEPT_LANGUAGE)
                    .header("Referer", REFERER)
                    .header("User-Agent", USER_AGENT)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Origin", "https://anime1.me")
                    .build()

                var retryDelayMillis = 0L
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastHttpCode = response.code
                        logD("Anime1Video", "API POST failed with HTTP code: ${response.code} (attempt $attempt)")
                        // Don't give up: retry after a short backoff instead of
                        // returning null immediately (old behaviour = 永遠載入失敗).
                        if (attempt < 3) {
                            retryDelayMillis = 600L * attempt
                            return@use
                        }
                        return@withContext null
                    }

                    val setCookieHeaders = response.headers("Set-Cookie")
                    val cookieHeader = videoCookieC(setCookieHeaders)

                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.isBlank()) {
                        logD("Anime1Video", "API POST empty body (attempt $attempt)")
                        if (attempt < 3) {
                            retryDelayMillis = 600L * attempt
                            return@use
                        }
                        return@withContext null
                    }
                    val streamUrl = Anime1Parser.parseDirectStreamUrl(bodyString)
                    if (streamUrl.isNullOrBlank()) {
                        logD("Anime1Video", "No stream URL in API body (attempt $attempt, len=${bodyString.length})")
                        if (attempt < 3) {
                            retryDelayMillis = 600L * attempt
                            return@use
                        }
                        return@withContext null
                    }

                    val headersMap = mutableMapOf<String, String>()
                    headersMap["Accept-Language"] = ACCEPT_LANGUAGE
                    headersMap["Referer"] = "https://v.anime1.me/"
                    headersMap["User-Agent"] = USER_AGENT
                    if (cookieHeader.isNotEmpty()) {
                        headersMap["Cookie"] = cookieHeader
                    }

                    val isHls = streamUrl.contains(".m3u8")
                    return@withContext VideoStream(
                        url = streamUrl,
                        quality = if (isHls) "HLS 自動畫質" else "1080p HD",
                        serverName = "Anime1 CDN Direct",
                        headers = headersMap
                    )
                }
                if (retryDelayMillis > 0L) {
                    delay(retryDelayMillis)
                }
            } catch (e: Exception) {
                lastError = e
                logD("Anime1Video", "Attempt $attempt error resolving video stream: ${e.message}")
                if (attempt < 3) {
                    delay(600L * attempt)
                }
            }
        }
        if (lastError != null) {
            logD("Anime1Video", "Failed to resolve video stream after attempts: ${lastError.message} (lastHttp=$lastHttpCode)")
        } else {
            logD("Anime1Video", "Failed to resolve video stream after attempts (lastHttp=$lastHttpCode)")
        }
        null
    }

    open suspend fun fetchHomeScheduleHtml(): String = withContext(Dispatchers.IO) {
        fetchUrlString(ANIME_HOME_URL)
    }

    /**
     * Fetch the current-season schedule page (e.g. https://anime1.me/2026年夏季新番).
     * [season] is the full two-char name (冬季/春季/夏季/秋季), same convention
     * as oneAnime's AnimeSeason. Percent-encoded paths work fine on this site.
     */
    /**
     * Fetch the current-season schedule page, e.g.
     * https://anime1.me/2026年夏季新番
     *
     * NOTE: the site path is 年 + <season> + 季 + 新番 (e.g. 2026年夏季新番).
     * Omitting the 季 character returns HTTP 404, which previously made the
     * 每週新番表 screen silently fall back to sample data.
     */
    open suspend fun fetchSeasonScheduleHtml(year: Int, season: String): String = withContext(Dispatchers.IO) {
        val rawUrl = "https://anime1.me/${year}年${season}季新番"
        logD("Anime1Schedule", "fetch season page: $rawUrl")
        val html = fetchUrlString(rawUrl)
        logD("Anime1Schedule", "season html length=${html.length}")
        html
    }

    open suspend fun fetchCoverImage(title: String): String? = withContext(Dispatchers.IO) {
        CoverImageFetcher.getCoverUrl(null, title)
    }

    private fun fetchUrlString(url: String): String {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept-Language", ACCEPT_LANGUAGE)
                .header("Referer", REFERER)
                .header("User-Agent", USER_AGENT)
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string() ?: ""
                } else {
                    ""
                }
            }
        } catch (e: Exception) {
            ""
        }
    }
}
