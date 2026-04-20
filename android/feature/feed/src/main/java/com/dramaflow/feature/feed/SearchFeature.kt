package com.dramaflow.feature.feed

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.dramaflow.core.common.DramaFlowMockData
import com.dramaflow.core.common.DramaInteractionFlags
import com.dramaflow.core.common.DramaInteractionRepository
import com.dramaflow.core.common.DramaInteractionState
import com.dramaflow.core.common.EntitlementRepository
import com.dramaflow.core.designsystem.component.DfEmptyCard
import com.dramaflow.core.designsystem.component.DfErrorCard
import com.dramaflow.core.designsystem.component.DfLoadingIndicator
import com.dramaflow.core.designsystem.component.DfWhiteMessageCard
import com.dramaflow.core.designsystem.theme.DramaFlowThemeTokens
import com.dramaflow.core.model.Drama
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.model.EntitlementState
import com.dramaflow.core.model.canAccessDrama
import com.dramaflow.core.ui.DfLoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private const val SearchInteractionLogTag = "SearchInteraction"

data class SearchQuickEntry(
    val id: String,
    val label: String,
    val icon: ImageVector,
)

data class HotSearchItem(
    val rank: Int,
    val title: String,
    val heatText: String,
)

data class SearchResultItem(
    val card: DramaCard,
    val isLiked: Boolean,
    val isFavorited: Boolean,
    val isLikeUpdating: Boolean = false,
    val isFavoriteUpdating: Boolean = false,
)

data class SearchPendingInteractionOverride(
    val liked: Boolean? = null,
    val favorited: Boolean? = null,
    val likeInFlight: Boolean = false,
    val favoriteInFlight: Boolean = false,
)

data class SearchContentState(
    val keyword: String = "",
    val discoveryState: DfLoadState = DfLoadState.SUCCESS,
    val resultState: DfLoadState = DfLoadState.SUCCESS,
    val history: List<String> = emptyList(),
    val quickEntries: List<SearchQuickEntry> = defaultQuickEntries(),
    val suggestCards: List<DramaCard> = emptyList(),
    val hotSearches: List<HotSearchItem> = emptyList(),
    val rawResultItems: List<DramaCard> = emptyList(),
    val errorMessage: String = "Search failed. Please try again.",
)

data class SearchUiState(
    val keyword: String = "",
    val discoveryState: DfLoadState = DfLoadState.SUCCESS,
    val resultState: DfLoadState = DfLoadState.SUCCESS,
    val history: List<String> = emptyList(),
    val quickEntries: List<SearchQuickEntry> = defaultQuickEntries(),
    val suggestCards: List<DramaCard> = emptyList(),
    val hotSearches: List<HotSearchItem> = emptyList(),
    val resultItems: List<SearchResultItem> = emptyList(),
    val errorMessage: String = "Search failed. Please try again.",
)

