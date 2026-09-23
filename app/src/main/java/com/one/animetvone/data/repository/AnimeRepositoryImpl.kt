package com.one.animetvone.data.repository

import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.model.Anime1EpisodeToken
import com.one.animetvone.data.model.Anime1Item
import com.one.animetvone.data.model.AnimeFeedSection
import com.one.animetvone.data.model.Episode
import com.one.animetvone.data.model.ScheduleDay
import com.one.animetvone.data.model.VideoStream
import com.one.animetvone.data.source.Anime1Parser
import com.one.animetvone.data.source.Anime1Service
import com.one.animetvone.data.source.AnimeListDiskCache
import com.one.animetvone.data.source.CoverImageFetcher
import com.one.animetvone.util.ChineseConverter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class AnimeRepositoryImpl(
    private val anime1Service: Anime1Service = Anime1Service(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AnimeRepository {

    private val _animeListState = MutableStateFlow<List<Anime>>(emptyList())
    val animeListState: StateFlow<List<Anime>> = _animeListState.asStateFlow()

    private var animeListCache: List<Anime1Item>? = null
    private val animeMapCache = ConcurrentHashMap<String, Anime>()
    private val episodeTokensCache = ConcurrentHashMap<String, List<Anime1EpisodeToken>>()
    private val episodeMapCache = ConcurrentHashMap<String, List<Episode>>()
    private val tokenByEpisodeIdMap = ConcurrentHashMap<String, String>()
    private val streamCache = ConcurrentHashMap<String, VideoStream>()
    // anime1.me cookies expire after ~7.6h; drop cached streams earlier so
    // ExoPlayer never reuses a dead cookie (which shows as endless black).
    private val streamCacheTime = ConcurrentHashMap<String, Long>()
    private val streamCacheTtlMs = 6L * 60 * 60 * 1000
    private val coverImageCache = ConcurrentHashMap<String, String>()

    private val repositoryScope = CoroutineScope(ioDispatcher + SupervisorJob())
    private var coverSaveJob: Job? = null

    fun invalidateCovers() {
        coverImageCache.clear()
        val reset = _animeListState.value.map { it.copy(coverUrl = "", bannerUrl = "") }
        reset.forEach { animeMapCache[it.id] = it }
        _animeListState.value = reset
        AnimeListDiskCache.saveToDisk(reset)
    }

    init {
        // 1. Instant local read (0ms) from disk cache into _animeListState
        val diskCachedList = AnimeListDiskCache.loadFromDisk()
        val initialList = if (diskCachedList.isNotEmpty()) {
            diskCachedList
        } else {
            SampleAnimeData.sampleAnimes
        }
        _animeListState.value = initialList
        initialList.forEach { animeMapCache[it.id] = it }

        // 2. Cover image update listener
        repositoryScope.launch {
            CoverImageFetcher.coverUpdates.collect { (cleanTitle, newUrl) ->
                coverImageCache[cleanTitle] = newUrl
                var updatedAny = false
                var matchCandidates = 0
                val currentList = _animeListState.value.map { anime ->
                    if (CoverImageFetcher.cleanAnimeTitle(anime.title) == cleanTitle) {
                        matchCandidates++
                        updatedAny = true
                        val updated = anime.copy(coverUrl = newUrl, bannerUrl = newUrl)
                        animeMapCache[anime.id] = updated
                        updated
                    } else {
                        anime
                    }
                }
                android.util.Log.i(
                    "Anime1Cover",
                    "update title=[$cleanTitle] matches=$matchCandidates listSize=${currentList.size}"
                )
                if (updatedAny) {
                    _animeListState.value = currentList
                    // Several covers may resolve together. Save the final list
                    // once instead of serializing 1,800 items for every image.
                    coverSaveJob?.cancel()
                    coverSaveJob = repositoryScope.launch {
                        delay(1_500)
                        AnimeListDiskCache.saveToDisk(_animeListState.value)
                    }
                }
            }
        }

        // 3. Simultaneously launch background silent sync to fetch fresh animelist.json
        repositoryScope.launch {
            syncNetworkAnimeListSilently()
        }
    }

    /**
     * Silent background sync for fresh animelist.json from network.
     * Merges network list with current state (preserving follow and progress).
     */
    suspend fun syncNetworkAnimeListSilently() = withContext(ioDispatcher) {
        // Keep startup instant/offline and avoid downloading the full catalogue
        // on every TV launch. The cache remains readable while offline.
        if (AnimeListDiskCache.loadFromDisk().isNotEmpty() && AnimeListDiskCache.isFresh()) {
            return@withContext
        }
        try {
            val freshNetworkItems = anime1Service.fetchAnimeList()
            if (freshNetworkItems.isNotEmpty()) {
                animeListCache = freshNetworkItems
                val currentList = _animeListState.value
                val currentStateMap = currentList.associateBy { it.id }

                // Phase 1: map everything instantly using ONLY 0ms fast covers (memory/disk/prepopulated).
                // Online cover resolution is kicked off in the background by the listener,
                // so the real anime1.me list appears in the UI immediately.
                val mappedList = freshNetworkItems.map { item ->
                    mapAnime1ItemToAnimeFast(item)
                }

                val mergedList = mappedList.map { freshAnime ->
                    val existingAnime = currentStateMap[freshAnime.id] ?: animeMapCache[freshAnime.id]

                    if (existingAnime != null) {
                        // Merge state: preserve user follow and progress
                        val preservedCover = if (freshAnime.coverUrl.isNotBlank() && !freshAnime.coverUrl.contains("unsplash")) {
                            freshAnime.coverUrl
                        } else if (!existingAnime.coverUrl.contains("unsplash")) {
                            existingAnime.coverUrl
                        } else {
                            freshAnime.coverUrl
                        }

                        val merged = freshAnime.copy(
                            coverUrl = preservedCover,
                            bannerUrl = preservedCover,
                            follow = existingAnime.follow,
                            progress = existingAnime.progress
                        )
                        animeMapCache[merged.id] = merged
                        merged
                    } else {
                        animeMapCache[freshAnime.id] = freshAnime
                        freshAnime
                    }
                }

                _animeListState.value = mergedList
                AnimeListDiskCache.saveToDisk(mergedList)

                // No proactive prefetch: covers load strictly on demand when a
                // card becomes visible (AnimeCard), respecting rate limits.
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
        }
    }

    override suspend fun warmUp() = withContext(ioDispatcher) {
        syncNetworkAnimeListSilently()
    }

    override fun getHomeFeed(): Flow<List<AnimeFeedSection>> =
        animeListState.map { currentList ->
            val list = currentList.ifEmpty {
                AnimeListDiskCache.loadFromDisk().ifEmpty { SampleAnimeData.sampleAnimes }
            }
            buildFeedSectionsFromAnimeList(list)
        }

    override fun getSchedule(): Flow<List<ScheduleDay>> = flow {
        android.util.Log.d("Anime1Schedule", "getSchedule() flow STARTED")
        try {
            val animeItems = getOrFetchAnimeList()
            if (animeItems.isNotEmpty()) {
                // 1) Try the current-season schedule page first: it carries the real
                //    weekly table (the homepage table-list does not).
                val (year, seasonFull) = currentSeasonYearAndName()
                var scheduleMap: Map<String, List<Anime1Item>>? = null
                try {
                    val seasonHtml = anime1Service.fetchSeasonScheduleHtml(year, seasonFull)
                    if (seasonHtml.isNotBlank()) {
                        scheduleMap = Anime1Parser.parseScheduleFromHtml(seasonHtml, animeItems)
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    e.printStackTrace()
                }

                // 2) Homepage HTML as a secondary source.
                if (scheduleMap == null || scheduleMap.values.all { it.isEmpty() }) {
                    try {
                        val html = anime1Service.fetchHomeScheduleHtml()
                        scheduleMap = Anime1Parser.parseScheduleFromHtml(html, animeItems)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        e.printStackTrace()
                    }
                }

                if (scheduleMap != null && scheduleMap.values.any { it.isNotEmpty() }) {
                    // val counts = scheduleMap.map { (day, items) -> "$day=${items.size}" }
                    // android.util.Log.i("Anime1Schedule", "parsed days: ${counts.joinToString(",")}")
                    // Fast 0ms construction: covers come from cache only, never
                    // triggers network here. Schedule must stay light for TV.
                    val scheduleDays = scheduleMap.map { (dayName, items) ->
                        ScheduleDay(
                            dayName = dayName,
                            animes = items.map { mapAnime1ItemToAnimeFast(it) }
                        )
                    }
                    emit(scheduleDays)
                    return@flow
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
        }

        val days = Anime1Parser.DAYS_OF_WEEK
        val currentList = _animeListState.value.ifEmpty { SampleAnimeData.sampleAnimes }
        val fallbackDays = days.mapIndexed { index, dayName ->
            ScheduleDay(
                dayName = dayName,
                animes = currentList.filterIndexed { idx, _ -> idx % days.size == index }
                    .ifEmpty { currentList }
            )
        }
        emit(fallbackDays)
    }

    override fun getAllAnimeList(): Flow<List<Anime>> = _animeListState

    override fun getFavoriteAnimeList(favoriteIds: Set<String>): Flow<List<Anime>> = flow {
        if (favoriteIds.isEmpty()) {
            emit(emptyList())
            return@flow
        }
        val currentList = _animeListState.value.ifEmpty {
            AnimeListDiskCache.loadFromDisk().ifEmpty { SampleAnimeData.sampleAnimes }
        }
        val matched = currentList.filter { favoriteIds.contains(it.id) }
        emit(matched)
    }

    override fun searchAnime(query: String, category: String?): Flow<List<Anime>> = flow {
        val trimmedQuery = query.trim()
        val requestedCategory = category?.trim()
        val currentList = _animeListState.value.ifEmpty {
            AnimeListDiskCache.loadFromDisk().ifEmpty { SampleAnimeData.sampleAnimes }
        }

        val filtered = currentList.filter { anime ->
            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                anime.title.contains(trimmedQuery, ignoreCase = true) ||
                        anime.description.contains(trimmedQuery, ignoreCase = true) ||
                        anime.genres.any { it.contains(trimmedQuery, ignoreCase = true) }
            }

            val matchesCategory = if (requestedCategory.isNullOrBlank() || requestedCategory.equals("All", ignoreCase = true) || requestedCategory.equals("全部", ignoreCase = true)) {
                true
            } else {
                anime.genres.any { it.equals(requestedCategory, ignoreCase = true) || it.contains(requestedCategory, ignoreCase = true) }
            }

            matchesQuery && matchesCategory
        }

        emit(filtered)
    }

    override fun getAnimeDetails(animeId: String): Flow<Anime?> = flow {
        animeMapCache[animeId]?.let {
            emit(it)
            return@flow
        }

        val currentList = _animeListState.value
        val matched = currentList.find { it.id == animeId }
        if (matched != null) {
            animeMapCache[animeId] = matched
            emit(matched)
            return@flow
        }

        val sampleAnime = SampleAnimeData.sampleAnimes.find { it.id == animeId }
        emit(sampleAnime)
    }

    override fun getEpisodes(animeId: String): Flow<List<Episode>> = flow {
        episodeMapCache[animeId]?.let {
            emit(it)
            return@flow
        }

        var resultEpisodes: List<Episode>? = null
        try {
            val isNumericId = animeId.toIntOrNull() != null
            if (isNumericId) {
                val tokens = anime1Service.fetchEpisodeTokens(animeId)
                if (tokens.isNotEmpty()) {
                    episodeTokensCache[animeId] = tokens
                    val parentAnime = animeMapCache[animeId] ?: getAnimeDetailsInternal(animeId)
                    val coverUrl = parentAnime?.coverUrl ?: ""

                    val episodes = tokens.map { token ->
                        val epId = "${animeId}_ep_${token.episodeNumber}_${token.token}"
                        tokenByEpisodeIdMap[epId] = token.token

                        Episode(
                            id = epId,
                            animeId = animeId,
                            episodeNumber = token.episodeNumber,
                            title = ChineseConverter.toTraditionalChinese(token.title),
                            thumbnailUrl = coverUrl,
                            duration = "24:00",
                            streamUrl = ""
                        )
                    }

                    episodeMapCache[animeId] = episodes
                    resultEpisodes = episodes
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
        }

        if (!resultEpisodes.isNullOrEmpty()) {
            emit(resultEpisodes)
        } else {
            val sampleEpisodes = SampleAnimeData.sampleEpisodesMap[animeId]
            if (!sampleEpisodes.isNullOrEmpty()) {
                emit(sampleEpisodes)
            } else {
                val parentAnime = animeMapCache[animeId] ?: getAnimeDetailsInternal(animeId)
                val coverUrl = parentAnime?.coverUrl ?: ""
                val epCount = parentAnime?.episodeCount ?: 12
                val fallbackEpList = List(epCount.coerceAtLeast(1)) { index ->
                    val epNum = index + 1
                    val epId = "${animeId}_ep_$epNum"
                    Episode(
                        id = epId,
                        animeId = animeId,
                        episodeNumber = epNum,
                        title = "第 $epNum 集",
                        thumbnailUrl = coverUrl,
                        duration = "24:00",
                        streamUrl = ""
                    )
                }
                emit(fallbackEpList)
            }
        }
    }

    override fun getVideoStream(episodeId: String): Flow<VideoStream?> = flow {
        streamCache[episodeId]?.let { cached ->
            val age = System.currentTimeMillis() - (streamCacheTime[episodeId] ?: 0L)
            if (age < streamCacheTtlMs) {
                emit(cached)
                return@flow
            }
            streamCache.remove(episodeId)
            streamCacheTime.remove(episodeId)
        }

        var resultStream: VideoStream? = null
        try {
            var token = tokenByEpisodeIdMap[episodeId]
                ?: extractTokenFromEpisodeId(episodeId)

            if (token.isNullOrBlank()) {
                val parts = episodeId.split("_ep_")
                val animeId = parts.firstOrNull()
                if (!animeId.isNullOrBlank()) {
                    val tokens = anime1Service.fetchEpisodeTokens(animeId)
                    if (tokens.isNotEmpty()) {
                        episodeTokensCache[animeId] = tokens
                        tokens.forEach { epToken ->
                            val epId = "${animeId}_ep_${epToken.episodeNumber}_${epToken.token}"
                            tokenByEpisodeIdMap[epId] = epToken.token
                            if (epId == episodeId || epToken.token == episodeId) {
                                token = epToken.token
                            }
                        }
                        if (token.isNullOrBlank()) {
                            val num = parts.getOrNull(1)?.split("_")?.firstOrNull()?.toIntOrNull()
                            if (num != null) {
                                token = tokens.find { it.episodeNumber == num }?.token
                            }
                        }
                    }
                }
            }

            if (!token.isNullOrBlank()) {
                val stream = anime1Service.fetchDirectStream(token)
                if (stream != null) {
                    streamCache[episodeId] = stream
                    streamCacheTime[episodeId] = System.currentTimeMillis()
                    resultStream = stream
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            e.printStackTrace()
        }

        // Emit null (never a sample video) so the player shows an error + retry
        // instead of playing the wrong content.
        emit(resultStream)
    }

    override fun evictVideoStream(episodeId: String) {
        streamCache.remove(episodeId)
        streamCacheTime.remove(episodeId)
    }

    /**
     * Season convention copied from oneAnime's AnimeSeason: full two-char
     * names (冬季/春季/夏季/秋季) with day-aware boundaries (season flips on
     * the 2nd of Jan/Apr/Jul/Oct).
     */
    private fun currentSeasonYearAndName(): Pair<Int, String> {
        val now = java.util.Calendar.getInstance()
        val year = now.get(java.util.Calendar.YEAR)
        val month = now.get(java.util.Calendar.MONTH) + 1
        val day = now.get(java.util.Calendar.DAY_OF_MONTH)
        val season = when {
            (month == 1 && day >= 2) || month == 2 || month == 3 || (month == 4 && day < 2) -> "冬季"
            (month == 4 && day >= 2) || month == 5 || month == 6 || (month == 7 && day < 2) -> "春季"
            (month == 7 && day >= 2) || month == 8 || month == 9 || (month == 10 && day < 2) -> "夏季"
            else -> "秋季"
        }
        return Pair(year, season)
    }

    private suspend fun getOrFetchAnimeList(): List<Anime1Item> {
        animeListCache?.let { return it }
        val fetchedList = anime1Service.fetchAnimeList()
        if (fetchedList.isNotEmpty()) {
            animeListCache = fetchedList
        }
        return fetchedList
    }

    private fun buildFeedSectionsFromAnimeList(items: List<Anime>): List<AnimeFeedSection> {
        val mappedAnimes = items.take(40)

        val recentAnime = mappedAnimes.take(10)
        val ongoingAnime = mappedAnimes.filter { anime ->
            anime.status.contains("連載") || anime.status.contains("更新") || anime.status.contains("ONGOING", ignoreCase = true)
        }.ifEmpty { mappedAnimes.drop(10).take(10) }

        val popularAnime = mappedAnimes.sortedByDescending { it.rating }.take(10)

        return listOf(
            AnimeFeedSection("section_recent", "今日更新", recentAnime),
            AnimeFeedSection("section_ongoing", "熱門連載", ongoingAnime),
            AnimeFeedSection("section_popular", "精選推薦", popularAnime)
        )
    }

    /**
     * Fast mapping path for the initial sync: uses only 0ms local covers
     * (memory / SharedPreferences disk / prepopulated dictionary) so the
     * real anime1.me list is emitted to the UI instantly. Remaining covers
     * are resolved in the background by CoverImageFetcher.prefetchCovers
     * and merged through the coverUpdates listener.
     */
    private fun mapAnime1ItemToAnimeFast(item: Anime1Item): Anime {
        val title = ChineseConverter.toTraditionalChinese(item.title)
        val coverUrl = CoverImageFetcher.getCoverUrlFast(null, title) ?: ""
        return buildAnimeFromItem(item, title, coverUrl)
    }

    private suspend fun mapAnime1ItemToAnime(item: Anime1Item): Anime {
        val animeId = item.id.toString()
        val existing = animeMapCache[animeId]

        val title = ChineseConverter.toTraditionalChinese(item.title)
        val coverUrl = existing?.coverUrl?.takeIf { it.isNotBlank() && !it.contains("unsplash") }
            ?: coverImageCache.getOrPut(title) {
                val fast = CoverImageFetcher.getCoverUrlFast(null, title)
                if (!fast.isNullOrBlank()) fast else (anime1Service.fetchCoverImage(title) ?: "")
            }

        return buildAnimeFromItem(item, title, coverUrl)
    }

    /** Shared Anime construction so fast and full paths stay identical. */
    private fun buildAnimeFromItem(item: Anime1Item, title: String, coverUrl: String): Anime {
        val animeId = item.id.toString()
        val existing = animeMapCache[animeId]

        val genres = mutableListOf<String>()
        if (item.category.isNotBlank()) genres.add(ChineseConverter.toTraditionalChinese(item.category))
        if (item.season.isNotBlank()) genres.add(ChineseConverter.toTraditionalChinese(item.season))
        if (genres.isEmpty()) genres.add("動畫")

        val rawStatus = if (item.episodeCountOrStatus.isBlank()) "已完結" else item.episodeCountOrStatus
        val statusText = ChineseConverter.toTraditionalChinese(rawStatus)
        val parsedYear = extractYearFromSeason(item.season) ?: 2024
        val episodeCount = extractEpisodeCount(item.episodeCountOrStatus) ?: 12

        val descriptionText = buildString {
            append("Anime1.me 動畫專區。")
            if (item.season.isNotBlank()) append(" 播放季度：${ChineseConverter.toTraditionalChinese(item.season)}。")
            if (item.category.isNotBlank()) append(" 分類：${ChineseConverter.toTraditionalChinese(item.category)}。")
            if (item.episodeCountOrStatus.isNotBlank()) append(" 集數狀態：${ChineseConverter.toTraditionalChinese(item.episodeCountOrStatus)}。")
        }

        val rating = 8.0 + (abs(animeId.hashCode()) % 18) / 10.0

        val anime = Anime(
            id = animeId,
            title = title,
            coverUrl = coverUrl,
            bannerUrl = coverUrl,
            description = descriptionText,
            rating = (rating * 10).toInt() / 10.0,
            genres = genres,
            year = parsedYear,
            status = statusText,
            episodeCount = episodeCount,
            follow = existing?.follow ?: false,
            progress = existing?.progress ?: 0
        )

        animeMapCache[animeId] = anime
        return anime
    }

    private suspend fun getAnimeDetailsInternal(animeId: String): Anime? {
        val items = getOrFetchAnimeList()
        val item = items.find { it.id.toString() == animeId } ?: return null
        return mapAnime1ItemToAnime(item)
    }

    private fun extractTokenFromEpisodeId(episodeId: String): String? {
        // Episode ids look like "<animeId>_ep_<num>_<urlEncodedJsonToken>".
        // The token is a long URL-encoded JSON blob; a bare number (e.g. the
        // trailing "5" of a synthetic "<id>_ep_5" id) is NOT a token and must
        // not be posted to the API (it can never resolve to a stream).
        val marker = "_ep_"
        val idx = episodeId.lastIndexOf(marker)
        if (idx < 0) return null
        val after = episodeId.substring(idx + marker.length)
        // after = "<num>" or "<num>_<token>"
        val tokenCandidate = after.substringAfter("_", missingDelimiterValue = "")
        if (tokenCandidate.isBlank()) return null
        // Real tokens are long encoded JSON (contain % or { or length >> 10).
        if (tokenCandidate.length < 12) return null
        if (!tokenCandidate.contains("%") && !tokenCandidate.contains("{") &&
            !tokenCandidate.contains("c") && !tokenCandidate.contains("s")
        ) return null
        return tokenCandidate
    }

    private fun extractYearFromSeason(season: String): Int? {
        val match = Regex("""(\d{4})""").find(season)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractEpisodeCount(status: String): Int? {
        val match = Regex("""(\d+)""").find(status)
        return match?.groupValues?.get(1)?.toIntOrNull()
    }
}
