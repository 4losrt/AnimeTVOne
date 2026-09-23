package com.one.animetvone.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        targetValue = if (isSidebarFocused) 248.dp else 72.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
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
                    Image(
                        painter = rememberVectorPainter(Icons.Rounded.Tv),
                        contentDescription = "ANIME TV Logo",
                        colorFilter = ColorFilter.tint(TvBackgroundTop),
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (isSidebarFocused) {
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicText(
                        text = "ANIME TV",
                        style = TextStyle(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontSize = 18.sp,
                            color = TvPureWhite
                        ),
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
                    val interactionSource = remember { MutableInteractionSource() }
                    val itemShape = RoundedCornerShape(12.dp)
                    val isHighlighted = isSelected || isItemFocused
                    val itemBackground = when {
                        isItemFocused -> TvAccentCyan.copy(alpha = if (isSelected) 0.34f else 0.22f)
                        isSelected -> TvCardFocusHighlight
                        else -> Color.Transparent
                    }
                    val itemBorder = when {
                        isItemFocused -> TvAccentCyan
                        isSelected -> TvAccentCyan.copy(alpha = 0.68f)
                        else -> Color.Transparent
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp)
                            .clip(itemShape)
                            .background(itemBackground)
                            .border(
                                width = if (isItemFocused) 2.dp else 1.dp,
                                color = itemBorder,
                                shape = itemShape
                            )
                            .onFocusChanged {
                                isItemFocused = it.isFocused
                            }
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onTabSelected(navItem) }
                            ),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .fillMaxHeight(0.62f)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        color = if (isHighlighted) TvAccentCyan else Color.Transparent
                                    )
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            // Icon
                            Image(
                                painter = rememberVectorPainter(navItem.icon),
                                contentDescription = navItem.title,
                                colorFilter = ColorFilter.tint(
                                    if (isHighlighted) TvPureWhite
                                    else TvPureWhite.copy(alpha = 0.65f)
                                ),
                                modifier = Modifier.size(24.dp)
                            )

                            if (isSidebarFocused) {
                                Row {
                                    Spacer(modifier = Modifier.width(12.dp))
                                    BasicText(
                                        text = navItem.title,
                                        style = TextStyle(
                                            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 17.sp,
                                            color = if (isHighlighted) TvPureWhite
                                                else TvPureWhite.copy(alpha = 0.86f)
                                        ),
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
