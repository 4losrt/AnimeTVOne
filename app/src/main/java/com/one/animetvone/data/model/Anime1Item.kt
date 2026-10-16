package com.one.animetvone.data.model

/**
 * One row from `anime1.me/animelist.json`. The wire format is a positional array:
 *
 *     [id, title, episodeCountOrStatus, year, season, subtitleGroup]
 *     e.g. [2003, "冰劍的魔術師...", "連載中(02)", "2026", "秋", "喵萌奶茶屋"]
 *
 * `year` is a 4-digit string (occasionally a range such as "2023/2024"),
 * `season` is a single character (春/夏/秋/冬, may be blank) and
 * `subtitleGroup` is the fansub group name (may be blank).
 */
data class Anime1Item(
    val id: Int,
    val title: String,
    val episodeCountOrStatus: String,
    val year: String,
    val season: String,
    val subtitleGroup: String
)

data class Anime1EpisodeToken(
    val episodeNumber: Int,
    val title: String,
    val token: String,
    val detailUrl: String? = null
)
