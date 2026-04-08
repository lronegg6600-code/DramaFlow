package com.dramaflow.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.HelpOutline
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dramaflow.core.common.DataResult
import com.dramaflow.core.common.DramaFlowMockData
import com.dramaflow.core.common.EntitlementRepository
import com.dramaflow.core.common.ProfileRepository
import com.dramaflow.core.common.ProgressRepository
import com.dramaflow.core.designsystem.component.DfDramaCard
import com.dramaflow.core.designsystem.component.DfPrimaryButton
import com.dramaflow.core.designsystem.component.DfWhiteMessageCard
import com.dramaflow.core.designsystem.theme.DramaFlowTheme
import com.dramaflow.core.designsystem.theme.DramaFlowThemeTokens
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
import kotlinx.coroutines.launch

data class ProfileUiState(
    val loadState: DfLoadState = DfLoadState.LOADING,
    val payload: ProfilePayload? = null,
    val errorMessage: String = "Unable to load account details.",
)

sealed interface ProfileAction {
    data object Retry : ProfileAction
    data object ResetEntitlement : ProfileAction
    data object ClearWatchState : ProfileAction
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val entitlementRepository: EntitlementRepository,
    private val progressRepository: ProgressRepository,
) : androidx.lifecycle.ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private var observeJob: Job? = null

    init {
        observeProfile()
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Retry -> observeProfile()
            ProfileAction.ResetEntitlement -> viewModelScope.launch { entitlementRepository.reset() }
            ProfileAction.ClearWatchState -> viewModelScope.launch { progressRepository.clearAll() }
        }
    }

    private fun observeProfile() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repository.observeProfile().collect { result ->
                _uiState.value = when (result) {
                    DataResult.Loading -> ProfileUiState(loadState = DfLoadState.LOADING)
                    DataResult.Empty -> ProfileUiState(loadState = DfLoadState.EMPTY)
                    is DataResult.Error -> ProfileUiState(loadState = DfLoadState.ERROR, errorMessage = result.message)
                    is DataResult.Success -> ProfileUiState(loadState = DfLoadState.SUCCESS, payload = result.value)
                }
            }
        }
    }
}

@Composable
fun ProfileRoute(
    onBack: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onContinueWatching: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onSubscriptionClick = onSubscriptionClick,
        onContinueWatching = onContinueWatching,
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onAction: (ProfileAction) -> Unit,
    onBack: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onContinueWatching: (String) -> Unit,
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
                    Icon(Icons.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.padding(12.dp))
                }

                uiState.payload?.let { payload ->
                    Surface(shape = DramaFlowThemeTokens.shapes.large, color = colors.surface) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(spacing.xxl),
                            verticalArrangement = Arrangement.spacedBy(spacing.md),
                        ) {
                            Text(payload.profile.displayName, style = DramaFlowThemeTokens.typography.headlineMedium, color = colors.textPrimary)
                            Text("${payload.profile.countryCode} · ${payload.profile.languageCode}", color = colors.textSecondary)
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
                    LibraryRow(title = "Favorites", items = payload.favorites, onDramaClick = { it.lastProgress?.episodeId?.let(onContinueWatching) })

                    SettingsAction(Icons.Rounded.WorkspacePremium, "Membership", onClick = onSubscriptionClick)
                    SettingsAction(Icons.Rounded.Refresh, "Reset mock entitlement", onClick = { onAction(ProfileAction.ResetEntitlement) })
                    SettingsAction(Icons.Rounded.DeleteOutline, "Clear watch progress", onClick = { onAction(ProfileAction.ClearWatchState) })
                    SettingsAction(Icons.Rounded.HelpOutline, "Help and feedback", onClick = {})
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
private fun LibraryRow(
    title: String,
    items: List<DramaCard>,
    onDramaClick: (DramaCard) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text(title, style = DramaFlowThemeTokens.typography.titleLarge, color = colors.textPrimary)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            items(items) { card ->
                DfDramaCard(card = card, onClick = { onDramaClick(card) })
            }
        }
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
            uiState = ProfileUiState(loadState = DfLoadState.SUCCESS, payload = payload),
            onAction = {},
            onBack = {},
            onSubscriptionClick = {},
            onContinueWatching = {},
        )
    }
}
