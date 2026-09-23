package com.one.animetvone.ui.animelist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.repository.SampleAnimeData
import com.one.animetvone.ui.components.AnimeCard
import com.one.animetvone.ui.theme.AnimetvoneTheme
import com.one.animetvone.ui.theme.TvAccentCyan
import com.one.animetvone.ui.theme.TvBackgroundBottom
import com.one.animetvone.ui.theme.TvBackgroundTop
import com.one.animetvone.ui.theme.TvCardBackground
import com.one.animetvone.ui.theme.TvCardBorder
import com.one.animetvone.ui.theme.TvOnSurfaceMuted
import com.one.animetvone.ui.theme.TvPureWhite
import com.one.animetvone.util.ChineseConverter

@Composable
fun AnimeListScreen(
    onAnimeSelect: (Anime) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnimeListViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnimeListContent(
        uiState = uiState,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onSeasonSelected = viewModel::onSeasonSelected,
        onGoToPage = viewModel::goToPage,
        onPreviousPage = viewModel::previousPage,
        onNextPage = viewModel::nextPage,
        onAnimeSelect = onAnimeSelect,
        onRetry = viewModel::loadAnimeList,
        modifier = modifier
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AnimeListContent(
    uiState: AnimeListUiState,
    onSearchQueryChanged: (String) -> Unit,
    onSeasonSelected: (String) -> Unit,
    onGoToPage: (Int) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onAnimeSelect: (Anime) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var isSearchFocused by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    LaunchedEffect(uiState.currentPage) {
        if (uiState.currentPageItems.isNotEmpty()) {
            gridState.scrollToItem(0)
        }
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = {
                    Text(
                        text = "搜尋動畫名稱或關鍵字...",
                        color = TvOnSurfaceMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = if (isSearchFocused) TvAccentCyan else TvPureWhite.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    // 遙控注意：清除鈕包在 TextField 內部時焦點到不了；
                    // 改為不可聚焦的純顯示（真要清除：反白文字按倒退鍵即可）。
                    // 保留 AnimatedVisibility 佔位但不搶焦點。
                    AnimatedVisibility(
                        visible = uiState.searchQuery.isNotEmpty(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "有輸入文字時顯示",
                                tint = TvPureWhite.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                },
                // 遙控器：搜尋框永遠不可搶初始焦點（否則會彈出鍵盤、海報按不到）。
                // 需要搜尋時按 OK 進入，搜尋鍵確認後自動交回海報牆。
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = TvCardBackground,
                    unfocusedContainerColor = TvCardBackground,
                    focusedBorderColor = TvAccentCyan,
                    unfocusedBorderColor = TvCardBorder,
                    focusedTextColor = TvPureWhite,
                    unfocusedTextColor = TvPureWhite,
                    cursorColor = TvAccentCyan
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .onFocusChanged { isSearchFocused = it.isFocused }
            )

            if (uiState.availableSeasons.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 12.dp)
                ) {
                    items(
                        items = uiState.availableSeasons,
                        key = { it }
                    ) { season ->
                        val isSelected = season == uiState.selectedSeason
                        val displaySeason = remember(season) { ChineseConverter.toTraditionalChinese(season) }

                        FilterChip(
                            selected = isSelected,
                            onClick = { onSeasonSelected(season) },
                            colors = FilterChipDefaults.colors(
                                containerColor = TvCardBackground,
                                contentColor = TvPureWhite.copy(alpha = 0.7f),
                                focusedContainerColor = TvAccentCyan,
                                focusedContentColor = TvBackgroundTop,
                                selectedContainerColor = TvAccentCyan,
                                selectedContentColor = TvBackgroundTop,
                                focusedSelectedContainerColor = TvAccentCyan,
                                focusedSelectedContentColor = TvBackgroundTop
                            ),
                            scale = FilterChipDefaults.scale(focusedScale = 1.08f),
                            border = FilterChipDefaults.border(
                                border = Border(border = BorderStroke(1.dp, TvCardBorder), shape = RoundedCornerShape(12.dp)),
                                focusedBorder = Border(border = BorderStroke(2.dp, TvAccentCyan), shape = RoundedCornerShape(12.dp))
                            ),
                            shape = FilterChipDefaults.shape(shape = RoundedCornerShape(12.dp))
                        ) {
                            Text(
                                text = displaySeason,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier.weight(1f)
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
                                text = "載入動畫列表中...",
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
                                text = uiState.errorMessage,
                                style = MaterialTheme.typography.titleMedium,
                                color = TvPureWhite
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            val listRetryRequester = remember { FocusRequester() }
                            LaunchedEffect(Unit) {
                                try { listRetryRequester.requestFocus() } catch (_: Exception) { }
                            }
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.focusRequester(listRetryRequester)
                            ) {
                                Text(text = "重試")
                            }
                        }
                    }
                }

                uiState.filteredAnimeList.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Movie,
                                contentDescription = "No Results",
                                tint = TvOnSurfaceMuted,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) "沒有找到符合「${uiState.searchQuery}」的動畫" else "沒有找到符合的動畫",
                                style = MaterialTheme.typography.titleMedium,
                                color = TvPureWhite,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        state = gridState,
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.currentPageItems,
                            key = { it.id }
                        ) { anime ->
                            AnimeCard(
                                anime = anime,
                                onClick = { onAnimeSelect(anime) },
                                cardWidth = 160.dp,
                                isTextOnly = true
                            )
                        }
                    }
                }
            }
        }

        if (uiState.filteredAnimeList.isNotEmpty() && uiState.totalPages >= 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPreviousPage,
                    enabled = uiState.currentPage > 1,
                    colors = OutlinedButtonDefaults.colors(
                        contentColor = TvPureWhite,
                        focusedContainerColor = TvAccentCyan,
                        focusedContentColor = TvBackgroundTop,
                        disabledContentColor = TvPureWhite.copy(alpha = 0.2f)
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
                            imageVector = Icons.AutoMirrored.Rounded.NavigateBefore,
                            contentDescription = "Previous Page",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "上一頁",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "第 ${uiState.currentPage} / ${uiState.totalPages} 頁",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        ),
                        color = TvPureWhite
                    )

                    if (uiState.totalPages in 2..8) {
                        Spacer(modifier = Modifier.width(8.dp))
                        for (p in 1..uiState.totalPages) {
                            val isCurrent = p == uiState.currentPage
                            Surface(
                                onClick = { onGoToPage(p) },
                                shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (isCurrent) TvAccentCyan else TvCardBackground,
                                    focusedContainerColor = TvAccentCyan
                                ),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = "$p",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isCurrent) TvBackgroundTop else TvPureWhite
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(共 ${uiState.totalItemsCount} 部)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TvOnSurfaceMuted
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                OutlinedButton(
                    onClick = onNextPage,
                    enabled = uiState.currentPage < uiState.totalPages,
                    colors = OutlinedButtonDefaults.colors(
                        contentColor = TvPureWhite,
                        focusedContainerColor = TvAccentCyan,
                        focusedContentColor = TvBackgroundTop,
                        disabledContentColor = TvPureWhite.copy(alpha = 0.2f)
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
                        Text(
                            text = "下一頁",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                            contentDescription = "Next Page",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(device = "id:tv_1080p", showBackground = true)
@Composable
fun AnimeListScreenPreview() {
    AnimetvoneTheme {
        AnimeListContent(
            uiState = AnimeListUiState(
                allAnimeList = SampleAnimeData.sampleAnimes,
                filteredAnimeList = SampleAnimeData.sampleAnimes,
                currentPageItems = SampleAnimeData.sampleAnimes,
                currentPage = 1,
                totalPages = 5,
                totalItemsCount = 120
            ),
            onSearchQueryChanged = {},
            onSeasonSelected = {},
            onGoToPage = {},
            onPreviousPage = {},
            onNextPage = {},
            onAnimeSelect = {},
            onRetry = {}
        )
    }
}