sealed interface SearchAction {
    data class UpdateKeyword(val keyword: String) : SearchAction
    data class SubmitSearch(val keyword: String) : SearchAction
    data class ClickHistory(val keyword: String) : SearchAction
    data class ToggleLike(val dramaId: String) : SearchAction
    data class ToggleFavorite(val dramaId: String) : SearchAction
    data object ClearHistory : SearchAction
    data object Retry : SearchAction
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val historyStore: SearchHistoryStore,
    private val interactionRepository: DramaInteractionRepository,
    private val entitlementRepository: EntitlementRepository,
) : androidx.lifecycle.ViewModel() {
    private val contentState = MutableStateFlow(
        SearchContentState(
            history = historyStore.load(),
            suggestCards = DramaFlowMockData.dramas.map { drama ->
                drama.toSearchDramaCard(statusLabel = "All ${drama.totalEpisodes} episodes")
            }.take(6),
            hotSearches = DramaFlowMockData.dramas.mapIndexed { index, drama ->
                HotSearchItem(
                    rank = index + 1,
                    title = drama.title,
                    heatText = "${drama.heatScore} trend score",
                )
            },
        ),
    )
    private val pendingInteractionOverrides =
        MutableStateFlow<Map<String, SearchPendingInteractionOverride>>(emptyMap())
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                contentState,
                interactionRepository.observeInteractionState(),
                pendingInteractionOverrides,
                entitlementRepository.observeEntitlement(),
            ) { content, interactionState, pendingOverrides, entitlementState ->
                SearchCombinedState(content, interactionState, pendingOverrides, entitlementState)
            }.collect { combined ->
                _uiState.value = reduceUiState(
                    content = combined.content,
                    interactionState = combined.interactionState,
                    pendingOverrides = combined.pendingOverrides,
                    entitlementState = combined.entitlementState,
                )
            }
        }
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.UpdateKeyword -> {
                contentState.value = contentState.value.copy(keyword = action.keyword)
            }

            is SearchAction.SubmitSearch -> submit(action.keyword)
            is SearchAction.ClickHistory -> {
                contentState.value = contentState.value.copy(keyword = action.keyword)
                submit(action.keyword)
            }

            is SearchAction.ToggleLike -> handleToggleLike(action.dramaId)
            is SearchAction.ToggleFavorite -> handleToggleFavorite(action.dramaId)

            SearchAction.ClearHistory -> {
                historyStore.clear()
                contentState.value = contentState.value.copy(history = emptyList())
            }

            SearchAction.Retry -> submit(contentState.value.keyword)
        }
    }

    private fun submit(rawKeyword: String) {
        val keyword = rawKeyword.trim()
        searchJob?.cancel()
        if (keyword.isBlank()) {
            contentState.value = contentState.value.copy(
                resultState = DfLoadState.SUCCESS,
                rawResultItems = emptyList(),
                keyword = "",
            )
            return
        }
        searchJob = viewModelScope.launch {
            contentState.value = contentState.value.copy(
                resultState = DfLoadState.LOADING,
                keyword = keyword,
            )
            delay(160)
            if (keyword.equals("error", ignoreCase = true)) {
                contentState.value = contentState.value.copy(
                    resultState = DfLoadState.ERROR,
                    rawResultItems = emptyList(),
                )
                return@launch
            }
            val matched = DramaFlowMockData.dramas.filter { drama ->
                drama.title.contains(keyword, ignoreCase = true) ||
                    drama.shortDescription.contains(keyword, ignoreCase = true) ||
                    drama.tags.any { it.label.contains(keyword, ignoreCase = true) } ||
                    drama.cast.any { it.contains(keyword, ignoreCase = true) }
            }.map { drama ->
                drama.toSearchDramaCard(statusLabel = "${drama.totalEpisodes} episodes")
            }
            val history = historyStore.push(keyword)
            contentState.value = contentState.value.copy(
                history = history,
                rawResultItems = matched,
                resultState = if (matched.isEmpty()) DfLoadState.EMPTY else DfLoadState.SUCCESS,
            )
        }
    }

    private fun reduceUiState(
        content: SearchContentState,
        interactionState: DramaInteractionState,
        pendingOverrides: Map<String, SearchPendingInteractionOverride>,
        entitlementState: EntitlementState,
    ): SearchUiState {
        val flags = interactionRepository.backfill(
            dramaIds = content.rawResultItems.map { it.drama.id },
            state = interactionState,
        )
        val resultItems = content.rawResultItems.map { card ->
            val entitledCard = card.withEntitlement(entitlementState)
            val currentFlags = flags[card.drama.id] ?: DramaInteractionFlags(
                isLiked = false,
                isFavorited = false,
            )
            val pending = pendingOverrides[card.drama.id]
            SearchResultItem(
                card = entitledCard,
                isLiked = pending?.liked ?: currentFlags.isLiked,
                isFavorited = pending?.favorited ?: currentFlags.isFavorited,
                isLikeUpdating = pending?.likeInFlight == true,
                isFavoriteUpdating = pending?.favoriteInFlight == true,
            )
        }
        Log.d(
            SearchInteractionLogTag,
            "search_result_interaction_refresh keyword=${content.keyword} count=${resultItems.size}",
        )
        return SearchUiState(
            keyword = content.keyword,
            discoveryState = content.discoveryState,
            resultState = content.resultState,
            history = content.history,
            quickEntries = content.quickEntries,
            suggestCards = content.suggestCards,
            hotSearches = content.hotSearches,
            resultItems = resultItems,
            errorMessage = content.errorMessage,
        )
    }

    private fun handleToggleLike(dramaId: String) {
        val currentItem = _uiState.value.resultItems.firstOrNull { it.card.drama.id == dramaId } ?: return
        if (currentItem.isLikeUpdating) return
        val nextLiked = !currentItem.isLiked
        Log.d(SearchInteractionLogTag, "search_like_click drama=$dramaId nextLiked=$nextLiked")
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
                    SearchInteractionLogTag,
                    "search_interaction_persist_failed drama=$dramaId action=like message=${error.message}",
                    error,
                )
                Log.d(SearchInteractionLogTag, "search_interaction_rollback drama=$dramaId action=like")
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
                clearPendingField(
                    dramaId = dramaId,
                    clearLiked = true,
                    clearFavorited = false,
                )
            }
        }
    }

    private fun handleToggleFavorite(dramaId: String) {
        val currentItem = _uiState.value.resultItems.firstOrNull { it.card.drama.id == dramaId } ?: return
        if (currentItem.isFavoriteUpdating) return
        val nextFavorited = !currentItem.isFavorited
        Log.d(SearchInteractionLogTag, "search_favorite_click drama=$dramaId nextFavorited=$nextFavorited")
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
                    SearchInteractionLogTag,
                    "search_interaction_persist_failed drama=$dramaId action=favorite message=${error.message}",
                    error,
                )
                Log.d(SearchInteractionLogTag, "search_interaction_rollback drama=$dramaId action=favorite")
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
                clearPendingField(
                    dramaId = dramaId,
                    clearLiked = false,
                    clearFavorited = true,
                )
            }
        }
    }

    private fun updatePendingOverride(
        dramaId: String,
        transform: (SearchPendingInteractionOverride) -> SearchPendingInteractionOverride,
    ) {
        pendingInteractionOverrides.value = pendingInteractionOverrides.value.toMutableMap().apply {
            val current = this[dramaId] ?: SearchPendingInteractionOverride()
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
}

private data class SearchCombinedState(
    val content: SearchContentState,
    val interactionState: DramaInteractionState,
    val pendingOverrides: Map<String, SearchPendingInteractionOverride>,
    val entitlementState: EntitlementState,
)

@Singleton
class SearchHistoryStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("dramaflow_search", Context.MODE_PRIVATE)
    private val key = "history"

    fun load(): List<String> {
        val raw = prefs.getString(key, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split("|").filter { it.isNotBlank() }
    }

    fun push(keyword: String): List<String> {
        val normalized = keyword.trim()
        if (normalized.isBlank()) return load()
        val latest = (listOf(normalized) + load().filterNot { it.equals(normalized, ignoreCase = true) }).take(10)
        prefs.edit().putString(key, latest.joinToString("|")).apply()
        return latest
    }

    fun clear() {
        prefs.edit().remove(key).apply()
    }
}

@Composable
fun SearchRoute(
    onBack: () -> Unit,
    onDramaClick: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        onDramaClick = onDramaClick,
    )
}

