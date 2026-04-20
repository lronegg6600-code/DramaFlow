package com.dramaflow.feature.player

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dramaflow.core.common.AppEnvironment
import com.dramaflow.core.common.DataResult
import com.dramaflow.core.common.DramaFlowMockData
import com.dramaflow.core.common.EntitlementRepository
import com.dramaflow.core.common.PlaybackRepository
import com.dramaflow.core.common.ProgressRepository
import com.dramaflow.core.designsystem.component.DfPrimaryButton
import com.dramaflow.core.designsystem.component.DfWhiteMessageCard
import com.dramaflow.core.designsystem.theme.DramaFlowTheme
import com.dramaflow.core.designsystem.theme.DramaFlowThemeTokens
import com.dramaflow.core.model.Drama
import com.dramaflow.core.model.EntitlementState
import com.dramaflow.core.model.Episode
import com.dramaflow.core.model.NextEpisodeHint
import com.dramaflow.core.model.PlaybackDataMode
import com.dramaflow.core.model.PreviewLimit
import com.dramaflow.core.model.WatchProgress
import com.dramaflow.core.model.canAccessEpisode
import com.dramaflow.core.player.AutoNextCoordinator
import com.dramaflow.core.player.BridgePlaybackState
import com.dramaflow.core.player.DefaultMedia3PlayerBridge
import com.dramaflow.core.player.Media3PlayerBridge
import com.dramaflow.core.player.PlaybackAction
import com.dramaflow.core.player.PlaybackPreloadCoordinator
import com.dramaflow.core.player.PlayerAnalyticsEvent
import com.dramaflow.core.player.PlayerEvent
import com.dramaflow.core.player.PlayerResumeState
import com.dramaflow.core.player.PreviewPolicy
import com.dramaflow.core.player.ProgressTicker
import com.dramaflow.core.player.RemotePlaybackCoordinator
import com.dramaflow.core.player.toMediaPlaybackSource
import com.dramaflow.core.ui.DfLoadState
import com.dramaflow.core.ui.DfScreenScaffold
import com.dramaflow.core.ui.DfStateLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PlayerEntitlementLogTag = "PlayerEntitlement"

enum class PlayerRemoteMode {
    FAKE_ONLY,
    HYBRID,
    REMOTE_PLAYBACK,
}

data class PlayerUiState(
    val loadState: DfLoadState = DfLoadState.LOADING,
    val drama: Drama? = null,
    val episode: Episode? = null,
    val entitlementState: EntitlementState = EntitlementState(false, null, emptyList(), "free"),
    val playbackState: BridgePlaybackState = BridgePlaybackState(),
    val previewLimit: PreviewLimit? = null,
    val previewCountdown: Int? = null,
    val paywallVisible: Boolean = false,
    val nextEpisodeHint: NextEpisodeHint? = null,
    val analyticsEvents: List<PlayerAnalyticsEvent> = emptyList(),
    val currentEpisodeLabel: String = "",
    val episodeSelectorSummary: String = "",
    val episodeSheetVisible: Boolean = false,
    val episodeItems: List<PlayerEpisodeItem> = emptyList(),
    val errorMessage: String = "Playback failed to initialize.",
    val sessionId: String? = null,
    val isRemotePlayback: Boolean = false,
    val remoteMode: PlayerRemoteMode = PlayerRemoteMode.FAKE_ONLY,
)

data class PlayerEpisodeItem(
    val episodeId: String,
    val episodeNumber: Int,
    val title: String,
    val isCurrent: Boolean,
    val isCompleted: Boolean,
    val isLocked: Boolean,
    val accessibilityLabel: String,
)

