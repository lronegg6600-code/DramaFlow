package com.dramaflow.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dramaflow.core.common.DramaFlowMockData
import com.dramaflow.core.common.DataResult
import com.dramaflow.core.common.FeedRepository
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.ui.DfLoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

sealed interface FeedAction {
    data class SelectPrimaryTab(val tab: HomePrimaryTab) : FeedAction
    data class SetRecommendActivePage(val page: Int) : FeedAction
    data class ToggleLike(val dramaId: String) : FeedAction
    data class ToggleFavorite(val dramaId: String) : FeedAction
    data class SelectWatchFilter(val filterId: String) : FeedAction
    data class ShareDrama(val dramaId: String) : FeedAction
    data object Retry : FeedAction
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

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

            is FeedAction.ToggleLike -> {
                _uiState.value = _uiState.value.copy(
                    recommendItems = _uiState.value.recommendItems.map { item ->
                        if (item.card.drama.id != action.dramaId) {
                            item
                        } else {
                            val nextLiked = !item.isLiked
                            item.copy(
                                isLiked = nextLiked,
                                likeCount = if (nextLiked) item.likeCount + 1 else maxOf(0, item.likeCount - 1),
                            )
                        }
                    },
                )
            }

            is FeedAction.ToggleFavorite -> {
                _uiState.value = _uiState.value.copy(
                    recommendItems = _uiState.value.recommendItems.map { item ->
                        if (item.card.drama.id != action.dramaId) item else item.copy(isFavorited = !item.isFavorited)
                    },
                )
            }

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

            is FeedAction.ShareDrama -> {
                // Share entry is wired for callback now; system share sheet can be plugged in next.
            }

            FeedAction.Retry -> observeFeed()
        }
    }

    private fun observeFeed() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            feedRepository.observeFeed().collect { result ->
                _uiState.value = when (result) {
                    DataResult.Loading -> FeedUiState(
                        loadState = DfLoadState.LOADING,
                        watchBrowse = WatchBrowseUiState(loadState = DfLoadState.LOADING),
                    )

                    DataResult.Empty -> FeedUiState(
                        loadState = DfLoadState.EMPTY,
                        watchBrowse = WatchBrowseUiState(loadState = DfLoadState.EMPTY),
                    )

                    is DataResult.Error -> FeedUiState(
                        loadState = DfLoadState.ERROR,
                        errorMessage = result.message,
                        watchBrowse = WatchBrowseUiState(
                            loadState = DfLoadState.ERROR,
                            errorMessage = result.message,
                        ),
                    )

                    is DataResult.Success -> {
                        val watchSource = (result.value.hotTitles + result.value.recommendations + result.value.continueWatching)
                            .distinctBy { it.drama.id }
                        val recommendSource = (result.value.recommendations + result.value.hotTitles + result.value.continueWatching)
                            .distinctBy { it.drama.id }

                        val recommendItems = recommendSource.mapIndexed { index, card ->
                            RecommendFeedItem(
                                id = card.drama.id,
                                card = card,
                                preview = buildPreviewMedia(card),
                                likeCount = estimateLikeCount(card, index),
                                commentCount = estimateCommentCount(card, index),
                                shareCount = estimateShareCount(card, index),
                            )
                        }

                        val defaultFilter = "hot"
                        val filteredWatchItems = applyWatchFilter(watchSource, defaultFilter)

                        FeedUiState(
                            loadState = DfLoadState.SUCCESS,
                            selectedTab = _uiState.value.selectedTab,
                            activeRecommendPage = _uiState.value.activeRecommendPage.coerceIn(
                                minimumValue = 0,
                                maximumValue = maxOf(recommendItems.lastIndex, 0),
                            ),
                            recommendItems = recommendItems,
                            watchBrowse = WatchBrowseUiState(
                                loadState = DfLoadState.SUCCESS,
                                filters = defaultWatchFilters(),
                                selectedFilterId = defaultFilter,
                                items = filteredWatchItems,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun estimateLikeCount(card: DramaCard, index: Int): Int {
        val base = (card.drama.heatScore.toFloatOrNull() ?: 7.5f) * 1000
        return base.toInt() + 500 + index * 137
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
        // 有本地/mock 对应集时直接拿首集预览，后续接真推荐接口时只需要把 previewUrl 改成后端字段。
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
                tag.label.contains("revenge", ignoreCase = true) || tag.label.contains("twist", ignoreCase = true)
            }
        }.ifEmpty { items }

        else -> items
    }
}
