package com.one.animetvone

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.activity.ComponentActivity
import androidx.compose.foundation.focusable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.one.animetvone.data.model.Anime
import com.one.animetvone.data.model.Episode
import com.one.animetvone.ui.player.EpisodeListOverlay
import com.one.animetvone.ui.player.PlayerControlsOverlay
import com.one.animetvone.ui.player.PlayerUiState
import com.one.animetvone.ui.theme.AnimetvoneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that every player control is reachable with the D-pad.
 *
 * Regression guard for the bug where focus got stuck on the centred play/pause
 * button: it has no vertically-overlapping neighbour, so Compose's default
 * directional search could never reach the top bar (選集 / 速度 / 追番 / 比例 /
 * 返回). The overlay now wires focus neighbours explicitly.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class PlayerControlsFocusTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val ep1 = Episode(
        id = "1_ep_1_token_ep1",
        animeId = "1",
        episodeNumber = 1,
        title = "第 1 集",
        thumbnailUrl = "",
        duration = "24:00",
        streamUrl = ""
    )
    private val ep2 = Episode(
        id = "1_ep_2_token_ep2",
        animeId = "1",
        episodeNumber = 2,
        title = "第 2 集",
        thumbnailUrl = "",
        duration = "24:00",
        streamUrl = ""
    )
    private val anime = Anime(
        id = "1",
        title = "測試動畫",
        coverUrl = "",
        bannerUrl = "",
        description = "",
        rating = 8.0,
        genres = emptyList(),
        year = 2024,
        status = "連載中",
        episodeCount = 2
    )

    private fun state() = PlayerUiState(
        episodeId = ep1.id,
        currentEpisode = ep1,
        currentAnime = anime,
        episodes = listOf(ep1, ep2),
        isPlaying = true,
        playbackPosition = 60_000,
        duration = 1_440_000,
        areControlsVisible = true
    )

    private fun ComposeContentTestRule.press(key: Key) {
        onRoot().performKeyInput { pressKey(key) }
        waitForIdle()
    }

    @Test
    fun dpadGraphIsFullyReachableFromPlayPause() {
        rule.setContent {
            val playPause = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                withFrameNanos { }
                runCatching { playPause.requestFocus() }
            }
            AnimetvoneTheme {
                PlayerControlsOverlay(
                    uiState = state(),
                    hasNextEpisode = true,
                    onPlayPauseToggle = {},
                    onRewind10 = {},
                    onForward10 = {},
                    onNextEpisode = {},
                    onSeek = {},
                    onBackClick = {},
                    playPauseFocusRequester = playPause
                )
            }
        }

        // Starts on the play/pause button.
        rule.onNodeWithContentDescription("Pause").assertIsFocused()

        // UP must reach the top bar (previously impossible).
        rule.press(Key.DirectionUp)
        rule.onNodeWithText("選集 (2)").assertIsFocused()

        // DOWN returns to play/pause.
        rule.press(Key.DirectionDown)
        rule.onNodeWithContentDescription("Pause").assertIsFocused()

        // RIGHT walks the centre row: play/pause -> +10s -> 下一集.
        rule.press(Key.DirectionRight)
        rule.onNodeWithContentDescription("+10s").assertIsFocused()
        rule.press(Key.DirectionRight)
        rule.onNodeWithText("下一集").assertIsFocused()

        // LEFT walks back: 下一集 -> +10s -> play/pause -> -10s.
        rule.press(Key.DirectionLeft)
        rule.onNodeWithContentDescription("+10s").assertIsFocused()
        rule.press(Key.DirectionLeft)
        rule.onNodeWithContentDescription("Pause").assertIsFocused()
        rule.press(Key.DirectionLeft)
        rule.onNodeWithContentDescription("-10s").assertIsFocused()

        // UP from -10s reaches the back button in the top-left.
        rule.press(Key.DirectionUp)
        rule.onNodeWithContentDescription("Back").assertIsFocused()

        // From the top bar we can reach 速度 / 追番 / 比例 horizontally.
        rule.press(Key.DirectionRight)
        rule.onNodeWithText("選集 (2)").assertIsFocused()
        rule.press(Key.DirectionRight)
        rule.onNodeWithText("速度 1.0x").assertIsFocused()
        rule.press(Key.DirectionRight)
        rule.onNodeWithText("追番").assertIsFocused()
        rule.press(Key.DirectionRight)
        rule.onNodeWithText("比例：適合").assertIsFocused()
    }

    @Test
    fun containerHandsFocusToPlayPause() {
        rule.setContent {
            val container = remember { FocusRequester() }
            val playPause = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                withFrameNanos { }
                runCatching { container.requestFocus() }
            }
            AnimetvoneTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(container)
                        .focusProperties {
                            up = playPause
                            down = playPause
                            left = playPause
                            right = playPause
                        }
                        .focusable()
                ) {
                    PlayerControlsOverlay(
                        uiState = state(),
                        hasNextEpisode = true,
                        onPlayPauseToggle = {},
                        onRewind10 = {},
                        onForward10 = {},
                        onNextEpisode = {},
                        onSeek = {},
                        onBackClick = {},
                        playPauseFocusRequester = playPause
                    )
                }
            }
        }

        // The full-screen container is focused: any direction must hand focus to
        // the play/pause button instead of leaving it stuck on the container.
        rule.press(Key.DirectionRight)
        rule.onNodeWithContentDescription("Pause").assertIsFocused()

        // ...and from there the rest of the graph still works.
        rule.press(Key.DirectionUp)
        rule.onNodeWithText("選集 (2)").assertIsFocused()
    }

    @Test
    fun episodePickerFocusesCurrentEpisodeAndScrolls() {
        val episodes = (1..8).map {
            Episode(
                id = "1_ep_${it}_token_ep$it",
                animeId = "1",
                episodeNumber = it,
                title = "標題 $it",
                thumbnailUrl = "",
                duration = "24:00",
                streamUrl = ""
            )
        }
        rule.setContent {
            AnimetvoneTheme {
                EpisodeListOverlay(
                    episodes = episodes,
                    currentEpisodeId = "1_ep_5_token_ep5",
                    onSelect = {}
                )
            }
        }

        // Opening lands on the episode that is currently playing. The overlay
        // scrolls to the item and requests focus once the scroll has settled,
        // so wait for the focus to actually land before asserting.
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("第 5 集").fetchSemanticsNodes().any {
                it.config.getOrNull(SemanticsProperties.Focused) == true
            }
        }
        rule.onNodeWithText("第 5 集").assertIsFocused()

        // D-pad can move to a neighbour in the list.
        rule.press(Key.DirectionDown)
        rule.onNodeWithText("第 6 集").assertIsFocused()
        rule.press(Key.DirectionUp)
        rule.onNodeWithText("第 5 集").assertIsFocused()
    }
}
