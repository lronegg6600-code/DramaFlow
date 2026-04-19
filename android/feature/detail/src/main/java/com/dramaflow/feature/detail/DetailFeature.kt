package com.dramaflow.feature.detail

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dramaflow.core.common.CatalogRepository
import com.dramaflow.core.common.DataResult
import com.dramaflow.core.common.DramaInteractionFlags
import com.dramaflow.core.common.DramaInteractionRepository
import com.dramaflow.core.common.DramaFlowMockData
import com.dramaflow.core.designsystem.component.DfCategoryChip
import com.dramaflow.core.designsystem.component.DfDramaCard
import com.dramaflow.core.designsystem.component.DfPrimaryButton
import com.dramaflow.core.designsystem.theme.DramaFlowTheme
import com.dramaflow.core.designsystem.theme.DramaFlowThemeTokens
import com.dramaflow.core.model.DetailPayload
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.model.EpisodeListItem
import com.dramaflow.core.ui.DfLoadState
import com.dramaflow.core.ui.DfScreenScaffold
import com.dramaflow.core.ui.DfScrollableColumn
import com.dramaflow.core.ui.DfStateLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val DetailInteractionLogTag = "DetailInteraction"

data class DetailUiState(
    val loadState: DfLoadState = DfLoadState.LOADING,
    val payload: DetailPayload? = null,
    val errorMessage: String = "Unable to load the drama details.",
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val isLikeUpdating: Boolean = false,
    val isFavoriteUpdating: Boolean = false,
)

sealed interface DetailAction {
    data object Retry : DetailAction
    data object ToggleLike : DetailAction
    data object ToggleFavorite : DetailAction
}

data class DetailPendingInteractionOverride(
    val liked: Boolean? = null,
    val favorited: Boolean? = null,
    val likeInFlight: Boolean = false,
    val favoriteInFlight: Boolean = false,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: CatalogRepository,
    private val interactionRepository: DramaInteractionRepository,
    savedStateHandle: SavedStateHandle,
) : androidx.lifecycle.ViewModel() {
    private val dramaId: String = savedStateHandle["dramaId"] ?: "df-neon-vows"
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null
    private val pendingInteractionOverride = MutableStateFlow(DetailPendingInteractionOverride())

    init {
        observe()
    }

    fun onAction(action: DetailAction) {
        when (action) {
            DetailAction.Retry -> observe()
            DetailAction.ToggleLike -> handleToggleLike()
            DetailAction.ToggleFavorite -> handleToggleFavorite()
        }
    }

    private fun observe() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                repository.observeDramaDetail(dramaId),
                interactionRepository.observeInteraction(dramaId),
                pendingInteractionOverride,
            ) { detailResult, interactionFlags, pendingOverride ->
                Triple(detailResult, interactionFlags, pendingOverride)
            }.collect { (detailResult, interactionFlags, pendingOverride) ->
                _uiState.value = when (detailResult) {
                    DataResult.Loading -> DetailUiState(
                        loadState = DfLoadState.LOADING,
                        isLiked = pendingOverride.liked ?: interactionFlags.isLiked,
                        isFavorited = pendingOverride.favorited ?: interactionFlags.isFavorited,
                        isLikeUpdating = pendingOverride.likeInFlight,
                        isFavoriteUpdating = pendingOverride.favoriteInFlight,
                    )

                    DataResult.Empty -> DetailUiState(
                        loadState = DfLoadState.EMPTY,
                        isLiked = pendingOverride.liked ?: interactionFlags.isLiked,
                        isFavorited = pendingOverride.favorited ?: interactionFlags.isFavorited,
                        isLikeUpdating = pendingOverride.likeInFlight,
                        isFavoriteUpdating = pendingOverride.favoriteInFlight,
                    )

                    is DataResult.Error -> DetailUiState(
                        loadState = DfLoadState.ERROR,
                        errorMessage = detailResult.message,
                        isLiked = pendingOverride.liked ?: interactionFlags.isLiked,
                        isFavorited = pendingOverride.favorited ?: interactionFlags.isFavorited,
                        isLikeUpdating = pendingOverride.likeInFlight,
                        isFavoriteUpdating = pendingOverride.favoriteInFlight,
                    )

                    is DataResult.Success -> DetailUiState(
                        loadState = DfLoadState.SUCCESS,
                        payload = detailResult.value,
                        isLiked = pendingOverride.liked ?: interactionFlags.isLiked,
                        isFavorited = pendingOverride.favorited ?: interactionFlags.isFavorited,
                        isLikeUpdating = pendingOverride.likeInFlight,
                        isFavoriteUpdating = pendingOverride.favoriteInFlight,
                    )
                }
            }
        }
    }

    private fun handleToggleLike() {
        val currentState = _uiState.value
        if (currentState.isLikeUpdating) return
        val nextLiked = !currentState.isLiked
        Log.d(DetailInteractionLogTag, "detail_like_click drama=$dramaId nextLiked=$nextLiked")
        pendingInteractionOverride.value = pendingInteractionOverride.value.copy(
            liked = nextLiked,
            likeInFlight = true,
        )
        viewModelScope.launch {
            runCatching {
                interactionRepository.toggleLike(dramaId)
            }.onFailure { error ->
                Log.e(
                    DetailInteractionLogTag,
                    "detail_interaction_persist_failed drama=$dramaId action=like message=${error.message}",
                    error,
                )
                Log.d(DetailInteractionLogTag, "detail_interaction_rollback drama=$dramaId action=like")
                pendingInteractionOverride.value = pendingInteractionOverride.value.copy(
                    liked = currentState.isLiked,
                    likeInFlight = false,
                )
                pendingInteractionOverride.value = pendingInteractionOverride.value.copy(
                    liked = null,
                    likeInFlight = false,
                )
            }.onSuccess {
                pendingInteractionOverride.value = pendingInteractionOverride.value.copy(
                    liked = null,
                    likeInFlight = false,
                )
            }
        }
    }

    private fun handleToggleFavorite() {
        val currentState = _uiState.value
        if (currentState.isFavoriteUpdating) return
        val nextFavorited = !currentState.isFavorited
        Log.d(DetailInteractionLogTag, "detail_favorite_click drama=$dramaId nextFavorited=$nextFavorited")
        pendingInteractionOverride.value = pendingInteractionOverride.value.copy(
            favorited = nextFavorited,
            favoriteInFlight = true,
        )
        viewModelScope.launch {
            runCatching {
                interactionRepository.toggleFavorite(dramaId)
            }.onFailure { error ->
                Log.e(
                    DetailInteractionLogTag,
                    "detail_interaction_persist_failed drama=$dramaId action=favorite message=${error.message}",
                    error,
                )
                Log.d(DetailInteractionLogTag, "detail_interaction_rollback drama=$dramaId action=favorite")
                pendingInteractionOverride.value = pendingInteractionOverride.value.copy(
                    favorited = currentState.isFavorited,
                    favoriteInFlight = false,
                )
                pendingInteractionOverride.value = pendingInteractionOverride.value.copy(
                    favorited = null,
                    favoriteInFlight = false,
                )
            }.onSuccess {
                pendingInteractionOverride.value = pendingInteractionOverride.value.copy(
                    favorited = null,
                    favoriteInFlight = false,
                )
            }
        }
    }
}

