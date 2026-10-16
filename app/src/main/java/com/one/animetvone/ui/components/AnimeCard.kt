package com.one.animetvone.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.source.CoverImageFetcher
import com.one.animetvone.ui.theme.TvAccent
import com.one.animetvone.ui.theme.TvAccentBright
import com.one.animetvone.ui.theme.TvCardBackground
import com.one.animetvone.ui.theme.TvCardBorder
import com.one.animetvone.ui.theme.TvOnSurfaceMuted
import com.one.animetvone.ui.theme.TvPureWhite
import com.one.animetvone.util.ChineseConverter

val DarkPosterBg = Color(0xFF0C1220)

/** Season tag such as "2026秋" derived from the real anime1 year + season. */
fun extractSeasonBadge(anime: Anime): String {
    val seasonStr = anime.genres.find { it.contains("年") || it.contains("季") || it.contains("期") }
        ?: anime.description

    val year = anime.year.takeIf { it in 1990..2100 }

    val quarter = when {
        seasonStr.contains("春") || seasonStr.contains("4月") || seasonStr.contains("四月") -> "春"
        seasonStr.contains("夏") || seasonStr.contains("7月") || seasonStr.contains("七月") -> "夏"
        seasonStr.contains("秋") || seasonStr.contains("10月") || seasonStr.contains("十月") -> "秋"
        seasonStr.contains("冬") || seasonStr.contains("1月") || seasonStr.contains("一月") -> "冬"
        else -> null
    }

    // Never fabricate a year/season that isn't in the data.
    return when {
        year != null && quarter != null -> "$year$quarter"
        quarter != null -> quarter
        year != null -> "$year"
        else -> "動畫"
    }
}

fun extractEpisodePill(anime: Anime): String = episodeCountLabel(anime)

/**
 * Text-only poster used when no cover art could be resolved. Styled like the
 * streaming apps: near-black tile, centered title, season tag in the corner.
 */
@Composable
fun DarkTitlePosterCard(
    anime: Anime,
    isFocused: Boolean,
    modifier: Modifier = Modifier
) {
    val traditionalTitle = remember(anime.title) { ChineseConverter.toTraditionalChinese(anime.title) }
    val cleanTitle = remember(traditionalTitle) { CoverImageFetcher.cleanAnimeTitle(traditionalTitle) }
    val seasonBadge = remember(anime) { extractSeasonBadge(anime) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF101830),
                        DarkPosterBg
                    )
                )
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .background(TvAccent.copy(alpha = 0.22f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = seasonBadge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = TvAccentBright
                )
            }

            Text(
                text = cleanTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center
                ),
                color = TvPureWhite,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            )

            Text(
                text = extractEpisodePill(anime),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                ),
                color = TvOnSurfaceMuted
            )
        }
    }
}

@Composable
fun AnimeCard(
    anime: Anime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 160.dp,
    onFocus: (() -> Unit)? = null,
) {
    var isFocused by remember { mutableStateOf(false) }

    val cardContext = LocalContext.current
    val displayTitle = remember(anime.title) { ChineseConverter.toTraditionalChinese(anime.title) }

    val hasUsableCover = remember(anime.coverUrl) {
        anime.coverUrl.isNotBlank() &&
            !anime.coverUrl.contains("placeholder") &&
            !anime.coverUrl.contains("unsplash.com")
    }

    // Never touch disk/prefs synchronously during composition; unknown titles are
    // resolved off the main thread below (cache first, then a polite fetch).
    var resolvedCoverUrl by remember(anime.id, anime.coverUrl) {
        mutableStateOf(if (hasUsableCover) anime.coverUrl else null)
    }

    LaunchedEffect(anime.id) {
        if (resolvedCoverUrl.isNullOrBlank()) {
            val fetched = CoverImageFetcher.getCoverUrl(cardContext, anime.title)
            if (!fetched.isNullOrBlank()) {
                resolvedCoverUrl = fetched
            }
        }
    }

    var imageLoadFailed by remember(anime.id, resolvedCoverUrl) { mutableStateOf(false) }
    val showImage = !resolvedCoverUrl.isNullOrBlank() && !imageLoadFailed

    Card(
        onClick = onClick,
        modifier = modifier
            .width(cardWidth)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused && onFocus != null) onFocus()
            },
        shape = CardDefaults.shape(shape = RoundedCornerShape(14.dp)),
        scale = CardDefaults.scale(focusedScale = 1.08f),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, TvCardBorder),
                shape = RoundedCornerShape(14.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, TvPureWhite),
                shape = RoundedCornerShape(14.dp)
            )
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow(
                elevationColor = TvAccent.copy(alpha = 0.55f),
                elevation = 18.dp
            )
        ),
        colors = CardDefaults.colors(
            containerColor = TvCardBackground,
            focusedContainerColor = TvCardBackground
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.66f)
                .clip(RoundedCornerShape(14.dp))
        ) {
            if (!showImage) {
                DarkTitlePosterCard(
                    anime = anime,
                    isFocused = isFocused,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                val coverUrl = resolvedCoverUrl.orEmpty()
                val requestBuilder = ImageRequest.Builder(cardContext)
                    .data(coverUrl)
                    .crossfade(true)
                if (!coverUrl.contains("lain.bgm.tv")) {
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
                        CoverImageFetcher.refreshCoverUrl(cardContext, anime.title)
                    },
                    onSuccess = { imageLoadFailed = false },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Bottom scrim + title (kept clean, streaming-app style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.92f)
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
                        color = TvPureWhite,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = extractEpisodePill(anime),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = if (isFocused) TvAccentBright else TvOnSurfaceMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
