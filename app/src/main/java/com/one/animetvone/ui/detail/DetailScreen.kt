package com.one.animetvone.ui.detail

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
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
import com.one.animetvone.R
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.model.Episode
import com.one.animetvone.data.model.HistoryItem
import com.one.animetvone.data.repository.SampleAnimeData
import com.one.animetvone.data.source.CoverImageFetcher
import com.one.animetvone.data.source.HistoryManager
import com.one.animetvone.ui.components.DarkTitlePosterCard
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
fun DetailScreen(
    animeId: String,
    onEpisodeSelect: (animeId: String, episodeId: String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(animeId) {
        viewModel.loadAnimeDetails(animeId, context)
    }

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
                            text = "載入動畫詳情...",
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.errorMessage ?: "載入失敗",
                            style = MaterialTheme.typography.titleMedium,
                            color = TvPureWhite
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val retryRequester = remember { FocusRequester() }
                        LaunchedEffect(Unit) {
                            try { retryRequester.requestFocus() } catch (_: Exception) { }
                        }
                        Button(
                            onClick = { viewModel.loadAnimeDetails(animeId, context) },
                            modifier = Modifier.focusRequester(retryRequester)
                        ) {
                            Text(text = "重試")
                        }
                    }
                }
            }

            uiState.anime != null -> {
                DetailContent(
                    anime = uiState.anime!!,
                    episodes = uiState.episodes,
                    isFavorite = uiState.isFavorite,
                    onToggleFavorite = { viewModel.toggleFavorite(context) },
                    onEpisodeSelect = { ep -> onEpisodeSelect(animeId, ep.id) },
                    onBackClick = onBackClick
                )
            }
        }
    }
}

