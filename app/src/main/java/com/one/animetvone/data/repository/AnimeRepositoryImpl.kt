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
                //    weekly table (the homepage table-list does not). The site does
                //    not always have this season's page yet, so walk back a few
                //    seasons until one exists.
                var scheduleMap: Map<String, List<Anime1Item>>? = null
                for ((seasonYear, seasonName) in recentSeasons()) {
                    try {
                        val seasonHtml = anime1Service.fetchSeasonScheduleHtml(seasonYear, seasonName)
                        if (seasonHtml.isNotBlank()) {
                            val parsed = Anime1Parser.parseScheduleFromHtml(seasonHtml, animeItems)
                            if (parsed.values.any { it.isNotEmpty() }) {
                                scheduleMap = parsed
                                break
                            }
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        e.printStackTrace()
                    }
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

        // No real schedule data available. Emit the empty weekly frame (honest
        // state) instead of fabricating a schedule by slicing the anime list:
        // a made-up schedule is worse than "no schedule yet".
        val emptyDays = Anime1Parser.DAYS_OF_WEEK.map { dayName ->
            ScheduleDay(dayName = dayName, animes = emptyList())
        }
        emit(emptyDays)
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

                    // The status string is only a hint: its range can be wider
                    // than what is actually published (or narrower). Now that
                    // the real list is in hand, publish the accurate total.
                    publishEpisodeCount(animeId, episodes.size, parentAnime)
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
    private val seasonNames = listOf("冬季", "春季", "夏季", "秋季")

    /** Current season as (year, index 0..3) with day-aware boundaries. */
    private fun currentSeason(): Pair<Int, Int> {
        val now = java.util.Calendar.getInstance()
        val year = now.get(java.util.Calendar.YEAR)
        val month = now.get(java.util.Calendar.MONTH) + 1
        val day = now.get(java.util.Calendar.DAY_OF_MONTH)
        val index = when {
            (month == 1 && day >= 2) || month == 2 || month == 3 || (month == 4 && day < 2) -> 0
            (month == 4 && day >= 2) || month == 5 || month == 6 || (month == 7 && day < 2) -> 1
            (month == 7 && day >= 2) || month == 8 || month == 9 || (month == 10 && day < 2) -> 2
            else -> 3
        }
        return year to index
    }

    /**
     * Season pages as (year, name), newest first. anime1.me does not always have
     * the current season's page yet (it 404s), so callers walk back until they
     * find a season page that actually exists.
     */
    private fun recentSeasons(count: Int = 3): List<Pair<Int, String>> {
        var (year, index) = currentSeason()
        val result = mutableListOf<Pair<Int, String>>()
        repeat(count) {
            result.add(year to seasonNames[index])
            index--
            if (index < 0) {
                index = seasonNames.lastIndex
                year--
            }
        }
        return result
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
        if (items.isEmpty()) return emptyList()

        val recentAnime = items.take(12)

        val ongoingAnime = items.filter { anime ->
            anime.status.contains("連載") || anime.status.contains("更新") ||
                anime.status.contains("ONGOING", ignoreCase = true)
        }.take(12)

        // 精選推薦: a deterministic spread across the whole catalogue so it does
        // not just repeat the first page. There is no real rating source, so we
        // must not fabricate or sort by one.
        val step = maxOf(1, items.size / 12)
        val featuredAnime = items.filterIndexed { index, _ -> index % step == 0 }.take(12)

        return buildList {
            add(AnimeFeedSection("section_recent", "今日更新", recentAnime))
            if (ongoingAnime.isNotEmpty()) {
                add(AnimeFeedSection("section_ongoing", "熱門連載", ongoingAnime))
            }
            add(AnimeFeedSection("section_popular", "精選推薦", featuredAnime))
        }
    }

    /**
     * Fast mapping path for the initial sync: uses only 0ms local covers
     * (memory / SharedPreferences disk / prepopulated dictionary) so the
     * real anime1.me list is emitted to the UI instantly. Remaining covers
     * are resolved on demand by AnimeCard and merged back through the
     * coverUpdates listener.
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

        val yearValue = extractYear(item.year)
        val seasonChar = item.season.trim().takeIf { it in SEASON_CHARS }

        // Store the season as a single genre tag like "2026年秋" so the season
        // filter chips and the poster season badge work off real data. (The
        // anime1.me year and season live in separate fields.)
        val seasonTag = when {
            yearValue != null && seasonChar != null -> "${yearValue}年$seasonChar"
            yearValue != null -> "${yearValue}年"
            else -> null
        }

        val genres = mutableListOf<String>()
        if (seasonTag != null) genres.add(seasonTag)
        if (genres.isEmpty()) genres.add("動畫")

        val rawStatus = if (item.episodeCountOrStatus.isBlank()) "已完結" else item.episodeCountOrStatus
        val statusText = ChineseConverter.toTraditionalChinese(rawStatus)
        val parsedYear = yearValue ?: 0
        val episodeCount = extractEpisodeCount(item.episodeCountOrStatus) ?: 0

        val descriptionText = buildString {
            append("Anime1.me 動畫專區。")
            if (seasonTag != null) append(" 播放季度：$seasonTag。")
            if (item.subtitleGroup.isNotBlank()) {
                append(" 字幕組：${ChineseConverter.toTraditionalChinese(item.subtitleGroup)}。")
            }
            if (item.episodeCountOrStatus.isNotBlank()) {
                append(" 集數狀態：${ChineseConverter.toTraditionalChinese(item.episodeCountOrStatus)}。")
            }
        }

        // anime1.me exposes no rating data. We intentionally do NOT fabricate one
        // (the old code hashed the id into a fake 8.0-9.7 score that looked like a
        // real user rating). 0.0 means "unknown" and the UI hides the star badge.
        val rating = 0.0

        val anime = Anime(
            id = animeId,
            title = title,
            coverUrl = coverUrl,
            bannerUrl = coverUrl,
            description = descriptionText,
            rating = rating,
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

    private fun extractYear(rawYear: String): Int? {
        return YEAR_PATTERN.find(rawYear)?.value?.toIntOrNull()
    }

    /**
     * Episode total from the status string animelist.json carries. The parsing
     * lives in [Anime1Parser.episodeCountFromStatus] because the disk cache
     * repairs counts stored by older builds with the same rule.
     */
    private fun extractEpisodeCount(status: String): Int? {
        val count = Anime1Parser.episodeCountFromStatus(status)
        return if (count > 0) count else null
    }

    /**
     * Raises the cached episode total for [animeId] to [count] when the scraped
     * list is longer than the status string suggested, so list cards and the
     * detail header stop under-reporting the episode count.
     */
    private fun publishEpisodeCount(animeId: String, count: Int, fallback: Anime?) {
        if (count <= 0) return
        val current = animeMapCache[animeId] ?: fallback ?: return
        if (current.episodeCount >= count) return

        val updated = current.copy(episodeCount = count)
        animeMapCache[animeId] = updated

        val list = _animeListState.value
        if (list.any { it.id == animeId }) {
            val updatedList = list.map { if (it.id == animeId) updated else it }
            _animeListState.value = updatedList
            AnimeListDiskCache.saveToDisk(updatedList)
        }
    }

    private companion object {
        val SEASON_CHARS = setOf("春", "夏", "秋", "冬")
        val YEAR_PATTERN = Regex("""\d{4}""")
    }
}
