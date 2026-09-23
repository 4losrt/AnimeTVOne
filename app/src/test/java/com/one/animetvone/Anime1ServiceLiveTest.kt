package com.one.animetvone

import com.one.animetvone.data.source.Anime1Service
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class Anime1ServiceLiveTest {

    @Test
    fun testLiveAnimeListAndDirectStreamResolutionWithCookies() = runBlocking {
        val service = Anime1Service()

        // 1. Fetch anime list
        println("Fetching live anime list...")
        val animeList = service.fetchAnimeList()
        assertNotNull("Anime list should not be null", animeList)
        assertTrue("Anime list should not be empty", animeList.isNotEmpty())
        println("Fetched ${animeList.size} anime items.")

        val targetAnime = animeList.firstOrNull { it.id > 0 }
        assertNotNull("Should find target anime", targetAnime)
        println("Target anime: ${targetAnime?.title} (ID: ${targetAnime?.id})")

        // 2. Fetch episode tokens
        println("Fetching episode tokens...")
        val tokens = service.fetchEpisodeTokens(targetAnime!!.id.toString())
        assertTrue("Tokens list should not be empty", tokens.isNotEmpty())
        val firstToken = tokens.first()
        println("Raw token from HTML parsing: ${firstToken.token}")

        // 3. Fetch direct stream (service handles token URL-decoding automatically)
        val stream = service.fetchDirectStream(firstToken.token)
        assertNotNull("Stream should be resolved directly from raw HTML token", stream)
        println("Resolved direct stream URL: ${stream?.url}")
        println("Resolved direct stream headers: ${stream?.headers}")

        assertNotNull("Headers map should exist", stream?.headers)
        val headers = stream!!.headers!!
        assertTrue("Cookie header should exist in response headers", headers.containsKey("Cookie"))
        println("Cookie header content: ${headers["Cookie"]}")

        // 4. Test actual HTTP GET Range request to the stream URL to ensure ExoPlayer / video player can stream video
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val reqBuilder = Request.Builder()
            .url(stream.url)
            .header("Range", "bytes=0-1023")

        headers.forEach { (key, value) ->
            reqBuilder.header(key, value)
        }

        client.newCall(reqBuilder.build()).execute().use { resp ->
            println("Stream Range request HTTP status code: ${resp.code}")
            println("Stream Content-Type: ${resp.header("Content-Type")}")
            println("Stream Content-Length: ${resp.header("Content-Length")}")
            assertTrue("Video stream request should return HTTP 200 or 206", resp.code == 200 || resp.code == 206)
        }
    }
}
