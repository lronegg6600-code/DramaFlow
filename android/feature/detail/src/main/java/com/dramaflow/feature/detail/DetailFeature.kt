package com.dramaflow.feature.detail

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.dramaflow.core.common.CatalogRepository
import com.dramaflow.core.common.DataResult
import com.dramaflow.core.common.DramaFlowMockData
import com.dramaflow.core.common.DramaInteractionRepository
import com.dramaflow.core.designsystem.component.DfCategoryChip
import com.dramaflow.core.designsystem.component.DfDramaCard
import com.dramaflow.core.designsystem.component.DfPrimaryButton
import com.dramaflow.core.designsystem.component.DfWhiteMessageCard
import com.dramaflow.core.designsystem.theme.DramaFlowTheme
import com.dramaflow.core.designsystem.theme.DramaFlowThemeTokens
import com.dramaflow.core.model.DetailPayload
import com.dramaflow.core.model.Drama
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.model.EpisodeListItem
import com.dramaflow.core.model.WatchProgress
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

enum class DetailSectionTab(val label: String) {
    BASIC("Info"),
    REVIEWS("Reviews"),
    ORIGINAL("Original"),
    RELATED("Related"),
    FOR_YOU("For you"),
}

data class DetailHeaderUiModel(
    val title: String,
    val totalEpisodesLabel: String,
    val heatLabel: String,
    val statusLabel: String,
    val posterUrl: String,
    val tags: List<String>,
    val heroNote: String,
    val isPremiumSeries: Boolean,
)

data class DetailCtaState(
    val watchLabel: String,
    val watchSubLabel: String,
    val episodeId: String?,
    val requiresSubscription: Boolean,
    val saveLabel: String,
)

data class DetailReviewItem(
    val id: String,
    val author: String,
    val avatarSeed: String,
    val summary: String,
    val body: String,
    val likeCount: String,
    val timeLabel: String,
)

data class DetailOriginalEntry(
    val title: String,
    val subtitle: String,
    val description: String,
    val actionLabel: String,
)

data class DetailCastMember(
    val name: String,
    val role: String,
    val initials: String,
)

data class DetailSeriesFacts(
    val releaseLabel: String,
    val episodeLabel: String,
    val accessLabel: String,
    val resumeLabel: String,
)

data class DetailUiState(
    val loadState: DfLoadState = DfLoadState.LOADING,
    val payload: DetailPayload? = null,
    val errorMessage: String = "Unable to load the drama details.",
    val isLiked: Boolean = false,
    val isFavorited: Boolean = false,
    val isLikeUpdating: Boolean = false,
    val isFavoriteUpdating: Boolean = false,
    val synopsisExpanded: Boolean = false,
    val selectedSection: DetailSectionTab = DetailSectionTab.BASIC,
    val header: DetailHeaderUiModel? = null,
    val ctaState: DetailCtaState? = null,
    val cast: List<DetailCastMember> = emptyList(),
    val reviews: List<DetailReviewItem> = emptyList(),
    val originalEntry: DetailOriginalEntry? = null,
    val relatedTitles: List<DramaCard> = emptyList(),
    val recommendedTitles: List<DramaCard> = emptyList(),
    val facts: DetailSeriesFacts? = null,
)

sealed interface DetailAction {
    data object Retry : DetailAction
    data object ToggleLike : DetailAction
    data object ToggleFavorite : DetailAction
    data object ToggleSynopsisExpanded : DetailAction
    data class SelectSection(val section: DetailSectionTab) : DetailAction
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
            DetailAction.ToggleSynopsisExpanded -> _uiState.value = _uiState.value.copy(
                synopsisExpanded = !_uiState.value.synopsisExpanded,
            )
            is DetailAction.SelectSection -> _uiState.value = _uiState.value.copy(selectedSection = action.section)
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
                    DataResult.Loading -> _uiState.value.copy(
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
                        synopsisExpanded = _uiState.value.synopsisExpanded,
                        selectedSection = _uiState.value.selectedSection,
                    )

                    is DataResult.Error -> DetailUiState(
                        loadState = DfLoadState.ERROR,
                        errorMessage = detailResult.message,
                        isLiked = pendingOverride.liked ?: interactionFlags.isLiked,
                        isFavorited = pendingOverride.favorited ?: interactionFlags.isFavorited,
                        isLikeUpdating = pendingOverride.likeInFlight,
                        isFavoriteUpdating = pendingOverride.favoriteInFlight,
                        synopsisExpanded = _uiState.value.synopsisExpanded,
                        selectedSection = _uiState.value.selectedSection,
                    )

