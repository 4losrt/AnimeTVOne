package com.one.animetvone.data.source

import android.content.Context
import com.one.animetvone.data.model.Anime
import com.squareup.moshi.JsonReader
import okio.Buffer
import java.io.File

object AnimeListDiskCache {

    private const val PREF_NAME = "anime_disk_cache_prefs"
    private const val KEY_ANIME_LIST_JSON = "anime_list_cache_json"
    private const val KEY_LAST_UPDATED = "anime_list_cache_last_updated"
    private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000
    private const val FILE_NAME = "anime_list_cache.json"

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var memoryCache: List<Anime>? = null

    private var customCacheFile: File? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun setCacheFile(file: File) {
        customCacheFile = file
    }

    /**
     * Reads local cache instantly (0ms) on app launch.
     */
    fun loadFromDisk(): List<Anime> {
        memoryCache?.let { return it }

        var jsonString: String? = null

        // 1. Try reading from disk file
        val file = getCacheFile()
        if (file != null && file.exists() && file.length() > 0) {
            try {
                jsonString = file.readText()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Fallback to SharedPreferences if file reading failed or file was missing
        if (jsonString.isNullOrBlank() && appContext != null) {
            try {
                val prefs = appContext!!.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                jsonString = prefs.getString(KEY_ANIME_LIST_JSON, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val parsedList = parseAnimeListJson(jsonString)
        if (parsedList.isNotEmpty()) {
            memoryCache = parsedList
        }
        return parsedList
    }

    /**
     * Saves fetched network list to disk.
     */
    fun saveToDisk(list: List<Anime>) {
        if (list.isEmpty()) return
        memoryCache = list

        val jsonString = serializeAnimeListJson(list)

        // 1. Write to disk file
        val file = getCacheFile()
        if (file != null) {
            try {
                file.parentFile?.mkdirs()
                file.writeText(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Write to SharedPreferences
        appContext?.let { ctx ->
            try {
                val prefs = ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putString(KEY_ANIME_LIST_JSON, jsonString)
                    .putLong(KEY_LAST_UPDATED, System.currentTimeMillis())
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Old data remains readable offline; this only controls network refreshes. */
    fun isFresh(now: Long = System.currentTimeMillis()): Boolean {
        val updated = appContext?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            ?.getLong(KEY_LAST_UPDATED, 0L) ?: 0L
        return updated > 0L && now - updated in 0 until CACHE_TTL_MS
    }

    fun lastUpdatedMillis(): Long = appContext
        ?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        ?.getLong(KEY_LAST_UPDATED, 0L) ?: 0L

    fun clearCache() {
        memoryCache = null
        val file = getCacheFile()
        if (file != null && file.exists()) {
            try {
                file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        appContext?.let { ctx ->
            try {
                ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().clear().apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getCacheFile(): File? {
        customCacheFile?.let { return it }
        return appContext?.let { File(it.filesDir, FILE_NAME) } ?: File(FILE_NAME)
    }

    private fun parseAnimeListJson(jsonString: String?): List<Anime> {
        if (jsonString.isNullOrBlank()) return emptyList()
        val list = mutableListOf<Anime>()
        try {
            val reader = JsonReader.of(Buffer().writeUtf8(jsonString))
            reader.isLenient = true
            if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                reader.beginArray()
                while (reader.hasNext()) {
                    if (reader.peek() == JsonReader.Token.BEGIN_OBJECT) {
                        reader.beginObject()
                        var id = ""
                        var title = ""
                        var coverUrl = ""
                        var bannerUrl = ""
                        var description = ""
                        var rating = 8.0
                        var year = 2024
                        var status = "已完結"
                        var episodeCount = 12
                        var follow = false
                        var progress = 0
                        val genresList = mutableListOf<String>()

                        while (reader.hasNext()) {
                            when (reader.nextName()) {
                                "id" -> id = parseNextString(reader)
                                "title" -> title = parseNextString(reader)
                                "coverUrl" -> coverUrl = parseNextString(reader)
                                "bannerUrl" -> bannerUrl = parseNextString(reader)
                                "description" -> description = parseNextString(reader)
                                "rating" -> rating = parseNextDouble(reader)
                                "year" -> year = parseNextInt(reader)
                                "status" -> status = parseNextString(reader)
                                "episodeCount" -> episodeCount = parseNextInt(reader)
                                "follow" -> follow = parseNextBoolean(reader)
                                "progress" -> progress = parseNextInt(reader)
                                "genres" -> {
                                    if (reader.peek() == JsonReader.Token.BEGIN_ARRAY) {
                                        reader.beginArray()
                                        while (reader.hasNext()) {
                                            val g = parseNextString(reader)
                                            if (g.isNotBlank()) genresList.add(g)
                                        }
                                        reader.endArray()
                                    } else {
                                        reader.skipValue()
                                    }
                                }
                                else -> reader.skipValue()
                            }
                        }
                        reader.endObject()

                        if (id.isNotBlank() && title.isNotBlank()) {
                            list.add(
                                Anime(
                                    id = id,
                                    title = title,
                                    coverUrl = coverUrl,
                                    bannerUrl = bannerUrl,
                                    description = description,
                                    rating = rating,
                                    genres = genresList,
                                    year = year,
                                    status = status,
                                    episodeCount = episodeCount,
                                    follow = follow,
                                    progress = progress
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
        return list
    }

    private fun serializeAnimeListJson(list: List<Anime>): String {
        val sb = StringBuilder()
        sb.append("[")
        list.forEachIndexed { index, anime ->
            if (index > 0) sb.append(",")
            sb.append("{")
            sb.append("\"id\":").append(escapeJsonString(anime.id)).append(",")
            sb.append("\"title\":").append(escapeJsonString(anime.title)).append(",")
            sb.append("\"coverUrl\":").append(escapeJsonString(anime.coverUrl)).append(",")
            sb.append("\"bannerUrl\":").append(escapeJsonString(anime.bannerUrl)).append(",")
            sb.append("\"description\":").append(escapeJsonString(anime.description)).append(",")
            sb.append("\"rating\":").append(anime.rating).append(",")
            sb.append("\"year\":").append(anime.year).append(",")
            sb.append("\"status\":").append(escapeJsonString(anime.status)).append(",")
            sb.append("\"episodeCount\":").append(anime.episodeCount).append(",")
            sb.append("\"follow\":").append(anime.follow).append(",")
            sb.append("\"progress\":").append(anime.progress).append(",")
            sb.append("\"genres\":[")
            anime.genres.forEachIndexed { gIdx, genre ->
                if (gIdx > 0) sb.append(",")
                sb.append(escapeJsonString(genre))
            }
            sb.append("]")
            sb.append("}")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun escapeJsonString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
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

    private fun parseNextInt(reader: JsonReader): Int {
        return try {
            when (reader.peek()) {
                JsonReader.Token.NUMBER -> reader.nextInt()
                JsonReader.Token.STRING -> reader.nextString().toIntOrNull() ?: 0
                else -> {
                    reader.skipValue()
                    0
                }
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun parseNextDouble(reader: JsonReader): Double {
        return try {
            when (reader.peek()) {
                JsonReader.Token.NUMBER -> reader.nextDouble()
                JsonReader.Token.STRING -> reader.nextString().toDoubleOrNull() ?: 0.0
                else -> {
                    reader.skipValue()
                    0.0
                }
            }
        } catch (e: Exception) {
            0.0
        }
    }

    private fun parseNextBoolean(reader: JsonReader): Boolean {
        return try {
            when (reader.peek()) {
                JsonReader.Token.BOOLEAN -> reader.nextBoolean()
                JsonReader.Token.STRING -> reader.nextString().toBoolean()
                else -> {
                    reader.skipValue()
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}
