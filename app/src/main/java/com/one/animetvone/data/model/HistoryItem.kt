package com.one.animetvone.data.model

data class HistoryItem(
    val animeId: String,
    val animeTitle: String,
    val coverUrl: String,
    val episodeId: String,
    val episodeTitle: String,
    val playbackPositionMs: Long,
    val durationMs: Long,
    val timestamp: Long
)
