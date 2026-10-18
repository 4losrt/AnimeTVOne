package com.one.animetvone.ui.components

import com.one.animetvone.data.model.Anime

/**
 * Human-readable episode label. anime1.me reports ongoing titles as
 * "連載中(05)" (5 episodes released so far) and finished ones as "1-12" (a
 * 12-episode season, not episode 1). Showing "全 5 集" for an ongoing show is
 * misleading, so the label differs.
 */
fun episodeCountLabel(anime: Anime): String {
    val ongoing = anime.status.contains("連載") ||
        anime.status.contains("更新") ||
        anime.status.contains("ONGOING", ignoreCase = true)
    val isMovie = anime.status.contains("劇場版") || anime.status.contains("电影") ||
        anime.status.contains("電影")
    val count = anime.episodeCount
    return when {
        // anime1.me marks films as 劇場版; they carry a single video entry, so
        // the status decides the label before the raw count does.
        isMovie -> "劇場版"
        count > 0 && ongoing -> "更新至 $count 集"
        count > 0 -> "全 $count 集"
        ongoing -> "連載中"
        else -> "集數未定"
    }
}
