package com.dramaflow.feature.detail

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
import kotlinx.coroutines.launch

data class DetailUiState(
    val loadState: DfLoadState = DfLoadState.LOADING,
    val payload: DetailPayload? = null,
    val errorMessage: String = "Unable to load the drama details.",
)

sealed interface DetailAction {
    data object Retry : DetailAction
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: CatalogRepository,
    savedStateHandle: SavedStateHandle,
) : androidx.lifecycle.ViewModel() {
    private val dramaId: String = savedStateHandle["dramaId"] ?: "df-neon-vows"
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    init {
        observe()
    }

    fun onAction(action: DetailAction) {
        if (action == DetailAction.Retry) observe()
    }

    private fun observe() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeDramaDetail(dramaId).collect { result ->
                _uiState.value = when (result) {
                    DataResult.Loading -> DetailUiState(loadState = DfLoadState.LOADING)
                    DataResult.Empty -> DetailUiState(loadState = DfLoadState.EMPTY)
                    is DataResult.Error -> DetailUiState(loadState = DfLoadState.ERROR, errorMessage = result.message)
                    is DataResult.Success -> DetailUiState(loadState = DfLoadState.SUCCESS, payload = result.value)
                }
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
            uiState = DetailUiState(loadState = DfLoadState.SUCCESS, payload = payload),
            onAction = {},
            onBack = {},
            onPlayEpisode = {},
            onSubscriptionClick = {},
            onDramaClick = {},
        )
    }
}
