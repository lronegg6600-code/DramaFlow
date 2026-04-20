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
import com.dramaflow.core.model.Drama
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.ui.DfLoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
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
    CHARTS("Charts"),
    CATEGORIES("Categories"),
}

enum class ChartType(val id: String, val label: String) {
    HOT("hot", "Hot"),
    NEW("new", "New"),
    RISING("rising", "Rising"),
    COMPLETE("complete", "Complete"),
    BUZZ("buzz", "Buzz"),
}

enum class CategoryAvailability(val id: String, val label: String) {
    ALL("all", "All"),
    FREE("free", "Free"),
    PREMIUM("premium", "Premium"),
    COMPLETE("complete", "Complete"),
    ONGOING("ongoing", "Ongoing"),
}

enum class CategorySort(val id: String, val label: String) {
    HOT("hot", "Hot"),
    LATEST("latest", "Latest"),
    SCORE("score", "Top rated"),
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

data class ChartTab(
    val type: ChartType,
    val label: String,
)

data class ChannelPlaybackEntry(
    val episodeId: String?,
    val ctaLabel: String,
    val supportingLabel: String,
)

data class ChannelBrowseItem(
    val card: DramaCard,
    val playbackEntry: ChannelPlaybackEntry,
)

data class ChartRankItem(
    val rank: Int,
    val browseItem: ChannelBrowseItem,
    val heatText: String,
    val statusText: String,
    val badgeText: String,
)

data class ChartsBrowseUiState(
    val loadState: DfLoadState = DfLoadState.LOADING,
    val chartTabs: List<ChartTab> = ChartType.entries.map { ChartTab(it, it.label) },
    val selectedChartType: ChartType = ChartType.HOT,
    val rankItems: List<ChartRankItem> = emptyList(),
    val spotlightItems: List<ChannelBrowseItem> = emptyList(),
    val errorMessage: String = "Chart refresh failed. Please try again.",
)

data class CategoryFilterOption(
    val id: String,
    val label: String,
)

data class CategoriesBrowseUiState(
    val loadState: DfLoadState = DfLoadState.LOADING,
    val genreFilters: List<CategoryFilterOption> = emptyList(),
    val selectedGenreId: String = "all",
    val availabilityFilters: List<CategoryFilterOption> = CategoryAvailability.entries.map {
        CategoryFilterOption(it.id, it.label)
    },
    val selectedAvailabilityId: String = CategoryAvailability.ALL.id,
    val sortFilters: List<CategoryFilterOption> = CategorySort.entries.map {
        CategoryFilterOption(it.id, it.label)
    },
    val selectedSortId: String = CategorySort.HOT.id,
    val resultCount: Int = 0,
    val items: List<ChannelBrowseItem> = emptyList(),
    val errorMessage: String = "Category refresh failed. Please try again.",
)

data class FeedUiState(
    val loadState: DfLoadState = DfLoadState.LOADING,
    val selectedTab: HomePrimaryTab = HomePrimaryTab.RECOMMEND,
    val activeRecommendPage: Int = 0,
    val recommendItems: List<RecommendFeedItem> = emptyList(),
    val chartsBrowse: ChartsBrowseUiState = ChartsBrowseUiState(),
    val categoriesBrowse: CategoriesBrowseUiState = CategoriesBrowseUiState(),
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
    data class SelectChartType(val type: ChartType) : FeedAction
    data class SelectCategoryGenre(val genreId: String) : FeedAction
    data class SelectCategoryAvailability(val availabilityId: String) : FeedAction
    data class SelectCategorySort(val sortId: String) : FeedAction
    data class ShareDrama(val dramaId: String) : FeedAction
    data object ResetCategoryFilters : FeedAction
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
            is FeedAction.SelectChartType -> {
                _uiState.value = _uiState.value.copy(
                    chartsBrowse = _uiState.value.chartsBrowse.copy(selectedChartType = action.type),
                )
            }

            is FeedAction.SelectCategoryGenre -> {
                _uiState.value = _uiState.value.copy(
                    categoriesBrowse = _uiState.value.categoriesBrowse.copy(selectedGenreId = action.genreId),
                )
            }

            is FeedAction.SelectCategoryAvailability -> {
                _uiState.value = _uiState.value.copy(
                    categoriesBrowse = _uiState.value.categoriesBrowse.copy(
                        selectedAvailabilityId = action.availabilityId,
                    ),
                )
            }

            is FeedAction.SelectCategorySort -> {
                _uiState.value = _uiState.value.copy(
                    categoriesBrowse = _uiState.value.categoriesBrowse.copy(selectedSortId = action.sortId),
                )
            }

            is FeedAction.ShareDrama -> handleShare(action.dramaId)
            FeedAction.ResetCategoryFilters -> {
                _uiState.value = _uiState.value.copy(
                    categoriesBrowse = _uiState.value.categoriesBrowse.copy(
                        selectedGenreId = "all",
                        selectedAvailabilityId = CategoryAvailability.ALL.id,
                        selectedSortId = CategorySort.HOT.id,
                    ),
                )
            }

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
                _uiState,
            ) { feedResult, interactionState, pendingOverrides, currentUi ->
                FeedReductionInput(feedResult, interactionState, pendingOverrides, currentUi)
            }.collect { input ->
                _uiState.value = reduceFeedState(input)
            }
        }
    }

    private fun reduceFeedState(
        input: FeedReductionInput,
    ): FeedUiState {
        val previousState = input.currentUi
        return when (val feedResult = input.feedResult) {
            DataResult.Loading -> previousState.copy(
                loadState = DfLoadState.LOADING,
                chartsBrowse = previousState.chartsBrowse.copy(loadState = DfLoadState.LOADING),
                categoriesBrowse = previousState.categoriesBrowse.copy(loadState = DfLoadState.LOADING),
            )

            DataResult.Empty -> previousState.copy(
                loadState = DfLoadState.EMPTY,
                activeRecommendPage = 0,
                recommendItems = emptyList(),
                chartsBrowse = previousState.chartsBrowse.copy(
                    loadState = DfLoadState.EMPTY,
                    rankItems = emptyList(),
                    spotlightItems = emptyList(),
                ),
                categoriesBrowse = previousState.categoriesBrowse.copy(
                    loadState = DfLoadState.EMPTY,
                    items = emptyList(),
                    resultCount = 0,
                ),
            )

            is DataResult.Error -> previousState.copy(
                loadState = DfLoadState.ERROR,
                errorMessage = feedResult.message,
                chartsBrowse = previousState.chartsBrowse.copy(
                    loadState = DfLoadState.ERROR,
                    errorMessage = feedResult.message,
                ),
                categoriesBrowse = previousState.categoriesBrowse.copy(
                    loadState = DfLoadState.ERROR,
                    errorMessage = feedResult.message,
                ),
            )

            is DataResult.Success -> {
                val allCards = buildChannelSource(feedResult.value)
                val recommendItems = buildRecommendItems(
                    cards = allCards,
                    interactionState = input.interactionState,
                    pendingOverrides = input.pendingOverrides,
                )
                val chartsState = buildChartsState(
                    allCards = allCards,
                    previousState = previousState.chartsBrowse,
                )
                val categoriesState = buildCategoriesState(
                    allCards = allCards,
                    previousState = previousState.categoriesBrowse,
                )
                previousState.copy(
                    loadState = DfLoadState.SUCCESS,
                    activeRecommendPage = previousState.activeRecommendPage.coerceIn(
                        0,
                        maxOf(recommendItems.lastIndex, 0),
                    ),
                    recommendItems = recommendItems,
                    chartsBrowse = chartsState,
                    categoriesBrowse = categoriesState,
                )
            }
        }
    }

    private fun buildRecommendItems(
        cards: List<DramaCard>,
        interactionState: DramaInteractionState,
        pendingOverrides: Map<String, PendingInteractionOverride>,
    ): List<RecommendFeedItem> {
        val interactionFlags = interactionRepository.backfill(
            dramaIds = cards.map { it.drama.id },
            state = interactionState,
        )
        return cards.mapIndexed { index, card ->
            val flags = interactionFlags[card.drama.id] ?: DramaInteractionFlags(false, false)
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
    }

    private fun buildChartsState(
        allCards: List<DramaCard>,
        previousState: ChartsBrowseUiState,
    ): ChartsBrowseUiState {
        val selectedType = previousState.selectedChartType
        val rankedSource = when (selectedType) {
            ChartType.HOT -> allCards.sortedByDescending { it.drama.heatValue() }
            ChartType.NEW -> allCards.sortedWith(
                compareByDescending<DramaCard> { it.isUpdated }
                    .thenByDescending { it.drama.heatValue() },
            )

            ChartType.RISING -> allCards.sortedByDescending { risingScore(it) }
            ChartType.COMPLETE -> allCards.sortedWith(
                compareByDescending<DramaCard> { !it.isUpdated }
                    .thenByDescending { it.drama.totalEpisodes }
                    .thenByDescending { it.drama.heatValue() },
            )

            ChartType.BUZZ -> allCards.sortedByDescending { buzzScore(it) }
        }
        val rankItems = rankedSource.mapIndexed { index, card ->
            ChartRankItem(
                rank = index + 1,
                browseItem = card.toChannelBrowseItem(),
                heatText = "${card.drama.heatScore} heat",
                statusText = card.drama.statusText(),
                badgeText = chartBadgeText(selectedType, card, index),
            )
        }
        return previousState.copy(
            loadState = if (rankItems.isEmpty()) DfLoadState.EMPTY else DfLoadState.SUCCESS,
            rankItems = rankItems,
            spotlightItems = rankedSource.take(6).map { it.toChannelBrowseItem() },
        )
    }

    private fun buildCategoriesState(
        allCards: List<DramaCard>,
        previousState: CategoriesBrowseUiState,
    ): CategoriesBrowseUiState {
        val genreFilters = buildGenreFilters(allCards)
        val selectedGenre = previousState.selectedGenreId
            .takeIf { selected -> genreFilters.any { it.id == selected } }
            ?: "all"
        val filtered = allCards
            .filter { card -> matchesGenre(card, selectedGenre) }
            .filter { card -> matchesAvailability(card, previousState.selectedAvailabilityId) }
            .let { items -> sortCategoryItems(items, previousState.selectedSortId) }
        return previousState.copy(
            loadState = if (filtered.isEmpty()) DfLoadState.EMPTY else DfLoadState.SUCCESS,
            genreFilters = genreFilters,
            selectedGenreId = selectedGenre,
            items = filtered.map { it.toChannelBrowseItem() },
            resultCount = filtered.size,
        )
    }

    private fun handleToggleLike(dramaId: String) {
        val currentItem = _uiState.value.recommendItems.firstOrNull { it.card.drama.id == dramaId } ?: return
        if (currentItem.isLikeUpdating) return
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
                clearPendingField(dramaId, clearLiked = true, clearFavorited = false)
            }.onSuccess {
                Log.d(FeedInteractionLogTag, "feed_like_persist_success drama=$dramaId liked=$nextLiked")
                clearPendingField(dramaId, clearLiked = true, clearFavorited = false)
            }
        }
    }

    private fun handleToggleFavorite(dramaId: String) {
        val currentItem = _uiState.value.recommendItems.firstOrNull { it.card.drama.id == dramaId } ?: return
        if (currentItem.isFavoriteUpdating) return
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
                clearPendingField(dramaId, clearLiked = false, clearFavorited = true)
            }.onSuccess {
                Log.d(
                    FeedInteractionLogTag,
                    "feed_favorite_persist_success drama=$dramaId favorited=$nextFavorited",
                )
                clearPendingField(dramaId, clearLiked = false, clearFavorited = true)
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
        val base = card.drama.heatValue() * 1000
        val seededCount = base.toInt() + 500 + index * 137
        return if (isLiked) seededCount + 1 else seededCount
    }

    private fun estimateCommentCount(card: DramaCard, index: Int): Int {
        val base = card.drama.heatValue() * 120
        return base.toInt() + 60 + index * 11
    }

    private fun estimateShareCount(card: DramaCard, index: Int): Int {
        val base = card.drama.heatValue() * 70
        return base.toInt() + 30 + index * 7
    }

    private fun buildPreviewMedia(card: DramaCard): RecommendPreviewMedia {
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

private data class FeedReductionInput(
    val feedResult: DataResult<com.dramaflow.core.model.FeedPayload>,
    val interactionState: DramaInteractionState,
    val pendingOverrides: Map<String, PendingInteractionOverride>,
    val currentUi: FeedUiState,
)

private fun buildChannelSource(
    payload: com.dramaflow.core.model.FeedPayload,
): List<DramaCard> {
    return (payload.recommendations + payload.hotTitles + payload.continueWatching + payload.banners)
        .distinctBy { it.drama.id }
}

private fun buildGenreFilters(items: List<DramaCard>): List<CategoryFilterOption> {
    val genres = items.flatMap { it.drama.tags.map { tag -> tag.label } }
        .distinct()
        .sorted()
        .take(8)
        .map { label ->
            CategoryFilterOption(
                id = label.lowercase(),
                label = label,
            )
        }
    return listOf(CategoryFilterOption("all", "All")) + genres
}

private fun matchesGenre(card: DramaCard, genreId: String): Boolean {
    if (genreId == "all") return true
    return card.drama.tags.any { it.label.equals(genreId, ignoreCase = true) }
}

private fun matchesAvailability(card: DramaCard, availabilityId: String): Boolean {
    return when (availabilityId) {
        CategoryAvailability.ALL.id -> true
        CategoryAvailability.FREE.id -> !card.drama.isPremiumSeries
        CategoryAvailability.PREMIUM.id -> card.drama.isPremiumSeries
        CategoryAvailability.COMPLETE.id -> !card.isUpdated
        CategoryAvailability.ONGOING.id -> card.isUpdated
        else -> true
    }
}

private fun sortCategoryItems(
    items: List<DramaCard>,
    sortId: String,
): List<DramaCard> {
    return when (sortId) {
        CategorySort.HOT.id -> items.sortedByDescending { it.drama.heatValue() }
        CategorySort.LATEST.id -> items.sortedWith(
            compareByDescending<DramaCard> { it.isUpdated }
                .thenByDescending { it.drama.heatValue() },
        )

        CategorySort.SCORE.id -> items.sortedByDescending { scoreValue(it.drama) }
        else -> items
    }
}

private fun chartBadgeText(type: ChartType, card: DramaCard, index: Int): String {
    return when (type) {
        ChartType.HOT -> if (index == 0) "Top heat" else "Trending"
        ChartType.NEW -> if (card.isUpdated) "Fresh drop" else "Completed"
        ChartType.RISING -> "Fast rising"
        ChartType.COMPLETE -> "Binge ready"
        ChartType.BUZZ -> if (card.drama.isPremiumSeries) "Premium buzz" else "All audience"
    }
}

private fun risingScore(card: DramaCard): Float {
    return card.drama.heatValue() + if (card.isUpdated) 1.5f else 0.5f
}

private fun buzzScore(card: DramaCard): Float {
    return card.drama.heatValue() + (card.drama.cast.size * 0.25f) + if (card.drama.isPremiumSeries) 0.6f else 0f
}

private fun scoreValue(drama: Drama): Float {
    return drama.heatValue() + (drama.tags.size * 0.12f)
}

private fun Drama.heatValue(): Float = heatScore.toFloatOrNull() ?: 7.5f

private fun DramaCard.toChannelBrowseItem(): ChannelBrowseItem {
    val resumeEpisode = lastProgress?.episodeId?.let(DramaFlowMockData::findEpisode)
    val entryEpisode = resumeEpisode ?: DramaFlowMockData.episodesForDrama(drama.id).firstOrNull()
    return ChannelBrowseItem(
        card = this,
        playbackEntry = ChannelPlaybackEntry(
            episodeId = entryEpisode?.id,
            ctaLabel = if (resumeEpisode != null) "Continue" else "Watch now",
            supportingLabel = entryEpisode?.let { "Episode ${it.episodeNumber}" } ?: "Open details",
        ),
    )
}

private fun Drama.statusText(): String {
    val releaseState = if (isFeatured) "Ongoing" else "Completed"
    val accessState = if (isPremiumSeries) "Premium" else "Free"
    return "$totalEpisodes eps · $releaseState · $accessState"
}
