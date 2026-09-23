package com.one.animetvone.data.model

data class Anime1Item(
    val id: Int,
    val title: String,
    val episodeCountOrStatus: String,
    val season: String,
    val subtitleGroup: String,
    val category: String
)

data class Anime1EpisodeToken(
    val episodeNumber: Int,
    val title: String,
    val token: String,
    val detailUrl: String? = null
)
