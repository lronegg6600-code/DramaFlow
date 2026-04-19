package com.dramaflow.feature.profile

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
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

private const val ProfileInteractionLogTag = "ProfileInteraction"

enum class ProfileLibrarySection(val title: String) {
    FAVORITES("Saved"),
    LIKED("Liked"),
}

data class ProfileInteractionItem(
    val card: DramaCard,
    val isLiked: Boolean,
    val isFavorited: Boolean,
    val isLikeUpdating: Boolean = false,
    val isFavoriteUpdating: Boolean = false,
)

data class ProfileUiState(
    val loadState: DfLoadState = DfLoadState.LOADING,
    val payload: ProfilePayload? = null,
    val selectedSection: ProfileLibrarySection = ProfileLibrarySection.FAVORITES,
    val favoriteItems: List<ProfileInteractionItem> = emptyList(),
    val likedItems: List<ProfileInteractionItem> = emptyList(),
    val errorMessage: String = "Unable to load account details.",
)

sealed interface ProfileAction {
    data object Retry : ProfileAction
    data object ResetEntitlement : ProfileAction
    data object ClearWatchState : ProfileAction
    data class SelectLibrarySection(val section: ProfileLibrarySection) : ProfileAction
    data class RemoveFavorite(val dramaId: String) : ProfileAction
    data class RemoveLike(val dramaId: String) : ProfileAction
}