                    is DataResult.Success -> detailResult.value.toUiState(
                        liked = pendingOverride.liked ?: interactionFlags.isLiked,
                        favorited = pendingOverride.favorited ?: interactionFlags.isFavorited,
                        likeUpdating = pendingOverride.likeInFlight,
                        favoriteUpdating = pendingOverride.favoriteInFlight,
                        synopsisExpanded = _uiState.value.synopsisExpanded,
                        selectedSection = _uiState.value.selectedSection,
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
    DfScreenScaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(innerPadding),
        ) {
            DfStateLayout(
                state = uiState.loadState,
                modifier = Modifier.fillMaxSize(),
                errorMessage = uiState.errorMessage,
                emptyTitle = "This title is unavailable",
                emptyMessage = "It may be hidden in your region or not scheduled yet.",
                onRetry = { onAction(DetailAction.Retry) },
            ) {
                DfScrollableColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.lg),
                ) {
                    uiState.payload?.let { payload ->
                        DetailTopBar(onBack = onBack)
                        Spacer(modifier = Modifier.height(spacing.lg))
                        DetailHeaderSection(header = uiState.header ?: payload.toHeader())
                        Spacer(modifier = Modifier.height(spacing.lg))
                        DetailEngagementRow(
                            isLiked = uiState.isLiked,
                            isFavorited = uiState.isFavorited,
                            isLikeUpdating = uiState.isLikeUpdating,
                            isFavoriteUpdating = uiState.isFavoriteUpdating,
                            onToggleLike = { onAction(DetailAction.ToggleLike) },
                            onToggleFavorite = { onAction(DetailAction.ToggleFavorite) },
                        )
                        Spacer(modifier = Modifier.height(spacing.lg))
                        DetailSectionTabs(
                            selectedSection = uiState.selectedSection,
                            onSelect = { onAction(DetailAction.SelectSection(it)) },
                        )
                        Spacer(modifier = Modifier.height(spacing.lg))
                        when (uiState.selectedSection) {
                            DetailSectionTab.BASIC -> DetailBasicInfoSection(
                                payload = payload,
                                facts = uiState.facts ?: payload.toSeriesFacts(),
                                cast = uiState.cast,
                                synopsisExpanded = uiState.synopsisExpanded,
                                onToggleSynopsis = { onAction(DetailAction.ToggleSynopsisExpanded) },
                                onPlayEpisode = { episodeId ->
                                    if (episodeId != null) {
                                        val selected = payload.episodes.firstOrNull { it.episode.id == episodeId }
                                        if (selected?.isLockedForUser == true) onSubscriptionClick() else onPlayEpisode(episodeId)
                                    }
                                },
                            )

                            DetailSectionTab.REVIEWS -> DetailReviewsSection(reviews = uiState.reviews)
                            DetailSectionTab.ORIGINAL -> DetailOriginalSection(entry = uiState.originalEntry)
                            DetailSectionTab.RELATED -> DetailRecommendationSection(
                                title = "Related titles",
                                subtitle = "Same tone, same audience, faster path into another binge.",
                                items = uiState.relatedTitles,
                                onDramaClick = onDramaClick,
                            )

                            DetailSectionTab.FOR_YOU -> DetailRecommendationSection(
                                title = "You may also like",
                                subtitle = "Curated from your current drama taste and recent viewing rhythm.",
                                items = uiState.recommendedTitles,
                                onDramaClick = onDramaClick,
                            )
                        }
                        Spacer(modifier = Modifier.height(128.dp))
                    }
                }
            }

            uiState.ctaState?.let { cta ->
                DetailBottomBar(
                    ctaState = cta,
                    isFavorited = uiState.isFavorited,
                    isFavoriteUpdating = uiState.isFavoriteUpdating,
                    onToggleFavorite = { onAction(DetailAction.ToggleFavorite) },
                    onPlay = {
                        val episodeId = cta.episodeId ?: return@DetailBottomBar
                        if (cta.requiresSubscription) onSubscriptionClick() else onPlayEpisode(episodeId)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DetailTopBar(
    onBack: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.clip(DramaFlowThemeTokens.shapes.pill).clickable(onClick = onBack),
            color = colors.surface.copy(alpha = 0.92f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = colors.textPrimary)
                Text("Back", color = colors.textPrimary, style = DramaFlowThemeTokens.typography.labelLarge)
            }
        }
        Surface(
            modifier = Modifier.clip(DramaFlowThemeTokens.shapes.pill),
            color = colors.surface.copy(alpha = 0.92f),
        ) {
            Icon(
                Icons.Rounded.MoreHoriz,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            )
        }
    }
}