sealed interface PlayerAction {
    data object TogglePlayPause : PlayerAction
    data object Retry : PlayerAction
    data object UnlockPremium : PlayerAction
    data object PlayNextNow : PlayerAction
    data object ShowEpisodeSheet : PlayerAction
    data object HideEpisodeSheet : PlayerAction
    data class SelectEpisode(val episodeId: String) : PlayerAction
    data object AppStarted : PlayerAction
    data object AppStopped : PlayerAction
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val progressRepository: ProgressRepository,
    private val entitlementRepository: EntitlementRepository,
    private val remotePlaybackCoordinator: RemotePlaybackCoordinator,
    private val preloadCoordinator: PlaybackPreloadCoordinator,
    val media3PlayerBridge: Media3PlayerBridge,
    savedStateHandle: SavedStateHandle,
) : androidx.lifecycle.ViewModel() {
    private val autoNextCoordinator = AutoNextCoordinator()
    private val previewPolicy = PreviewPolicy()
    private val progressTicker = ProgressTicker()
    private val episodeIdArg: String = savedStateHandle["episodeId"] ?: "df-neon-vows-e4"
    private val _uiState = MutableStateFlow(
        PlayerUiState(
            remoteMode = when (AppEnvironment.current.playbackDataMode) {
                PlaybackDataMode.FAKE_ONLY -> PlayerRemoteMode.FAKE_ONLY
                PlaybackDataMode.HYBRID -> PlayerRemoteMode.HYBRID
                PlaybackDataMode.REMOTE_PLAYBACK -> PlayerRemoteMode.REMOTE_PLAYBACK
            },
        ),
    )
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentEpisodeId: String = episodeIdArg
    private var tickerJob: Job? = null
    private var lastHeartbeatSecond: Int = -1
    private var preloadedEpisodeId: String? = null

    init {
        observeEntitlement()
        observeBridge()
        observeBridgeEvents()
        loadEpisode(currentEpisodeId, autoPlay = true)
    }

    fun onAction(action: PlayerAction) {
        when (action) {
            PlayerAction.TogglePlayPause -> media3PlayerBridge.dispatch(PlaybackAction.TogglePlayPause)
            PlayerAction.Retry -> loadEpisode(currentEpisodeId, autoPlay = true)
            PlayerAction.UnlockPremium -> _uiState.update { it.copy(paywallVisible = true) }
            PlayerAction.PlayNextNow -> uiState.value.nextEpisodeHint?.episodeId?.let(::switchToEpisode)
            PlayerAction.ShowEpisodeSheet -> _uiState.update { it.copy(episodeSheetVisible = true) }
            PlayerAction.HideEpisodeSheet -> _uiState.update { it.copy(episodeSheetVisible = false) }
            is PlayerAction.SelectEpisode -> {
                _uiState.update { it.copy(episodeSheetVisible = false) }
                switchToEpisode(action.episodeId)
            }
            PlayerAction.AppStarted -> {
                if (uiState.value.playbackState.isPrepared && !uiState.value.paywallVisible) {
                    media3PlayerBridge.dispatch(PlaybackAction.Play)
                }
                startTicker()
            }
            PlayerAction.AppStopped -> {
                tickerJob?.cancel()
                media3PlayerBridge.dispatch(PlaybackAction.Pause)
                persistCurrentProgress(markComplete = false)
            }
        }
    }

    override fun onCleared() {
        persistCurrentProgress(markComplete = uiState.value.playbackState.hasEnded)
        tickerJob?.cancel()
        media3PlayerBridge.dispatch(PlaybackAction.Pause)
        super.onCleared()
    }

    private fun observeEntitlement() {
        viewModelScope.launch {
            entitlementRepository.observeEntitlement().collect { entitlement ->
                val episode = uiState.value.episode
                _uiState.update { current ->
                    val shouldDismissPaywall =
                        current.paywallVisible && episode != null && entitlement.canAccessEpisode(episode)
                    current.copy(
                        entitlementState = entitlement,
                        episodeItems = refreshEpisodeItems(
                            current.episodeItems,
                            entitlement = entitlement,
                            currentEpisodeId = currentEpisodeId,
                        ),
                        episodeSelectorSummary = buildEpisodeSelectorSummary(
                            episodeCount = current.episodeItems.size,
                            currentEpisode = episode,
                            entitlement = entitlement,
                        ),
                        paywallVisible = if (shouldDismissPaywall) false else current.paywallVisible,
                        previewLimit = if (shouldDismissPaywall) null else current.previewLimit,
                        previewCountdown = if (shouldDismissPaywall) null else current.previewCountdown,
                    )
                }
                if (episode != null && entitlement.canAccessEpisode(episode) && uiState.value.playbackState.isPrepared) {
                    media3PlayerBridge.dispatch(PlaybackAction.Play)
                }
            }
        }
    }

    private fun observeBridge() {
        viewModelScope.launch {
            media3PlayerBridge.playbackState.collect { bridgeState ->
                _uiState.update { state -> state.copy(playbackState = bridgeState) }
            }
        }
    }

    private fun observeBridgeEvents() {
        viewModelScope.launch {
            media3PlayerBridge.events.collect { event ->
                when (event) {
                    PlayerEvent.FirstFrameRendered -> appendAnalytics(PlayerAnalyticsEvent.FIRST_FRAME)
                    PlayerEvent.PlaybackCompleted -> {
                        appendAnalytics(PlayerAnalyticsEvent.EPISODE_COMPLETE)
                        sendCompletion()
                        persistCurrentProgress(markComplete = true)
                        uiState.value.nextEpisodeHint?.episodeId?.let(::switchToEpisode)
                    }
                    is PlayerEvent.PlaybackError -> {
                        _uiState.update { it.copy(errorMessage = event.message) }
                    }
                    is PlayerEvent.PreviewLimitReached -> {
                        appendAnalytics(PlayerAnalyticsEvent.PREVIEW_LIMIT_REACHED)
                        Log.d(
                            PlayerEntitlementLogTag,
                            "player_paywall_shown episode=${uiState.value.episode?.id} source=player_event premium=${uiState.value.entitlementState.isPremium}",
                        )
                        _uiState.update {
                            it.copy(
                                previewLimit = event.previewLimit,
                                previewCountdown = 0,
                                paywallVisible = true,
                            )
                        }
                    }
                    is PlayerEvent.AutoNextAvailable -> _uiState.update { it.copy(nextEpisodeHint = event.hint) }
                }
            }
        }
    }

    private fun loadEpisode(
        episodeId: String,
        autoPlay: Boolean,
    ) {
        currentEpisodeId = episodeId
        lastHeartbeatSecond = -1
        preloadedEpisodeId = null
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loadState = DfLoadState.LOADING,
                    paywallVisible = false,
                    previewLimit = null,
                    previewCountdown = null,
                    sessionId = null,
                )
            }
            appendAnalytics(PlayerAnalyticsEvent.PLAYBACK_SESSION_REQUESTED)
            when (val result = playbackRepository.loadPlaybackSession(episodeId)) {
                DataResult.Loading -> _uiState.update { it.copy(loadState = DfLoadState.LOADING) }
                DataResult.Empty -> _uiState.update { it.copy(loadState = DfLoadState.EMPTY) }
                is DataResult.Error -> {
                    appendAnalytics(PlayerAnalyticsEvent.PLAYBACK_SESSION_FAILED)
                    _uiState.update { it.copy(loadState = DfLoadState.ERROR, errorMessage = result.message) }
                }
                is DataResult.Success -> {
                    val session = result.value
                    val descriptor = session.playbackDescriptor
                    val nextHint = descriptor?.nextEpisodeHint ?: session.nextEpisode?.let {
                        NextEpisodeHint(it.id, "Episode ${it.episodeNumber}", 5)
                    }
                    val episodeItems = buildEpisodeItems(
                        dramaId = session.drama.id,
                        currentEpisodeId = session.episode.id,
                        entitlement = session.entitlementState,
                    )
                    _uiState.update {
                        it.copy(
                            loadState = DfLoadState.SUCCESS,
                            drama = session.drama,
                            episode = session.episode,
                            entitlementState = session.entitlementState,
                            nextEpisodeHint = nextHint,
                            currentEpisodeLabel = formatEpisodeLabel(session.episode),
                            episodeSelectorSummary = buildEpisodeSelectorSummary(
                                episodeCount = episodeItems.size,
                                currentEpisode = session.episode,
                                entitlement = session.entitlementState,
                            ),
                            episodeItems = episodeItems,
                            episodeSheetVisible = false,
                            errorMessage = "",
                            sessionId = descriptor?.sessionId,
                            isRemotePlayback = session.isRemotePlayback,
                        )
                    }
                    if (!session.isRemotePlayback && AppEnvironment.current.playbackDataMode != PlaybackDataMode.FAKE_ONLY) {
                        appendAnalytics(PlayerAnalyticsEvent.PLAYBACK_FALLBACK_USED)
                    } else {
                        appendAnalytics(PlayerAnalyticsEvent.PLAYBACK_SESSION_READY)
                    }
                    media3PlayerBridge.prepare(
                        source = descriptor?.toMediaPlaybackSource()
                            ?: com.dramaflow.core.player.MediaPlaybackSource(session.episode.streamUrl),
                        resumeState = PlayerResumeState(
                            positionMs = session.savedProgress?.positionMs ?: 0L,
                            shouldAutoResume = autoPlay,
                        ),
                        playWhenReady = autoPlay,
                    )
                    appendAnalytics(PlayerAnalyticsEvent.PLAYBACK_START)
                    progressRepository.setLastPlayedEpisodeId(session.episode.id)
                    startTicker()
                }
            }
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = progressTicker.start(viewModelScope) {
            media3PlayerBridge.syncPosition()
            evaluatePlayback()
        }
    }

    private suspend fun evaluatePlayback() {
        val state = uiState.value
        val episode = state.episode ?: return
        val bridgeState = state.playbackState

        val preview = previewPolicy.previewLimitFor(episode, state.entitlementState, bridgeState.positionMs)
        val nextEpisode = state.nextEpisodeHint?.episodeId?.let(DramaFlowMockData::findEpisode) ?: playbackRepository.nextEpisodeFor(episode.id)
        val nextHint = autoNextCoordinator.buildHint(episode, nextEpisode, bridgeState) ?: state.nextEpisodeHint
        _uiState.update {
            it.copy(
                previewCountdown = preview?.remainingSeconds?.takeIf { remaining -> remaining in 1..10 },
                nextEpisodeHint = nextHint,
            )
        }

        if (!state.paywallVisible) {
            preview?.takeIf { it.remainingSeconds <= 0 }?.let { blocked ->
                media3PlayerBridge.dispatch(PlaybackAction.Pause)
                _uiState.update { current ->
                    current.copy(previewLimit = blocked, previewCountdown = 0, paywallVisible = true)
                }
                appendAnalytics(PlayerAnalyticsEvent.PAYWALL_SHOWN)
                Log.d(
                    PlayerEntitlementLogTag,
                    "player_paywall_shown episode=${episode.id} source=preview_policy premium=${state.entitlementState.isPremium}",
                )
                return
            }
        }

        if (state.isRemotePlayback && state.sessionId != null) {
            val positionSeconds = (bridgeState.positionMs / 1000L).toInt()
            val useHeartbeatEvery = state.episode?.previewWindowSeconds?.let { maxOf(5, minOf(it, 15)) } ?: 15
            if (positionSeconds > 0 && positionSeconds % useHeartbeatEvery == 0 && positionSeconds != lastHeartbeatSecond) {
                lastHeartbeatSecond = positionSeconds
                remotePlaybackCoordinator.heartbeat(
                    sessionId = state.sessionId,
                    positionSeconds = positionSeconds,
                    bufferedPositionSeconds = (bridgeState.bufferedPositionMs / 1000L).toInt(),
                    isPlaying = bridgeState.isPlaying,
                    playerState = when {
                        bridgeState.isBuffering -> "buffering"
                        bridgeState.hasEnded -> "ended"
                        bridgeState.isPlaying -> "playing"
                        else -> "paused"
                    },
                )?.let { heartbeat ->
                    appendAnalytics(PlayerAnalyticsEvent.PLAYBACK_HEARTBEAT_SENT)
                    if (heartbeat.shouldRefreshUrl) {
                        playbackRepository.refreshPlaybackSession(state.sessionId)?.let { refreshed ->
                            appendAnalytics(PlayerAnalyticsEvent.PLAYBACK_SESSION_REFRESH)
                            val descriptor = refreshed.playbackDescriptor
                            if (descriptor != null) {
                                media3PlayerBridge.prepare(
                                    source = descriptor.toMediaPlaybackSource(),
                                    resumeState = PlayerResumeState(
                                        positionMs = bridgeState.positionMs,
                                        shouldAutoResume = bridgeState.isPlaying,
                                    ),
                                    playWhenReady = bridgeState.isPlaying,
                                )
                                _uiState.update {
                                    it.copy(
                                        episode = refreshed.episode,
                                        sessionId = descriptor.sessionId,
                                    )
                                }
                            }
                        }
                    }
                    if (heartbeat.previewRemainingSeconds != null) {
                        _uiState.update { it.copy(previewCountdown = heartbeat.previewRemainingSeconds) }
                    }
                    if (heartbeat.nextAction == "show_paywall") {
                        media3PlayerBridge.dispatch(PlaybackAction.Pause)
                        _uiState.update {
                            it.copy(
                                paywallVisible = true,
                                previewLimit = PreviewLimit(
                                    previewWindowSeconds = episode.previewWindowSeconds ?: 0,
                                    remainingSeconds = 0,
                                    blockReason = "Preview ended. Start subscription to continue instantly.",
                                ),
                            )
                        }
                        Log.d(
                            PlayerEntitlementLogTag,
                            "player_paywall_shown episode=${episode.id} source=remote_heartbeat premium=${state.entitlementState.isPremium}",
                        )
                    }
                }
            }
        }

        val shouldPreload = bridgeState.durationMs > 0L &&
            !bridgeState.hasEnded &&
            nextHint != null &&
            preloadedEpisodeId != nextHint.episodeId &&
            bridgeState.durationMs - bridgeState.positionMs <= 8_000
        if (shouldPreload) {
            preloadedEpisodeId = nextHint.episodeId
            appendAnalytics(PlayerAnalyticsEvent.NEXT_EPISODE_PRELOAD_REQUESTED)
            preloadCoordinator.preloadNextEpisode(nextHint.episodeId)
        }

        if (bridgeState.durationMs > 0L) {
            persistCurrentProgress(markComplete = bridgeState.hasEnded)
            appendAnalytics(PlayerAnalyticsEvent.PROGRESS_SAVED)
        }
    }

    private fun switchToEpisode(episodeId: String) {
        appendAnalytics(PlayerAnalyticsEvent.AUTO_NEXT_TRIGGERED)
        loadEpisode(episodeId, autoPlay = true)
    }

    private fun sendCompletion() {
        val state = uiState.value
        val sessionId = state.sessionId ?: return
        viewModelScope.launch {
            remotePlaybackCoordinator.complete(
                sessionId = sessionId,
                finalPositionSeconds = (state.playbackState.positionMs / 1000L).toInt(),
                completed = true,
                watchedSeconds = (state.playbackState.positionMs / 1000L).toInt(),
            )
            appendAnalytics(PlayerAnalyticsEvent.PLAYBACK_COMPLETE_SENT)
        }
    }

    private fun persistCurrentProgress(markComplete: Boolean) {
        val state = uiState.value
        val episode = state.episode ?: return
        val drama = state.drama ?: return
        val duration = state.playbackState.durationMs.takeIf { it > 0L } ?: return
        val position = state.playbackState.positionMs.coerceAtLeast(0L)
        viewModelScope.launch {
            val progress = WatchProgress(
                dramaId = drama.id,
                episodeId = episode.id,
                positionMs = if (markComplete) duration else position,
                durationMs = duration,
                progressPercent = if (duration == 0L) 0f else (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                lastUpdatedEpochMs = System.currentTimeMillis(),
                completed = markComplete || position >= duration,
            )
            progressRepository.saveProgress(progress)
            if (markComplete || progress.progressPercent >= 0.9f) {
                progressRepository.saveEpisodeComplete(episode, drama, progress.progressPercent)
            }
        }
    }

    private fun appendAnalytics(event: PlayerAnalyticsEvent) {
        _uiState.update { it.copy(analyticsEvents = (it.analyticsEvents + event).takeLast(14)) }
    }

    private suspend fun buildEpisodeItems(
        dramaId: String,
        currentEpisodeId: String,
        entitlement: EntitlementState,
    ): List<PlayerEpisodeItem> {
        return DramaFlowMockData.episodesForDrama(dramaId).map { episode ->
            val progress = progressRepository.getProgressForEpisode(episode.id)
            PlayerEpisodeItem(
                episodeId = episode.id,
                episodeNumber = episode.episodeNumber,
                title = episode.title,
                isCurrent = episode.id == currentEpisodeId,
                isCompleted = progress?.completed == true || (progress?.progressPercent ?: 0f) >= 0.9f,
                isLocked = !entitlement.canAccessEpisode(episode),
                accessibilityLabel = when {
                    episode.id == currentEpisodeId -> "Current episode"
                    progress?.completed == true -> "Watched"
                    !entitlement.canAccessEpisode(episode) -> "Premium locked"
                    else -> "Available"
                },
            )
        }
    }

    private fun refreshEpisodeItems(
        currentItems: List<PlayerEpisodeItem>,
        entitlement: EntitlementState,
        currentEpisodeId: String,
    ): List<PlayerEpisodeItem> {
        return currentItems.map { item ->
            item.copy(
                isCurrent = item.episodeId == currentEpisodeId,
                isLocked = DramaFlowMockData.findEpisode(item.episodeId)?.let { !entitlement.canAccessEpisode(it) } ?: item.isLocked,
            )
        }
    }
}

