package com.dramaflow.feature.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dramaflow.core.common.DataResult
import com.dramaflow.core.common.DramaFlowMockData
import com.dramaflow.core.common.DramaInteractionFlags
import com.dramaflow.core.common.DramaInteractionRepository
import com.dramaflow.core.common.DramaInteractionState
import com.dramaflow.core.common.FeedRepository
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.ui.DfLoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val FeedInteractionLogTag = "FeedInteraction"

enum class HomePrimaryTab(val label: String) {
    RECOMMEND("Recommend"),
    WATCH("Watch"),
    COMIC("Comic"),
    RECENT("Recent"),
    FAVORITE("Favorite"),
}

data class RecommendFeedItem(
    val id: String,
    val card: DramaCard,
    val preview: RecommendPreviewMedia,
    val likeCount: Int,
    val commentCount: Int,
    val shareCount: Int,
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val isLikeUpdating: Boolean = false,
    val isFavoriteUpdating: Boolean = false,
)

data class RecommendPreviewMedia(
    val mediaId: String,
    val dramaId: String,
    val entryEpisodeId: String?,
    val previewUrl: String?,
    val coverUrl: String,
    val autoplayDelayMs: Long = 220L,
)

data class WatchFilter(
    val id: String,
    val label: String,
)

data class WatchBrowseUiState(
    val loadState: DfLoadState = DfLoadState.LOADING,
    val filters: List<WatchFilter> = defaultWatchFilters(),
    val selectedFilterId: String = "hot",
    val items: List<DramaCard> = emptyList(),
    val errorMessage: String = "Watch list refresh failed. Please try again.",
)

data class FeedUiState(
    val loadState: DfLoadState = DfLoadState.LOADING,
    val selectedTab: HomePrimaryTab = HomePrimaryTab.RECOMMEND,
    val activeRecommendPage: Int = 0,
    val recommendItems: List<RecommendFeedItem> = emptyList(),
    val watchBrowse: WatchBrowseUiState = WatchBrowseUiState(),
    val errorMessage: String = "Home refresh failed. Please retry.",
)

data class ShareDramaPayload(
    val dramaId: String,
    val title: String,
    val description: String,
    val link: String,
)

data class PendingInteractionOverride(
    val liked: Boolean? = null,
    val favorited: Boolean? = null,
    val likeInFlight: Boolean = false,
    val favoriteInFlight: Boolean = false,
)

sealed interface FeedAction {
    data class SelectPrimaryTab(val tab: HomePrimaryTab) : FeedAction
    data class SetRecommendActivePage(val page: Int) : FeedAction
    data class ToggleLike(val dramaId: String) : FeedAction
    data class ToggleFavorite(val dramaId: String) : FeedAction
    data class SelectWatchFilter(val filterId: String) : FeedAction
    data class ShareDrama(val dramaId: String) : FeedAction
    data object Retry : FeedAction
}

sealed interface FeedEffect {
    data class OpenShareSheet(val payload: ShareDramaPayload) : FeedEffect
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val interactionRepository: DramaInteractionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<FeedEffect>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val effects: SharedFlow<FeedEffect> = _effects.asSharedFlow()
    private var observeJob: Job? = null

    private val pendingInteractionOverrides =
        MutableStateFlow<Map<String, PendingInteractionOverride>>(emptyMap())

    init {
        observeFeed()
    }

    fun onAction(action: FeedAction) {
        when (action) {
            is FeedAction.SelectPrimaryTab -> {
                _uiState.value = _uiState.value.copy(selectedTab = action.tab)
            }

            is FeedAction.SetRecommendActivePage -> {
                if (action.page != _uiState.value.activeRecommendPage) {
                    _uiState.value = _uiState.value.copy(activeRecommendPage = action.page)
                }
            }

            is FeedAction.ToggleLike -> handleToggleLike(action.dramaId)
            is FeedAction.ToggleFavorite -> handleToggleFavorite(action.dramaId)

            is FeedAction.SelectWatchFilter -> {
                val current = _uiState.value
                _uiState.value = current.copy(
                    watchBrowse = current.watchBrowse.copy(
                        selectedFilterId = action.filterId,
                        items = applyWatchFilter(
                            items = current.watchBrowse.items,
                            filterId = action.filterId,
                        ),
                    ),
                )
            }

            is FeedAction.ShareDrama -> handleShare(action.dramaId)
            FeedAction.Retry -> observeFeed()
        }
    }