@Composable
fun DetailContent(
    anime: Anime,
    episodes: List<Episode>,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onEpisodeSelect: (Episode) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val historyList = remember(anime.id) { HistoryManager.getHistoryList(context) }
    val historyMap = remember(historyList) { historyList.associateBy { it.episodeId } }

    val lastWatchedHistory = remember(historyList, anime.id) {
        historyList.filter { it.animeId == anime.id }
            .maxByOrNull { it.timestamp }
    }

    val (playButtonText, targetEpisode) = remember(lastWatchedHistory, episodes) {
        if (lastWatchedHistory != null) {
            val matchedEp = episodes.find { it.id == lastWatchedHistory.episodeId }
            if (matchedEp != null) {
                "繼續播放（第 ${matchedEp.episodeNumber} 集）" to matchedEp
            } else {
                "繼續播放" to (episodes.firstOrNull())
            }
        } else {
            "播放第 1 集" to (episodes.firstOrNull())
        }
    }

    var isReversed by remember { mutableStateOf(false) }
    val displayedEpisodes = remember(episodes, isReversed) {
        if (isReversed) episodes.reversed() else episodes
    }

    var isRefreshingCover by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshingCover) {
        if (isRefreshingCover) {
            delay(2500)
            isRefreshingCover = false
        }
    }

    val displayTitle = remember(anime.title) { ChineseConverter.toTraditionalChinese(anime.title) }

    Box(modifier = modifier.fillMaxSize()) {
        // Fullscreen Ambient Backdrop Artwork (skip when no art: gradients + dark bg suffice)
        val backdropUrl = anime.bannerUrl.ifEmpty { anime.coverUrl }
        if (backdropUrl.isNotBlank()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = displayTitle,
                placeholder = painterResource(id = R.drawable.placeholder),
                error = painterResource(id = R.drawable.placeholder),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(TvBackgroundTop)
            )
        }

        // Dark Gradients
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            TvBackgroundTop.copy(alpha = 0.98f),
                            TvBackgroundTop.copy(alpha = 0.90f),
                            TvBackgroundTop.copy(alpha = 0.60f),
                            TvBackgroundTop.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            TvBackgroundTop.copy(alpha = 0.70f),
                            TvBackgroundTop.copy(alpha = 0.85f),
                            TvBackgroundBottom
                        )
                    )
                )
        )

        // Back Button Top-Left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 36.dp, top = 24.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                colors = OutlinedButtonDefaults.colors(
                    contentColor = TvPureWhite,
                    focusedContainerColor = TvCardFocusHighlight,
                    focusedContentColor = TvPureWhite
                ),
                border = OutlinedButtonDefaults.border(
                    border = Border(
                        border = BorderStroke(1.dp, TvCardBorder),
                        shape = RoundedCornerShape(10.dp)
                    ),
                    focusedBorder = Border(
                        border = BorderStroke(2.dp, TvAccentCyan),
                        shape = RoundedCornerShape(10.dp)
                    )
                ),
                shape = OutlinedButtonDefaults.shape(shape = RoundedCornerShape(10.dp))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "返回",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        // Split View Layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 36.dp, end = 36.dp, top = 84.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            // LEFT PANEL: Poster, Metadata Badges (高清 1080P, 全集), Action Buttons
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.70f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(TvCardBackground)
                ) {
                    if (anime.coverUrl.isBlank()) {
                        DarkTitlePosterCard(
                            anime = anime,
                            isFocused = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = anime.coverUrl,
                            contentDescription = displayTitle,
                            placeholder = painterResource(id = R.drawable.placeholder),
                            error = painterResource(id = R.drawable.placeholder),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                        lineHeight = 28.sp
                    ),
                    color = TvPureWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata Badges (高清 1080P, 全集)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(TvCardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "高清 1080P",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TvAccentCyan
                        )
                    }

                    val epCountText = if (anime.episodeCount > 0) "全 ${anime.episodeCount} 集" else "全集"
                    Box(
                        modifier = Modifier
                            .background(TvCardBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = epCountText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TvPureWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                                // Action Buttons: ▶ 繼續播放/播放第 1 集 & ＋ 追番
                // 遙控器：一進詳情頁自動聚焦播放鍵，右鍵移到追番，右鍵再進劇集牆。
                val playRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    try { playRequester.requestFocus() } catch (_: Exception) { }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { targetEpisode?.let(onEpisodeSelect) },
                        enabled = targetEpisode != null,
                        colors = ButtonDefaults.colors(
                            containerColor = TvAccentCyan,
                            contentColor = TvBackgroundTop,
                            focusedContainerColor = TvPureWhite,
                            focusedContentColor = TvBackgroundTop
                        ),
                        scale = ButtonDefaults.scale(focusedScale = 1.06f),
                        shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp)),
                        modifier = Modifier.weight(1f).height(46.dp).focusRequester(playRequester)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = playButtonText,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onToggleFavorite,
                        colors = OutlinedButtonDefaults.colors(
                            contentColor = TvPureWhite,
                            focusedContainerColor = TvCardFocusHighlight,
                            focusedContentColor = TvPureWhite
                        ),
                        border = OutlinedButtonDefaults.border(
                            border = Border(
                                border = BorderStroke(1.dp, TvCardBorder),
                                shape = RoundedCornerShape(10.dp)
                            ),
                            focusedBorder = Border(
                                border = BorderStroke(2.dp, TvAccentCyan),
                                shape = RoundedCornerShape(10.dp)
                            )
                        ),
                        scale = OutlinedButtonDefaults.scale(focusedScale = 1.06f),
                        shape = OutlinedButtonDefaults.shape(shape = RoundedCornerShape(10.dp)),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Rounded.Check else Icons.Rounded.Add,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) TvAccentCyan else TvPureWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isFavorite) "已在追番" else "追番",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                        }
                    }
                }
            }

            // RIGHT PANEL: Episode Cards Grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "劇集列表",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = TvPureWhite
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "共 ${episodes.size} 集",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TvAccentCyan
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Refresh Cover Image Button
                        OutlinedButton(
                            onClick = {
                                if (!isRefreshingCover) {
                                    isRefreshingCover = true
                                    Toast.makeText(context, "正在重新抓取海報封面...", Toast.LENGTH_SHORT).show()
                                    CoverImageFetcher.refreshCoverUrl(context, anime.title)
                                }
                            },
                            colors = OutlinedButtonDefaults.colors(
                                contentColor = TvPureWhite,
                                focusedContainerColor = TvCardFocusHighlight,
                                focusedContentColor = TvAccentCyan
                            ),
                            border = OutlinedButtonDefaults.border(
                                border = Border(border = BorderStroke(1.dp, TvCardBorder), shape = RoundedCornerShape(8.dp)),
                                focusedBorder = Border(border = BorderStroke(2.dp, TvAccentCyan), shape = RoundedCornerShape(8.dp))
                            ),
                            shape = OutlinedButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isRefreshingCover) {
                                    CircularProgressIndicator(
                                        color = TvAccentCyan,
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "抓取中...",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TvAccentCyan
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Refresh Cover",
                                        tint = TvPureWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "重新整理封面",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = onToggleFavorite,
                            colors = OutlinedButtonDefaults.colors(
                                contentColor = TvPureWhite,
                                focusedContainerColor = TvCardFocusHighlight,
                                focusedContentColor = TvPureWhite
                            ),
                            border = OutlinedButtonDefaults.border(
                                border = Border(border = BorderStroke(1.dp, TvCardBorder), shape = RoundedCornerShape(8.dp)),
                                focusedBorder = Border(border = BorderStroke(2.dp, TvAccentCyan), shape = RoundedCornerShape(8.dp))
                            ),
                            shape = OutlinedButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isFavorite) Icons.Rounded.Check else Icons.Rounded.Add,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorite) TvAccentCyan else TvPureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isFavorite) "已在追番" else "追番",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { isReversed = !isReversed },
                            colors = OutlinedButtonDefaults.colors(
                                contentColor = TvPureWhite,
                                focusedContainerColor = TvCardFocusHighlight,
                                focusedContentColor = TvPureWhite
                            ),
                            border = OutlinedButtonDefaults.border(
                                border = Border(border = BorderStroke(1.dp, TvCardBorder), shape = RoundedCornerShape(8.dp)),
                                focusedBorder = Border(border = BorderStroke(2.dp, TvAccentCyan), shape = RoundedCornerShape(8.dp))
                            ),
                            shape = OutlinedButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = if (isReversed) "排序：倒序" else "排序：正序",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = displayedEpisodes,
                        key = { it.id }
                    ) { episode ->
                        val history = historyMap[episode.id]
                        EpisodeCard(
                            episode = episode,
                            animeCoverUrl = anime.coverUrl,
                            historyItem = history,
                            onClick = { onEpisodeSelect(episode) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeCard(
    episode: Episode,
    animeCoverUrl: String,
    historyItem: HistoryItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayEpTitle = remember(episode.title) { ChineseConverter.toTraditionalChinese(episode.title) }

    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.08f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvCardBackground,
            focusedContainerColor = TvCardBackground
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(border = BorderStroke(1.dp, TvCardBorder), shape = RoundedCornerShape(12.dp)),
            focusedBorder = Border(border = BorderStroke(2.dp, TvAccentCyan), shape = RoundedCornerShape(12.dp))
        ),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                AsyncImage(
                    model = animeCoverUrl,
                    contentDescription = displayEpTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "第 ${episode.episodeNumber} 集",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TvAccentCyan
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                        .background(TvAccentCyan.copy(alpha = 0.85f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = TvBackgroundTop,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (historyItem != null && historyItem.durationMs > 0) {
                    val progressFraction = (historyItem.playbackPositionMs.toFloat() / historyItem.durationMs.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(4.dp),
                        color = TvAccentCyan,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = displayEpTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = TvPureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(device = "id:tv_1080p", showBackground = true)
@Composable
fun DetailScreenPreview() {
    AnimetvoneTheme {
        DetailContent(
            anime = SampleAnimeData.sampleAnimes.first(),
            episodes = SampleAnimeData.sampleEpisodesMap.values.first(),
            isFavorite = false,
            onToggleFavorite = {},
            onEpisodeSelect = {},
            onBackClick = {}
        )
    }
}
