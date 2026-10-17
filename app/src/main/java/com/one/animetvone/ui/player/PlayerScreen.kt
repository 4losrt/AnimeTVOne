package com.one.animetvone.ui.player

import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.activity.compose.BackHandler
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MimeTypes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.one.animetvone.data.model.Episode
import com.one.animetvone.data.repository.SampleAnimeData
import com.one.animetvone.data.source.HistoryManager
import com.one.animetvone.data.source.logD
import com.one.animetvone.ui.components.TvErrorView
import com.one.animetvone.ui.components.TvLoadingView
import com.one.animetvone.ui.theme.AnimetvoneTheme
import com.one.animetvone.ui.theme.TvAccent
import com.one.animetvone.ui.theme.TvBackgroundTop
import com.one.animetvone.ui.theme.TvCardBackground
import com.one.animetvone.ui.theme.TvCardBorder
import com.one.animetvone.ui.theme.TvCardFocusHighlight
import com.one.animetvone.ui.theme.TvPureWhite
import com.one.animetvone.util.ChineseConverter
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    episodeId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    // Shared with the overlay so the container can hand focus back to the
    // play/pause button (and fall back onto it if it is focused itself).
    val playPauseFocusRequester = remember { FocusRequester() }

    val playbackSpeed = uiState.playbackSpeed
    val resizeMode = uiState.resizeMode

    // Fast-forward / rewind accumulator used by consecutive ±10s operations.
    // ExoPlayer's currentPosition lags behind seekTo() for a beat, so naive
    // repeated presses all add 10s to the *stale* position and never compound.
    var seekAnchorMs by remember { mutableStateOf<Long?>(null) }
    var lastSeekPressMs by remember { mutableStateOf(0L) }

    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15_000,
                /* maxBufferMs = */ 50_000,
                /* bufferForPlaybackMs = */ 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ 3_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    val trackSelector = remember { DefaultTrackSelector(context) }
    val exoPlayer = remember(context, trackSelector, loadControl) {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
            .apply {
                playWhenReady = true
            }
    }

    LaunchedEffect(episodeId) {
        seekAnchorMs = null
        lastSeekPressMs = 0L
        viewModel.loadEpisode(episodeId)
    }

    LaunchedEffect(uiState.videoStream?.url, uiState.videoStream?.headers, uiState.retryCount) {
        val streamUrl = uiState.videoStream?.url
        val headers = uiState.videoStream?.headers ?: emptyMap()
        if (!streamUrl.isNullOrEmpty()) {
            logD("Anime1Video", "PlayerScreen loading stream URL: $streamUrl")

            val userAgent = headers["User-Agent"]
                ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setAllowCrossProtocolRedirects(true)

            val requestProperties = mutableMapOf<String, String>()
            requestProperties["Referer"] = "https://v.anime1.me/"
            val cookieStr = headers["Cookie"]
            if (!cookieStr.isNullOrEmpty()) {
                requestProperties["Cookie"] = cookieStr
            }
            headers.forEach { (k, v) ->
                if (k != "User-Agent" && k != "Referer") {
                    requestProperties[k] = v
                }
            }

            httpDataSourceFactory.setDefaultRequestProperties(requestProperties)

            val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

            val isHls = streamUrl.contains(".m3u8")
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(streamUrl.toUri())
            if (isHls) {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            val mediaItem = mediaItemBuilder.build()
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

            exoPlayer.setMediaSource(mediaSource)
            // Re-apply the user's chosen speed: the ViewModel remembers it across
            // episode changes, and a fresh media source can otherwise reset it.
            exoPlayer.setPlaybackSpeed(uiState.playbackSpeed)
            exoPlayer.prepare()

            val history = HistoryManager.getHistoryList(context)
            // Use the ViewModel's current episode id (not the route parameter):
            // the user can switch episodes from the in-player picker without the
            // route changing, and resume must follow the episode actually playing.
            val activeEpisodeId = uiState.episodeId ?: episodeId
            val savedHistory = history.find { it.episodeId == activeEpisodeId }
            if (savedHistory != null && savedHistory.playbackPositionMs > 5000L) {
                exoPlayer.seekTo(savedHistory.playbackPositionMs)
            }

            exoPlayer.playWhenReady = true
            exoPlayer.play()
        }
    }

    LaunchedEffect(exoPlayer, uiState.currentAnime, uiState.currentEpisode) {
        // History disk-writes are throttled: saveHistoryItem re-parses and
        // re-serializes the whole history list, so doing it every second
        // caused periodic jank during playback. 5s granularity is plenty
        // for resume purposes (dispose still saves precisely).
        var lastHistorySaveMs = 0L
        while (isActive) {
            if (exoPlayer.isPlaying) {
                val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val duration = exoPlayer.duration.coerceAtLeast(0L)
                viewModel.updatePlaybackPosition(
                    position = currentPos,
                    duration = duration
                )
                viewModel.setPlaying(true)

                val anime = uiState.currentAnime
                val episode = uiState.currentEpisode
                val now = System.currentTimeMillis()
                if (anime != null && episode != null && currentPos > 0L &&
                    now - lastHistorySaveMs >= 5000L
                ) {
                    lastHistorySaveMs = now
                    HistoryManager.saveHistoryItem(
                        context = context,
                        animeId = anime.id,
                        animeTitle = anime.title,
                        coverUrl = anime.coverUrl,
                        episodeId = episode.id,
                        episodeTitle = episode.title,
                        playbackPositionMs = currentPos,
                        durationMs = duration
                    )
                }
            } else {
                viewModel.setPlaying(exoPlayer.isPlaying)
            }
            delay(1000)
        }
    }

    // Auto-hide controls after 4s, but reset the timer on any user interaction
    // (button clicks, key presses) so the controls don't vanish mid-navigation.
    // Never auto-hide while the episode list is open.
    LaunchedEffect(
        uiState.areControlsVisible,
        uiState.controlsInteractionCount,
        uiState.isEpisodeListVisible,
        uiState.isPlaying
    ) {
        // Keep the controls while the video is paused or has ended (so the user
        // can still see where they are), and only auto-hide during playback.
        // Any remote input restarts the countdown.
        if (uiState.areControlsVisible && !uiState.isEpisodeListVisible && uiState.isPlaying) {
            delay(4000)
            viewModel.setControlsVisible(false)
        }
    }

    // Use rememberUpdatedState so onDispose always reads the LATEST anime/episode
    // instead of the stale first-composition values (which are always null).
    val currentAnime by rememberUpdatedState(uiState.currentAnime)
    val currentEpisode by rememberUpdatedState(uiState.currentEpisode)

    DisposableEffect(exoPlayer) {
        val playerListener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                logD("Anime1Video", "ExoPlayer error: ${error.message}")
                viewModel.setPlaybackError("影片播放失敗，請重試")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (viewModel.hasNextEpisode()) {
                        viewModel.playNextEpisode()
                    } else {
                        viewModel.setControlsVisible(true)
                    }
                }
            }
        }
        exoPlayer.addListener(playerListener)
        onDispose {
            exoPlayer.removeListener(playerListener)
            val anime = currentAnime
            val episode = currentEpisode
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (anime != null && episode != null && pos > 0L) {
                HistoryManager.saveHistoryItem(
                    context = context,
                    animeId = anime.id,
                    animeTitle = anime.title,
                    coverUrl = anime.coverUrl,
                    episodeId = episode.id,
                    episodeTitle = episode.title,
                    playbackPositionMs = pos,
                    durationMs = if (dur > 0L) dur else 0L
                )
            }
            exoPlayer.release()
        }
    }

    // The overlay is on screen only when the user asked for it, the stream is
    // ready and no error dialog is up. Used both to render it and to decide
    // where focus should live.
    val controlsOverlayVisible = uiState.areControlsVisible && !uiState.isLoading &&
        uiState.errorMessage == null && !uiState.isEpisodeListVisible

    // Focus management:
    //  - while the controls are on screen, park focus on the play/pause button
    //    (the overlay attaches its requester right after the next frame);
    //  - when they are hidden, park focus on the container so the D-pad keys
    //    still reach the onKeyEvent handler (seek / show controls).
    LaunchedEffect(controlsOverlayVisible, uiState.isEpisodeListVisible) {
        if (controlsOverlayVisible) {
            withFrameNanos { }
            runCatching { playPauseFocusRequester.requestFocus() }
        } else if (!uiState.isEpisodeListVisible) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    // Keep the system bars hidden while playing so the video gets the whole
    // screen on Fire/Android TV; restore them on exit (operator/back).
    // Uses WindowInsetsControllerCompat so it works on every supported API
    // (including API 25/28 Fire OS where the legacy flags are deprecated).
    val activityContext = context
    DisposableEffect(activityContext) {
        val window = (activityContext as? android.app.Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val previousBehavior = controller?.systemBarsBehavior
        if (controller != null) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (controller != null && previousBehavior != null) {
                controller.systemBarsBehavior = previousBehavior
            }
        }
    }

    // Back priority: episode list → control overlay → leave the player.
    BackHandler(enabled = true) {
        when {
            uiState.isEpisodeListVisible -> viewModel.setEpisodeListVisible(false)
            uiState.areControlsVisible -> viewModel.setControlsVisible(false)
            else -> onBackClick()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .focusProperties {
                // Safety net: if the container itself is focused while the
                // controls are visible, send the D-pad to the play/pause button
                // instead of leaving focus stuck on the full-screen node.
                // (focusProperties must precede focusable to attach to it.)
                if (uiState.areControlsVisible) {
                    up = playPauseFocusRequester
                    down = playPauseFocusRequester
                    left = playPauseFocusRequester
                    right = playPauseFocusRequester
                }
            }
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    val keyCode = keyEvent.nativeKeyEvent.keyCode
                    // Any remote input while the controls are on screen restarts
                    // the auto-hide countdown, so they never vanish mid-navigation
                    // (previously only button clicks reset it, so a slow D-pad
                    //  navigation could hide the controls before reaching a button).
                    if (uiState.areControlsVisible) {
                        viewModel.notifyControlsInteraction()
                    }
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER,
                        KeyEvent.KEYCODE_ENTER,
                        KeyEvent.KEYCODE_NUMPAD_ENTER,
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            if (uiState.errorMessage != null || uiState.isLoading) {
                                false
                            } else if (uiState.areControlsVisible && keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                                // Let the focused child button handle OK.
                                false
                            } else {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                    viewModel.setPlaying(false)
                                } else {
                                    exoPlayer.play()
                                    viewModel.setPlaying(true)
                                }
                                viewModel.setControlsVisible(true)
                                true
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            exoPlayer.play()
                            viewModel.setPlaying(true)
                            viewModel.setControlsVisible(true)
                            true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            exoPlayer.pause()
                            viewModel.setPlaying(false)
                            viewModel.setControlsVisible(true)
                            true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_MEDIA_REWIND -> {
                            // 遙控器友好：控制列顯示時，左右鍵留給焦點移動（切換按鈕）；
                            // 控制列隱藏時，左右鍵才是快退/快進。
                            if (uiState.areControlsVisible) {
                                false
                            } else {
                                val nowMs = System.currentTimeMillis()
                                val basePos = if (seekAnchorMs != null && nowMs - lastSeekPressMs < 2000L) {
                                    seekAnchorMs!!
                                } else {
                                    exoPlayer.currentPosition
                                }
                                val newPos = (basePos - 10_000).coerceAtLeast(0L)
                                seekAnchorMs = newPos
                                lastSeekPressMs = nowMs
                                exoPlayer.seekTo(newPos)
                                viewModel.updatePlaybackPosition(newPos, exoPlayer.duration.coerceAtLeast(0L))
                                viewModel.setControlsVisible(true)
                                true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT,
                        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                            if (uiState.areControlsVisible) {
                                false
                            } else {
                                val dur = exoPlayer.duration.coerceAtLeast(0L)
                                val nowMs = System.currentTimeMillis()
                                val basePos = if (seekAnchorMs != null && nowMs - lastSeekPressMs < 2000L) {
                                    seekAnchorMs!!
                                } else {
                                    exoPlayer.currentPosition
                                }
                                val newPos = (basePos + 10_000).coerceAtMost(if (dur > 0) dur else Long.MAX_VALUE)
                                seekAnchorMs = newPos
                                lastSeekPressMs = nowMs
                                exoPlayer.seekTo(newPos.coerceAtMost(if (dur > 0) dur else basePos + 10_000))
                                viewModel.updatePlaybackPosition(newPos, exoPlayer.duration.coerceAtLeast(0L))
                                viewModel.setControlsVisible(true)
                                true
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            // 遙控器友好：上下鍵永遠先叫出控制列並把焦點交給按鈕。
                            if (!uiState.areControlsVisible) {
                                viewModel.setControlsVisible(true)
                                true
                            } else {
                                false
                            }
                        }
                        KeyEvent.KEYCODE_BACK -> {
                            when {
                                uiState.isEpisodeListVisible -> {
                                    viewModel.setEpisodeListVisible(false)
                                    true
                                }
                                uiState.areControlsVisible -> {
                                    viewModel.setControlsVisible(false)
                                    true
                                }
                                else -> false
                            }
                        }
                        else -> {
                            viewModel.setControlsVisible(true)
                            false
                        }
                    }
                } else {
                    false
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    keepScreenOn = true
                    this.resizeMode = resizeMode
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.keepScreenOn = true
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
            ) {
                TvLoadingView(message = "影片載入中…")
            }
        }

        if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
            ) {
                TvErrorView(
                    message = uiState.errorMessage ?: "影片播放失敗",
                    onRetry = { viewModel.retryEpisode() }
                )
            }
        }

        AnimatedVisibility(
            visible = controlsOverlayVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerControlsOverlay(
                uiState = uiState,
                hasNextEpisode = viewModel.hasNextEpisode(),
                resizeMode = resizeMode,
                onToggleResizeMode = {
                    val newMode = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    viewModel.setResizeMode(newMode)
                },
                onToggleFavorite = { viewModel.toggleFavorite(context) },
                onPlayPauseToggle = {
                    seekAnchorMs = null
                    lastSeekPressMs = 0L
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                        viewModel.setPlaying(false)
                    } else {
                        exoPlayer.play()
                        viewModel.setPlaying(true)
                    }
                },
                onRewind10 = {
                    val nowMs = System.currentTimeMillis()
                    val duration = exoPlayer.duration.coerceAtLeast(0L)
                    val basePos = if (seekAnchorMs != null && nowMs - lastSeekPressMs < 2000L) {
                        seekAnchorMs!!
                    } else {
                        exoPlayer.currentPosition
                    }
                    val newPos = (basePos - 10_000).coerceAtLeast(0L)
                    seekAnchorMs = newPos
                    lastSeekPressMs = nowMs
                    exoPlayer.seekTo(newPos)
                    viewModel.updatePlaybackPosition(newPos, exoPlayer.duration.coerceAtLeast(0L))
                },
                onForward10 = {
                    val nowMs = System.currentTimeMillis()
                    val duration = exoPlayer.duration.coerceAtLeast(0L)
                    val basePos = if (seekAnchorMs != null && nowMs - lastSeekPressMs < 2000L) {
                        seekAnchorMs!!
                    } else {
                        exoPlayer.currentPosition
                    }
                    val cap = if (duration > 0L) duration else basePos + 10_000
                    val newPos = (basePos + 10_000).coerceAtMost(cap)
                    seekAnchorMs = newPos
                    lastSeekPressMs = nowMs
                    exoPlayer.seekTo(newPos)
                    viewModel.updatePlaybackPosition(newPos, exoPlayer.duration.coerceAtLeast(0L))
                },
                onNextEpisode = {
                    viewModel.playNextEpisode()
                },
                onSeek = { seekFraction ->
                    seekAnchorMs = null
                    lastSeekPressMs = 0L
                    val duration = exoPlayer.duration
                    // duration is -1/0 until the media is prepared; a seek based
                    // on it would jump to a nonsense position, so ignore scrubs
                    // until the player knows the real duration.
                    if (duration > 0L) {
                        val newPos = (seekFraction * duration).toLong().coerceIn(0L, duration)
                        exoPlayer.seekTo(newPos)
                        viewModel.updatePlaybackPosition(newPos, duration)
                    }
                },
                onBackClick = onBackClick
                ,playbackSpeed = playbackSpeed,
                onCyclePlaybackSpeed = {
                    val newSpeed = when (playbackSpeed) {
                        1.0f -> 1.25f
                        1.25f -> 1.5f
                        1.5f -> 2.0f
                        else -> 1.0f
                    }
                    viewModel.setPlaybackSpeed(newSpeed)
                    exoPlayer.setPlaybackSpeed(newSpeed)
                },
                onInteraction = { viewModel.notifyControlsInteraction() },
                onToggleEpisodeList = { viewModel.toggleEpisodeList() },
                playPauseFocusRequester = playPauseFocusRequester
            )
        }

        AnimatedVisibility(
            visible = uiState.isEpisodeListVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            EpisodeListOverlay(
                episodes = uiState.episodes,
                currentEpisodeId = uiState.episodeId,
                onSelect = { ep ->
                    viewModel.notifyControlsInteraction()
                    viewModel.setEpisodeListVisible(false)
                    if (ep.id != uiState.episodeId) viewModel.loadEpisode(ep.id)
                }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerControlsOverlay(
    uiState: PlayerUiState,
    hasNextEpisode: Boolean,
    onPlayPauseToggle: () -> Unit,
    onRewind10: () -> Unit,
    onForward10: () -> Unit,
    onNextEpisode: () -> Unit,
    onSeek: (Float) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    onToggleResizeMode: () -> Unit = {},
    onToggleFavorite: () -> Unit = {}
    ,playbackSpeed: Float = 1.0f,
    onCyclePlaybackSpeed: () -> Unit = {},
    onInteraction: () -> Unit = {},
    onToggleEpisodeList: () -> Unit = {},
    // Owned by PlayerScreen so its container can fall back onto it when the
    // overlay is focused as a whole.
    playPauseFocusRequester: FocusRequester = remember { FocusRequester() }
) {
    val displayAnimeTitle = remember(uiState.currentAnime?.title) {
        ChineseConverter.toTraditionalChinese(uiState.currentAnime?.title)
    }
    val displayEpTitle = remember(uiState.currentEpisode?.title, uiState.currentEpisode?.episodeNumber, uiState.episodeId) {
        val rawTitle = uiState.currentEpisode?.title
        if (!rawTitle.isNullOrBlank()) {
            ChineseConverter.toTraditionalChinese(rawTitle)
        } else {
            val epNum = uiState.currentEpisode?.episodeNumber?.takeIf { it > 0 }
                ?: uiState.episodeId?.substringAfter("_ep_", missingDelimiterValue = "")?.substringBefore("_", missingDelimiterValue = "")?.toIntOrNull()
            if (epNum != null && epNum > 0) "第 $epNum 集" else ""
        }
    }

    val resizeModeLabel = when (resizeMode) {
        AspectRatioFrameLayout.RESIZE_MODE_FIT -> "比例：適合"
        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "比例：鋪滿"
        AspectRatioFrameLayout.RESIZE_MODE_FILL -> "比例：滿版"
        else -> "比例：適合"
    }

    // Show the real stream quality when the source exposes it, otherwise a
    // neutral "高清" badge (never a made-up "1080P").
    val qualityLabel = remember(uiState.videoStream?.quality) {
        val q = uiState.videoStream?.quality?.trim().orEmpty()
        if (q.isBlank() || q.equals("auto", ignoreCase = true)) "高清" else q
    }

    // Explicit D-pad focus graph.
    // The centred play/pause button has no vertically-overlapping neighbour, so
    // Compose's default directional search cannot reach the top bar (or the
    // bottom of the screen) from it. Wiring every neighbour by hand guarantees
    // that all controls are reachable on a remote.
    val backFocusRequester = remember { FocusRequester() }
    val episodeListFocusRequester = remember { FocusRequester() }
    val speedFocusRequester = remember { FocusRequester() }
    val favoriteFocusRequester = remember { FocusRequester() }
    val resizeFocusRequester = remember { FocusRequester() }
    val rewindFocusRequester = remember { FocusRequester() }
    val forwardFocusRequester = remember { FocusRequester() }
    val nextEpisodeFocusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.85f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.90f)
                    )
                )
            )
            .padding(horizontal = 48.dp, vertical = 28.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onInteraction(); onBackClick() },
                modifier = Modifier
                    .focusRequester(backFocusRequester)
                    .focusProperties {
                        down = playPauseFocusRequester
                        right = episodeListFocusRequester
                    },
                colors = OutlinedButtonDefaults.colors(
                    contentColor = TvPureWhite,
                    focusedContainerColor = TvCardFocusHighlight,
                    focusedContentColor = TvPureWhite
                ),
                border = OutlinedButtonDefaults.border(
                    border = Border(
                        border = BorderStroke(1.dp, TvCardBorder),
                        shape = RoundedCornerShape(10.dp)
                    ),
                    focusedBorder = Border(
                        border = BorderStroke(2.dp, TvAccent),
                        shape = RoundedCornerShape(10.dp)
                    )
                ),
                shape = OutlinedButtonDefaults.shape(shape = RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayAnimeTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = TvPureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .background(TvCardBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = qualityLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TvAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = displayEpTitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = TvPureWhite.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { onInteraction(); onToggleEpisodeList() },
                    modifier = Modifier
                        .focusRequester(episodeListFocusRequester)
                        .focusProperties {
                            down = playPauseFocusRequester
                            left = backFocusRequester
                            right = speedFocusRequester
                        },
                    colors = OutlinedButtonDefaults.colors(
                        contentColor = TvPureWhite,
                        focusedContainerColor = TvCardFocusHighlight,
                        focusedContentColor = TvAccent
                    ),
                    border = OutlinedButtonDefaults.border(
                        border = Border(border = BorderStroke(1.dp, TvCardBorder), shape = RoundedCornerShape(8.dp)),
                        focusedBorder = Border(border = BorderStroke(2.dp, TvAccent), shape = RoundedCornerShape(8.dp))
                    ),
                    shape = OutlinedButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = if (uiState.episodes.isNotEmpty()) "選集 (${uiState.episodes.size})" else "選集",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = { onInteraction(); onCyclePlaybackSpeed() },
                    modifier = Modifier
                        .focusRequester(speedFocusRequester)
                        .focusProperties {
                            down = playPauseFocusRequester
                            left = episodeListFocusRequester
                            right = favoriteFocusRequester
                        },
                    colors = OutlinedButtonDefaults.colors(
                        contentColor = TvPureWhite,
                        focusedContainerColor = TvCardFocusHighlight,
                        focusedContentColor = TvAccent
                    ),
                    border = OutlinedButtonDefaults.border(
                        border = Border(border = BorderStroke(1.dp, TvCardBorder), shape = RoundedCornerShape(8.dp)),
                        focusedBorder = Border(border = BorderStroke(2.dp, TvAccent), shape = RoundedCornerShape(8.dp))
                    ),
                    shape = OutlinedButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                ) {
                    Text(text = "速度 ${playbackSpeed}x", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                }

                // Favorite Button in Player Overlay
                OutlinedButton(
                    onClick = { onInteraction(); onToggleFavorite() },
                    modifier = Modifier
                        .focusRequester(favoriteFocusRequester)
                        .focusProperties {
                            down = playPauseFocusRequester
                            left = speedFocusRequester
                            right = resizeFocusRequester
                        },
                    colors = OutlinedButtonDefaults.colors(
                        contentColor = TvPureWhite,
                        focusedContainerColor = TvCardFocusHighlight,
                        focusedContentColor = TvAccent
                    ),
                    border = OutlinedButtonDefaults.border(
                        border = Border(border = BorderStroke(1.dp, TvCardBorder), shape = RoundedCornerShape(8.dp)),
                        focusedBorder = Border(border = BorderStroke(2.dp, TvAccent), shape = RoundedCornerShape(8.dp))
                    ),
                    shape = OutlinedButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Rounded.Check else Icons.Rounded.Add,
                            contentDescription = "Watchlist",
                            tint = if (uiState.isFavorite) TvAccent else TvPureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (uiState.isFavorite) "已在追番" else "追番",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TvPureWhite
                        )
                    }
                }

                // Aspect Ratio Toggle Button
                OutlinedButton(
                    onClick = { onInteraction(); onToggleResizeMode() },
                    modifier = Modifier
                        .focusRequester(resizeFocusRequester)
                        .focusProperties {
                            down = playPauseFocusRequester
                            left = favoriteFocusRequester
                        },
                    colors = OutlinedButtonDefaults.colors(
                        contentColor = TvPureWhite,
                        focusedContainerColor = TvCardFocusHighlight,
                        focusedContentColor = TvAccent
                    ),
                    border = OutlinedButtonDefaults.border(
                        border = Border(border = BorderStroke(1.dp, TvCardBorder), shape = RoundedCornerShape(8.dp)),
                        focusedBorder = Border(border = BorderStroke(2.dp, TvAccent), shape = RoundedCornerShape(8.dp))
                    ),
                    shape = OutlinedButtonDefaults.shape(shape = RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = resizeModeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TvPureWhite
                    )
                }
            }
        }

        // Center D-Pad Action Controls. Focus neighbours are wired explicitly
        // (see the requesters above) because the centred button has no vertical
        // overlap with the top bar.
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { onInteraction(); onRewind10() },
                modifier = Modifier
                    .size(48.dp)
                    .focusRequester(rewindFocusRequester)
                    .focusProperties {
                        right = playPauseFocusRequester
                        up = backFocusRequester
                    },
                shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = TvCardBackground,
                    focusedContainerColor = TvAccent
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Replay10,
                        contentDescription = "-10s",
                        tint = TvPureWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Surface(
                onClick = { onInteraction(); onPlayPauseToggle() },
                modifier = Modifier
                    .size(68.dp)
                    .focusRequester(playPauseFocusRequester)
                    .focusProperties {
                        left = rewindFocusRequester
                        right = forwardFocusRequester
                        up = episodeListFocusRequester
                    },
                shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = TvAccent,
                    focusedContainerColor = TvPureWhite
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        tint = TvBackgroundTop,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Surface(
                onClick = { onInteraction(); onForward10() },
                modifier = Modifier
                    .size(48.dp)
                    .focusRequester(forwardFocusRequester)
                    .focusProperties {
                        left = playPauseFocusRequester
                        right = if (hasNextEpisode) nextEpisodeFocusRequester else FocusRequester.Default
                        up = speedFocusRequester
                    },
                shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = TvCardBackground,
                    focusedContainerColor = TvAccent
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.15f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.Forward10,
                        contentDescription = "+10s",
                        tint = TvPureWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            if (hasNextEpisode) {
                Surface(
                    onClick = { onInteraction(); onNextEpisode() },
                    modifier = Modifier
                        .height(48.dp)
                        .focusRequester(nextEpisodeFocusRequester)
                        .focusProperties {
                            left = forwardFocusRequester
                            up = favoriteFocusRequester
                        },
                    shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(24.dp)),
                    colors = ClickableSurfaceDefaults.colors(
                        containerColor = TvCardBackground,
                        focusedContainerColor = TvAccent
                    ),
                    scale = ClickableSurfaceDefaults.scale(focusedScale = 1.10f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "下一集",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = TvPureWhite
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.NavigateNext,
                            contentDescription = "Next Episode",
                            tint = TvPureWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Bottom Timeline
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            val positionText = formatTimeMs(uiState.playbackPosition)
            val durationText = formatTimeMs(uiState.duration)
            val progressFraction = if (uiState.duration > 0) {
                (uiState.playbackPosition.toFloat() / uiState.duration.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = positionText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TvPureWhite
                )

                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = TvPureWhite.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = progressFraction,
                onValueChange = { onSeek(it) },
                colors = SliderDefaults.colors(
                    thumbColor = TvAccent,
                    activeTrackColor = TvAccent,
                    inactiveTrackColor = TvPureWhite.copy(alpha = 0.25f)
                ),
                // 遙控器：進度條不可搶焦點（否則左右鍵被吃掉、按不到按鈕）。
                // 快進/快退請用 -10s/+10s 按鈕或隱藏控制列時的左右鍵。
                modifier = Modifier
                    .fillMaxWidth()
                    .focusProperties { canFocus = false }
            )
        }
    }
}

/**
 * Full-screen episode picker that opens from the player's "選集" button.
 * Focus lands on the currently playing episode; the list scrolls to it.
 */
@Composable
fun EpisodeListOverlay(
    episodes: List<Episode>,
    currentEpisodeId: String?,
    onSelect: (Episode) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val currentIndex = remember(episodes, currentEpisodeId) {
        episodes.indexOfFirst { it.id == currentEpisodeId }
    }
    val currentItemRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }

    LaunchedEffect(episodes.size, currentEpisodeId) {
        if (currentIndex >= 0) {
            listState.scrollToItem((currentIndex - 3).coerceAtLeast(0))
        }
        // Wait for the target item to be composed, then move focus onto it.
        // If the current episode isn't in the list (e.g. still loading), fall
        // back to focusing the list itself so the D-pad still works.
        delay(80)
        runCatching {
            if (currentIndex >= 0) currentItemRequester.requestFocus()
            else listFocusRequester.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 80.dp, vertical = 40.dp)
        ) {
            Text(
                text = if (episodes.isEmpty()) "集數載入中…" else "選擇集數（共 ${episodes.size} 集）",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TvPureWhite
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "按返回鍵可關閉",
                style = MaterialTheme.typography.bodySmall,
                color = TvPureWhite.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(listFocusRequester)
            ) {
                items(episodes, key = { it.id }) { ep ->
                    val isCurrent = ep.id == currentEpisodeId
                    Surface(
                        onClick = { onSelect(ep) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isCurrent) Modifier.focusRequester(currentItemRequester)
                                else Modifier
                            ),
                        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(10.dp)),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isCurrent) TvAccent else TvCardBackground,
                            focusedContainerColor = TvCardFocusHighlight
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "第 ${ep.episodeNumber} 集",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = if (isCurrent) TvBackgroundTop else TvPureWhite
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = ChineseConverter.toTraditionalChinese(ep.title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isCurrent) TvBackgroundTop else TvPureWhite.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isCurrent) {
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "播放中",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = TvBackgroundTop
                                )
                            }
                        }
                    }
                }
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
fun PlayerScreenPreview() {
    AnimetvoneTheme {
        PlayerControlsOverlay(
            uiState = PlayerUiState(
                currentAnime = SampleAnimeData.sampleAnimes.first(),
                currentEpisode = SampleAnimeData.sampleEpisodesMap.values.first().first(),
                isPlaying = true,
                playbackPosition = 125000,
                duration = 1440000
            ),
            hasNextEpisode = true,
            onPlayPauseToggle = {},
            onRewind10 = {},
            onForward10 = {},
            onNextEpisode = {},
            onSeek = {},
            onBackClick = {}
        )
    }
}
