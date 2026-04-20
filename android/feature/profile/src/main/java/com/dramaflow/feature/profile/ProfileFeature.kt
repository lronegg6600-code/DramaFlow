package com.dramaflow.feature.profile

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dramaflow.core.common.DataResult
import com.dramaflow.core.common.DramaFlowMockData
import com.dramaflow.core.common.DramaInteractionFlags
import com.dramaflow.core.common.DramaInteractionRepository
import com.dramaflow.core.common.DramaInteractionState
import com.dramaflow.core.common.EntitlementRepository
import com.dramaflow.core.common.ProfileRepository
import com.dramaflow.core.common.ProgressRepository
import com.dramaflow.core.designsystem.component.DfDramaCard
import com.dramaflow.core.designsystem.component.DfPrimaryButton
import com.dramaflow.core.designsystem.component.DfWhiteMessageCard
import com.dramaflow.core.designsystem.theme.DramaFlowTheme
import com.dramaflow.core.designsystem.theme.DramaFlowThemeTokens
import com.dramaflow.core.model.Drama
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.model.ProfilePayload
import com.dramaflow.core.model.WatchHistoryItem
import com.dramaflow.core.ui.DfLoadState
import com.dramaflow.core.ui.DfScreenScaffold
import com.dramaflow.core.ui.DfScrollableColumn
import com.dramaflow.core.ui.DfStateLayout
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val ProfileInteractionLogTag = "ProfileInteraction"
private const val HistoryInteractionLogTag = "HistoryInteraction"

enum class ProfileLibrarySection(val title: String) {
    FAVORITES("Saved"),
    LIKED("Liked"),
}

data class PendingInteractionOverride(
    val liked: Boolean? = null,
    val favorited: Boolean? = null,
    val likeInFlight: Boolean = false,
    val favoriteInFlight: Boolean = false,
)

data class ProfileInteractionItem(
    val card: DramaCard,
    val isLiked: Boolean,
    val isFavorited: Boolean,
    val isLikeUpdating: Boolean = false,
    val isFavoriteUpdating: Boolean = false,
)

data class ProfileHistoryItem(
    val history: WatchHistoryItem,
    val card: DramaCard,
    val episodeLabel: String,
    val progressLabel: String,
    val watchedLabel: String,
    val isLiked: Boolean,
    val isFavorited: Boolean,
    val isLikeUpdating: Boolean = false,
    val isFavoriteUpdating: Boolean = false,
    val isRemoving: Boolean = false,
)

data class ProfileUiState(
    val loadState: DfLoadState = DfLoadState.LOADING,
    val payload: ProfilePayload? = null,
    val selectedSection: ProfileLibrarySection = ProfileLibrarySection.FAVORITES,
    val favoriteItems: List<ProfileInteractionItem> = emptyList(),
    val likedItems: List<ProfileInteractionItem> = emptyList(),
    val historyItems: List<ProfileHistoryItem> = emptyList(),
    val isHistoryClearing: Boolean = false,
    val errorMessage: String = "Unable to load account details.",
)

sealed interface ProfileAction {
    data object Retry : ProfileAction
    data object ResetEntitlement : ProfileAction
    data object ClearWatchState : ProfileAction
    data object ClearHistory : ProfileAction
    data class SelectLibrarySection(val section: ProfileLibrarySection) : ProfileAction
    data class RemoveFavorite(val dramaId: String) : ProfileAction
    data class RemoveLike(val dramaId: String) : ProfileAction
    data class RemoveHistory(val dramaId: String) : ProfileAction
    data class ToggleHistoryLike(val dramaId: String) : ProfileAction
    data class ToggleHistoryFavorite(val dramaId: String) : ProfileAction
}