@Composable
private fun DetailHeaderSection(
    header: DetailHeaderUiModel,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        shape = DramaFlowThemeTokens.shapes.large,
        color = colors.surface,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.accentSoft.copy(alpha = 0.88f),
                            colors.surface.copy(alpha = 0.98f),
                        ),
                    ),
                )
                .padding(spacing.lg),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
                AsyncImage(
                    model = header.posterUrl,
                    contentDescription = header.title,
                    modifier = Modifier
                        .width(112.dp)
                        .height(156.dp)
                        .clip(DramaFlowThemeTokens.shapes.medium),
                    contentScale = ContentScale.Crop,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                ) {
                    Text(
                        text = header.title,
                        style = DramaFlowThemeTokens.typography.headlineSmall,
                        color = colors.textPrimary,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DetailInfoPill(
                            label = header.totalEpisodesLabel,
                            color = colors.surface.copy(alpha = 0.94f),
                        )
                        DetailInfoPill(
                            label = header.heatLabel,
                            color = colors.surface.copy(alpha = 0.94f),
                        )
                        if (header.isPremiumSeries) {
                            DetailInfoPill(
                                label = "Premium",
                                color = colors.warning.copy(alpha = 0.18f),
                                textColor = colors.warning,
                            )
                        }
                    }
                    Text(
                        text = header.statusLabel,
                        style = DramaFlowThemeTokens.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        header.tags.forEach { tag ->
                            DfCategoryChip(label = tag)
                        }
                    }
                    Text(
                        text = header.heroNote,
                        style = DramaFlowThemeTokens.typography.bodyMedium,
                        color = colors.accentStrong,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailInfoPill(
    label: String,
    color: Color,
    textColor: Color = DramaFlowThemeTokens.colors.textPrimary,
) {
    val spacing = DramaFlowThemeTokens.spacing
    Surface(shape = DramaFlowThemeTokens.shapes.pill, color = color) {
        Text(
            text = label,
            color = textColor,
            style = DramaFlowThemeTokens.typography.labelMedium,
            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
        )
    }
}

@Composable
private fun DetailEngagementRow(
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
private fun DetailSectionTabs(
    selectedSection: DetailSectionTab,
    onSelect: (DetailSectionTab) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        DetailSectionTab.entries.forEach { section ->
            val selected = section == selectedSection
            Surface(
                modifier = Modifier
                    .clip(DramaFlowThemeTokens.shapes.pill)
                    .clickable { onSelect(section) },
                shape = DramaFlowThemeTokens.shapes.pill,
                color = if (selected) DramaFlowThemeTokens.colors.accentStrong else DramaFlowThemeTokens.colors.surface,
            ) {
                Text(
                    text = section.label,
                    color = if (selected) DramaFlowThemeTokens.colors.textInverse else DramaFlowThemeTokens.colors.textSecondary,
                    style = DramaFlowThemeTokens.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun DetailBasicInfoSection(
    payload: DetailPayload,
    facts: DetailSeriesFacts,
    cast: List<DetailCastMember>,
    synopsisExpanded: Boolean,
    onToggleSynopsis: () -> Unit,
    onPlayEpisode: (String?) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        DetailSectionCard(title = "Synopsis") {
            Text(
                text = payload.drama.longDescription,
                style = DramaFlowThemeTokens.typography.bodyLarge,
                color = DramaFlowThemeTokens.colors.textSecondary,
                maxLines = if (synopsisExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = if (synopsisExpanded) "Collapse" else "Expand",
                style = DramaFlowThemeTokens.typography.labelLarge,
                color = DramaFlowThemeTokens.colors.accentStrong,
                modifier = Modifier.clickable(onClick = onToggleSynopsis),
            )
        }

        DetailSectionCard(title = "Series status") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                DetailFactRow("Release", facts.releaseLabel)
                DetailFactRow("Episodes", facts.episodeLabel)
                DetailFactRow("Access", facts.accessLabel)
                DetailFactRow("Continue", facts.resumeLabel)
            }
        }

        DetailSectionCard(title = "Continue watching") {
            Text(
                text = facts.resumeLabel,
                color = DramaFlowThemeTokens.colors.textPrimary,
                style = DramaFlowThemeTokens.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(
                text = payload.watchProgress?.let { progress ->
                    "Resume from ${progress.progressPercent.toPercentLabel()} in Episode ${episodeNumberForProgress(payload, progress)}."
                } ?: "Start from the opening episode and keep your progress synced automatically.",
                color = DramaFlowThemeTokens.colors.textSecondary,
                style = DramaFlowThemeTokens.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(spacing.md))
            DetailInlinePrimaryAction(
                label = payload.watchProgress?.let {
                    "Continue Episode ${episodeNumberForProgress(payload, it)}"
                } ?: "Start Episode 1",
                onClick = { onPlayEpisode(payload.watchProgress?.episodeId ?: payload.primaryEpisode?.id) },
            )
        }

        DetailSectionCard(title = "Cast") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                items(cast) { member ->
                    DetailCastCard(member = member)
                }
            }
        }

        DetailSectionCard(title = "Episodes") {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                payload.episodes.take(6).forEach { item ->
                    EpisodeRow(
                        item = item,
                        onClick = { onPlayEpisode(item.episode.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    Surface(shape = DramaFlowThemeTokens.shapes.large, color = DramaFlowThemeTokens.colors.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = title,
                style = DramaFlowThemeTokens.typography.titleLarge,
                color = DramaFlowThemeTokens.colors.textPrimary,
            )
            content()
        }
    }
}

@Composable
private fun DetailFactRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = DramaFlowThemeTokens.colors.textSecondary)
        Text(
            text = value,
            color = DramaFlowThemeTokens.colors.textPrimary,
            style = DramaFlowThemeTokens.typography.labelLarge,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun DetailInlinePrimaryAction(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clip(DramaFlowThemeTokens.shapes.pill).clickable(onClick = onClick),
        shape = DramaFlowThemeTokens.shapes.pill,
        color = DramaFlowThemeTokens.colors.accentStrong,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DramaFlowThemeTokens.spacing.lg, vertical = DramaFlowThemeTokens.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(DramaFlowThemeTokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = DramaFlowThemeTokens.colors.textInverse)
            Text(
                text = label,
                color = DramaFlowThemeTokens.colors.textInverse,
                style = DramaFlowThemeTokens.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun DetailCastCard(
    member: DetailCastMember,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Column(
        modifier = Modifier.width(108.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = member.initials,
                style = DramaFlowThemeTokens.typography.titleMedium,
                color = colors.accentStrong,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = member.name,
            color = colors.textPrimary,
            style = DramaFlowThemeTokens.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = member.role,
            color = colors.textSecondary,
            style = DramaFlowThemeTokens.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DetailReviewsSection(
    reviews: List<DetailReviewItem>,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        DetailSectionCard(title = "Audience reviews") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = "${reviews.size} reviews",
                        color = colors.textPrimary,
                        style = DramaFlowThemeTokens.typography.titleMedium,
                    )
                    Text(
                        text = "Rating input can connect to a real review endpoint later.",
                        color = colors.textSecondary,
                        style = DramaFlowThemeTokens.typography.bodyMedium,
                    )
                }
                DetailInfoPill(
                    label = "Rate +",
                    color = colors.accentSoft,
                    textColor = colors.accentStrong,
                )
            }
        }

        reviews.forEach { review ->
            Surface(shape = DramaFlowThemeTokens.shapes.large, color = colors.surface) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.accentSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = review.avatarSeed.take(2).uppercase(),
                                color = colors.accentStrong,
                                style = DramaFlowThemeTokens.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(review.author, color = colors.textPrimary, style = DramaFlowThemeTokens.typography.labelLarge)
                            Text(review.summary, color = colors.textSecondary, style = DramaFlowThemeTokens.typography.labelMedium)
                        }
                        Text(review.timeLabel, color = colors.textSecondary, style = DramaFlowThemeTokens.typography.labelMedium)
                    }
                    Text(
                        text = review.body,
                        color = colors.textPrimary,
                        style = DramaFlowThemeTokens.typography.bodyMedium,
                    )
                    HorizontalDivider(color = colors.surfaceMuted)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("${review.likeCount} likes", color = colors.textSecondary, style = DramaFlowThemeTokens.typography.labelMedium)
                        Text("More", color = colors.accentStrong, style = DramaFlowThemeTokens.typography.labelMedium)
                    }
                }
            }
        }

        Surface(shape = DramaFlowThemeTokens.shapes.large, color = colors.surfaceMuted) {
            Text(
                text = "See more audience reactions",
                color = colors.textPrimary,
                style = DramaFlowThemeTokens.typography.labelLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.lg),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DetailOriginalSection(
    entry: DetailOriginalEntry?,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    val original = entry ?: DetailOriginalEntry(
        title = "Original story unavailable",
        subtitle = "Structure ready for novel or book-center integration",
        description = "This section is reserved for book metadata, acquisition source, and launch routing when a real original IP feed is available.",
        actionLabel = "Coming soon",
    )
    Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        DetailSectionCard(title = "Original story") {
            Surface(shape = DramaFlowThemeTokens.shapes.large, color = colors.whiteCard) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    Text(original.title, color = colors.textPrimary, style = DramaFlowThemeTokens.typography.titleMedium)
                    Text(original.subtitle, color = colors.accentStrong, style = DramaFlowThemeTokens.typography.labelLarge)
                    Text(original.description, color = colors.textSecondary, style = DramaFlowThemeTokens.typography.bodyMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(original.actionLabel, color = colors.textPrimary, style = DramaFlowThemeTokens.typography.labelLarge)
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRecommendationSection(
    title: String,
    subtitle: String,
    items: List<DramaCard>,
    onDramaClick: (String) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.lg)) {
        DetailSectionCard(title = title) {
            Text(
                text = subtitle,
                color = DramaFlowThemeTokens.colors.textSecondary,
                style = DramaFlowThemeTokens.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(spacing.md))
            if (items.isEmpty()) {
                DfWhiteMessageCard(
                    title = "Nothing to surface yet",
                    body = "Related recommendations will appear when the catalog or recommendation feed has more matching titles.",
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                    items(items) { card ->
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            DfDramaCard(card = card, onClick = { onDramaClick(card.drama.id) })
                            Text(
                                text = card.drama.shortDescription,
                                color = DramaFlowThemeTokens.colors.textSecondary,
                                style = DramaFlowThemeTokens.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.width(156.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailBottomBar(
    ctaState: DetailCtaState,
    isFavorited: Boolean,
    isFavoriteUpdating: Boolean,
    onToggleFavorite: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        modifier = modifier,
        color = colors.background.copy(alpha = 0.96f),
        shadowElevation = DramaFlowThemeTokens.elevation.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .clip(DramaFlowThemeTokens.shapes.medium)
                    .clickable(enabled = !isFavoriteUpdating, onClick = onToggleFavorite),
                shape = DramaFlowThemeTokens.shapes.medium,
                color = if (isFavorited) colors.accentSoft else colors.surface,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = null,
                        tint = if (isFavorited) colors.accentStrong else colors.textPrimary,
                    )
                    Text(
                        text = ctaState.saveLabel,
                        color = if (isFavorited) colors.accentStrong else colors.textPrimary,
                        style = DramaFlowThemeTokens.typography.labelMedium,
                    )
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = DramaFlowThemeTokens.shapes.large,
                color = Color.Transparent,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    Text(
                        text = ctaState.watchSubLabel,
                        color = colors.textSecondary,
                        style = DramaFlowThemeTokens.typography.labelMedium,
                    )
                    DfPrimaryButton(
                        label = ctaState.watchLabel,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onPlay,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailInteractionButton(
    icon: ImageVector,
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = DramaFlowThemeTokens.shapes.medium,
        color = colors.whiteCard,
    ) {
        Row(
            modifier = Modifier.padding(spacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Episode ${item.episode.episodeNumber} 路 ${item.episode.title}",
                    color = colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                val stateLine = when {
                    item.isCurrentEpisode && watchProgress != null -> "Continue from ${(watchProgress.progressPercent * 100).toInt()}%"
                    watchProgress?.completed == true -> "Watched"
                    item.episode.requiresPremium && !item.isLockedForUser -> "Premium unlocked"
                    item.episode.requiresPremium -> "Preview before unlock"
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

private fun DetailPayload.toUiState(
    liked: Boolean,
    favorited: Boolean,
    likeUpdating: Boolean,
    favoriteUpdating: Boolean,
    synopsisExpanded: Boolean,
    selectedSection: DetailSectionTab,
): DetailUiState {
    return DetailUiState(
        loadState = DfLoadState.SUCCESS,
        payload = this,
        isLiked = liked,
        isFavorited = favorited,
        isLikeUpdating = likeUpdating,
        isFavoriteUpdating = favoriteUpdating,
        synopsisExpanded = synopsisExpanded,
        selectedSection = selectedSection,
        header = toHeader(),
        ctaState = toCtaState(favorited),
        cast = drama.cast.mapIndexed { index, name -> name.toCastMember(index) },
        reviews = buildDetailReviews(drama),
        originalEntry = buildOriginalEntry(drama),
        relatedTitles = relatedTitles,
        recommendedTitles = buildRecommendedTitles(drama.id, entitlementState.isPremium),
        facts = toSeriesFacts(),
    )
}

private fun DetailPayload.toHeader(): DetailHeaderUiModel {
    val totalEpisodes = listOf(
        drama.totalEpisodes,
        episodes.size,
        primaryEpisode?.episodeNumber ?: 0,
    ).maxOrNull()?.takeIf { it > 0 } ?: 1
    val heatLabel = drama.heatScore
        .takeIf { score -> score.any(Char::isDigit) }
        ?.let { "$it heat" }
        ?: "Hot right now"
    val lockState = if (entitlementState.isPremium || !drama.isPremiumSeries) {
        "Ready to watch now"
    } else {
        "Premium episodes unlock after subscription"
    }
    return DetailHeaderUiModel(
        title = drama.title,
        totalEpisodesLabel = "$totalEpisodes episodes",
        heatLabel = heatLabel,
        statusLabel = lockState,
        posterUrl = drama.portraitPosterUrl,
        tags = drama.tags.map { it.label } + drama.labels.take(2),
        heroNote = drama.heroNote,
        isPremiumSeries = drama.isPremiumSeries,
    )
}

private fun DetailPayload.toSeriesFacts(): DetailSeriesFacts {
    val totalEpisodes = listOf(
        drama.totalEpisodes,
        episodes.size,
        primaryEpisode?.episodeNumber ?: 0,
    ).maxOrNull()?.takeIf { it > 0 } ?: 1
    val release = if (episodes.size >= drama.totalEpisodes) "Completed run" else "Still releasing"
    val access = when {
        entitlementState.isPremium -> "Premium unlocked"
        drama.isPremiumSeries -> "Premium series with free preview"
        else -> "Free to watch"
    }
    val resume = watchProgress?.let { progress ->
        "Continue from Episode ${episodeNumberForProgress(this, progress)}"
    } ?: "Start from Episode ${primaryEpisode?.episodeNumber ?: 1}"
    return DetailSeriesFacts(
        releaseLabel = release,
        episodeLabel = "$totalEpisodes total episodes",
        accessLabel = access,
        resumeLabel = resume,
    )
}

private fun DetailPayload.toCtaState(
    favorited: Boolean,
): DetailCtaState {
    val progress = watchProgress
    val episode = progress?.let {
        episodes.firstOrNull { it.episode.id == progress.episodeId }?.episode
    } ?: primaryEpisode
    val requiresSubscription = episode?.requiresPremium == true && !entitlementState.isPremium && !episode.isPreviewEnabled
    val watchLabel = when {
        progress != null -> "Continue watching"
        requiresSubscription -> "Unlock to watch"
        else -> "Start watching"
    }
    val watchSubLabel = when {
        progress != null -> "Episode ${episodeNumberForProgress(this, progress)}"
        episode != null -> "Episode ${episode.episodeNumber}"
        else -> "Episode 1"
    }
    return DetailCtaState(
        watchLabel = watchLabel,
        watchSubLabel = watchSubLabel,
        episodeId = episode?.id,
        requiresSubscription = requiresSubscription,
        saveLabel = if (favorited) "Saved" else "Save",
    )
}

private fun episodeNumberForProgress(
    payload: DetailPayload,
    progress: WatchProgress,
): Int {
    return payload.episodes.firstOrNull { it.episode.id == progress.episodeId }?.episode?.episodeNumber ?: 1
}

private fun String.toCastMember(index: Int): DetailCastMember {
    val role = when (index) {
        0 -> "Lead"
        1 -> "Second lead"
        else -> "Supporting"
    }
    val initials = split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { take(2).uppercase() }
    return DetailCastMember(
        name = this,
        role = role,
        initials = initials,
    )
}

private fun buildDetailReviews(
    drama: Drama,
): List<DetailReviewItem> {
    return listOf(
        DetailReviewItem(
            id = "${drama.id}-review-1",
            author = "Avery Chen",
            avatarSeed = "AC",
            summary = "Pulled me in after the first twist",
            body = "The pacing is exactly what I want from a short-form drama. The cliffhangers land quickly, and the chemistry is strong enough to justify continuing into the premium arc.",
            likeCount = "2.1k",
            timeLabel = "Today",
        ),
        DetailReviewItem(
            id = "${drama.id}-review-2",
            author = "Mina Foster",
            avatarSeed = "MF",
            summary = "Good binge structure",
            body = "Episodes move fast, and the second lead is more interesting than expected. The detail page should make it obvious where to resume, and now it does.",
            likeCount = "845",
            timeLabel = "Yesterday",
        ),
        DetailReviewItem(
            id = "${drama.id}-review-3",
            author = "Dylan Hart",
            avatarSeed = "DH",
            summary = "Worth opening the next episode",
            body = "I came from the feed preview and stayed because the drama card, cast, and episode positioning made the story feel trustworthy instead of random.",
            likeCount = "402",
            timeLabel = "2d ago",
        ),
    )
}

private fun buildOriginalEntry(
    drama: Drama,
): DetailOriginalEntry {
    return DetailOriginalEntry(
        title = "${drama.title}: Original Story",
        subtitle = "Adapted from a serialized relationship thriller",
        description = "Follow the source material, compare plot turns, and prepare a future jump into the novel, book detail, or web experience without changing this detail page structure.",
        actionLabel = "Open original entry",
    )
}

private fun buildRecommendedTitles(
    currentDramaId: String,
    isPremium: Boolean,
): List<DramaCard> {
    return DramaFlowMockData.dramas
        .filter { it.id != currentDramaId }
        .shuffled()
        .map {
            DramaCard(
                drama = it,
                lastProgress = null,
                isUpdated = it.isFeatured,
                isLockedForUser = it.isPremiumSeries && !isPremium,
                statusLabel = if (it.isPremiumSeries && !isPremium) "Premium" else "Recommended",
            )
        }
}

private fun Float.toPercentLabel(): String {
    return "${(coerceIn(0f, 1f) * 100).toInt()}%"
}

@Preview
@Composable
private fun DetailPreview() {
    val drama = DramaFlowMockData.dramas.first()
    val payload = DetailPayload(
        drama = drama,
        primaryEpisode = DramaFlowMockData.episodesForDrama(drama.id).first(),
        watchProgress = WatchProgress(
            dramaId = drama.id,
            episodeId = "df-neon-vows-e2",
            positionMs = 48_000L,
            durationMs = 180_000L,
            progressPercent = 0.42f,
            lastUpdatedEpochMs = System.currentTimeMillis(),
            completed = false,
        ),
        entitlementState = com.dramaflow.core.model.EntitlementState(false, null, emptyList(), "free"),
        episodes = DramaFlowMockData.episodesForDrama(drama.id).mapIndexed { index, episode ->
            EpisodeListItem(
                episode = episode,
                watchProgress = if (index == 1) {
                    WatchProgress(
                        dramaId = drama.id,
                        episodeId = episode.id,
                        positionMs = 48_000L,
                        durationMs = 180_000L,
                        progressPercent = 0.42f,
                        lastUpdatedEpochMs = System.currentTimeMillis(),
                        completed = false,
                    )
                } else {
                    null
                },
                isLockedForUser = episode.requiresPremium,
                isCurrentEpisode = index == 1,
            )
        },
        relatedTitles = DramaFlowMockData.dramas.drop(1).map {
            DramaCard(it, null, false, false, "Updated")
        },
    )
    DramaFlowTheme {
        DetailScreen(
            uiState = payload.toUiState(
                liked = true,
                favorited = false,
                likeUpdating = false,
                favoriteUpdating = false,
                synopsisExpanded = false,
                selectedSection = DetailSectionTab.BASIC,
            ),
            onAction = {},
            onBack = {},
            onPlayEpisode = {},
            onSubscriptionClick = {},
            onDramaClick = {},
        )
    }
}
