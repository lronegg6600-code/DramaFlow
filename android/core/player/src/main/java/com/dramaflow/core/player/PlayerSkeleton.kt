package com.dramaflow.core.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.dramaflow.core.model.EntitlementState
import com.dramaflow.core.model.Episode
import com.dramaflow.core.model.NextEpisodeHint
import com.dramaflow.core.model.PreviewLimit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface PlaybackAction {
    data object Play : PlaybackAction
    data object Pause : PlaybackAction
    data class SeekTo(val positionMs: Long) : PlaybackAction
    data object Retry : PlaybackAction
    data object TogglePlayPause : PlaybackAction
}

sealed interface PlayerEvent {
    data object FirstFrameRendered : PlayerEvent
    data object PlaybackCompleted : PlayerEvent
    data class PlaybackError(val message: String) : PlayerEvent
    data class PreviewLimitReached(val previewLimit: PreviewLimit) : PlayerEvent
    data class AutoNextAvailable(val hint: NextEpisodeHint) : PlayerEvent
}

enum class PlayerAnalyticsEvent {
    PLAYBACK_START,
    FIRST_FRAME,
    PREVIEW_LIMIT_REACHED,
    PAYWALL_SHOWN,
    PLAYBACK_ERROR,
    EPISODE_COMPLETE,
    AUTO_NEXT_TRIGGERED,
    PROGRESS_SAVED,
    PLAYBACK_SESSION_REQUESTED,
    PLAYBACK_SESSION_READY,
    PLAYBACK_SESSION_REFRESH,
    PLAYBACK_SESSION_FAILED,
    PLAYBACK_HEARTBEAT_SENT,
    PLAYBACK_COMPLETE_SENT,
    PLAYBACK_FALLBACK_USED,
    NEXT_EPISODE_PRELOAD_REQUESTED,
}

data class PlayerResumeState(
    val positionMs: Long,
    val shouldAutoResume: Boolean,
)

data class PlaybackUiState(
    val isPrepared: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val hasEnded: Boolean = false,
    val errorMessage: String? = null,
)

data class BridgePlaybackState(
    val isPrepared: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val hasEnded: Boolean = false,
    val errorMessage: String? = null,
)

interface Media3PlayerBridge {
    val player: Player
    val playbackState: StateFlow<BridgePlaybackState>
    val events: SharedFlow<PlayerEvent>
    fun prepare(
        source: MediaPlaybackSource,
        resumeState: PlayerResumeState,
        playWhenReady: Boolean,
    )
    fun dispatch(action: PlaybackAction)
    fun syncPosition()
    fun release()
}

