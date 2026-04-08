package com.dramaflow.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WorkspacePremium
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dramaflow.core.common.DataResult
import com.dramaflow.core.common.DramaFlowMockData
import com.dramaflow.core.common.FeedRepository
import com.dramaflow.core.designsystem.component.DfCategoryChip
import com.dramaflow.core.designsystem.component.DfDramaCard
import com.dramaflow.core.designsystem.component.DfPrimaryButton
import com.dramaflow.core.designsystem.component.DfTopBar
import com.dramaflow.core.designsystem.component.DfWhiteMessageCard
import com.dramaflow.core.designsystem.theme.DramaFlowTheme
import com.dramaflow.core.designsystem.theme.DramaFlowThemeTokens
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.model.DramaTag
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

data class FeedUiState(
    val loadState: DfLoadState = DfLoadState.LOADING,
    val featured: DramaCard? = null,
    val tags: List<DramaTag> = emptyList(),
    val continueWatching: List<DramaCard> = emptyList(),
    val hotTitles: List<DramaCard> = emptyList(),
    val recommendations: List<DramaCard> = emptyList(),
    val selectedTag: String? = null,
    val errorMessage: String = "Unable to refresh the home feed.",
)

sealed interface FeedAction {
    data class SelectTag(val tag: String) : FeedAction
    data object Retry : FeedAction
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
) : androidx.lifecycle.ViewModel() {
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    init {
        observeFeed()
    }

    fun onAction(action: FeedAction) {
        when (action) {
            is FeedAction.SelectTag -> _uiState.value = _uiState.value.copy(selectedTag = action.tag)
            FeedAction.Retry -> observeFeed()
        }
    }

    private fun observeFeed() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            feedRepository.observeFeed().collect { result ->
                _uiState.value = when (result) {
                    DataResult.Loading -> FeedUiState(loadState = DfLoadState.LOADING)
                    DataResult.Empty -> FeedUiState(loadState = DfLoadState.EMPTY)
                    is DataResult.Error -> FeedUiState(loadState = DfLoadState.ERROR, errorMessage = result.message)
                    is DataResult.Success -> FeedUiState(
                        loadState = DfLoadState.SUCCESS,
                        featured = result.value.featured,
                        tags = result.value.tags,
                        continueWatching = result.value.continueWatching,
                        hotTitles = result.value.hotTitles,
                        recommendations = result.value.recommendations,
                        selectedTag = result.value.tags.firstOrNull()?.label,
                    )
                }
            }
        }
    }
}

@Composable
fun FeedRoute(
    onDramaClick: (String) -> Unit,
    onContinueWatching: (String) -> Unit,
    onProfileClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FeedScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onDramaClick = onDramaClick,
        onContinueWatching = onContinueWatching,
        onProfileClick = onProfileClick,
        onSubscriptionClick = onSubscriptionClick,
    )
}

