package com.one.animetvone.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.one.animetvone.R
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.source.CoverImageFetcher
import com.one.animetvone.ui.theme.TvAccentCyan
import com.one.animetvone.ui.theme.TvCardBorder
import com.one.animetvone.ui.theme.TvPureWhite
import com.one.animetvone.util.ChineseConverter

val DarkPosterBg = Color(0xFF101828)

fun extractSeasonBadge(anime: Anime): String {
    val seasonStr = anime.genres.find { it.contains("季") || it.contains("期") || it.contains("年") }
        ?: anime.description

    val year = anime.year.takeIf { it in 2000..2030 } ?: 2024

    val quarter = when {
        seasonStr.contains("春") || seasonStr.contains("4月") || seasonStr.contains("四月") -> "春"
        seasonStr.contains("夏") || seasonStr.contains("7月") || seasonStr.contains("七月") -> "夏"
        seasonStr.contains("秋") || seasonStr.contains("10月") || seasonStr.contains("十月") -> "秋"
        seasonStr.contains("冬") || seasonStr.contains("1月") || seasonStr.contains("一月") -> "冬"
        else -> "春"
    }

    return "$year$quarter"
}

fun extractEpisodePill(anime: Anime): String {
    val count = if (anime.episodeCount > 0) anime.episodeCount else {
        val match = Regex("""(\d+)""").find(anime.status)
        match?.groupValues?.get(1)?.toIntOrNull() ?: 12
    }
    return "全 $count 集"
}

@Composable
fun DarkTitlePosterCard(
    anime: Anime,
    isFocused: Boolean,
    modifier: Modifier = Modifier
) {
    val traditionalTitle = remember(anime.title) { ChineseConverter.toTraditionalChinese(anime.title) }
    val cleanTitle = remember(traditionalTitle) { CoverImageFetcher.cleanAnimeTitle(traditionalTitle) }
    val seasonBadge = remember(anime) { extractSeasonBadge(anime) }
    val episodePill = remember(anime) { extractEpisodePill(anime) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        DarkPosterBg,
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            )
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF0284C7).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = seasonBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = TvAccentCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = cleanTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = if (isFocused) TvAccentCyan else TvPureWhite,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF0369A1).copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            BorderStroke(0.5.dp, TvAccentCyan.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = episodePill,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        ),
                        color = TvAccentCyan
                    )
                }
            }
        }
    }
}

@Composable
fun PureTextAnimeCard(
    anime: Anime,
    isFocused: Boolean,
    modifier: Modifier = Modifier
) {
    val traditionalTitle = remember(anime.title) { ChineseConverter.toTraditionalChinese(anime.title) }
    val cleanTitle = remember(traditionalTitle) { CoverImageFetcher.cleanAnimeTitle(traditionalTitle) }
    val seasonBadge = remember(anime) { extractSeasonBadge(anime) }
    val episodePill = remember(anime) { extractEpisodePill(anime) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        DarkPosterBg,
                        Color(0xFF0F172A),
                        Color(0xFF1E293B)
                    )
                )
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF0284C7).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = seasonBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = TvAccentCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = cleanTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    ),
                    color = if (isFocused) TvAccentCyan else TvPureWhite,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF0369A1).copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            BorderStroke(0.5.dp, TvAccentCyan.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = episodePill,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp
                        ),
                        color = TvAccentCyan
                    )
                }
            }
        }
    }
}

@Composable
fun AnimeCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 160.dp,
    isTextOnly: Boolean = false,
    onFocus: (() -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    val displayTitle = remember(anime.title) { ChineseConverter.toTraditionalChinese(anime.title) }

    val resolvedCoverUrl = remember(anime.id, anime.coverUrl, isTextOnly) {
        if (!isTextOnly && anime.coverUrl.isNotBlank() && !anime.coverUrl.contains("placeholder") && !anime.coverUrl.contains("unsplash.com")) {
            anime.coverUrl
        } else if (!isTextOnly) {
            CoverImageFetcher.getCoverUrlFast(null, anime.title)
        } else {
            null
        }
    }

    // On-demand only: when a visible card has no image, queue exactly one
    // polite background fetch. The result flows back via coverUpdates ->
    // repository list update -> recompose. Cancelled on dispose.
    LaunchedEffect(anime.id, isTextOnly) {
        if (!isTextOnly && resolvedCoverUrl.isNullOrBlank()) {
            CoverImageFetcher.getCoverUrl(null, anime.title)
        }
    }

    // If Coil fails to decode/load the URL (dead link, 403, bad bytes),
    // fall back to the text poster so cards never show a broken/blank image.
    // Reset whenever the card or URL changes.
    var imageLoadFailed by remember(anime.id, resolvedCoverUrl) { mutableStateOf(false) }
    val showImage = !isTextOnly && !resolvedCoverUrl.isNullOrBlank() && !imageLoadFailed

    Card(
        onClick = onClick,
        modifier = modifier
            .width(cardWidth)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused && onFocus != null) {
                    onFocus()
                }
            },
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)),
        scale = CardDefaults.scale(
            focusedScale = 1.05f
        ),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, TvCardBorder),
                shape = RoundedCornerShape(12.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvAccentCyan),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(
                elevationColor = TvAccentCyan.copy(alpha = 0.6f),
                elevation = 16.dp
            )
        ),
        colors = CardDefaults.colors(
            containerColor = DarkPosterBg,
            focusedContainerColor = DarkPosterBg
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.70f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (!showImage) {
                if (isTextOnly) {
                    PureTextAnimeCard(
                        anime = anime,
                        isFocused = isFocused,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    DarkTitlePosterCard(
                        anime = anime,
                        isFocused = isFocused,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                val cardContext = LocalContext.current
                val requestBuilder = ImageRequest.Builder(cardContext)
                    .data(resolvedCoverUrl)
                    .crossfade(true)
                // Only add referer for anime1.me domains; lain.bgm.tv may block it.
                if (!resolvedCoverUrl.contains("lain.bgm.tv")) {
                    requestBuilder.addHeader("Referer", "https://anime1.me/")
                }
                requestBuilder.addHeader(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
                )
                AsyncImage(
                    model = requestBuilder.build(),
                    contentDescription = displayTitle,
                    contentScale = ContentScale.Crop,
                    onError = {
                        imageLoadFailed = true
                        // A stale/403 poster must not remain permanently cached.
                        // Resolve a replacement from the other cover providers.
                        CoverImageFetcher.refreshCoverUrl(cardContext, anime.title)
                    },
                    onSuccess = { imageLoadFailed = false },
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    val epText = if (anime.episodeCount > 0) "${anime.episodeCount} 集" else ChineseConverter.toTraditionalChinese(anime.status)
                    Text(
                        text = epText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = TvAccentCyan
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.65f),
                                    Color.Black.copy(alpha = 0.95f)
                                )
                            )
                        )
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                lineHeight = 17.sp
                            ),
                            color = if (isFocused) TvAccentCyan else TvPureWhite,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