class DefaultMedia3PlayerBridge(context: Context) : Media3PlayerBridge {
    private val appContext = context.applicationContext
    private val exoPlayer = ExoPlayer.Builder(appContext).build()
    private val _playbackState = MutableStateFlow(BridgePlaybackState())
    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 16)
    private var firstFrameEmitted = false

    override val player: Player = exoPlayer
    override val playbackState: StateFlow<BridgePlaybackState> = _playbackState.asStateFlow()
    override val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()

    init {
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    publishState()
                    if (playbackState == Player.STATE_READY && !firstFrameEmitted) {
                        firstFrameEmitted = true
                        _events.tryEmit(PlayerEvent.FirstFrameRendered)
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        _events.tryEmit(PlayerEvent.PlaybackCompleted)
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    publishState()
                }

                override fun onPlayerError(error: PlaybackException) {
                    publishState(error.message)
                    _events.tryEmit(PlayerEvent.PlaybackError(error.message ?: "Playback error"))
                }
            },
        )
    }

    override fun prepare(
        source: MediaPlaybackSource,
        resumeState: PlayerResumeState,
        playWhenReady: Boolean,
    ) {
        firstFrameEmitted = false
        val httpFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(Media3RequestHeaderFactory.build(source.requestHeaders))
        val dataSourceFactory = DefaultDataSource.Factory(appContext, httpFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        val mediaItem = MediaItem.fromUri(source.mediaUrl)
        exoPlayer.setMediaSource(mediaSourceFactory.createMediaSource(mediaItem))
        exoPlayer.prepare()
        if (resumeState.positionMs > 0L) {
            exoPlayer.seekTo(resumeState.positionMs)
        }
        exoPlayer.playWhenReady = playWhenReady || resumeState.shouldAutoResume
        publishState()
    }

    override fun dispatch(action: PlaybackAction) {
        when (action) {
            PlaybackAction.Play -> exoPlayer.play()
            PlaybackAction.Pause -> exoPlayer.pause()
            is PlaybackAction.SeekTo -> exoPlayer.seekTo(action.positionMs)
            PlaybackAction.Retry -> {
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
            PlaybackAction.TogglePlayPause -> if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
        }
        publishState()
    }

    override fun syncPosition() {
        publishState()
    }

    override fun release() {
        exoPlayer.release()
    }

    private fun publishState(errorMessage: String? = null) {
        _playbackState.value = PlaybackStateMapper.fromPlayer(exoPlayer, errorMessage)
    }
}

object PlaybackStateMapper {
    fun fromPlayer(player: Player, errorMessage: String? = null): BridgePlaybackState {
        return BridgePlaybackState(
            isPrepared = player.playbackState != Player.STATE_IDLE,
            isPlaying = player.isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            durationMs = if (player.duration == C.TIME_UNSET) 0L else player.duration,
            positionMs = player.currentPosition,
            bufferedPositionMs = player.bufferedPosition,
            hasEnded = player.playbackState == Player.STATE_ENDED,
            errorMessage = errorMessage,
        )
    }
}

class PreviewPolicy {
    fun previewLimitFor(
        episode: Episode,
        entitlementState: EntitlementState,
        positionMs: Long,
    ): PreviewLimit? {
        val previewWindow = episode.previewWindowSeconds ?: return null
        if (!episode.requiresPremium || entitlementState.isPremium) return null
        val remaining = previewWindow - (positionMs / 1000L).toInt()
        return if (remaining <= 0) {
            PreviewLimit(
                previewWindowSeconds = previewWindow,
                remainingSeconds = 0,
                blockReason = "Preview ended. Start subscription to continue instantly.",
            )
        } else {
            PreviewLimit(
                previewWindowSeconds = previewWindow,
                remainingSeconds = remaining,
                blockReason = "Preview ends in a few seconds.",
            )
        }
    }
}

class FakePlaybackPolicy(
    private val previewPolicy: PreviewPolicy = PreviewPolicy(),
) {
    fun shouldPauseForPreview(
        episode: Episode,
        entitlementState: EntitlementState,
        positionMs: Long,
    ): PreviewLimit? {
        return previewPolicy.previewLimitFor(episode, entitlementState, positionMs)
            ?.takeIf { it.remainingSeconds <= 0 }
    }
}

class AutoNextCoordinator {
    fun buildHint(
        currentEpisode: Episode,
        nextEpisode: Episode?,
        playbackState: BridgePlaybackState,
    ): NextEpisodeHint? {
        if (nextEpisode == null) return null
        val remainingMs = playbackState.durationMs - playbackState.positionMs
        return if (playbackState.hasEnded || (playbackState.durationMs > 0 && remainingMs in 1..5000)) {
            NextEpisodeHint(
                episodeId = nextEpisode.id,
                title = "Episode ${nextEpisode.episodeNumber}",
                autoPlayInSeconds = if (playbackState.hasEnded) 0 else 5,
            )
        } else {
            null
        }
    }
}

class ProgressTicker {
    fun start(
        scope: CoroutineScope,
        onTick: suspend () -> Unit,
    ): Job {
        return scope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                onTick()
                delay(1_000)
            }
        }
    }
}
