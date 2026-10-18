package com.one.animetvone.data.model

data class HistoryItem(
    val animeId: String,
    val animeTitle: String,
    val coverUrl: String,
    val episodeId: String,
    val episodeTitle: String,
    val playbackPositionMs: Long,
    val durationMs: Long,
    val timestamp: Long,
    /**
     * 1-based episode number. Stored explicitly because an episode can be
     * referenced by different id shapes over time (`<anime>_ep_3` from the
     * fallback list vs `<anime>_ep_3_<token>` from the scraped list), and
     * matching on the id alone produced duplicate history entries for the very
     * same episode. 0 = unknown (older records).
     */
    val episodeNumber: Int = 0
)
