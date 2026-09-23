package com.one.animetvone.data.model

data class Anime(
    val id: String,
    val title: String,
    val coverUrl: String,
    val bannerUrl: String,
    val description: String,
    val rating: Double,
    val genres: List<String>,
    val year: Int,
    val status: String,
    val episodeCount: Int,
    val follow: Boolean = false,
    val progress: Int = 0
)
