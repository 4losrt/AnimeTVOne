package com.one.animetvone.ui.history

import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.one.animetvone.data.model.HistoryItem
import com.one.animetvone.ui.components.TvEmptyView
import com.one.animetvone.ui.theme.AnimetvoneTheme
import com.one.animetvone.ui.theme.TvAccent
import com.one.animetvone.ui.theme.TvBackgroundBottom
import com.one.animetvone.ui.theme.TvBackgroundTop
import com.one.animetvone.ui.theme.TvCardBackground
import com.one.animetvone.ui.theme.TvCardBorder
import com.one.animetvone.ui.theme.TvCardFocusHighlight
import com.one.animetvone.ui.theme.TvOnSurfaceMuted
import com.one.animetvone.ui.theme.TvPureWhite
import com.one.animetvone.util.ChineseConverter
import java.util.Locale

@Composable
fun HistoryScreen(
    onAnimeSelect: (String) -> Unit,
    onEpisodeSelect: (String, String) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadHistory(context)
    }

    HistoryContent(
        historyItems = uiState.historyItems,
        onEpisodeSelect = { animeId, episodeId -> onEpisodeSelect(animeId, episodeId) },
        onAnimeDetail = { animeId -> onAnimeSelect(animeId) },
        onClearHistory = { viewModel.clearHistory(context) },
        onRemoveItem = { episodeId -> viewModel.removeHistoryItem(context, episodeId) },
        onExploreClick = onExploreClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HistoryContent(
    historyItems: List<HistoryItem>,
    onEpisodeSelect: (String, String) -> Unit,
    onAnimeDetail: (String) -> Unit,
    onClearHistory: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmClear by remember { mutableStateOf(false) }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { androidx.compose.material3.Text("清除所有播放紀錄？") },
            text = { androidx.compose.material3.Text("續播進度將無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearHistory()
                }) { androidx.compose.material3.Text("確認清除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { androidx.compose.material3.Text("取消") }
            }
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(TvBackgroundTop, TvBackgroundBottom)
                )
            )
            .padding(horizontal = 48.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = "History",
                    tint = TvAccent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "歷史紀錄",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ),
                    color = TvPureWhite
                )
                if (historyItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "(${historyItems.size} 部紀錄)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvOnSurfaceMuted
                    )
                }
            }

            if (historyItems.isNotEmpty()) {
                OutlinedButton(
                    onClick = { confirmClear = true },
                    colors = OutlinedButtonDefaults.colors(
                        contentColor = TvPureWhite,
                        focusedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                        focusedContentColor = TvPureWhite
                    ),
                    border = OutlinedButtonDefaults.border(
                        border = Border(
                            border = BorderStroke(1.dp, TvCardBorder),
                            shape = RoundedCornerShape(8.dp)
                        ),
                        focusedBorder = Border(
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        )
                    ),
                    shape = OutlinedButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Clear",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "清除紀錄", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        if (historyItems.isEmpty()) {
            TvEmptyView(
                icon = Icons.Rounded.History,
                title = "尚無播放紀錄",
                description = "觀看的動畫集數會自動記錄在此處，方便您隨時續播",
                actionLabel = "瀏覽動畫列表",
                onAction = onExploreClick
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(bottom = 48.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = historyItems,
                    key = { it.episodeId }
                ) { item ->
                    HistoryCard(
                        item = item,
                        onClick = { onEpisodeSelect(item.animeId, item.episodeId) },
                        onAnimeDetail = { onAnimeDetail(item.animeId) },
                        onRemoveItem = { onRemoveItem(item.episodeId) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HistoryCard(
    item: HistoryItem,
    onClick: () -> Unit,
    onAnimeDetail: () -> Unit,
    modifier: Modifier = Modifier,
    onRemoveItem: () -> Unit = {}
) {
    val displayAnimeTitle = remember(item.animeTitle) { ChineseConverter.toTraditionalChinese(item.animeTitle) }
    val displayEpTitle = remember(item.episodeTitle) { ChineseConverter.toTraditionalChinese(item.episodeTitle) }

    var isCardFocused by remember { mutableStateOf(false) }

    // TV 遙控：外層用不可點擊 Box（只追蹤高亮），內放兩個兄弟級可聚焦按鈕
    // （播放主 Surface + 刪除小 Surface）。之前刪除鈕包在可聚焦 Surface
    // 裡面，焦點永遠進不去 = 按不到。
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { isCardFocused = it.isFocused || it.hasFocus }
            .border(
                width = if (isCardFocused) 2.dp else 1.dp,
                color = if (isCardFocused) TvAccent else TvCardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(TvCardBackground),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = displayAnimeTitle,
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

                // 播放主按鈕：佔滿整張封面（右上小按鈕區除外），遙控 OK = 繼續播放
                Surface(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 0.dp, end = 76.dp, bottom = 4.dp),
                    colors = ClickableSurfaceDefaults.colors(containerColor = Color.Transparent),
                    border = ClickableSurfaceDefaults.border(
                        focusedBorder = Border(
                            border = BorderStroke(2.dp, TvAccent),
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                        )
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.0f),
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "繼續播放",
                            tint = TvPureWhite.copy(alpha = 0.9f),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = displayEpTitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TvAccent
                    )
                }

                // 右上：詳情 + 刪除（兄弟級獨立按鈕，左右鍵可達、OK 各自觸發）
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = onAnimeDetail,
                        modifier = Modifier.size(32.dp),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.Black.copy(alpha = 0.65f),
                            focusedContainerColor = TvCardFocusHighlight
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(
                                border = BorderStroke(2.dp, TvPureWhite),
                                shape = RoundedCornerShape(14.dp)
                            )
                        ),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f),
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = "查看詳情",
                                tint = TvPureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = onRemoveItem,
                        modifier = Modifier.size(32.dp),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = Color.Black.copy(alpha = 0.65f),
                            focusedContainerColor = MaterialTheme.colorScheme.error
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(
                                border = BorderStroke(2.dp, TvPureWhite),
                                shape = RoundedCornerShape(14.dp)
                            )
                        ),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f),
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(14.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "刪除此紀錄",
                                tint = TvPureWhite,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                val progressFraction = if (item.durationMs > 0) {
                    (item.playbackPositionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(4.dp),
                    color = TvAccent,
                    trackColor = Color.White.copy(alpha = 0.25f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = displayAnimeTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = TvPureWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                val posText = formatTimeMs(item.playbackPositionMs)
                val durText = formatTimeMs(item.durationMs)

                Text(
                    text = if (item.durationMs > 0) "已觀看 $posText / $durText" else "已開始播放",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = TvOnSurfaceMuted,
                    maxLines = 1
                )
            }
        }
    }
}

private fun formatTimeMs(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hours = minutes / 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@Preview(device = "id:tv_1080p", showBackground = true)
@Composable
fun HistoryScreenPreview() {
    AnimetvoneTheme {
        HistoryContent(
            historyItems = listOf(
                HistoryItem(
                    animeId = "1",
                    animeTitle = "葬送的芙莉蓮",
                    coverUrl = "",
                    episodeId = "1_ep_1",
                    episodeTitle = "第 1 集",
                    playbackPositionMs = 860000,
                    durationMs = 1440000,
                    timestamp = System.currentTimeMillis()
                )
            ),
            onEpisodeSelect = { _, _ -> },
            onAnimeDetail = {},
            onClearHistory = {},
            onRemoveItem = {},
            onExploreClick = {}
        )
    }
}
