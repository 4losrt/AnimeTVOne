package com.one.animetvone.data.repository

import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.model.AnimeFeedSection
import com.one.animetvone.data.model.Episode
import com.one.animetvone.data.model.ScheduleDay
import com.one.animetvone.data.model.VideoStream
import kotlinx.coroutines.flow.Flow

interface AnimeRepository {
    suspend fun warmUp()
    fun getHomeFeed(): Flow<List<AnimeFeedSection>>
    fun getSchedule(): Flow<List<ScheduleDay>>
    fun getAllAnimeList(): Flow<List<Anime>>
    fun getFavoriteAnimeList(favoriteIds: Set<String>): Flow<List<Anime>>
    fun searchAnime(query: String, category: String? = null): Flow<List<Anime>>
    fun getAnimeDetails(animeId: String): Flow<Anime?>
    fun getEpisodes(animeId: String): Flow<List<Episode>>
    fun getVideoStream(episodeId: String): Flow<VideoStream?>
    fun evictVideoStream(episodeId: String)
}
