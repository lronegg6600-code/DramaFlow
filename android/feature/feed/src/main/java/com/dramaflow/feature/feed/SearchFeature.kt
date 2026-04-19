package com.dramaflow.feature.feed

import android.content.Context
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.dramaflow.core.common.DramaFlowMockData
import com.dramaflow.core.designsystem.component.DfEmptyCard
import com.dramaflow.core.designsystem.component.DfErrorCard
import com.dramaflow.core.designsystem.component.DfLoadingIndicator
import com.dramaflow.core.designsystem.component.DfWhiteMessageCard
import com.dramaflow.core.designsystem.theme.DramaFlowThemeTokens
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.ui.DfLoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

data class SearchUiState(
    val keyword: String = "",
    val discoveryState: DfLoadState = DfLoadState.SUCCESS,
    val resultState: DfLoadState = DfLoadState.SUCCESS,
    val history: List<String> = emptyList(),
    val quickEntries: List<SearchQuickEntry> = defaultQuickEntries(),
    val suggestCards: List<DramaCard> = emptyList(),
    val hotSearches: List<HotSearchItem> = emptyList(),
    val resultItems: List<DramaCard> = emptyList(),
    val errorMessage: String = "Search failed. Please try again.",
)

sealed interface SearchAction {
    data class UpdateKeyword(val keyword: String) : SearchAction
    data class SubmitSearch(val keyword: String) : SearchAction
    data class ClickHistory(val keyword: String) : SearchAction
    data object ClearHistory : SearchAction
    data object Retry : SearchAction
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val historyStore: SearchHistoryStore,
) : androidx.lifecycle.ViewModel() {
    private val _uiState = MutableStateFlow(
        SearchUiState(
            history = historyStore.load(),
            suggestCards = DramaFlowMockData.dramas.map { drama ->
                DramaCard(
                    drama = drama,
                    lastProgress = null,
                    isUpdated = drama.isFeatured,
                    isLockedForUser = false,
                    statusLabel = "All ${drama.totalEpisodes} episodes",
                )
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
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.UpdateKeyword -> _uiState.value = _uiState.value.copy(keyword = action.keyword)
            is SearchAction.SubmitSearch -> submit(action.keyword)
            is SearchAction.ClickHistory -> {
                _uiState.value = _uiState.value.copy(keyword = action.keyword)
                submit(action.keyword)
            }

            SearchAction.ClearHistory -> {
                historyStore.clear()
                _uiState.value = _uiState.value.copy(history = emptyList())
            }

            SearchAction.Retry -> submit(_uiState.value.keyword)
        }
    }

    private fun submit(rawKeyword: String) {
        val keyword = rawKeyword.trim()
        if (keyword.isBlank()) {
            _uiState.value = _uiState.value.copy(resultState = DfLoadState.SUCCESS, resultItems = emptyList(), keyword = "")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(resultState = DfLoadState.LOADING, keyword = keyword)
            delay(160)
            if (keyword.equals("error", ignoreCase = true)) {
                _uiState.value = _uiState.value.copy(resultState = DfLoadState.ERROR)
                return@launch
            }
            val matched = DramaFlowMockData.dramas.filter { drama ->
                drama.title.contains(keyword, ignoreCase = true) ||
                    drama.shortDescription.contains(keyword, ignoreCase = true) ||
                    drama.tags.any { it.label.contains(keyword, ignoreCase = true) } ||
                    drama.cast.any { it.contains(keyword, ignoreCase = true) }
            }.map { drama ->
                DramaCard(
                    drama = drama,
                    lastProgress = null,
                    isUpdated = drama.isFeatured,
                    isLockedForUser = false,
                    statusLabel = "${drama.totalEpisodes} episodes",
                )
            }
            val history = historyStore.push(keyword)
            _uiState.value = _uiState.value.copy(
                history = history,
                resultItems = matched,
                resultState = if (matched.isEmpty()) DfLoadState.EMPTY else DfLoadState.SUCCESS,
            )
        }
    }
}

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
    val colors = DramaFlowThemeTokens.colors
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
                items(uiState.resultItems) { card ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDramaClick(card.drama.id) },
                        shape = DramaFlowThemeTokens.shapes.medium,
                        color = colors.whiteCard,
                    ) {
                        Row(
                            modifier = Modifier.padding(spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(spacing.md),
                        ) {
                            AsyncImage(
                                model = card.drama.portraitPosterUrl,
                                contentDescription = card.drama.title,
                                modifier = Modifier
                                    .width(92.dp)
                                    .height(124.dp),
                                contentScale = ContentScale.Crop,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                                Text(
                                    text = card.drama.title,
                                    style = DramaFlowThemeTokens.typography.titleMedium,
                                    color = colors.textPrimary,
                                )
                                Text(
                                    text = card.drama.shortDescription,
                                    color = colors.textSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = card.drama.tags.joinToString(" · ") { it.label },
                                    color = colors.textSecondary,
                                )
                            }
                        }
                    }
                }
            }
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