data class ProfilePendingState(
    val removingFavoriteIds: Set<String> = emptySet(),
    val removingLikeIds: Set<String> = emptySet(),
    val removingHistoryIds: Set<String> = emptySet(),
    val clearingHistory: Boolean = false,
    val interactionOverrides: Map<String, PendingInteractionOverride> = emptyMap(),
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val entitlementRepository: EntitlementRepository,
    private val progressRepository: ProgressRepository,
    private val interactionRepository: DramaInteractionRepository,
) : androidx.lifecycle.ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null
    private val pendingState = MutableStateFlow(ProfilePendingState())

    init {
        Log.d(ProfileInteractionLogTag, "profile_open_favorites")
        observeProfile()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Retry -> observeProfile()
            ProfileAction.ResetEntitlement -> viewModelScope.launch { entitlementRepository.reset() }
            ProfileAction.ClearWatchState -> viewModelScope.launch { progressRepository.clearAll() }
            ProfileAction.ClearHistory -> handleClearHistory()
            is ProfileAction.SelectLibrarySection -> {
                val current = _uiState.value.selectedSection
                if (current != action.section) {
                    Log.d(
                        ProfileInteractionLogTag,
                        if (action.section == ProfileLibrarySection.FAVORITES) {
                            "profile_open_favorites"
                        } else {
                            "profile_open_likes"
                        },
                    )
                    _uiState.value = _uiState.value.copy(selectedSection = action.section)
                }
            }

            is ProfileAction.RemoveFavorite -> handleRemoveFavorite(action.dramaId)
            is ProfileAction.RemoveLike -> handleRemoveLike(action.dramaId)
            is ProfileAction.RemoveHistory -> handleRemoveHistory(action.dramaId)
            is ProfileAction.ToggleHistoryLike -> handleToggleHistoryLike(action.dramaId)
            is ProfileAction.ToggleHistoryFavorite -> handleToggleHistoryFavorite(action.dramaId)
        }
    }

    private fun observeProfile() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                repository.observeProfile(),
                interactionRepository.observeInteractionState(),
                pendingState,
            ) { profileResult, interactionState, pending ->
                Triple(profileResult, interactionState, pending)
            }.collect { (profileResult, interactionState, pending) ->
                _uiState.value = reduceProfileState(
                    profileResult = profileResult,
                    interactionState = interactionState,
                    pendingState = pending,
                    previousState = _uiState.value,
                )
            }
        }
    }

    private fun reduceProfileState(
        profileResult: DataResult<ProfilePayload>,
        interactionState: DramaInteractionState,
        pendingState: ProfilePendingState,
        previousState: ProfileUiState,
    ): ProfileUiState {
        return when (profileResult) {
            DataResult.Loading -> previousState.copy(loadState = DfLoadState.LOADING)
            DataResult.Empty -> previousState.copy(loadState = DfLoadState.EMPTY, payload = null)
            is DataResult.Error -> previousState.copy(
                loadState = DfLoadState.ERROR,
                errorMessage = profileResult.message,
            )

            is DataResult.Success -> {
                val favoriteItems = buildInteractionItems(
                    targetIds = interactionState.favoriteDramaIds - pendingState.removingFavoriteIds,
                    payload = profileResult.value,
                    interactionState = interactionState,
                    pendingState = pendingState,
                )
                val likedItems = buildInteractionItems(
                    targetIds = interactionState.likedDramaIds - pendingState.removingLikeIds,
                    payload = profileResult.value,
                    interactionState = interactionState,
                    pendingState = pendingState,
                )
                val historyItems = if (pendingState.clearingHistory) {
                    emptyList()
                } else {
                    buildHistoryItems(
                        historyItems = profileResult.value.watchHistory,
                        payload = profileResult.value,
                        interactionState = interactionState,
                        pendingState = pendingState,
                    )
                }
                Log.d(
                    HistoryInteractionLogTag,
                    "history_interaction_refresh count=${historyItems.size}",
                )
                Log.d(
                    ProfileInteractionLogTag,
                    "profile_interaction_list_refresh favorites=${favoriteItems.size} liked=${likedItems.size}",
                )
                previousState.copy(
                    loadState = DfLoadState.SUCCESS,
                    payload = profileResult.value,
                    favoriteItems = favoriteItems,
                    likedItems = likedItems,
                    historyItems = historyItems,
                    isHistoryClearing = pendingState.clearingHistory,
                )
            }
        }
    }

    private fun handleRemoveFavorite(dramaId: String) {
        val current = _uiState.value
        if (dramaId in pendingState.value.removingFavoriteIds) return
        if (current.favoriteItems.none { it.card.drama.id == dramaId && it.isFavorited }) return
        Log.d(ProfileInteractionLogTag, "profile_remove_favorite_click drama=$dramaId")
        pendingState.value = pendingState.value.copy(
            removingFavoriteIds = pendingState.value.removingFavoriteIds + dramaId,
        )
        viewModelScope.launch {
            runCatching {
                interactionRepository.toggleFavorite(dramaId)
            }.onFailure { error ->
                Log.e(
                    ProfileInteractionLogTag,
                    "profile_remove_favorite_failed drama=$dramaId message=${error.message}",
                    error,
                )
                pendingState.value = pendingState.value.copy(
                    removingFavoriteIds = pendingState.value.removingFavoriteIds - dramaId,
                )
            }.onSuccess {
                pendingState.value = pendingState.value.copy(
                    removingFavoriteIds = pendingState.value.removingFavoriteIds - dramaId,
                )
            }
        }
    }

    private fun handleRemoveLike(dramaId: String) {
        val current = _uiState.value
        if (dramaId in pendingState.value.removingLikeIds) return
        if (current.likedItems.none { it.card.drama.id == dramaId && it.isLiked }) return
        Log.d(ProfileInteractionLogTag, "profile_remove_like_click drama=$dramaId")
        pendingState.value = pendingState.value.copy(
            removingLikeIds = pendingState.value.removingLikeIds + dramaId,
        )
        viewModelScope.launch {
            runCatching {
                interactionRepository.toggleLike(dramaId)
            }.onFailure { error ->
                Log.e(
                    ProfileInteractionLogTag,
                    "profile_remove_like_failed drama=$dramaId message=${error.message}",
                    error,
                )
                pendingState.value = pendingState.value.copy(
                    removingLikeIds = pendingState.value.removingLikeIds - dramaId,
                )
            }.onSuccess {
                pendingState.value = pendingState.value.copy(
                    removingLikeIds = pendingState.value.removingLikeIds - dramaId,
                )
            }
        }
    }

    private fun handleRemoveHistory(dramaId: String) {
        if (dramaId in pendingState.value.removingHistoryIds) return
        pendingState.value = pendingState.value.copy(
            removingHistoryIds = pendingState.value.removingHistoryIds + dramaId,
        )
        viewModelScope.launch {
            runCatching {
                progressRepository.removeHistoryForDrama(dramaId)
            }.onFailure { error ->
                Log.e(
                    HistoryInteractionLogTag,
                    "history_persist_failed action=remove drama=$dramaId message=${error.message}",
                    error,
                )
                pendingState.value = pendingState.value.copy(
                    removingHistoryIds = pendingState.value.removingHistoryIds - dramaId,
                )
            }.onSuccess {
                pendingState.value = pendingState.value.copy(
                    removingHistoryIds = pendingState.value.removingHistoryIds - dramaId,
                )
            }
        }
    }

    private fun handleClearHistory() {
        if (pendingState.value.clearingHistory) return
        pendingState.value = pendingState.value.copy(clearingHistory = true)
        viewModelScope.launch {
            runCatching {
                progressRepository.clearHistory()
            }.onFailure { error ->
                Log.e(
                    HistoryInteractionLogTag,
                    "history_persist_failed action=clear_all message=${error.message}",
                    error,
                )
                pendingState.value = pendingState.value.copy(clearingHistory = false)
            }.onSuccess {
                pendingState.value = pendingState.value.copy(clearingHistory = false)
            }
        }
    }

    private fun handleToggleHistoryLike(dramaId: String) {
        val item = _uiState.value.historyItems.firstOrNull { it.card.drama.id == dramaId } ?: return
        if (item.isLikeUpdating) return
        val nextLiked = !item.isLiked
        Log.d(HistoryInteractionLogTag, "history_like_click drama=$dramaId nextLiked=$nextLiked")
        updateInteractionOverride(dramaId) { current ->
            current.copy(liked = nextLiked, likeInFlight = true)
        }
        viewModelScope.launch {
            runCatching {
                interactionRepository.toggleLike(dramaId)
            }.onFailure { error ->
                Log.e(
                    HistoryInteractionLogTag,
                    "history_persist_failed action=like drama=$dramaId message=${error.message}",
                    error,
                )
                Log.d(HistoryInteractionLogTag, "history_interaction_rollback drama=$dramaId action=like")
                updateInteractionOverride(dramaId) { current ->
                    current.copy(liked = item.isLiked, likeInFlight = false)
                }
                clearInteractionOverride(dramaId, clearLiked = true, clearFavorited = false)
            }.onSuccess {
                clearInteractionOverride(dramaId, clearLiked = true, clearFavorited = false)
            }
        }
    }

    private fun handleToggleHistoryFavorite(dramaId: String) {
        val item = _uiState.value.historyItems.firstOrNull { it.card.drama.id == dramaId } ?: return
        if (item.isFavoriteUpdating) return
        val nextFavorited = !item.isFavorited
        Log.d(HistoryInteractionLogTag, "history_favorite_click drama=$dramaId nextFavorited=$nextFavorited")
        updateInteractionOverride(dramaId) { current ->
            current.copy(favorited = nextFavorited, favoriteInFlight = true)
        }
        viewModelScope.launch {
            runCatching {
                interactionRepository.toggleFavorite(dramaId)
            }.onFailure { error ->
                Log.e(
                    HistoryInteractionLogTag,
                    "history_persist_failed action=favorite drama=$dramaId message=${error.message}",
                    error,
                )
                Log.d(HistoryInteractionLogTag, "history_interaction_rollback drama=$dramaId action=favorite")
                updateInteractionOverride(dramaId) { current ->
                    current.copy(favorited = item.isFavorited, favoriteInFlight = false)
                }
                clearInteractionOverride(dramaId, clearLiked = false, clearFavorited = true)
            }.onSuccess {
                clearInteractionOverride(dramaId, clearLiked = false, clearFavorited = true)
            }
        }
    }

    private fun updateInteractionOverride(
        dramaId: String,
        transform: (PendingInteractionOverride) -> PendingInteractionOverride,
    ) {
        pendingState.value = pendingState.value.copy(
            interactionOverrides = pendingState.value.interactionOverrides.toMutableMap().apply {
                val current = this[dramaId] ?: PendingInteractionOverride()
                this[dramaId] = transform(current)
            },
        )
    }

    private fun clearInteractionOverride(
        dramaId: String,
        clearLiked: Boolean,
        clearFavorited: Boolean,
    ) {
        pendingState.value = pendingState.value.copy(
            interactionOverrides = pendingState.value.interactionOverrides.toMutableMap().apply {
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
            },
        )
    }
}

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onContinueWatching: (String) -> Unit,
    onDramaClick: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onSubscriptionClick = onSubscriptionClick,
        onContinueWatching = onContinueWatching,
        onDramaClick = onDramaClick,
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    onBack: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onContinueWatching: (String) -> Unit,
    onDramaClick: (String) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    DfScreenScaffold {
        DfStateLayout(
            state = uiState.loadState,
            modifier = Modifier.padding(it),
            errorMessage = uiState.errorMessage,
            emptyTitle = "No profile data yet",
            emptyMessage = "Sign-in surfaces and account sync will populate this page.",
            onRetry = { onAction(ProfileAction.Retry) },
        ) {
            DfScrollableColumn(modifier = Modifier.padding(it)) {
                Surface(
                    modifier = Modifier.clip(DramaFlowThemeTokens.shapes.pill).clickable(onClick = onBack),
                    color = colors.surface,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.padding(12.dp))
                }

                uiState.payload?.let { payload ->
                    Surface(shape = DramaFlowThemeTokens.shapes.large, color = colors.surface) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(spacing.xxl),
                            verticalArrangement = Arrangement.spacedBy(spacing.md),
                        ) {
                            Text(
                                payload.profile.displayName,
                                style = DramaFlowThemeTokens.typography.headlineMedium,
                                color = colors.textPrimary,
                            )
                            Text(
                                "${payload.profile.countryCode} / ${payload.profile.languageCode}",
                                color = colors.textSecondary,
                            )
                            Text(
                                if (payload.entitlementState.isPremium) "Premium active" else "Free member",
                                color = if (payload.entitlementState.isPremium) colors.accentStrong else colors.textPrimary,
                            )
                            Text(payload.unlockedSummary, color = colors.textSecondary)
                        }
                    }

                    payload.continueWatching?.let { card ->
                        DfPrimaryButton(
                            label = "Continue watching",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { card.lastProgress?.episodeId?.let(onContinueWatching) },
                        )
                    }

                    HistorySection(
                        uiState = uiState,
                        onAction = onAction,
                        onContinueWatching = onContinueWatching,
                    )

                    InteractionLibrarySection(
                        uiState = uiState,
                        onAction = onAction,
                        onDramaClick = onDramaClick,
                    )

                    SettingsAction(Icons.Rounded.WorkspacePremium, "Membership", onClick = onSubscriptionClick)
                    SettingsAction(Icons.Rounded.Refresh, "Reset mock entitlement", onClick = { onAction(ProfileAction.ResetEntitlement) })
                    SettingsAction(Icons.Rounded.DeleteOutline, "Clear watch progress", onClick = { onAction(ProfileAction.ClearWatchState) })
                    SettingsAction(Icons.AutoMirrored.Rounded.HelpOutline, "Help and feedback", onClick = {})
                }
            }
        }
    }
}