private fun formatEpisodeLabel(episode: Episode?): String {
    return episode?.let { "Episode ${it.episodeNumber}" }.orEmpty()
}

private fun buildEpisodeSelectorSummary(
    episodeCount: Int,
    currentEpisode: Episode?,
    entitlement: EntitlementState,
): String {
    if (episodeCount == 0) return "Episodes"
    val accessLabel = when {
        entitlement.isPremium -> "Premium unlocked"
        currentEpisode?.requiresPremium == true -> "Preview available"
        else -> "Free to watch"
    }
    return "Episodes · $episodeCount total · $accessLabel"
}

@Composable
fun PlayerRoute(
    onBack: () -> Unit,
    onUnlock: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAction(PlayerAction.AppStarted)
                Lifecycle.Event.ON_STOP -> viewModel.onAction(PlayerAction.AppStopped)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    PlayerScreen(
        uiState = uiState,
        playerBridge = viewModel.media3PlayerBridge,
        onAction = viewModel::onAction,
        onBack = onBack,
        onUnlock = onUnlock,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    playerBridge: Media3PlayerBridge,
    onAction: (PlayerAction) -> Unit,
    onBack: () -> Unit,
    onUnlock: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    val playbackErrorMessage = uiState.playbackState.errorMessage
    DfScreenScaffold {
        DfStateLayout(
            state = uiState.loadState,
            modifier = Modifier.padding(it),
            errorMessage = uiState.errorMessage,
            emptyTitle = "Episode missing",
            emptyMessage = "This episode is not ready for playback yet.",
            onRetry = { onAction(PlayerAction.Retry) },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .background(colors.background)
                    .padding(spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(DramaFlowThemeTokens.shapes.large)
                        .background(colors.surface),
                ) {
                    AndroidView(
                        factory = { context ->
                            PlayerView(context).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                player = playerBridge.player
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { it.player = playerBridge.player },
                    )

                    PlayerTopOverlay(
                        episodeLabel = uiState.currentEpisodeLabel,
                        onBack = onBack,
                    )

                    EpisodeSelectorBar(
                        summary = uiState.episodeSelectorSummary,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = spacing.lg, vertical = spacing.lg),
                        onClick = { onAction(PlayerAction.ShowEpisodeSheet) },
                    )

                    if (uiState.playbackState.isBuffering) {
                        OverlayMessage(
                            text = "Buffering episode...",
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    uiState.previewCountdown?.let { remaining ->
                        OverlayMessage(
                            text = "Preview ends in ${remaining}s",
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 92.dp),
                        )
                    }
                    if (uiState.paywallVisible && uiState.previewLimit != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(start = spacing.lg, end = spacing.lg, bottom = 92.dp),
                            shape = DramaFlowThemeTokens.shapes.large,
                            color = colors.whiteCard,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(spacing.xl),
                                verticalArrangement = Arrangement.spacedBy(spacing.md),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                                ) {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.warning)
                                    Text("Preview ended", color = colors.textPrimary)
                                }
                                Text(uiState.previewLimit.blockReason, color = colors.textSecondary)
                                DfPrimaryButton(
                                    label = "Start subscription",
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        onAction(PlayerAction.UnlockPremium)
                                        onUnlock()
                                    },
                                )
                            }
                        }
                    }
                }

                Surface(shape = DramaFlowThemeTokens.shapes.medium, color = colors.surface) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        Text(uiState.episode?.synopsis.orEmpty(), color = colors.textPrimary)
                        Text(
                            "Progress ${(uiState.playbackState.positionMs / 1000)}s / ${(uiState.playbackState.durationMs / 1000)}s",
                            color = colors.textSecondary,
                        )
                        Text(
                            if (uiState.isRemotePlayback) "Remote playback session active" else "Fake playback fallback",
                            color = colors.textSecondary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                            DfPrimaryButton(
                                label = if (uiState.playbackState.isPlaying) "Pause" else "Play",
                                modifier = Modifier.weight(1f),
                                onClick = { onAction(PlayerAction.TogglePlayPause) },
                            )
                            DfPrimaryButton(
                                label = "Retry",
                                modifier = Modifier.weight(1f),
                                onClick = { onAction(PlayerAction.Retry) },
                            )
                        }
                    }
                }

                uiState.nextEpisodeHint?.let { next ->
                    Surface(shape = DramaFlowThemeTokens.shapes.medium, color = colors.surface) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.lg),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Up next", color = colors.textPrimary)
                                Text("${next.title} starts automatically", color = colors.textSecondary)
                            }
                            Surface(
                                modifier = Modifier.clip(DramaFlowThemeTokens.shapes.pill).clickable {
                                    onAction(PlayerAction.PlayNextNow)
                                },
                                color = colors.accentSoft,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = colors.accentStrong)
                                    Text(" Play now", color = colors.accentStrong)
                                }
                            }
                        }
                    }
                }

                if (playbackErrorMessage != null) {
                    Surface(shape = DramaFlowThemeTokens.shapes.medium, color = colors.surface) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = colors.warning)
                            Text(playbackErrorMessage, color = colors.textSecondary)
                        }
                    }
                }

                DfWhiteMessageCard(
                    title = "Playback events",
                    body = uiState.analyticsEvents.joinToString(separator = " | ") { it.name.lowercase() },
                )
            }

            if (uiState.episodeSheetVisible) {
                ModalBottomSheet(
                    onDismissRequest = { onAction(PlayerAction.HideEpisodeSheet) },
                    containerColor = colors.background,
                ) {
                    EpisodeSelectorSheet(
                        dramaTitle = uiState.drama?.title.orEmpty(),
                        summary = uiState.episodeSelectorSummary,
                        items = uiState.episodeItems,
                        onEpisodeClick = { onAction(PlayerAction.SelectEpisode(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerTopOverlay(
    episodeLabel: String,
    onBack: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg, vertical = spacing.lg),
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.clip(DramaFlowThemeTokens.shapes.pill).clickable(onClick = onBack),
            color = colors.surface.copy(alpha = 0.88f),
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = colors.textPrimary,
                modifier = Modifier.padding(12.dp),
            )
        }
        Surface(
            shape = DramaFlowThemeTokens.shapes.pill,
            color = colors.surface.copy(alpha = 0.88f),
        ) {
            Text(
                text = episodeLabel,
                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                color = colors.textPrimary,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EpisodeSelectorBar(
    summary: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(DramaFlowThemeTokens.shapes.large)
            .clickable(onClick = onClick),
        color = Color.Black.copy(alpha = 0.58f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = summary,
                color = Color.White,
                style = DramaFlowThemeTokens.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Rounded.ExpandLess,
                contentDescription = "Open episode selector",
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun EpisodeSelectorSheet(
    dramaTitle: String,
    summary: String,
    items: List<PlayerEpisodeItem>,
    onEpisodeClick: (String) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = dramaTitle,
            style = DramaFlowThemeTokens.typography.titleLarge,
            color = colors.textPrimary,
        )
        Text(
            text = summary,
            style = DramaFlowThemeTokens.typography.bodyMedium,
            color = colors.textSecondary,
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
            contentPadding = PaddingValues(bottom = spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items(items, key = { it.episodeId }) { item ->
                EpisodeSelectorItem(
                    item = item,
                    onClick = { onEpisodeClick(item.episodeId) },
                )
            }
        }
    }
}

@Composable
private fun EpisodeSelectorItem(
    item: PlayerEpisodeItem,
    onClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    val background = when {
        item.isCurrent -> colors.accentStrong
        item.isLocked -> colors.surfaceMuted
        else -> colors.surface
    }
    val contentColor = when {
        item.isCurrent -> colors.textInverse
        item.isLocked -> colors.textSecondary
        else -> colors.textPrimary
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DramaFlowThemeTokens.shapes.medium)
            .clickable(onClick = onClick),
        color = background,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = item.episodeNumber.toString(),
                color = contentColor,
                style = DramaFlowThemeTokens.typography.titleMedium,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    item.isLocked -> Icon(Icons.Rounded.Lock, contentDescription = item.accessibilityLabel, tint = contentColor)
                    item.isCompleted -> Icon(Icons.Rounded.CheckCircle, contentDescription = item.accessibilityLabel, tint = contentColor)
                    item.isCurrent -> Icon(Icons.Rounded.PlayArrow, contentDescription = item.accessibilityLabel, tint = contentColor)
                    else -> Icon(Icons.Rounded.ExpandMore, contentDescription = item.accessibilityLabel, tint = contentColor)
                }
                Text(
                    text = when {
                        item.isCurrent -> "Playing"
                        item.isLocked -> "Locked"
                        item.isCompleted -> "Watched"
                        else -> "Open"
                    },
                    color = contentColor,
                    style = DramaFlowThemeTokens.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
private fun OverlayMessage(
    text: String,
    modifier: Modifier = Modifier,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        modifier = modifier,
        color = colors.surface.copy(alpha = 0.9f),
        shape = DramaFlowThemeTokens.shapes.pill,
    ) {
        Text(text, modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm), color = colors.textPrimary)
    }
}

@Preview
@Composable
private fun PlayerPreview() {
    DramaFlowTheme {
        PlayerScreen(
            uiState = PlayerUiState(
                loadState = DfLoadState.SUCCESS,
                drama = DramaFlowMockData.dramas.first(),
                episode = DramaFlowMockData.episodesForDrama("df-neon-vows")[3],
                previewLimit = PreviewLimit(15, 0, "Preview ended. Start subscription to continue instantly."),
                paywallVisible = true,
                playbackState = BridgePlaybackState(isPrepared = true, durationMs = 33_000, positionMs = 15_000),
                nextEpisodeHint = NextEpisodeHint("df-neon-vows-e5", "Episode 5", 5),
                isRemotePlayback = true,
                remoteMode = PlayerRemoteMode.HYBRID,
            ),
            playerBridge = DefaultMedia3PlayerBridge(LocalContext.current),
            onAction = {},
            onBack = {},
            onUnlock = {},
        )
    }
}
