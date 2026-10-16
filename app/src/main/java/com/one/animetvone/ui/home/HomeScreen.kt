package com.one.animetvone.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.model.AnimeFeedSection
import com.one.animetvone.data.repository.FavoritesManager
import com.one.animetvone.data.repository.SampleAnimeData
import com.one.animetvone.ui.components.AnimeCard
import com.one.animetvone.ui.components.TvErrorView
import com.one.animetvone.ui.components.TvLoadingView
import com.one.animetvone.ui.components.TvSectionHeader
import com.one.animetvone.ui.components.episodeCountLabel
import com.one.animetvone.ui.theme.AnimetvoneTheme
import com.one.animetvone.ui.theme.TvAccentBright
import com.one.animetvone.ui.theme.TvBackgroundBottom
import com.one.animetvone.ui.theme.TvBackgroundTop
import com.one.animetvone.ui.theme.TvOnSurfaceMuted
import com.one.animetvone.ui.theme.TvPureWhite
import com.one.animetvone.util.ChineseConverter

private val H_PADDING = 56.dp

@Composable
fun HomeScreen(
    onAnimeSelect: (Anime) -> Unit,
    onWatchClick: (Anime) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(TvBackgroundTop, TvBackgroundBottom)
                )
            )
    ) {
        when {
            uiState.isLoading -> TvLoadingView(message = "載入首頁中…")

            uiState.errorMessage != null -> {
                TvErrorView(
                    message = uiState.errorMessage ?: "發生錯誤",
                    onRetry = { viewModel.loadHomeFeed() }
                )
            }

            else -> {
                HomeContent(
                    sections = uiState.sections,
                    featuredAnime = uiState.featuredAnime,
                    onAnimeSelect = onAnimeSelect,
                    onWatchClick = onWatchClick
                )
            }
        }
    }
}

@Composable
fun HomeContent(
    sections: List<AnimeFeedSection>,
    featuredAnime: Anime?,
    onAnimeSelect: (Anime) -> Unit,
    onWatchClick: (Anime) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    // The hero stays pinned to featuredAnime so focusing a poster never rebuilds
    // the tall billboard (which would scroll the feed back to the top).
    val heroAnime = featuredAnime

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 56.dp)
    ) {
        item {
            HeroBillboardBanner(
                anime = heroAnime,
                onWatchClick = { heroAnime?.let(onWatchClick) },
                onToggleWatchlist = { anime: Anime ->
                    FavoritesManager.toggleFavorite(context, anime.id)
                }
            )
        }

        items(
            items = sections,
            key = { it.id }
        ) { section ->
            val sectionTitle = remember(section.title) { ChineseConverter.toTraditionalChinese(section.title) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                TvSectionHeader(
                    title = sectionTitle,
                    trailing = "${section.animes.size} 部作品",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = H_PADDING, end = H_PADDING, top = 8.dp, bottom = 14.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = H_PADDING),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        items = section.animes,
                        key = { "${section.id}_${it.id}" }
                    ) { anime ->
                        AnimeCard(
                            anime = anime,
                            onClick = { onAnimeSelect(anime) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium, fontSize = 15.sp),
        color = TvPureWhite.copy(alpha = 0.85f)
    )
}

@Composable
private fun MetaDot() {
    Text(
        text = "·",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
        color = TvOnSurfaceMuted,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
fun HeroBillboardBanner(
    anime: Anime?,
    onWatchClick: () -> Unit,
    onToggleWatchlist: (Anime) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isFavorite by remember(anime) {
        mutableStateOf(anime?.let { FavoritesManager.isFavorite(context, it.id) } ?: false)
    }

    val displayTitle = remember(anime?.title) { ChineseConverter.toTraditionalChinese(anime?.title) }
    val displayDesc = remember(anime?.description) { ChineseConverter.toTraditionalChinese(anime?.description) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        if (anime != null) {
            Crossfade(
                targetState = anime.bannerUrl.ifEmpty { anime.coverUrl },
                animationSpec = tween(durationMillis = 400),
                label = "HeroBillboardCrossfade"
            ) { bannerUrl ->
                val requestBuilder = ImageRequest.Builder(context)
                    .data(bannerUrl)
                    .crossfade(true)
                if (!bannerUrl.contains("lain.bgm.tv")) {
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
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Left-to-right scrim for text legibility.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                TvBackgroundTop.copy(alpha = 0.96f),
                                TvBackgroundTop.copy(alpha = 0.78f),
                                TvBackgroundTop.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = 1200f
                        )
                    )
            )

            // Bottom scrim blending into the feed.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                TvBackgroundTop.copy(alpha = 0.55f),
                                TvBackgroundTop
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.58f)
                    .padding(start = H_PADDING, bottom = 34.dp)
            ) {
                Text(
                    text = "精選推薦",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = TvAccentBright
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 38.sp,
                        lineHeight = 44.sp
                    ),
                    color = TvPureWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (anime.year > 0) {
                        MetaText("${anime.year}")
                        MetaDot()
                    }
                    MetaText(episodeCountLabel(anime))
                    MetaDot()
                    MetaText("高清 1080P")
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = displayDesc,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    color = TvPureWhite.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onWatchClick,
                        colors = ButtonDefaults.colors(
                            containerColor = TvPureWhite,
                            contentColor = TvBackgroundTop,
                            focusedContainerColor = TvPureWhite,
                            focusedContentColor = TvBackgroundTop
                        ),
                        scale = ButtonDefaults.scale(focusedScale = 1.08f),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "播放",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            onToggleWatchlist(anime)
                            isFavorite = !isFavorite
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = TvPureWhite.copy(alpha = 0.16f),
                            contentColor = TvPureWhite,
                            focusedContainerColor = TvPureWhite.copy(alpha = 0.30f),
                            focusedContentColor = TvPureWhite
                        ),
                        scale = ButtonDefaults.scale(focusedScale = 1.08f),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Check else Icons.Rounded.Add,
                                contentDescription = "Watchlist",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFavorite) "已追番" else "追番",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = "id:tv_1080p", showBackground = true)
@Composable
fun HomeScreenPreview() {
    AnimetvoneTheme {
        HomeContent(
            sections = SampleAnimeData.sampleFeedSections,
            featuredAnime = SampleAnimeData.sampleAnimes.firstOrNull(),
            onAnimeSelect = {},
            onWatchClick = {},
        )
    }
}
