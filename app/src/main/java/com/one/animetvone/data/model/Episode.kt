package com.one.animetvone.data.model

data class Episode(
    val id: String,
    val animeId: String,
    val episodeNumber: Int,
    val title: String,
    val thumbnailUrl: String,
    val duration: String,
    val streamUrl: String
)
