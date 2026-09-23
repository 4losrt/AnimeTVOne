package com.one.animetvone.data.repository

import kotlinx.coroutines.Dispatchers

/**
 * App-wide singleton provider so every ViewModel observes the SAME
 * AnimeRepositoryImpl instance (and therefore the same _animeListState).
 * Without this, each ViewModel created its own repository copy and the
 * background anime1.me sync done by one instance was invisible to the rest,
 * leaving screens stuck on stale sample data forever.
 */
object AnimeRepositoryProvider {

    val instance: AnimeRepository by lazy {
        AnimeRepositoryImpl(ioDispatcher = Dispatchers.IO)
    }
}