package com.one.animetvone.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.model.AnimeFeedSection
import com.one.animetvone.data.repository.FavoritesManager
import com.one.animetvone.data.repository.SampleAnimeData
import com.one.animetvone.ui.components.AnimeCard
import com.one.animetvone.ui.theme.AnimetvoneTheme
import com.one.animetvone.ui.theme.TvAccentCyan
import com.one.animetvone.ui.theme.TvBackgroundBottom
import com.one.animetvone.ui.theme.TvBackgroundTop
import com.one.animetvone.ui.theme.TvCardBackground
import com.one.animetvone.ui.theme.TvCardBorder
import com.one.animetvone.ui.theme.TvCardFocusHighlight
import com.one.animetvone.ui.theme.TvPureWhite
import com.one.animetvone.util.ChineseConverter
import java.util.Locale

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
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = TvAccentCyan)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "載入首頁中...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TvPureWhite.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.errorMessage ?: "發生錯誤",
                            style = MaterialTheme.typography.titleMedium,
                            color = TvPureWhite
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val homeRetryRequester = remember { FocusRequester() }
                        LaunchedEffect(Unit) {
                            try { homeRetryRequester.requestFocus() } catch (_: Exception) { }
                        }
                        Button(
                            onClick = { viewModel.loadHomeFeed() },
                            modifier = Modifier.focusRequester(homeRetryRequester)
                        ) {
                            Text(text = "重試")
                        }
                    }
                }
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
    // Hero 固定顯示 featuredAnime，不隨海報焦點更換內容：
    // 之前 onFocus -> currentFeatured = anime 會重組 Hero（440dp 高橫幅），
    // Compose 會把 LazyColumn 捲回頂部 =「每動一下就拉上去」。
    val heroAnime = featuredAnime

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {
        // Hero Billboard Banner
        item {
            HeroBillboardBanner(
                anime = heroAnime,
                onWatchClick = { heroAnime?.let(onWatchClick) },
                onToggleWatchlist = { anime: Anime ->
                    FavoritesManager.toggleFavorite(context, anime.id)
                }
            )
        }

        // Feed Sections (今日更新, 熱門連載, 精選推薦)
        items(
            items = sections,
            key = { it.id }
        ) { section ->
            val sectionTitle = remember(section.title) { ChineseConverter.toTraditionalChinese(section.title) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.width(5.dp).height(24.dp)
                            .background(TvAccentCyan, RoundedCornerShape(3.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = sectionTitle,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                        color = TvPureWhite
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${section.animes.size} 部作品",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = TvPureWhite.copy(alpha = .55f)
                    )
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
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
            .height(360.dp)
    ) {
        if (anime != null) {
            // Background Backdrop Image with Crossfade
            Crossfade(
                targetState = anime.bannerUrl.ifEmpty { anime.coverUrl },
                animationSpec = tween(durationMillis = 400),
                label = "HeroBillboardCrossfade"
            ) { bannerUrl ->
                val cardContext = LocalContext.current
                val requestBuilder = ImageRequest.Builder(cardContext)
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

            // Dark Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                TvBackgroundTop.copy(alpha = 0.98f),
                                TvBackgroundTop.copy(alpha = 0.85f),
                                TvBackgroundTop.copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            startX = 0f,
                            endX = 1300f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                TvBackgroundTop.copy(alpha = 0.6f),
                                TvBackgroundTop
                            )
                        )
                    )
            )

            // Hero Content Details
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.76f)
                    .padding(start = 48.dp, bottom = 22.dp)
            ) {
                // Category Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = TvAccentCyan.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "精選推薦 FEATURED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        ),
                        color = TvAccentCyan
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 30.sp,
                        lineHeight = 36.sp
                    ),
                    color = TvPureWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata Badges (高清 1080P, 2024, 全 12 集)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rating Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = "Rating",
                            tint = TvAccentCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format(Locale.US, "%.1f", anime.rating),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TvPureWhite
                        )
                    }

                    // 高清 1080P Badge
                    Box(
                        modifier = Modifier
                            .background(TvCardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "高清 1080P",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TvAccentCyan
                        )
                    }

                    // Year Badge
                    Box(
                        modifier = Modifier
                            .background(TvCardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${anime.year}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TvPureWhite
                        )
                    }

                    // Episode Count Badge
                    val epCountText = if (anime.episodeCount > 0) "全 ${anime.episodeCount} 集" else ChineseConverter.toTraditionalChinese(anime.status)
                    Box(
                        modifier = Modifier
                            .background(TvCardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = epCountText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TvPureWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Synopsis Text
                Text(
                    text = displayDesc,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = TvPureWhite.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: ▶ 立即播放 & ＋ 追番
                // 遙控器：一進首頁焦點就停在「立即播放」，保證 D-pad 按得到。
                // LaunchedEffect 綁定 Unit：只一次聚焦，不因資料更新重複觸發
                val heroPlayRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    try { heroPlayRequester.requestFocus() } catch (_: Exception) { }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onWatchClick,
                        modifier = Modifier.focusRequester(heroPlayRequester),
                        colors = ButtonDefaults.colors(
                            containerColor = TvAccentCyan,
                            contentColor = TvBackgroundTop,
                            focusedContainerColor = TvPureWhite,
                            focusedContentColor = TvBackgroundTop
                        ),
                        scale = ButtonDefaults.scale(focusedScale = 1.08f),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "立即播放",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onToggleWatchlist(anime)
                            isFavorite = !isFavorite
                        },
                        colors = OutlinedButtonDefaults.colors(
                            contentColor = TvPureWhite,
                            focusedContainerColor = TvCardFocusHighlight,
                            focusedContentColor = TvPureWhite
                        ),
                        border = OutlinedButtonDefaults.border(
                            border = Border(
                                border = BorderStroke(1.5.dp, TvPureWhite.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(10.dp)
                            ),
                            focusedBorder = Border(
                                border = BorderStroke(2.dp, TvAccentCyan),
                                shape = RoundedCornerShape(10.dp)
                            )
                        ),
                        scale = OutlinedButtonDefaults.scale(focusedScale = 1.08f),
                        shape = OutlinedButtonDefaults.shape(shape = RoundedCornerShape(10.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Check else Icons.Rounded.Add,
                                contentDescription = "Watchlist",
                                modifier = Modifier.size(20.dp),
                                tint = if (isFavorite) TvAccentCyan else TvPureWhite
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFavorite) "已在追番" else "追番",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
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
