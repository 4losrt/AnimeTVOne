package com.one.animetvone.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Border
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.model.ScheduleDay
import com.one.animetvone.data.repository.SampleAnimeData
import com.one.animetvone.ui.components.AnimeCard
import com.one.animetvone.ui.components.TvEmptyView
import com.one.animetvone.ui.components.TvErrorView
import com.one.animetvone.ui.components.TvLoadingView
import com.one.animetvone.ui.theme.AnimetvoneTheme
import com.one.animetvone.ui.theme.TvAccent
import com.one.animetvone.ui.theme.TvBackgroundBottom
import com.one.animetvone.ui.theme.TvBackgroundTop
import com.one.animetvone.ui.theme.TvCardBackground
import com.one.animetvone.ui.theme.TvCardBorder
import com.one.animetvone.ui.theme.TvPureWhite

private val DISPLAY_DAYS = listOf("週一", "週二", "週三", "週四", "週五", "週六", "週日")

@Composable
fun ScheduleScreen(
    onAnimeSelect: (Anime) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScheduleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ScheduleContent(
        uiState = uiState,
        onDaySelect = viewModel::selectDay,
        onAnimeSelect = onAnimeSelect,
        onRetry = viewModel::loadSchedule,
        modifier = modifier
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ScheduleContent(
    uiState: ScheduleUiState,
    onDaySelect: (String?) -> Unit,
    onAnimeSelect: (Anime) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dayNameMap = remember(uiState.scheduleDays) {
        uiState.scheduleDays.associateBy {
            formatDayName(it.dayName)
        }
    }

    val selectedDayTab = if (uiState.selectedDayName != null) {
        formatDayName(uiState.selectedDayName)
    } else {
        DISPLAY_DAYS.firstOrNull { dayNameMap.containsKey(it) } ?: "週一"
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
        // Horizontal Day Selector Tabs: 週一 週二 週三 週四 週五 週六 週日
        LazyRow(
            contentPadding = PaddingValues(end = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            items(
                items = DISPLAY_DAYS,
                key = { it }
            ) { shortDay ->
                val dayData = dayNameMap[shortDay]
                val animeCount = dayData?.animes?.size ?: 0
                val isSelected = selectedDayTab == shortDay

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val originalName = dayData?.dayName ?: shortDay
                        onDaySelect(originalName)
                    },
                    colors = FilterChipDefaults.colors(
                        containerColor = TvCardBackground,
                        contentColor = TvPureWhite.copy(alpha = 0.7f),
                        focusedContainerColor = TvAccent,
                        focusedContentColor = TvBackgroundTop,
                        selectedContainerColor = TvAccent,
                        selectedContentColor = TvBackgroundTop,
                        focusedSelectedContainerColor = TvAccent,
                        focusedSelectedContentColor = TvBackgroundTop
                    ),
                    scale = FilterChipDefaults.scale(focusedScale = 1.08f),
                    border = FilterChipDefaults.border(
                        border = Border(border = BorderStroke(1.dp, TvCardBorder), shape = RoundedCornerShape(12.dp)),
                        focusedBorder = Border(border = BorderStroke(2.5.dp, TvAccent), shape = RoundedCornerShape(12.dp))
                    ),
                    glow = FilterChipDefaults.glow(
                        focusedGlow = Glow(
                            elevationColor = TvAccent.copy(alpha = 0.5f),
                            elevation = 12.dp
                        )
                    ),
                    shape = FilterChipDefaults.shape(shape = RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "$shortDay ($animeCount)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 16.sp
                        ),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Focused Day Anime Grid
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading -> TvLoadingView(message = "載入新番表中…")

                uiState.errorMessage != null -> TvErrorView(
                    message = uiState.errorMessage,
                    onRetry = onRetry
                )

                else -> {
                    val currentDayAnimes = dayNameMap[selectedDayTab]?.animes ?: emptyList()

                    if (currentDayAnimes.isEmpty()) {
                        TvEmptyView(
                            icon = Icons.Rounded.CalendarToday,
                            title = "$selectedDayTab 尚無排程動畫",
                            description = "此日目前沒有排定新番，可切換其他星期查看"
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            contentPadding = PaddingValues(bottom = 48.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = currentDayAnimes,
                                key = { it.id }
                            ) { anime ->
                                AnimeCard(
                                    anime = anime,
                                    onClick = { onAnimeSelect(anime) },
                                    cardWidth = 160.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDayName(raw: String): String {
    return when {
        raw.contains("一") || raw.contains("Mon", ignoreCase = true) -> "週一"
        raw.contains("二") || raw.contains("Tue", ignoreCase = true) -> "週二"
        raw.contains("三") || raw.contains("Wed", ignoreCase = true) -> "週三"
        raw.contains("四") || raw.contains("Thu", ignoreCase = true) -> "週四"
        raw.contains("五") || raw.contains("Fri", ignoreCase = true) -> "週五"
        raw.contains("六") || raw.contains("Sat", ignoreCase = true) -> "週六"
        raw.contains("日") || raw.contains("天") || raw.contains("Sun", ignoreCase = true) -> "週日"
        else -> raw.replace("星期", "週")
    }
}

@Preview(device = "id:tv_1080p", showBackground = true)
@Composable
fun ScheduleScreenPreview() {
    AnimetvoneTheme {
        ScheduleContent(
            uiState = ScheduleUiState(
                scheduleDays = listOf(
                    ScheduleDay("星期一", SampleAnimeData.sampleAnimes),
                    ScheduleDay("星期二", SampleAnimeData.sampleAnimes)
                )
            ),
            onDaySelect = {},
            onAnimeSelect = {},
            onRetry = {}
        )
    }
}
