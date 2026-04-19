package com.dramaflow.feature.feed

import android.graphics.Color as AndroidColor
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.dramaflow.core.designsystem.theme.DramaFlowThemeTokens
import com.dramaflow.core.player.DefaultMedia3PlayerBridge
import com.dramaflow.core.player.Media3PlayerBridge
import com.dramaflow.core.player.MediaPlaybackSource
import com.dramaflow.core.player.PlayerEvent
import com.dramaflow.core.player.PlayerResumeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext

private const val FeedPreviewLogTag = "FeedPreview"

data class PreviewPlaybackState(
    val mediaId: String? = null,
    val isBuffering: Boolean = false,
    val isReady: Boolean = false,
    val firstFrameRendered: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val isFallbackOnly: Boolean = false,
)

@Stable
class FeedPreviewPlayerController(
    private val bridge: Media3PlayerBridge,
    private val scope: CoroutineScope,
) {
    private val _uiState = MutableStateFlow(PreviewPlaybackState())
    val uiState: StateFlow<PreviewPlaybackState> = _uiState.asStateFlow()

    private var activePreview: RecommendPreviewMedia? = null
    private var activationJob: Job? = null
    private var observerJob: Job? = null
    private var eventJob: Job? = null

    init {
        observerJob = scope.launch {
            bridge.playbackState.collect { playbackState ->
                val current = _uiState.value
                if (current.mediaId == null || current.isFallbackOnly) return@collect
                _uiState.value = current.copy(
                    isBuffering = playbackState.isBuffering,
                    isReady = playbackState.isPrepared && !playbackState.isBuffering && playbackState.errorMessage == null,
                    hasError = playbackState.errorMessage != null,
                    errorMessage = playbackState.errorMessage,
                )
            }
        }
        eventJob = scope.launch {
            bridge.events.collect { event ->
                when (event) {
                    PlayerEvent.FirstFrameRendered -> {
                        Log.d(FeedPreviewLogTag, "preview_first_frame media=${activePreview?.mediaId}")
                        _uiState.value = _uiState.value.copy(
                            firstFrameRendered = true,
                            isBuffering = false,
                            hasError = false,
                            errorMessage = null,
                        )
                    }

                    is PlayerEvent.PlaybackError -> {
                        Log.d(FeedPreviewLogTag, "preview_error media=${activePreview?.mediaId} message=${event.message}")
                        _uiState.value = _uiState.value.copy(
                            hasError = true,
                            isBuffering = false,
                            errorMessage = event.message,
                        )
                    }

                    else -> Unit
                }
            }
        }
    }

    val player: Player
        get() = bridge.player

    fun isAttachedTo(preview: RecommendPreviewMedia): Boolean {
        return _uiState.value.mediaId == preview.mediaId && !preview.previewUrl.isNullOrBlank()
    }

    fun activate(preview: RecommendPreviewMedia?, autoplayEnabled: Boolean) {
        activationJob?.cancel()
        if (!autoplayEnabled || preview == null) {
            pause()
            return
        }
        activePreview = preview
        if (preview.previewUrl.isNullOrBlank()) {
            _uiState.value = PreviewPlaybackState(
                mediaId = preview.mediaId,
                isFallbackOnly = true,
            )
            return
        }
        if (_uiState.value.mediaId == preview.mediaId && !_uiState.value.hasError) {
            bridge.player.volume = 0f
            bridge.player.repeatMode = Player.REPEAT_MODE_ONE
            bridge.dispatch(com.dramaflow.core.player.PlaybackAction.Play)
            return
        }
        _uiState.value = PreviewPlaybackState(
            mediaId = preview.mediaId,
            isBuffering = true,
        )
        activationJob = scope.launch {
            delay(preview.autoplayDelayMs)
            Log.d(FeedPreviewLogTag, "preview_prepare_start media=${preview.mediaId}")
            bridge.player.volume = 0f
            bridge.player.repeatMode = Player.REPEAT_MODE_ONE
            bridge.prepare(
                source = MediaPlaybackSource(mediaUrl = preview.previewUrl),
                resumeState = PlayerResumeState(positionMs = 0L, shouldAutoResume = false),
                playWhenReady = true,
            )
        }
    }

    fun retry() {
        val preview = activePreview ?: return
        activate(preview = preview, autoplayEnabled = true)
    }

    fun pause() {
        activationJob?.cancel()
        bridge.dispatch(com.dramaflow.core.player.PlaybackAction.Pause)
    }

    fun release() {
        activationJob?.cancel()
        observerJob?.cancel()
        eventJob?.cancel()
        bridge.release()
    }
}

@Composable
fun rememberFeedPreviewPlayerController(): FeedPreviewPlayerController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(context, scope) {
        FeedPreviewPlayerController(
            bridge = DefaultMedia3PlayerBridge(context),
            scope = scope,
        )
    }
    DisposableEffect(controller) {
        onDispose {
            controller.release()
        }
    }
    return controller
}

@Composable
fun FeedPreviewPlayer(
    preview: RecommendPreviewMedia,
    isActive: Boolean,
    controller: FeedPreviewPlayerController,
    modifier: Modifier = Modifier,
    onOpenPlayer: () -> Unit,
) {
    val previewState by controller.uiState.collectAsState()
    val colors = DramaFlowThemeTokens.colors
    val spacing = DramaFlowThemeTokens.spacing
    val shouldRenderPlayer = isActive && controller.isAttachedTo(preview)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onOpenPlayer),
    ) {
        AsyncImage(
            model = preview.coverUrl,
            contentDescription = preview.mediaId,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        if (shouldRenderPlayer) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = android.view.ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                        player = controller.player
                    }
                },
                update = { view ->
                    view.player = controller.player
                },
            )
        }

        if (preview.previewUrl != null && isActive) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = spacing.lg, bottom = 152.dp),
                shape = DramaFlowThemeTokens.shapes.pill,
                color = colors.surface.copy(alpha = 0.82f),
            ) {
                Text(
                    text = "Muted Preview",
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
                )
            }
        }

        if (previewState.isBuffering && shouldRenderPlayer) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .wrapContentSize(),
                shape = DramaFlowThemeTokens.shapes.pill,
                color = colors.surface.copy(alpha = 0.88f),
            ) {
                Text(
                    text = "Loading preview...",
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                )
            }
        }

        if (previewState.hasError && shouldRenderPlayer) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clickable { controller.retry() },
                shape = DramaFlowThemeTokens.shapes.large,
                color = colors.surface.copy(alpha = 0.92f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Preview unavailable",
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "Tap to retry",
                        color = colors.accentStrong,
                    )
                }
            }
        }

        if (preview.previewUrl == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = spacing.lg, bottom = 152.dp)
                    .background(colors.surface.copy(alpha = 0.82f), DramaFlowThemeTokens.shapes.pill),
            ) {
                Text(
                    text = "Poster only",
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
                )
            }
        }
    }
}