@Composable
fun FeedScreen(
    uiState: FeedUiState,
    onAction: (FeedAction) -> Unit,
    onDramaClick: (String) -> Unit,
    onContinueWatching: (String) -> Unit,
    onProfileClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    DfScreenScaffold {
        DfStateLayout(
            state = uiState.loadState,
            modifier = Modifier.padding(it),
            errorMessage = uiState.errorMessage,
            emptyTitle = "Your feed is warming up",
            emptyMessage = "Once titles are published, your daily stack will appear here.",
            onRetry = { onAction(FeedAction.Retry) },
        ) {
            DfScrollableColumn(modifier = Modifier.padding(it)) {
                DfTopBar(
                    title = "DramaFlow",
                    subtitle = "Emotional cliffhangers, built for one-more-episode behavior",
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            Surface(
                                modifier = Modifier.clip(DramaFlowThemeTokens.shapes.pill).clickable(onClick = onSubscriptionClick),
                                color = colors.surface,
                            ) {
                                Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = colors.accentStrong, modifier = Modifier.padding(12.dp))
                            }
                            Surface(
                                modifier = Modifier.clip(DramaFlowThemeTokens.shapes.pill).clickable(onClick = onProfileClick),
                                color = colors.surface,
                            ) {
                                Icon(Icons.Rounded.Person, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.padding(12.dp))
                            }
                        }
                    },
                )

                Surface(shape = DramaFlowThemeTokens.shapes.medium, color = colors.surface, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textSecondary)
                        Spacer(modifier = Modifier.padding(horizontal = spacing.sm))
                        Text("Search and campaign landing slot", color = colors.textSecondary)
                    }
                }

                uiState.featured?.let { featured ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(DramaFlowThemeTokens.shapes.large)
                            .background(DramaFlowThemeTokens.gradients.hero)
                            .padding(spacing.xxl),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                            Text("Featured tonight", style = DramaFlowThemeTokens.typography.labelLarge, color = colors.accentStrong)
                            Text(featured.drama.title, style = DramaFlowThemeTokens.typography.headlineMedium, color = colors.textPrimary)
                            Text(featured.drama.shortDescription, style = DramaFlowThemeTokens.typography.bodyLarge, color = colors.textSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                                DfPrimaryButton(
                                    label = featured.statusLabel ?: "Watch now",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val targetEpisode = featured.lastProgress?.episodeId ?: DramaFlowMockData.episodesForDrama(featured.drama.id).firstOrNull()?.id
                                        if (targetEpisode != null) onContinueWatching(targetEpisode) else onDramaClick(featured.drama.id)
                                    },
                                )
                                DfPrimaryButton(
                                    label = "View details",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onDramaClick(featured.drama.id) },
                                )
                            }
                        }
                    }
                }

                if (uiState.continueWatching.isNotEmpty()) {
                    SectionRow(
                        title = "Continue watching",
                        items = uiState.continueWatching,
                        onDramaClick = onDramaClick,
                        onContinueWatching = onContinueWatching,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    Text("Browse by vibe", style = DramaFlowThemeTokens.typography.titleLarge, color = colors.textPrimary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm), contentPadding = PaddingValues(end = spacing.md)) {
                        items(uiState.tags) { tag ->
                            DfCategoryChip(
                                label = tag.label,
                                selected = uiState.selectedTag == tag.label,
                                onClick = { onAction(FeedAction.SelectTag(tag.label)) },
                            )
                        }
                    }
                }

                SectionRow(
                    title = "Trending now",
                    items = uiState.hotTitles,
                    onDramaClick = onDramaClick,
                    onContinueWatching = onContinueWatching,
                )

                DfWhiteMessageCard(
                    title = "Live promo slot",
                    body = "This block is reserved for seasonal campaigns, premium conversion pushes, and territory-specific highlights.",
                )

                SectionRow(
                    title = "Because you finish episodes",
                    items = uiState.recommendations,
                    onDramaClick = onDramaClick,
                    onContinueWatching = onContinueWatching,
                )
            }
        }
    }
}

@Composable
private fun SectionRow(
    title: String,
    items: List<DramaCard>,
    onDramaClick: (String) -> Unit,
    onContinueWatching: (String) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text(title, style = DramaFlowThemeTokens.typography.titleLarge, color = colors.textPrimary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md), contentPadding = PaddingValues(end = spacing.md)) {
            items(items) { card ->
                Column(modifier = Modifier.width(190.dp), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    DfDramaCard(card = card, onClick = { onDramaClick(card.drama.id) })
                    card.lastProgress?.let { progress ->
                        DfPrimaryButton(
                            label = "Continue watching",
                            onClick = { onContinueWatching(progress.episodeId) },
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun FeedSuccessPreview() {
    val cards = DramaFlowMockData.dramas.map { it ->
        DramaCard(drama = it, lastProgress = null, isUpdated = true, isLockedForUser = false, statusLabel = "Updated")
    }
    DramaFlowTheme {
        FeedScreen(
            uiState = FeedUiState(
                loadState = DfLoadState.SUCCESS,
                featured = cards.first(),
                tags = DramaFlowMockData.tags,
                continueWatching = cards.take(1),
                hotTitles = cards,
                recommendations = cards.reversed(),
                selectedTag = DramaFlowMockData.tags.first().label,
            ),
            onAction = {},
            onDramaClick = {},
            onContinueWatching = {},
            onProfileClick = {},
            onSubscriptionClick = {},
        )
    }
}
