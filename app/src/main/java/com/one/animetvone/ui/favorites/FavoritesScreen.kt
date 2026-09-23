package com.one.animetvone.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.repository.SampleAnimeData
import com.one.animetvone.ui.components.AnimeCard
import com.one.animetvone.ui.theme.AnimetvoneTheme
import com.one.animetvone.ui.theme.TvAccentCyan
import com.one.animetvone.ui.theme.TvBackgroundBottom
import com.one.animetvone.ui.theme.TvBackgroundTop
import com.one.animetvone.ui.theme.TvOnSurfaceMuted
import com.one.animetvone.ui.theme.TvPureWhite

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
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = TvAccentCyan)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "載入追番列表中...",
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
                            val favRetryRequester = remember { FocusRequester() }
                            LaunchedEffect(Unit) {
                                try { favRetryRequester.requestFocus() } catch (_: Exception) { }
                            }
                            Button(
                                onClick = onRetry,
                                modifier = Modifier.focusRequester(favRetryRequester)
                            ) {
                                Text(text = "重試")
                            }
                        }
                    }
                }

                uiState.favoriteAnimeList.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 80.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FavoriteBorder,
                                contentDescription = "No Favorites",
                                tint = TvOnSurfaceMuted,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "尚未加入追番，在動畫頁面點擊【追番】即可加入",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                                color = TvPureWhite,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onExploreClick,
                                colors = ButtonDefaults.colors(
                                    containerColor = TvAccentCyan,
                                    contentColor = TvBackgroundTop,
                                    focusedContainerColor = TvAccentCyan,
                                    focusedContentColor = TvBackgroundTop
                                ),
                                shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp))
                            ) {
                                Text(
                                    text = "前往動畫列表探索",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

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
