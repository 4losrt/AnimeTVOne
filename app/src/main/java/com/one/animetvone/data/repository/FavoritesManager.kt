package com.one.animetvone.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FavoritesManager {
    private const val PREF_NAME = "anime_favorites_prefs"
    private const val KEY_FAVORITES = "favorite_anime_ids"

    private val _favoritesState = MutableStateFlow<Set<String>>(emptySet())
    val favoritesState: StateFlow<Set<String>> = _favoritesState.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        _favoritesState.value = set
    }

    fun getFavoriteIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        _favoritesState.value = set
        return set
    }

    fun isFavorite(context: Context, animeId: String): Boolean {
        return getFavoriteIds(context).contains(animeId)
    }

    fun toggleFavorite(context: Context, animeId: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val current = (prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()).toMutableSet()
        val isFav = if (current.contains(animeId)) {
            current.remove(animeId)
            false
        } else {
            current.add(animeId)
            true
        }
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
        _favoritesState.value = current
        return isFav
    }
}
