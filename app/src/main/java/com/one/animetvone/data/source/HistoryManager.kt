package com.one.animetvone.data.source

import android.content.Context
import com.one.animetvone.data.model.HistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object HistoryManager {
    private const val PREF_NAME = "anime_history_prefs"
    private const val KEY_HISTORY = "history_items_json"
    private const val MAX_HISTORY_ITEMS = 100

    private val _historyState = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyState: StateFlow<List<HistoryItem>> = _historyState.asStateFlow()

    fun init(context: Context) {
        getHistoryList(context)
    }

    fun getHistoryList(context: Context): List<HistoryItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_HISTORY, null)
        val list = parseHistoryJson(jsonString)
        val sorted = list.sortedByDescending { it.timestamp }
        _historyState.value = sorted
        return sorted
    }

    fun saveHistoryItem(
        context: Context,
        animeId: String,
        animeTitle: String,
        coverUrl: String,
        episodeId: String,
        episodeTitle: String,
        playbackPositionMs: Long,
        durationMs: Long
    ) {
        if (animeId.isBlank() || episodeId.isBlank()) return

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentList = getHistoryList(context).toMutableList()

        // Remove duplicate entry for same episode or same anime if updating to latest watched episode
        currentList.removeAll { it.episodeId == episodeId }

        val newItem = HistoryItem(
            animeId = animeId,
            animeTitle = animeTitle,
            coverUrl = coverUrl,
            episodeId = episodeId,
            episodeTitle = episodeTitle,
            playbackPositionMs = playbackPositionMs,
            durationMs = durationMs,
            timestamp = System.currentTimeMillis()
        )

        currentList.add(0, newItem)

        val trimmedList = if (currentList.size > MAX_HISTORY_ITEMS) {
            currentList.take(MAX_HISTORY_ITEMS)
        } else {
            currentList
        }

        try {
            val jsonString = serializeHistoryJson(trimmedList)
            prefs.edit().putString(KEY_HISTORY, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _historyState.value = trimmedList
    }

    fun removeHistoryItem(context: Context, episodeId: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentList = getHistoryList(context).toMutableList()
        currentList.removeAll { it.episodeId == episodeId }

        try {
            val jsonString = serializeHistoryJson(currentList)
            prefs.edit().putString(KEY_HISTORY, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _historyState.value = currentList
    }

    fun clearHistory(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
        _historyState.value = emptyList()
    }

    private fun parseHistoryJson(jsonString: String?): List<HistoryItem> {
        if (jsonString.isNullOrBlank()) return emptyList()
        val list = mutableListOf<HistoryItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                val animeId = obj.optString("animeId", "")
                val animeTitle = obj.optString("animeTitle", "")
                val coverUrl = obj.optString("coverUrl", "")
                val episodeId = obj.optString("episodeId", "")
                val episodeTitle = obj.optString("episodeTitle", "")
                val playbackPositionMs = obj.optLong("playbackPositionMs", 0L)
                val durationMs = obj.optLong("durationMs", 0L)
                val timestamp = obj.optLong("timestamp", 0L)

                if (animeId.isNotBlank() && episodeId.isNotBlank()) {
                    list.add(
                        HistoryItem(
                            animeId = animeId,
                            animeTitle = animeTitle,
                            coverUrl = coverUrl,
                            episodeId = episodeId,
                            episodeTitle = episodeTitle,
                            playbackPositionMs = playbackPositionMs,
                            durationMs = durationMs,
                            timestamp = timestamp
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun serializeHistoryJson(list: List<HistoryItem>): String {
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("animeId", item.animeId)
            obj.put("animeTitle", item.animeTitle)
            obj.put("coverUrl", item.coverUrl)
            obj.put("episodeId", item.episodeId)
            obj.put("episodeTitle", item.episodeTitle)
            obj.put("playbackPositionMs", item.playbackPositionMs)
            obj.put("durationMs", item.durationMs)
            obj.put("timestamp", item.timestamp)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}
