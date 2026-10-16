package com.one.animetvone.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.repository.SampleAnimeData
import com.one.animetvone.ui.components.AnimeCard
import com.one.animetvone.ui.components.TvEmptyView
import com.one.animetvone.ui.components.TvErrorView
import com.one.animetvone.ui.components.TvLoadingView
import com.one.animetvone.ui.theme.AnimetvoneTheme
import com.one.animetvone.ui.theme.TvBackgroundBottom
import com.one.animetvone.ui.theme.TvBackgroundTop

@Composable
fun FavoritesScreen(
    onAnimeSelect: (Anime) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadFavorites(context)
    }

    FavoritesContent(
        uiState = uiState,
        onAnimeSelect = onAnimeSelect,
        onExploreClick = onExploreClick,
        onRetry = { viewModel.loadFavorites(context) },
        modifier = modifier
    )
}

@Composable
fun FavoritesContent(
    uiState: FavoritesUiState,
    onAnimeSelect: (Anime) -> Unit,
    onExploreClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
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
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.isLoading -> TvLoadingView(message = "載入追番列表中…")

                uiState.errorMessage != null -> TvErrorView(
                    message = uiState.errorMessage,
                    onRetry = onRetry
                )

                uiState.favoriteAnimeList.isEmpty() -> TvEmptyView(
                    icon = Icons.Rounded.FavoriteBorder,
                    title = "尚未加入追番",
                    description = "在動畫頁面點擊【追番】即可加入",
                    actionLabel = "前往動畫列表探索",
                    onAction = onExploreClick
                )

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 48.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.favoriteAnimeList,
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

@Preview(device = "id:tv_1080p", showBackground = true)
@Composable
fun FavoritesScreenPreview() {
    AnimetvoneTheme {
        FavoritesContent(
            uiState = FavoritesUiState(
                favoriteAnimeList = SampleAnimeData.sampleAnimes
            ),
            onAnimeSelect = {},
            onExploreClick = {},
            onRetry = {}
        )
    }
}
