package com.one.animetvone.ui.components

import com.one.animetvone.data.model.Anime

/**
 * Human-readable episode label. anime1.me reports ongoing titles as
 * "連載中(05)" (5 episodes released so far) and finished ones as "12話".
 * Showing "全 5 集" for an ongoing show is misleading, so the label differs.
 */
fun episodeCountLabel(anime: Anime): String {
    val ongoing = anime.status.contains("連載") ||
        anime.status.contains("更新") ||
        anime.status.contains("ONGOING", ignoreCase = true)
    val count = anime.episodeCount
    return when {
        count > 0 && ongoing -> "更新至 $count 集"
        count > 0 -> "全 $count 集"
        ongoing -> "連載中"
        else -> "全集"
    }
}
