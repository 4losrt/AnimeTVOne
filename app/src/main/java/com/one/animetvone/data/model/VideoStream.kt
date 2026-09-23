package com.one.animetvone.data.model

data class VideoStream(
    val url: String,
    val quality: String = "1080p",
    val serverName: String = "Anime1 CDN Direct",
    val headers: Map<String, String>? = null
)
