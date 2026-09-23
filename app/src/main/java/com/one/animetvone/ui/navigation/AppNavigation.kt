package com.one.animetvone.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.one.animetvone.ui.animelist.AnimeListScreen
import com.one.animetvone.ui.detail.DetailScreen
import com.one.animetvone.ui.favorites.FavoritesScreen
import com.one.animetvone.ui.history.HistoryScreen
import com.one.animetvone.ui.home.HomeScreen
import com.one.animetvone.ui.player.PlayerScreen
import com.one.animetvone.ui.schedule.ScheduleScreen
import com.one.animetvone.ui.settings.SettingsScreen
import com.one.animetvone.ui.theme.TvBackgroundBrush

sealed interface Screen {
    data class MainTab(val tab: NavTab) : Screen
    data class Detail(val animeId: String) : Screen
    data class Player(val animeId: String, val episodeId: String) : Screen
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(NavTab.HOME) }
    val detailOrPlayerBackStack = remember { mutableStateListOf<Screen>() }

    val currentScreen = detailOrPlayerBackStack.lastOrNull() ?: Screen.MainTab(selectedTab)

    BackHandler(enabled = detailOrPlayerBackStack.isNotEmpty()) {
        detailOrPlayerBackStack.removeAt(detailOrPlayerBackStack.lastIndex)
    }

    val navigateToDetail: (String) -> Unit = { animeId ->
        detailOrPlayerBackStack.add(Screen.Detail(animeId))
    }

    val navigateToPlayer: (String, String) -> Unit = { animeId, episodeId ->
        detailOrPlayerBackStack.add(Screen.Player(animeId, episodeId))
    }

    val navigateBack: () -> Unit = {
        if (detailOrPlayerBackStack.isNotEmpty()) {
            detailOrPlayerBackStack.removeAt(detailOrPlayerBackStack.lastIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackgroundBrush)
    ) {
        when (currentScreen) {
            is Screen.MainTab -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Navigation Rail / Sidebar
                    SideNavigationRail(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            selectedTab = tab
                        }
                    )

                    // Right Main Content Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (selectedTab) {
                            NavTab.HOME -> {
                                HomeScreen(
                                    onAnimeSelect = { anime -> navigateToDetail(anime.id) },
                                    onWatchClick = { anime ->
                                        navigateToDetail(anime.id)
                                    },
                                )
                            }
                            NavTab.SCHEDULE -> {
                                ScheduleScreen(
                                    onAnimeSelect = { anime -> navigateToDetail(anime.id) }
                                )
                            }
                            NavTab.ANIME_LIST -> {
                                AnimeListScreen(
                                    onAnimeSelect = { anime -> navigateToDetail(anime.id) }
                                )
                            }
                            NavTab.FAVORITES -> {
                                FavoritesScreen(
                                    onAnimeSelect = { anime -> navigateToDetail(anime.id) },
                                    onExploreClick = { selectedTab = NavTab.ANIME_LIST }
                                )
                            }
                            NavTab.HISTORY -> {
                                HistoryScreen(
                                    onAnimeSelect = { animeId -> navigateToDetail(animeId) },
                                    onEpisodeSelect = { animeId, episodeId -> navigateToPlayer(animeId, episodeId) },
                                    onExploreClick = { selectedTab = NavTab.ANIME_LIST }
                                )
                            }
                            NavTab.SETTINGS -> SettingsScreen()
                        }
                    }
                }
            }

            is Screen.Detail -> {
                DetailScreen(
                    animeId = currentScreen.animeId,
                    onEpisodeSelect = { animeId, episodeId ->
                        navigateToPlayer(animeId, episodeId)
                    },
                    onBackClick = navigateBack
                )
            }

            is Screen.Player -> {
                PlayerScreen(
                    episodeId = currentScreen.episodeId,
                    onBackClick = navigateBack
                )
            }
        }
    }
}