data class ProfilePendingInteractionState(
    val removingFavoriteIds: Set<String> = emptySet(),
    val removingLikeIds: Set<String> = emptySet(),
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
    private val pendingInteractionState = MutableStateFlow(ProfilePendingInteractionState())

    init {
        Log.d(ProfileInteractionLogTag, "profile_open_favorites")
        observeProfile()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Retry -> observeProfile()
            ProfileAction.ResetEntitlement -> viewModelScope.launch { entitlementRepository.reset() }
            ProfileAction.ClearWatchState -> viewModelScope.launch { progressRepository.clearAll() }
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
        }
    }

    private fun observeProfile() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                repository.observeProfile(),
                interactionRepository.observeInteractionState(),
                pendingInteractionState,
            ) { profileResult, interactionState, pendingState ->
                Triple(profileResult, interactionState, pendingState)
            }.collect { (profileResult, interactionState, pendingState) ->
                _uiState.value = reduceProfileState(
                    profileResult = profileResult,
                    interactionState = interactionState,
                    pendingState = pendingState,
                    previousState = _uiState.value,
                )
            }
        }
    }

    private fun reduceProfileState(
        profileResult: DataResult<ProfilePayload>,
        interactionState: DramaInteractionState,
        pendingState: ProfilePendingInteractionState,
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
                Log.d(
                    ProfileInteractionLogTag,
                    "profile_interaction_list_refresh favorites=${favoriteItems.size} liked=${likedItems.size}",
                )
                previousState.copy(
                    loadState = DfLoadState.SUCCESS,
                    payload = profileResult.value,
                    favoriteItems = favoriteItems,
                    likedItems = likedItems,
                )
            }
        }
    }

    private fun handleRemoveFavorite(dramaId: String) {
        val current = _uiState.value
        if (dramaId in pendingInteractionState.value.removingFavoriteIds) {
            return
        }
        if (current.favoriteItems.none { it.card.drama.id == dramaId && it.isFavorited }) {
            return
        }
        Log.d(ProfileInteractionLogTag, "profile_remove_favorite_click drama=$dramaId")
        pendingInteractionState.value = pendingInteractionState.value.copy(
            removingFavoriteIds = pendingInteractionState.value.removingFavoriteIds + dramaId,
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
                pendingInteractionState.value = pendingInteractionState.value.copy(
                    removingFavoriteIds = pendingInteractionState.value.removingFavoriteIds - dramaId,
                )
            }.onSuccess {
                pendingInteractionState.value = pendingInteractionState.value.copy(
                    removingFavoriteIds = pendingInteractionState.value.removingFavoriteIds - dramaId,
                )
            }
        }
    }

    private fun handleRemoveLike(dramaId: String) {
        val current = _uiState.value
        if (dramaId in pendingInteractionState.value.removingLikeIds) {
            return
        }
        if (current.likedItems.none { it.card.drama.id == dramaId && it.isLiked }) {
            return
        }
        Log.d(ProfileInteractionLogTag, "profile_remove_like_click drama=$dramaId")
        pendingInteractionState.value = pendingInteractionState.value.copy(
            removingLikeIds = pendingInteractionState.value.removingLikeIds + dramaId,
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
                pendingInteractionState.value = pendingInteractionState.value.copy(
                    removingLikeIds = pendingInteractionState.value.removingLikeIds - dramaId,
                )
            }.onSuccess {
                pendingInteractionState.value = pendingInteractionState.value.copy(
                    removingLikeIds = pendingInteractionState.value.removingLikeIds - dramaId,
                )
            }
        }
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

                    HistoryRow(title = "Recently watched", items = payload.watchHistory.map { it.title })

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
private fun HistoryRow(
    title: String,
    items: List<String>,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text(title, style = DramaFlowThemeTokens.typography.titleLarge, color = colors.textPrimary)
        items.forEach { entry ->
            DfWhiteMessageCard(title = entry, body = "Recent watch history item")
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
            )
            InteractionStatePill(
                icon = if (item.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = if (item.isLiked) "Liked" else "Not liked",
                selected = item.isLiked,
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
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
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
            modifier = Modifier.fillMaxWidth().padding(spacing.lg),
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
    pendingState: ProfilePendingInteractionState,
): List<ProfileInteractionItem> {
    if (targetIds.isEmpty()) return emptyList()
    val knownCards = buildKnownProfileCards(payload)
    val orderedIds = orderedInteractionIds(targetIds, payload)
    return orderedIds.mapNotNull { dramaId ->
        val card = knownCards[dramaId] ?: return@mapNotNull null
        ProfileInteractionItem(
            card = card,
            isLiked = dramaId in interactionState.likedDramaIds,
            isFavorited = dramaId in interactionState.favoriteDramaIds,
            isLikeUpdating = dramaId in pendingState.removingLikeIds,
            isFavoriteUpdating = dramaId in pendingState.removingFavoriteIds,
        )
    }
}

private fun orderedInteractionIds(
    targetIds: Set<String>,
    payload: ProfilePayload,
): List<String> {
    val ordered = mutableListOf<String>()
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
    DramaFlowMockData.dramas.forEach { drama ->
        knownCards.putIfAbsent(drama.id, drama.toProfileInteractionCard(payload))
    }
    return knownCards
}

private fun Drama.toProfileInteractionCard(
    payload: ProfilePayload,
): DramaCard {
    val continueCard = payload.continueWatching?.takeIf { it.drama.id == id }
    return DramaCard(
        drama = this,
        lastProgress = continueCard?.lastProgress,
        isUpdated = isFeatured,
        isLockedForUser = isPremiumSeries && !payload.entitlementState.isPremium,
        statusLabel = when {
            continueCard != null -> "Continue watching"
            isPremiumSeries && !payload.entitlementState.isPremium -> "Premium"
            isFeatured -> "Updated"
            else -> null
        },
    )
}

@Preview
@Composable
private fun ProfilePreview() {
    val payload = ProfilePayload(
        profile = DramaFlowMockData.profile,
        entitlementState = com.dramaflow.core.model.EntitlementState(true, "premium_access", emptyList(), "mock"),
        continueWatching = DramaCard(DramaFlowMockData.dramas.first(), null, false, false, "Continue watching"),
        watchHistory = emptyList(),
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
                    pendingState = ProfilePendingInteractionState(),
                ),
                likedItems = buildInteractionItems(
                    targetIds = setOf("df-midnight-contract"),
                    payload = payload,
                    interactionState = DramaInteractionState(
                        favoriteDramaIds = setOf("df-neon-vows", "df-midnight-contract"),
                        likedDramaIds = setOf("df-midnight-contract"),
                    ),
                    pendingState = ProfilePendingInteractionState(),
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