    private fun observeFeed() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                feedRepository.observeFeed(),
                interactionRepository.observeInteractionState(),
                pendingInteractionOverrides,
            ) { feedResult, interactionState, pendingOverrides ->
                Triple(feedResult, interactionState, pendingOverrides)
            }.collect { (feedResult, interactionState, pendingOverrides) ->
                _uiState.value = reduceFeedState(
                    feedResult = feedResult,
                    interactionState = interactionState,
                    pendingOverrides = pendingOverrides,
                    previousState = _uiState.value,
                )
            }
        }
    }

    private fun reduceFeedState(
        feedResult: DataResult<com.dramaflow.core.model.FeedPayload>,
        interactionState: DramaInteractionState,
        pendingOverrides: Map<String, PendingInteractionOverride>,
        previousState: FeedUiState,
    ): FeedUiState {
        return when (feedResult) {
            DataResult.Loading -> FeedUiState(
                loadState = DfLoadState.LOADING,
                selectedTab = previousState.selectedTab,
                activeRecommendPage = previousState.activeRecommendPage,
                watchBrowse = previousState.watchBrowse.copy(loadState = DfLoadState.LOADING),
            )

            DataResult.Empty -> FeedUiState(
                loadState = DfLoadState.EMPTY,
                selectedTab = previousState.selectedTab,
                activeRecommendPage = 0,
                watchBrowse = previousState.watchBrowse.copy(loadState = DfLoadState.EMPTY, items = emptyList()),
            )

            is DataResult.Error -> FeedUiState(
                loadState = DfLoadState.ERROR,
                selectedTab = previousState.selectedTab,
                activeRecommendPage = previousState.activeRecommendPage,
                errorMessage = feedResult.message,
                watchBrowse = previousState.watchBrowse.copy(
                    loadState = DfLoadState.ERROR,
                    errorMessage = feedResult.message,
                ),
            )

            is DataResult.Success -> {
                val watchSource = (feedResult.value.hotTitles + feedResult.value.recommendations + feedResult.value.continueWatching)
                    .distinctBy { it.drama.id }
                val recommendSource = (feedResult.value.recommendations + feedResult.value.hotTitles + feedResult.value.continueWatching)
                    .distinctBy { it.drama.id }
                val interactionFlags = interactionRepository.backfill(
                    dramaIds = recommendSource.map { it.drama.id },
                    state = interactionState,
                )

                val recommendItems = recommendSource.mapIndexed { index, card ->
                    val flags = interactionFlags[card.drama.id] ?: DramaInteractionFlags(
                        isLiked = false,
                        isFavorited = false,
                    )
                    val pending = pendingOverrides[card.drama.id]
                    val effectiveLiked = pending?.liked ?: flags.isLiked
                    val effectiveFavorited = pending?.favorited ?: flags.isFavorited
                    RecommendFeedItem(
                        id = card.drama.id,
                        card = card,
                        preview = buildPreviewMedia(card),
                        likeCount = estimateLikeCount(card, index, effectiveLiked),
                        commentCount = estimateCommentCount(card, index),
                        shareCount = estimateShareCount(card, index),
                        isLiked = effectiveLiked,
                        isFavorited = effectiveFavorited,
                        isLikeUpdating = pending?.likeInFlight == true,
                        isFavoriteUpdating = pending?.favoriteInFlight == true,
                    )
                }

                val selectedWatchFilter = previousState.watchBrowse.selectedFilterId
                    .takeIf { filter -> defaultWatchFilters().any { it.id == filter } }
                    ?: "hot"

                FeedUiState(
                    loadState = DfLoadState.SUCCESS,
                    selectedTab = previousState.selectedTab,
                    activeRecommendPage = previousState.activeRecommendPage.coerceIn(
                        minimumValue = 0,
                        maximumValue = maxOf(recommendItems.lastIndex, 0),
                    ),
                    recommendItems = recommendItems,
                    watchBrowse = WatchBrowseUiState(
                        loadState = DfLoadState.SUCCESS,
                        filters = defaultWatchFilters(),
                        selectedFilterId = selectedWatchFilter,
                        items = applyWatchFilter(watchSource, selectedWatchFilter),
                    ),
                )
            }
        }
    }

    private fun handleToggleLike(dramaId: String) {
        val currentItem = _uiState.value.recommendItems.firstOrNull { it.card.drama.id == dramaId } ?: return
        if (currentItem.isLikeUpdating) {
            return
        }
        val nextLiked = !currentItem.isLiked
        Log.d(FeedInteractionLogTag, "feed_like_click drama=$dramaId nextLiked=$nextLiked")
        updatePendingOverride(dramaId) { current ->
            current.copy(
                liked = nextLiked,
                likeInFlight = true,
            )
        }
        viewModelScope.launch {
            runCatching {
                interactionRepository.toggleLike(dramaId)
            }.onFailure { error ->
                Log.e(
                    FeedInteractionLogTag,
                    "feed_like_persist_failed drama=$dramaId message=${error.message}",
                    error,
                )
                updatePendingOverride(dramaId) { current ->
                    current.copy(
                        liked = currentItem.isLiked,
                        likeInFlight = false,
                    )
                }
                clearPendingField(
                    dramaId = dramaId,
                    clearLiked = true,
                    clearFavorited = false,
                )
            }.onSuccess {
                Log.d(FeedInteractionLogTag, "feed_like_persist_success drama=$dramaId liked=$nextLiked")
                clearPendingField(
                    dramaId = dramaId,
                    clearLiked = true,
                    clearFavorited = false,
                )
            }
        }
    }

    private fun handleToggleFavorite(dramaId: String) {
        val currentItem = _uiState.value.recommendItems.firstOrNull { it.card.drama.id == dramaId } ?: return
        if (currentItem.isFavoriteUpdating) {
            return
        }
        val nextFavorited = !currentItem.isFavorited
        Log.d(FeedInteractionLogTag, "feed_favorite_click drama=$dramaId nextFavorited=$nextFavorited")
        updatePendingOverride(dramaId) { current ->
            current.copy(
                favorited = nextFavorited,
                favoriteInFlight = true,
            )
        }
        viewModelScope.launch {
            runCatching {
                interactionRepository.toggleFavorite(dramaId)
            }.onFailure { error ->
                Log.e(
                    FeedInteractionLogTag,
                    "feed_favorite_persist_failed drama=$dramaId message=${error.message}",
                    error,
                )
                updatePendingOverride(dramaId) { current ->
                    current.copy(
                        favorited = currentItem.isFavorited,
                        favoriteInFlight = false,
                    )
                }
                clearPendingField(
                    dramaId = dramaId,
                    clearLiked = false,
                    clearFavorited = true,
                )
            }.onSuccess {
                Log.d(
                    FeedInteractionLogTag,
                    "feed_favorite_persist_success drama=$dramaId favorited=$nextFavorited",
                )
                clearPendingField(
                    dramaId = dramaId,
                    clearLiked = false,
                    clearFavorited = true,
                )
            }
        }
    }

    private fun handleShare(dramaId: String) {
        val item = _uiState.value.recommendItems.firstOrNull { it.card.drama.id == dramaId } ?: return
        val payload = ShareDramaPayload(
            dramaId = item.card.drama.id,
            title = item.card.drama.title,
            description = item.card.drama.shortDescription,
            link = "https://www.dramaflow.app/drama/${item.card.drama.id}",
        )
        Log.d(FeedInteractionLogTag, "feed_share_click drama=$dramaId")
        _effects.tryEmit(FeedEffect.OpenShareSheet(payload))
    }

    private fun updatePendingOverride(
        dramaId: String,
        transform: (PendingInteractionOverride) -> PendingInteractionOverride,
    ) {
        pendingInteractionOverrides.value = pendingInteractionOverrides.value.toMutableMap().apply {
            val current = this[dramaId] ?: PendingInteractionOverride()
            this[dramaId] = transform(current)
        }
    }

    private fun clearPendingField(
        dramaId: String,
        clearLiked: Boolean,
        clearFavorited: Boolean,
    ) {
        pendingInteractionOverrides.value = pendingInteractionOverrides.value.toMutableMap().apply {
            val current = this[dramaId] ?: return@apply
            val next = current.copy(
                liked = if (clearLiked) null else current.liked,
                favorited = if (clearFavorited) null else current.favorited,
                likeInFlight = if (clearLiked) false else current.likeInFlight,
                favoriteInFlight = if (clearFavorited) false else current.favoriteInFlight,
            )
            if (
                next.liked == null &&
                next.favorited == null &&
                !next.likeInFlight &&
                !next.favoriteInFlight
            ) {
                remove(dramaId)
            } else {
                this[dramaId] = next
            }
        }
    }

    private fun estimateLikeCount(card: DramaCard, index: Int, isLiked: Boolean): Int {
        val base = (card.drama.heatScore.toFloatOrNull() ?: 7.5f) * 1000
        val seededCount = base.toInt() + 500 + index * 137
        return if (isLiked) seededCount + 1 else seededCount
    }

    private fun estimateCommentCount(card: DramaCard, index: Int): Int {
        val base = (card.drama.heatScore.toFloatOrNull() ?: 7.5f) * 120
        return base.toInt() + 60 + index * 11
    }

    private fun estimateShareCount(card: DramaCard, index: Int): Int {
        val base = (card.drama.heatScore.toFloatOrNull() ?: 7.5f) * 70
        return base.toInt() + 30 + index * 7
    }

    private fun buildPreviewMedia(card: DramaCard): RecommendPreviewMedia {
        // 推荐流预览先走最稳的 entry episode。
        // 当前后端还没有专门的 preview 字段时，先用 mock episode 的 streamUrl 做轻量预览。
        // 后续接入真实推荐接口时，只需要把 previewUrl 换成服务端字段，不需要重写页面结构。
        val entryEpisode = card.lastProgress?.episodeId?.let(DramaFlowMockData::findEpisode)
            ?: DramaFlowMockData.episodesForDrama(card.drama.id).firstOrNull()
        return RecommendPreviewMedia(
            mediaId = card.drama.id,
            dramaId = card.drama.id,
            entryEpisodeId = entryEpisode?.id,
            previewUrl = entryEpisode?.streamUrl,
            coverUrl = card.drama.heroImageUrl.ifBlank { card.drama.portraitPosterUrl },
        )
    }
}

private fun defaultWatchFilters(): List<WatchFilter> {
    return listOf(
        WatchFilter("hot", "Hot"),
        WatchFilter("new", "New"),
        WatchFilter("modern", "Modern"),
        WatchFilter("revenge", "Revenge"),
        WatchFilter("urban", "Urban"),
        WatchFilter("more", "More"),
    )
}

private fun applyWatchFilter(
    items: List<DramaCard>,
    filterId: String,
): List<DramaCard> {
    return when (filterId) {
        "hot" -> items.sortedByDescending { it.drama.heatScore.toFloatOrNull() ?: 0f }
        "new" -> items.sortedByDescending { it.isUpdated }
        "modern", "urban" -> items.filter { card ->
            card.drama.tags.any { tag ->
                tag.label.contains("modern", ignoreCase = true) ||
                    tag.label.contains("urban", ignoreCase = true) ||
                    tag.label.contains("ceo", ignoreCase = true)
            }
        }.ifEmpty { items }

        "revenge" -> items.filter { card ->
            card.drama.tags.any { tag ->
                tag.label.contains("revenge", ignoreCase = true) ||
                    tag.label.contains("twist", ignoreCase = true)
            }
        }.ifEmpty { items }

        else -> items
    }
}
