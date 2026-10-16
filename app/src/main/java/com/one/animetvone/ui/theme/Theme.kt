package com.one.animetvone.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.tv.material3.MaterialTheme as TvMaterialTheme
import androidx.tv.material3.darkColorScheme as tvDarkColorScheme

private val TvDarkColorScheme = darkColorScheme(
    primary = TvAccent,
    secondary = TvAccentBright,
    tertiary = TvAccentViolet,
    background = TvBackgroundTop,
    surface = TvCardBackground,
    surfaceVariant = TvCardFocusHighlight,
    onBackground = TvOnBackground,
    onSurface = TvOnBackground,
    onPrimary = TvPureWhite,
    onSecondary = TvPureWhite,
)

private val TvMaterialDarkColorScheme = tvDarkColorScheme(
    primary = TvAccent,
    onPrimary = TvPureWhite,
    secondary = TvAccentBright,
    onSecondary = TvPureWhite,
    background = TvBackgroundTop,
    onBackground = TvOnBackground,
    surface = TvCardBackground,
    onSurface = TvOnBackground,
    surfaceVariant = TvCardFocusHighlight,
    onSurfaceVariant = TvOnSurfaceMuted,
)

val TvBackgroundBrush = Brush.verticalGradient(
    colors = listOf(
        TvBackgroundTop,
        TvBackgroundBottom
    )
)

@Composable
fun AnimetvoneTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        typography = Typography
    ) {
        TvMaterialTheme(
            colorScheme = TvMaterialDarkColorScheme,
            content = content
        )
    }
}

@Composable
fun TvBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = TvBackgroundBrush)
    ) {
        content()
    }
}