@Composable
private fun HistorySection(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    onContinueWatching: (String) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Continue watching",
                style = DramaFlowThemeTokens.typography.titleLarge,
                color = colors.textPrimary,
            )
            if (uiState.historyItems.isNotEmpty()) {
                ProfileRemoveAction(
                    label = if (uiState.isHistoryClearing) "Clearing..." else "Clear all",
                    enabled = !uiState.isHistoryClearing,
                    onClick = { onAction(ProfileAction.ClearHistory) },
                )
            }
        }
        if (uiState.historyItems.isEmpty()) {
            DfWhiteMessageCard(
                title = "No recent dramas yet",
                body = "Start watching and continue from here.",
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                items(uiState.historyItems, key = { it.card.drama.id }) { item ->
                    HistoryCard(
                        item = item,
                        onResume = {
                            Log.d(
                                HistoryInteractionLogTag,
                                "history_resume_click drama=${item.card.drama.id} episode=${item.history.episodeId}",
                            )
                            onContinueWatching(item.history.episodeId)
                        },
                        onToggleLike = { onAction(ProfileAction.ToggleHistoryLike(item.card.drama.id)) },
                        onToggleFavorite = { onAction(ProfileAction.ToggleHistoryFavorite(item.card.drama.id)) },
                        onRemove = { onAction(ProfileAction.RemoveHistory(item.card.drama.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    item: ProfileHistoryItem,
    onResume: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        modifier = Modifier.width(280.dp),
        shape = DramaFlowThemeTokens.shapes.large,
        color = colors.whiteCard,
        shadowElevation = DramaFlowThemeTokens.elevation.low,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            DfDramaCard(
                card = item.card,
                modifier = Modifier.width(280.dp),
                onClick = onResume,
            )
            Surface(
                modifier = Modifier
                    .padding(horizontal = spacing.lg)
                    .clip(DramaFlowThemeTokens.shapes.pill)
                    .clickable(onClick = onResume),
                color = colors.accentStrong,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = colors.textInverse)
                    Text("Resume", color = colors.textInverse, style = DramaFlowThemeTokens.typography.labelLarge)
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text(
                    text = item.card.drama.title,
                    style = DramaFlowThemeTokens.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.episodeLabel,
                    style = DramaFlowThemeTokens.typography.labelLarge,
                    color = colors.accentStrong,
                )
                Text(
                    text = item.progressLabel,
                    style = DramaFlowThemeTokens.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Text(
                    text = item.watchedLabel,
                    style = DramaFlowThemeTokens.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                LinearProgressIndicator(
                    progress = { item.history.progressPercent.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.accentStrong,
                    trackColor = colors.surfaceMuted,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    InteractionStatePill(
                        icon = if (item.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        label = if (item.isLiked) "Liked" else "Like",
                        selected = item.isLiked,
                        enabled = !item.isLikeUpdating,
                        onClick = onToggleLike,
                    )
                    InteractionStatePill(
                        icon = if (item.isFavorited) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        label = if (item.isFavorited) "Saved" else "Save",
                        selected = item.isFavorited,
                        enabled = !item.isFavoriteUpdating,
                        onClick = onToggleFavorite,
                    )
                }
                ProfileRemoveAction(
                    label = if (item.isRemoving) "Removing..." else "Remove history",
                    enabled = !item.isRemoving,
                    onClick = onRemove,
                )
            }
        }
    }
}

@Composable
private fun InteractionLibrarySection(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    onDramaClick: (String) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    val selectedItems = when (uiState.selectedSection) {
        ProfileLibrarySection.FAVORITES -> uiState.favoriteItems
        ProfileLibrarySection.LIKED -> uiState.likedItems
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text(
            text = "My library",
            style = DramaFlowThemeTokens.typography.titleLarge,
            color = colors.textPrimary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            ProfileSectionChip(
                label = "Saved (${uiState.favoriteItems.size})",
                selected = uiState.selectedSection == ProfileLibrarySection.FAVORITES,
                onClick = { onAction(ProfileAction.SelectLibrarySection(ProfileLibrarySection.FAVORITES)) },
            )
            ProfileSectionChip(
                label = "Liked (${uiState.likedItems.size})",
                selected = uiState.selectedSection == ProfileLibrarySection.LIKED,
                onClick = { onAction(ProfileAction.SelectLibrarySection(ProfileLibrarySection.LIKED)) },
            )
        }
        if (selectedItems.isEmpty()) {
            val title = if (uiState.selectedSection == ProfileLibrarySection.FAVORITES) {
                "No saved dramas yet"
            } else {
                "No liked dramas yet"
            }
            val body = if (uiState.selectedSection == ProfileLibrarySection.FAVORITES) {
                "Save titles from Recommend or Detail and they will show up here."
            } else {
                "Like titles from Recommend or Detail and they will show up here."
            }
            DfWhiteMessageCard(title = title, body = body)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                items(
                    items = selectedItems,
                    key = { item -> "${uiState.selectedSection.name}:${item.card.drama.id}" },
                ) { item ->
                    ProfileInteractionCard(
                        item = item,
                        selectedSection = uiState.selectedSection,
                        onDramaClick = { onDramaClick(item.card.drama.id) },
                        onRemoveFavorite = { onAction(ProfileAction.RemoveFavorite(item.card.drama.id)) },
                        onRemoveLike = { onAction(ProfileAction.RemoveLike(item.card.drama.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        modifier = Modifier.clip(DramaFlowThemeTokens.shapes.pill).clickable(onClick = onClick),
        shape = DramaFlowThemeTokens.shapes.pill,
        color = if (selected) colors.accentStrong else colors.surface,
    ) {
        Text(
            text = label,
            color = if (selected) colors.textInverse else colors.textPrimary,
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
        )
    }
}

@Composable
private fun ProfileInteractionCard(
    item: ProfileInteractionItem,
    selectedSection: ProfileLibrarySection,
    onDramaClick: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onRemoveLike: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        DfDramaCard(card = item.card, onClick = onDramaClick)
        Text(
            text = item.card.drama.shortDescription,
            style = DramaFlowThemeTokens.typography.bodyMedium,
            color = colors.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = spacing.xs),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            InteractionStatePill(
                icon = if (item.isFavorited) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                label = if (item.isFavorited) "Saved" else "Not saved",
                selected = item.isFavorited,
                enabled = false,
                onClick = {},
            )
            InteractionStatePill(
                icon = if (item.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = if (item.isLiked) "Liked" else "Not liked",
                selected = item.isLiked,
                enabled = false,
                onClick = {},
            )
        }
        val removeFavoriteEnabled = item.isFavorited && !item.isFavoriteUpdating
        val removeLikeEnabled = item.isLiked && !item.isLikeUpdating
        when (selectedSection) {
            ProfileLibrarySection.FAVORITES -> ProfileRemoveAction(
                label = if (item.isFavoriteUpdating) "Removing..." else "Remove from saved",
                enabled = removeFavoriteEnabled,
                onClick = onRemoveFavorite,
            )

            ProfileLibrarySection.LIKED -> ProfileRemoveAction(
                label = if (item.isLikeUpdating) "Removing..." else "Remove from liked",
                enabled = removeLikeEnabled,
                onClick = onRemoveLike,
            )
        }
    }
}

@Composable
private fun InteractionStatePill(
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
        shape = DramaFlowThemeTokens.shapes.pill,
        color = if (selected) colors.accentSoft else colors.surfaceMuted,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.accentStrong else colors.textSecondary,
            )
            Text(
                text = label,
                color = if (selected) colors.accentStrong else colors.textSecondary,
                style = DramaFlowThemeTokens.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun ProfileRemoveAction(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        modifier = Modifier
            .clip(DramaFlowThemeTokens.shapes.pill)
            .clickable(enabled = enabled, onClick = onClick),
        shape = DramaFlowThemeTokens.shapes.pill,
        color = if (enabled) colors.surface else colors.surfaceMuted,
    ) {
        Text(
            text = label,
            color = if (enabled) colors.textPrimary else colors.textSecondary,
            style = DramaFlowThemeTokens.typography.labelLarge,
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.sm),
        )
    }
}

@Composable
private fun SettingsAction(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = DramaFlowThemeTokens.shapes.medium,
        color = colors.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = colors.textPrimary)
            Text(title, color = colors.textPrimary)
        }
    }
}

private fun buildInteractionItems(
    targetIds: Set<String>,
    payload: ProfilePayload,
    interactionState: DramaInteractionState,
    pendingState: ProfilePendingState,
): List<ProfileInteractionItem> {
    if (targetIds.isEmpty()) return emptyList()
    val knownCards = buildKnownProfileCards(payload)
    val orderedIds = orderedInteractionIds(targetIds, payload)
    return orderedIds.mapNotNull { dramaId ->
        val card = knownCards[dramaId] ?: return@mapNotNull null
        val pending = pendingState.interactionOverrides[dramaId]
        ProfileInteractionItem(
            card = card,
            isLiked = pending?.liked ?: (dramaId in interactionState.likedDramaIds),
            isFavorited = pending?.favorited ?: (dramaId in interactionState.favoriteDramaIds),
            isLikeUpdating = dramaId in pendingState.removingLikeIds || pending?.likeInFlight == true,
            isFavoriteUpdating = dramaId in pendingState.removingFavoriteIds || pending?.favoriteInFlight == true,
        )
    }
}

private fun buildHistoryItems(
    historyItems: List<WatchHistoryItem>,
    payload: ProfilePayload,
    interactionState: DramaInteractionState,
    pendingState: ProfilePendingState,
): List<ProfileHistoryItem> {
    if (historyItems.isEmpty()) return emptyList()
    val knownCards = buildKnownProfileCards(payload)
    val flags = interactionState.backfillFlags(historyItems.map { it.dramaId })
    return historyItems
        .sortedByDescending { it.watchedAtEpochMs }
        .filterNot { it.dramaId in pendingState.removingHistoryIds }
        .mapNotNull { history ->
            val card = knownCards[history.dramaId] ?: return@mapNotNull null
            val pending = pendingState.interactionOverrides[history.dramaId]
            val interaction = flags[history.dramaId] ?: DramaInteractionFlags(
                isLiked = false,
                isFavorited = false,
            )
            ProfileHistoryItem(
                history = history,
                card = card.copy(lastProgress = card.lastProgress ?: history.toProgress()),
                episodeLabel = history.episodeLabel(),
                progressLabel = history.progressLabel(),
                watchedLabel = history.watchedLabel(),
                isLiked = pending?.liked ?: interaction.isLiked,
                isFavorited = pending?.favorited ?: interaction.isFavorited,
                isLikeUpdating = pending?.likeInFlight == true,
                isFavoriteUpdating = pending?.favoriteInFlight == true,
                isRemoving = history.dramaId in pendingState.removingHistoryIds,
            )
        }
}

private fun orderedInteractionIds(
    targetIds: Set<String>,
    payload: ProfilePayload,
): List<String> {
    val ordered = mutableListOf<String>()
    payload.watchHistory.map { it.dramaId }
        .filter { it in targetIds }
        .forEach { if (it !in ordered) ordered += it }
    DramaFlowMockData.dramas.map { it.id }
        .filter { it in targetIds }
        .forEach { if (it !in ordered) ordered += it }
    payload.favorites.map { it.drama.id }
        .filter { it in targetIds }
        .forEach { if (it !in ordered) ordered += it }
    payload.continueWatching?.drama?.id
        ?.takeIf { it in targetIds && it !in ordered }
        ?.let { ordered += it }
    return ordered
}

private fun buildKnownProfileCards(
    payload: ProfilePayload,
): Map<String, DramaCard> {
    val knownCards = linkedMapOf<String, DramaCard>()
    payload.favorites.forEach { card ->
        knownCards[card.drama.id] = card
    }
    payload.continueWatching?.let { card ->
        knownCards[card.drama.id] = card
    }
    payload.watchHistory.forEach { history ->
        val drama = DramaFlowMockData.findDrama(history.dramaId) ?: return@forEach
        knownCards.putIfAbsent(
            history.dramaId,
            drama.toProfileInteractionCard(payload, history.toProgress()),
        )
    }
    DramaFlowMockData.dramas.forEach { drama ->
        knownCards.putIfAbsent(drama.id, drama.toProfileInteractionCard(payload, null))
    }
    return knownCards
}

private fun Drama.toProfileInteractionCard(
    payload: ProfilePayload,
    progress: com.dramaflow.core.model.WatchProgress?,
): DramaCard {
    val continueCard = payload.continueWatching?.takeIf { it.drama.id == id }
    return DramaCard(
        drama = this,
        lastProgress = continueCard?.lastProgress ?: progress,
        isUpdated = isFeatured,
        isLockedForUser = isPremiumSeries && !payload.entitlementState.isPremium,
        statusLabel = when {
            continueCard != null || progress != null -> "Continue watching"
            isPremiumSeries && !payload.entitlementState.isPremium -> "Premium"
            isFeatured -> "Updated"
            else -> null
        },
    )
}

private fun WatchHistoryItem.toProgress(): com.dramaflow.core.model.WatchProgress {
    return com.dramaflow.core.model.WatchProgress(
        dramaId = dramaId,
        episodeId = episodeId,
        positionMs = positionMs,
        durationMs = durationMs,
        progressPercent = progressPercent,
        lastUpdatedEpochMs = watchedAtEpochMs,
        completed = progressPercent >= 0.98f,
    )
}

private fun WatchHistoryItem.episodeLabel(): String {
    return if (episodeNumber > 0) "Episode $episodeNumber" else episodeTitle
}

private fun WatchHistoryItem.progressLabel(): String {
    val safeDurationMs = durationMs.takeIf { it > 0 } ?: 1L
    val positionText = formatDuration(positionMs)
    val percentText = "${(progressPercent.coerceIn(0f, 1f) * 100).toInt()}%"
    return "Continue from $positionText • $percentText watched"
}

private fun WatchHistoryItem.watchedLabel(): String {
    val now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
    val watchedDate = Instant.ofEpochMilli(watchedAtEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
    return when {
        watchedDate == now -> "Watched today"
        watchedDate == now.minusDays(1) -> "Watched yesterday"
        else -> "Watched ${watchedDate.format(DateTimeFormatter.ofPattern("MMM d"))}"
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

private fun DramaInteractionState.backfillFlags(dramaIds: Collection<String>): Map<String, DramaInteractionFlags> {
    return dramaIds.associateWith { dramaId ->
        DramaInteractionFlags(
            isLiked = dramaId in likedDramaIds,
            isFavorited = dramaId in favoriteDramaIds,
        )
    }
}

@Preview
@Composable
private fun ProfilePreview() {
    val payload = ProfilePayload(
        profile = DramaFlowMockData.profile,
        entitlementState = com.dramaflow.core.model.EntitlementState(true, "premium_access", emptyList(), "mock"),
        continueWatching = DramaCard(DramaFlowMockData.dramas.first(), null, false, false, "Continue watching"),
        watchHistory = listOf(
            WatchHistoryItem(
                dramaId = "df-neon-vows",
                episodeId = "df-neon-vows-e2",
                dramaTitle = "Neon Vows",
                episodeTitle = "Episode 2",
                episodeNumber = 2,
                artworkUrl = DramaFlowMockData.dramas.first().portraitPosterUrl,
                watchedAtEpochMs = System.currentTimeMillis(),
                progressPercent = 0.42f,
                positionMs = 84_000L,
                durationMs = 200_000L,
            ),
        ),
        favorites = DramaFlowMockData.dramas.map { DramaCard(it, null, false, false, "Updated") },
        unlockedSummary = "Premium unlocked",
    )
    DramaFlowTheme {
        ProfileScreen(
            uiState = ProfileUiState(
                loadState = DfLoadState.SUCCESS,
                payload = payload,
                favoriteItems = buildInteractionItems(
                    targetIds = setOf("df-neon-vows", "df-midnight-contract"),
                    payload = payload,
                    interactionState = DramaInteractionState(
                        favoriteDramaIds = setOf("df-neon-vows", "df-midnight-contract"),
                        likedDramaIds = setOf("df-midnight-contract"),
                    ),
                    pendingState = ProfilePendingState(),
                ),
                likedItems = buildInteractionItems(
                    targetIds = setOf("df-midnight-contract"),
                    payload = payload,
                    interactionState = DramaInteractionState(
                        favoriteDramaIds = setOf("df-neon-vows", "df-midnight-contract"),
                        likedDramaIds = setOf("df-midnight-contract"),
                    ),
                    pendingState = ProfilePendingState(),
                ),
                historyItems = buildHistoryItems(
                    historyItems = payload.watchHistory,
                    payload = payload,
                    interactionState = DramaInteractionState(
                        favoriteDramaIds = setOf("df-neon-vows", "df-midnight-contract"),
                        likedDramaIds = setOf("df-midnight-contract"),
                    ),
                    pendingState = ProfilePendingState(),
                ),
            ),
            onAction = {},
            onBack = {},
            onSubscriptionClick = {},
            onContinueWatching = {},
            onDramaClick = {},
        )
    }
}
