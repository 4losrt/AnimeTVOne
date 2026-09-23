package com.one.animetvone.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Border
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.one.animetvone.ui.theme.AnimetvoneTheme
import com.one.animetvone.ui.theme.TvAccentCyan
import com.one.animetvone.ui.theme.TvBackgroundTop
import com.one.animetvone.ui.theme.TvCardBorder
import com.one.animetvone.ui.theme.TvCardFocusHighlight
import com.one.animetvone.ui.theme.TvPureWhite

enum class NavTab(
    val title: String,
    val icon: ImageVector,
) {
    HOME("首頁", Icons.Rounded.Home),
    SCHEDULE("每週新番表", Icons.Rounded.CalendarToday),
    ANIME_LIST("動畫列表", Icons.AutoMirrored.Rounded.FormatListBulleted),
    FAVORITES("追番", Icons.Rounded.Favorite),
    HISTORY("歷史紀錄", Icons.Rounded.History),
    SETTINGS("設定", Icons.Rounded.Settings)
}

@Composable
fun SideNavigationRail(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    onSidebarFocusChanged: ((Boolean) -> Unit)? = null
) {
    var isSidebarFocused by remember { mutableStateOf(false) }

    val sidebarWidth by animateDpAsState(
        targetValue = if (isSidebarFocused) 210.dp else 64.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "SideNavigationRailWidthAnimation"
    )

    Box(
        modifier = modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(TvBackgroundTop)
            .border(
                width = 1.dp,
                color = TvCardBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            )
            .onFocusChanged { focusState ->
                isSidebarFocused = focusState.hasFocus
                onSidebarFocusChanged?.invoke(focusState.hasFocus)
            }
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start
        ) {
            // App Icon / Logo Top
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(TvAccentCyan, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tv,
                        contentDescription = "ANIME TV Logo",
                        tint = TvBackgroundTop,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (isSidebarFocused) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "ANIME TV",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 18.sp
                        ),
                        color = TvPureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Navigation Items List
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NavTab.entries.forEach { navItem ->
                    val isSelected = selectedTab == navItem
                    var isItemFocused by remember { mutableStateOf(false) }

                    Surface(
                        onClick = { onTabSelected(navItem) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 8.dp)
                            .onFocusChanged {
                                isItemFocused = it.isFocused
                            },
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) TvCardFocusHighlight else Color.Transparent,
                            focusedContainerColor = if (isSelected) TvCardFocusHighlight
                                else TvPureWhite.copy(alpha = 0.08f),
                            pressedContainerColor = TvCardFocusHighlight
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(
                                border = BorderStroke(
                                    1.5.dp,
                                    if (isSelected) TvAccentCyan else TvPureWhite.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        ),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Active Cyan Indicator Bar: selected tab ONLY.
                            // Focus alone shows border + brighter icon (see below),
                            // so focused and selected never look like two active tabs.
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight(0.65f)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        color = if (isSelected) TvAccentCyan else Color.Transparent
                                    )
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Icon
                            Icon(
                                imageVector = navItem.icon,
                                contentDescription = navItem.title,
                                tint = if (isSelected) TvAccentCyan
                                    else if (isItemFocused) TvPureWhite
                                    else TvPureWhite.copy(alpha = 0.65f),
                                modifier = Modifier.size(22.dp)
                            )

                            // Expanded Title Label
                            AnimatedVisibility(
                                visible = isSidebarFocused,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Row {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = navItem.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 15.sp
                                        ),
                                        color = if (isSelected) TvPureWhite else TvPureWhite.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = "id:tv_1080p", showBackground = true)
@Composable
fun SideNavigationRailPreview() {
    AnimetvoneTheme {
        SideNavigationRail(
            selectedTab = NavTab.HOME,
            onTabSelected = {}
        )
    }
}