@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit,
    onBack: () -> Unit,
    onDramaClick: (String) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = spacing.lg, vertical = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = colors.textPrimary,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Spacer(modifier = Modifier.width(spacing.md))
            OutlinedTextField(
                value = uiState.keyword,
                onValueChange = { onAction(SearchAction.UpdateKeyword(it)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Search title, actor, genre") },
            )
            Spacer(modifier = Modifier.width(spacing.md))
            Text(
                text = "Search",
                color = colors.accentStrong,
                style = DramaFlowThemeTokens.typography.titleMedium,
                modifier = Modifier.clickable { onAction(SearchAction.SubmitSearch(uiState.keyword)) },
            )
        }

        if (uiState.keyword.isBlank()) {
            DiscoveryContent(
                uiState = uiState,
                onAction = onAction,
                onDramaClick = onDramaClick,
            )
        } else {
            SearchResultContent(
                uiState = uiState,
                onAction = onAction,
                onDramaClick = onDramaClick,
            )
        }
    }
}

@Composable
private fun DiscoveryContent(
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit,
    onDramaClick: (String) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors

    when (uiState.discoveryState) {
        DfLoadState.LOADING -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DfLoadingIndicator()
        }

        DfLoadState.EMPTY -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DfEmptyCard(
                title = "Nothing to discover yet",
                message = "Suggestions and hot search will appear when data is available.",
            )
        }

        DfLoadState.ERROR -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DfErrorCard(
                message = uiState.errorMessage,
                actionLabel = "Retry",
                onAction = { onAction(SearchAction.Retry) },
            )
        }

        DfLoadState.SUCCESS -> {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                contentPadding = PaddingValues(bottom = spacing.section),
            ) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        items(uiState.quickEntries) { entry ->
                            Surface(
                                modifier = Modifier.clickable { onAction(SearchAction.UpdateKeyword(entry.label)) },
                                shape = DramaFlowThemeTokens.shapes.pill,
                                color = colors.surface,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(entry.icon, contentDescription = null, tint = colors.accentStrong)
                                    Spacer(modifier = Modifier.width(spacing.xs))
                                    Text(entry.label, color = colors.textPrimary)
                                }
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Search history",
                            style = DramaFlowThemeTokens.typography.titleLarge,
                            color = colors.textPrimary,
                        )
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Clear history",
                            tint = colors.textSecondary,
                            modifier = Modifier.clickable { onAction(SearchAction.ClearHistory) },
                        )
                    }
                }
                item {
                    if (uiState.history.isEmpty()) {
                        DfWhiteMessageCard(
                            title = "No search history",
                            body = "Keywords are persisted after search submission.",
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                            items(uiState.history) { keyword ->
                                Surface(
                                    modifier = Modifier.clickable { onAction(SearchAction.ClickHistory(keyword)) },
                                    shape = DramaFlowThemeTokens.shapes.pill,
                                    color = colors.surface,
                                ) {
                                    Text(
                                        text = keyword,
                                        modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                                        color = colors.textPrimary,
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = "You may also search",
                        style = DramaFlowThemeTokens.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                        items(uiState.suggestCards) { card ->
                            Surface(
                                modifier = Modifier
                                    .width(140.dp)
                                    .clickable { onDramaClick(card.drama.id) },
                                color = colors.whiteCard,
                                shape = DramaFlowThemeTokens.shapes.medium,
                            ) {
                                Column {
                                    AsyncImage(
                                        model = card.drama.portraitPosterUrl,
                                        contentDescription = card.drama.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                        contentScale = ContentScale.Crop,
                                    )
                                    Column(
                                        modifier = Modifier.padding(spacing.sm),
                                        verticalArrangement = Arrangement.spacedBy(spacing.xs),
                                    ) {
                                        Text(
                                            text = card.drama.title,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "All ${card.drama.totalEpisodes} episodes",
                                            color = colors.textSecondary,
                                            style = DramaFlowThemeTokens.typography.labelMedium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = "Hot search ranking",
                        style = DramaFlowThemeTokens.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                }
                itemsIndexed(uiState.hotSearches) { _, item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction(SearchAction.SubmitSearch(item.title)) },
                        shape = DramaFlowThemeTokens.shapes.medium,
                        color = colors.whiteCard,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.rank.toString(),
                                    color = colors.accentStrong,
                                    style = DramaFlowThemeTokens.typography.titleMedium,
                                )
                                Spacer(modifier = Modifier.width(spacing.md))
                                Text(item.title, color = colors.textPrimary)
                            }
                            Text(item.heatText, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultContent(
    uiState: SearchUiState,
    onAction: (SearchAction) -> Unit,
    onDramaClick: (String) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    when (uiState.resultState) {
        DfLoadState.LOADING -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DfLoadingIndicator()
        }

        DfLoadState.EMPTY -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DfEmptyCard(
                title = "No result",
                message = "Try another keyword.",
            )
        }

        DfLoadState.ERROR -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DfErrorCard(
                message = uiState.errorMessage,
                actionLabel = "Retry",
                onAction = { onAction(SearchAction.Retry) },
            )
        }

        DfLoadState.SUCCESS -> {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(spacing.md),
                contentPadding = PaddingValues(bottom = spacing.section),
            ) {
                items(uiState.resultItems, key = { it.card.drama.id }) { item ->
                    SearchResultCard(
                        item = item,
                        onDramaClick = { onDramaClick(item.card.drama.id) },
                        onToggleLike = { onAction(SearchAction.ToggleLike(item.card.drama.id)) },
                        onToggleFavorite = { onAction(SearchAction.ToggleFavorite(item.card.drama.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: SearchResultItem,
    onDramaClick: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDramaClick),
        shape = DramaFlowThemeTokens.shapes.medium,
        color = colors.whiteCard,
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            AsyncImage(
                model = item.card.drama.portraitPosterUrl,
                contentDescription = item.card.drama.title,
                modifier = Modifier
                    .width(92.dp)
                    .height(124.dp),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    text = item.card.drama.title,
                    style = DramaFlowThemeTokens.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = item.card.drama.shortDescription,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.card.isLockedForUser || !item.card.statusLabel.isNullOrBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (item.card.isLockedForUser) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = colors.warning,
                            )
                        }
                        Text(
                            text = item.card.statusLabel ?: if (item.card.isLockedForUser) "Premium" else "",
                            color = if (item.card.isLockedForUser) colors.warning else colors.textSecondary,
                            style = DramaFlowThemeTokens.typography.labelMedium,
                        )
                    }
                }
                Text(
                    text = item.card.drama.tags.joinToString(" · ") { it.label },
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SearchInteractionChip(
                        icon = if (item.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        label = if (item.isLiked) "Liked" else "Like",
                        selected = item.isLiked,
                        enabled = !item.isLikeUpdating,
                        onClick = onToggleLike,
                    )
                    SearchInteractionChip(
                        icon = if (item.isFavorited) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        label = if (item.isFavorited) "Saved" else "Save",
                        selected = item.isFavorited,
                        enabled = !item.isFavoriteUpdating,
                        onClick = onToggleFavorite,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchInteractionChip(
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
        color = when {
            selected -> colors.accentSoft
            enabled -> colors.surface
            else -> colors.surfaceMuted
        },
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

private fun defaultQuickEntries(): List<SearchQuickEntry> {
    return listOf(
        SearchQuickEntry("actor", "Actor", Icons.Rounded.Person),
        SearchQuickEntry("category", "Category", Icons.Rounded.Apps),
        SearchQuickEntry("reserve", "Reserve", Icons.Rounded.CalendarMonth),
        SearchQuickEntry("new", "New", Icons.Rounded.SmartDisplay),
        SearchQuickEntry("smart", "Suggest", Icons.Rounded.AutoAwesome),
    )
}

private fun Drama.toSearchDramaCard(
    statusLabel: String,
): DramaCard {
    return DramaCard(
        drama = this,
        lastProgress = null,
        isUpdated = isFeatured,
        isLockedForUser = false,
        statusLabel = statusLabel,
    )
}

private fun DramaCard.withEntitlement(
    entitlementState: EntitlementState,
): DramaCard {
    val isLocked = !entitlementState.canAccessDrama(drama)
    return copy(
        isLockedForUser = isLocked,
        statusLabel = when {
            isLocked -> "Premium"
            statusLabel == "Premium" -> "${drama.totalEpisodes} episodes"
            else -> statusLabel
        },
    )
}
