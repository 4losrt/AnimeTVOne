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
    private const val EPISODE_MARKER = "_ep_"

    // "<number>" at the start of the tail after "_ep_" (tokens may follow).
    private val EPISODE_NUMBER_PATTERN = Regex("""(\d+)""")

    private val _historyState = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyState: StateFlow<List<HistoryItem>> = _historyState.asStateFlow()

    fun init(context: Context) {
        getHistoryList(context)
    }

    fun getHistoryList(context: Context): List<HistoryItem> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_HISTORY, null)
        val list = parseHistoryJson(jsonString)
        // Collapse duplicates written by older versions (same anime + same
        // episode stored twice under two different id shapes).
        val collapsed = dedupeByEpisode(list).sortedByDescending { it.timestamp }
        if (collapsed.size != list.size) {
            persist(context, collapsed)
        }
        _historyState.value = collapsed
        return collapsed
    }

    /**
     * Keeps at most one entry per (anime, episode). Records from older builds
     * only carry an id, so the episode number is derived from it when the field
     * itself is missing. The newest record wins (the list is timestamp-sorted
     * before this runs).
     */
    internal fun dedupeByEpisode(items: List<HistoryItem>): List<HistoryItem> {
        val kept = LinkedHashMap<String, HistoryItem>()
        for (item in items.sortedByDescending { it.timestamp }) {
            val number = item.episodeNumber.takeIf { it > 0 } ?: episodeNumberFromId(item.episodeId)
            val key = if (number > 0) "${item.animeId}#$number" else item.episodeId
            if (!kept.containsKey(key)) {
                kept[key] = item
            }
        }
        return kept.values.toList()
    }

    fun saveHistoryItem(
        context: Context,
        animeId: String,
        animeTitle: String,
        coverUrl: String,
        episodeId: String,
        episodeTitle: String,
        playbackPositionMs: Long,
        durationMs: Long,
        episodeNumber: Int = 0
    ) {
        if (animeId.isBlank() || episodeId.isBlank()) return

        val currentList = getHistoryList(context).toMutableList()

        // One entry per episode, no matter which id shape it arrived with.
        val newNumber = episodeNumber.takeIf { it > 0 } ?: episodeNumberFromId(episodeId)
        currentList.removeAll { existing ->
            existing.episodeId == episodeId ||
                (newNumber > 0 &&
                    existing.animeId == animeId &&
                    (existing.episodeNumber.takeIf { it > 0 }
                        ?: episodeNumberFromId(existing.episodeId)) == newNumber)
        }

        val newItem = HistoryItem(
            animeId = animeId,
            animeTitle = animeTitle,
            coverUrl = coverUrl,
            episodeId = episodeId,
            episodeTitle = episodeTitle,
            playbackPositionMs = playbackPositionMs,
            durationMs = durationMs,
            timestamp = System.currentTimeMillis(),
            episodeNumber = newNumber
        )

        currentList.add(0, newItem)

        val trimmedList = if (currentList.size > MAX_HISTORY_ITEMS) {
            currentList.take(MAX_HISTORY_ITEMS)
        } else {
            currentList
        }

        persist(context, trimmedList)
        _historyState.value = trimmedList
    }

    private fun persist(context: Context, list: List<HistoryItem>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        try {
            val jsonString = serializeHistoryJson(list)
            prefs.edit().putString(KEY_HISTORY, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Episode number encoded in an episode id: `<animeId>_ep_<n>[_<token>]`.
     * Returns 0 when the id carries no `_ep_` marker (the number cannot be
     * trusted then, and guessing from arbitrary digits collapsed unrelated
     * episodes into one history entry).
     */
    fun episodeNumberFromId(episodeId: String): Int {
        if (episodeId.isBlank()) return 0
        val markerIndex = episodeId.lastIndexOf(EPISODE_MARKER)
        if (markerIndex < 0) return 0
        val tail = episodeId.substring(markerIndex + EPISODE_MARKER.length)
        return EPISODE_NUMBER_PATTERN.find(tail)?.groupValues?.get(1)?.toIntOrNull() ?: 0
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
                            timestamp = timestamp,
                            episodeNumber = obj.optInt("episodeNumber", 0)
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
            obj.put("episodeNumber", item.episodeNumber)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}