@Composable
fun DetailRoute(
    onBack: () -> Unit,
    onPlayEpisode: (String) -> Unit,
    onSubscriptionClick: () -> Unit,
    onDramaClick: (String) -> Unit,
    viewModel: DetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DetailScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onPlayEpisode = onPlayEpisode,
        onSubscriptionClick = onSubscriptionClick,
        onDramaClick = onDramaClick,
    )
}

@Composable
fun DetailScreen(
    uiState: DetailUiState,
    onAction: (DetailAction) -> Unit,
    onBack: () -> Unit,
    onPlayEpisode: (String) -> Unit,
    onSubscriptionClick: () -> Unit,
    onDramaClick: (String) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    DfScreenScaffold {
        DfStateLayout(
            state = uiState.loadState,
            modifier = Modifier.padding(it),
            errorMessage = uiState.errorMessage,
            emptyTitle = "This title is unavailable",
            emptyMessage = "It may be hidden in your region or not scheduled yet.",
            onRetry = { onAction(DetailAction.Retry) },
        ) {
            DfScrollableColumn(modifier = Modifier.padding(it)) {
                Surface(
                    modifier = Modifier.clip(DramaFlowThemeTokens.shapes.pill).clickable(onClick = onBack),
                    color = colors.surface,
                ) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.padding(12.dp))
                }

                uiState.payload?.let { payload ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DramaFlowThemeTokens.shapes.large)
                            .background(DramaFlowThemeTokens.gradients.hero)
                            .padding(spacing.xxl),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                            Text(payload.drama.title, style = DramaFlowThemeTokens.typography.headlineMedium, color = colors.textPrimary)
                            Text(payload.drama.longDescription, style = DramaFlowThemeTokens.typography.bodyLarge, color = colors.textSecondary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                                items(payload.drama.tags) { tag -> DfCategoryChip(label = tag.label) }
                            }
                            DetailInteractionRow(
                                isLiked = uiState.isLiked,
                                isFavorited = uiState.isFavorited,
                                isLikeUpdating = uiState.isLikeUpdating,
                                isFavoriteUpdating = uiState.isFavoriteUpdating,
                                onToggleLike = { onAction(DetailAction.ToggleLike) },
                                onToggleFavorite = { onAction(DetailAction.ToggleFavorite) },
                            )
                            Text("Cast: ${payload.drama.cast.joinToString()}", color = colors.textSecondary)
                            Text(payload.drama.heroNote, color = colors.accentStrong)
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                                DfPrimaryButton(
                                    label = primaryCtaLabel(payload),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val episodeId = payload.watchProgress?.episodeId ?: payload.primaryEpisode?.id
                                        if (episodeId != null) {
                                            val selected = payload.episodes.firstOrNull { it.episode.id == episodeId }
                                            if (selected?.isLockedForUser == true) onSubscriptionClick() else onPlayEpisode(episodeId)
                                        }
                                    },
                                )
                                if (!payload.entitlementState.isPremium && payload.drama.isPremiumSeries) {
                                    DfPrimaryButton(
                                        label = "Unlock premium",
                                        modifier = Modifier.weight(1f),
                                        onClick = onSubscriptionClick,
                                    )
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                        Text("Episodes", style = DramaFlowThemeTokens.typography.titleLarge, color = colors.textPrimary)
                        payload.episodes.forEach { item ->
                            EpisodeRow(
                                item = item,
                                onClick = {
                                    if (item.isLockedForUser) onSubscriptionClick() else onPlayEpisode(item.episode.id)
                                },
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                        Text("More like this", style = DramaFlowThemeTokens.typography.titleLarge, color = colors.textPrimary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                            items(payload.relatedTitles) { card ->
                                DfDramaCard(card = card, onClick = { onDramaClick(card.drama.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInteractionRow(
    isLiked: Boolean,
    isFavorited: Boolean,
    isLikeUpdating: Boolean,
    isFavoriteUpdating: Boolean,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
        DetailInteractionButton(
            icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            label = if (isLiked) "Liked" else "Like",
            selected = isLiked,
            enabled = !isLikeUpdating,
            onClick = onToggleLike,
        )
        DetailInteractionButton(
            icon = if (isFavorited) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            label = if (isFavorited) "Saved" else "Save",
            selected = isFavorited,
            enabled = !isFavoriteUpdating,
            onClick = onToggleFavorite,
        )
    }
}

@Composable
private fun DetailInteractionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        modifier = Modifier
            .clip(DramaFlowThemeTokens.shapes.pill)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (selected) colors.accentStrong else colors.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.textInverse else colors.textPrimary,
            )
            Text(
                text = label,
                color = if (selected) colors.textInverse else colors.textPrimary,
            )
        }
    }
}

@Composable
private fun EpisodeRow(
    item: EpisodeListItem,
    onClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    val watchProgress = item.watchProgress
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = DramaFlowThemeTokens.shapes.medium,
        color = colors.surface,
    ) {
        Row(
            modifier = Modifier.padding(spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ep ${item.episode.episodeNumber} · ${item.episode.title}",
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                val stateLine = when {
                    item.isCurrentEpisode && watchProgress != null -> "Continue from ${(watchProgress.progressPercent * 100).toInt()}%"
                    watchProgress?.completed == true -> "Watched"
                    item.episode.requiresPremium && !item.isLockedForUser -> "Premium unlocked"
                    item.episode.requiresPremium -> "Premium preview"
                    else -> "Free episode"
                }
                Text(stateLine, color = colors.textSecondary)
            }
            Icon(
                imageVector = if (item.isLockedForUser) Icons.Rounded.Lock else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (item.isLockedForUser) colors.warning else colors.accentStrong,
            )
        }
    }
}

private fun primaryCtaLabel(payload: DetailPayload): String {
    return when {
        payload.watchProgress != null -> "Continue watching"
        payload.primaryEpisode?.requiresPremium == true && !payload.entitlementState.isPremium -> "Unlock to watch"
        else -> "Watch now"
    }
}

@Preview
@Composable
private fun DetailPreview() {
    val drama = DramaFlowMockData.dramas.first()
    val payload = DetailPayload(
        drama = drama,
        primaryEpisode = DramaFlowMockData.episodesForDrama(drama.id).first(),
        watchProgress = null,
        entitlementState = com.dramaflow.core.model.EntitlementState(false, null, emptyList(), "free"),
        episodes = DramaFlowMockData.episodesForDrama(drama.id).map {
            EpisodeListItem(it, null, it.requiresPremium, false)
        },
        relatedTitles = DramaFlowMockData.dramas.drop(1).map {
            DramaCard(it, null, false, false, "Updated")
        },
    )
    DramaFlowTheme {
        DetailScreen(
            uiState = DetailUiState(
                loadState = DfLoadState.SUCCESS,
                payload = payload,
                isLiked = true,
                isFavorited = false,
            ),
            onAction = {},
            onBack = {},
            onPlayEpisode = {},
            onSubscriptionClick = {},
            onDramaClick = {},
        )
    }
}
