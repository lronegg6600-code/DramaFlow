package com.dramaflow.feature.feed

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.dramaflow.core.common.DramaFlowMockData
import com.dramaflow.core.designsystem.component.DfCategoryChip
import com.dramaflow.core.designsystem.component.DfEmptyCard
import com.dramaflow.core.designsystem.component.DfErrorCard
import com.dramaflow.core.designsystem.component.DfLoadingIndicator
import com.dramaflow.core.designsystem.component.DfPrimaryButton
import com.dramaflow.core.designsystem.component.DfWhiteMessageCard
import com.dramaflow.core.designsystem.theme.DramaFlowThemeTokens
import com.dramaflow.core.model.Drama
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.ui.DfLoadState
import kotlinx.coroutines.flow.collectLatest

private const val FeedShareLogTag = "FeedShare"

@Composable
fun FeedRoute(
    onDramaClick: (String) -> Unit,
    onContinueWatching: (String) -> Unit,
    onSearchClick: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel, context) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is FeedEffect.OpenShareSheet -> {
                    val shareText = buildString {
                        append(effect.payload.title)
                        append("\n")
                        append(effect.payload.description)
                        append("\n")
                        append(effect.payload.link)
                    }
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, effect.payload.title)
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    val chooserIntent = Intent.createChooser(shareIntent, "Share drama").apply {
                        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching {
                        Log.d(FeedShareLogTag, "feed_share_sheet_open drama=${effect.payload.dramaId}")
                        context.startActivity(chooserIntent)
                    }.onFailure { error ->
                        Log.e(
                            FeedShareLogTag,
                            "feed_share_failed drama=${effect.payload.dramaId} message=${error.message}",
                            error,
                        )
                    }
                }
            }
        }
    }

    FeedScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onDramaClick = onDramaClick,
        onContinueWatching = onContinueWatching,
        onSearchClick = onSearchClick,
    )
}

@Composable
fun FeedScreen(
    uiState: FeedUiState,
    onAction: (FeedAction) -> Unit,
    onDramaClick: (String) -> Unit,
    onContinueWatching: (String) -> Unit,
    onSearchClick: () -> Unit,
) {
    when (uiState.selectedTab) {
        HomePrimaryTab.RECOMMEND -> RecommendTabScreen(
            uiState = uiState,
            onAction = onAction,
            onDramaClick = onDramaClick,
            onContinueWatching = onContinueWatching,
            onSearchClick = onSearchClick,
        )

        HomePrimaryTab.CHARTS -> ChartsTabScreen(
            uiState = uiState,
            onAction = onAction,
            onDramaClick = onDramaClick,
            onSearchClick = onSearchClick,
        )

        HomePrimaryTab.CATEGORIES -> CategoriesTabScreen(
            uiState = uiState,
            onAction = onAction,
            onDramaClick = onDramaClick,
            onSearchClick = onSearchClick,
        )
    }
}

@Composable
private fun RecommendTabScreen(
    uiState: FeedUiState,
    onAction: (FeedAction) -> Unit,
    onDramaClick: (String) -> Unit,
    onContinueWatching: (String) -> Unit,
    onSearchClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val items = uiState.recommendItems
    val lifecycleOwner = LocalLifecycleOwner.current
    var canAutoplay by remember { mutableStateOf(true) }
    val previewController = rememberFeedPreviewPlayerController()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> canAutoplay = true
                Lifecycle.Event.ON_STOP -> {
                    canAutoplay = false
                    previewController.pause()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (uiState.loadState) {
            DfLoadState.LOADING -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DfLoadingIndicator()
            }

            DfLoadState.EMPTY -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.xl, vertical = spacing.section),
                contentAlignment = Alignment.Center,
            ) {
                DfEmptyCard(
                    title = "No recommendations yet",
                    message = "No titles are available in the recommendation stream.",
                )
            }

            DfLoadState.ERROR -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = spacing.xl, vertical = spacing.section),
                contentAlignment = Alignment.Center,
            ) {
                DfErrorCard(
                    message = uiState.errorMessage,
                    actionLabel = "Reload",
                    onAction = { onAction(FeedAction.Retry) },
                )
            }

            DfLoadState.SUCCESS -> {
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = spacing.xl, vertical = spacing.section),
                        contentAlignment = Alignment.Center,
                    ) {
                        DfEmptyCard(
                            title = "No recommendations yet",
                            message = "No titles are available in the recommendation stream.",
                        )
                    }
                } else {
                    val pagerState = rememberPagerState(
                        initialPage = uiState.activeRecommendPage.coerceIn(0, items.lastIndex),
                        pageCount = { items.size },
                    )
                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
                            onAction(FeedAction.SetRecommendActivePage(page))
                        }
                    }
                    LaunchedEffect(uiState.activeRecommendPage, canAutoplay, items) {
                        val activeItem = items.getOrNull(uiState.activeRecommendPage)
                        previewController.activate(
                            preview = activeItem?.preview,
                            autoplayEnabled = canAutoplay && activeItem != null,
                        )
                    }
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        RecommendPagerCard(
                            item = items[page],
                            isActive = page == uiState.activeRecommendPage && canAutoplay,
                            previewController = previewController,
                            onDramaClick = onDramaClick,
                            onContinueWatching = onContinueWatching,
                            onAction = onAction,
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = spacing.lg, vertical = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            FeedTopBar(
                hint = "Search hot dramas, actors, and tags",
                darkMode = true,
                onSearchClick = onSearchClick,
                onCategoryClick = { onAction(FeedAction.SelectPrimaryTab(HomePrimaryTab.CATEGORIES)) },
            )
            PrimaryTabRow(
                selectedTab = HomePrimaryTab.RECOMMEND,
                onSelectTab = { onAction(FeedAction.SelectPrimaryTab(it)) },
                darkMode = true,
            )
        }
    }
}

@Composable
private fun RecommendPagerCard(
    item: RecommendFeedItem,
    isActive: Boolean,
    previewController: FeedPreviewPlayerController,
    onDramaClick: (String) -> Unit,
    onContinueWatching: (String) -> Unit,
    onAction: (FeedAction) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing

    fun openPrimaryPlayback() {
        Log.d("FeedPreview", "preview_click_enter_player drama=${item.card.drama.id}")
        val episodeId = item.preview.entryEpisodeId
            ?: item.card.lastProgress?.episodeId
            ?: DramaFlowMockData.episodesForDrama(item.card.drama.id).firstOrNull()?.id
        if (episodeId != null) {
            onContinueWatching(episodeId)
        } else {
            onDramaClick(item.card.drama.id)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FeedPreviewPlayer(
            preview = item.preview,
            isActive = isActive,
            controller = previewController,
            modifier = Modifier.fillMaxSize(),
            onOpenPlayer = ::openPrimaryPlayback,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f),
                        ),
                    ),
                ),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .size(76.dp)
                .clip(DramaFlowThemeTokens.shapes.pill)
                .clickable { openPrimaryPlayback() },
            color = Color.White.copy(alpha = 0.22f),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = spacing.lg, end = 88.dp, bottom = spacing.section),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            DramaTitleEntry(
                title = item.card.drama.title,
                tags = item.card.drama.tags.take(2).map { it.label },
                onClick = { onDramaClick(item.card.drama.id) },
            )
            Text(
                text = item.card.drama.shortDescription,
                style = DramaFlowThemeTokens.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = spacing.lg, bottom = spacing.section + 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            SideActionButton(
                icon = if (item.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = formatCount(item.likeCount),
                onClick = { onAction(FeedAction.ToggleLike(item.card.drama.id)) },
                selected = item.isLiked,
                enabled = !item.isLikeUpdating,
            )
            SideActionButton(
                icon = if (item.isFavorited) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                label = if (item.isFavorited) "Saved" else "Save",
                onClick = { onAction(FeedAction.ToggleFavorite(item.card.drama.id)) },
                selected = item.isFavorited,
                enabled = !item.isFavoriteUpdating,
            )
            SideActionButton(
                icon = Icons.Rounded.Share,
                label = formatCount(item.shareCount),
                onClick = { onAction(FeedAction.ShareDrama(item.card.drama.id)) },
                selected = false,
                enabled = true,
            )
        }
    }
}

@Composable
private fun ChartsTabScreen(
    uiState: FeedUiState,
    onAction: (FeedAction) -> Unit,
    onDramaClick: (String) -> Unit,
    onSearchClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    val chartsState = uiState.chartsBrowse
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = spacing.lg, vertical = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        FeedTopBar(
            hint = "Search charting titles and genres",
            darkMode = false,
            onSearchClick = onSearchClick,
            onCategoryClick = { onAction(FeedAction.SelectPrimaryTab(HomePrimaryTab.CATEGORIES)) },
        )
        PrimaryTabRow(
            selectedTab = HomePrimaryTab.CHARTS,
            onSelectTab = { onAction(FeedAction.SelectPrimaryTab(it)) },
            darkMode = false,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            items(chartsState.chartTabs) { tab ->
                DfCategoryChip(
                    label = tab.label,
                    selected = chartsState.selectedChartType == tab.type,
                    onClick = { onAction(FeedAction.SelectChartType(tab.type)) },
                )
            }
        }

        when (chartsState.loadState) {
            DfLoadState.LOADING -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DfLoadingIndicator()
            }

            DfLoadState.EMPTY -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DfEmptyCard(
                    title = "No charts available",
                    message = "Chart data will appear when titles are available.",
                )
            }

            DfLoadState.ERROR -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DfErrorCard(
                    message = chartsState.errorMessage,
                    actionLabel = "Retry",
                    onAction = { onAction(FeedAction.Retry) },
                )
            }

            DfLoadState.SUCCESS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                    contentPadding = PaddingValues(bottom = spacing.section),
                ) {
                    item {
                        DfWhiteMessageCard(
                            title = "${chartsState.selectedChartType.label} chart",
                            body = "Track the strongest titles before dropping into the immersive feed.",
                        )
                    }
                    items(chartsState.rankItems.take(10), key = { it.card.drama.id }) { item ->
                        ChartRankCard(
                            item = item,
                            onClick = { onDramaClick(item.card.drama.id) },
                        )
                    }
                    item {
                        Text(
                            text = "Keep watching",
                            style = DramaFlowThemeTokens.typography.titleLarge,
                            color = colors.textPrimary,
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                            items(chartsState.spotlightItems, key = { it.drama.id }) { card ->
                                SpotlightDramaCard(
                                    card = card,
                                    onDramaClick = onDramaClick,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoriesTabScreen(
    uiState: FeedUiState,
    onAction: (FeedAction) -> Unit,
    onDramaClick: (String) -> Unit,
    onSearchClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    val categoriesState = uiState.categoriesBrowse
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = spacing.lg, vertical = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        FeedTopBar(
            hint = "Search by tag, type, or cast",
            darkMode = false,
            onSearchClick = onSearchClick,
            onCategoryClick = {},
        )
        PrimaryTabRow(
            selectedTab = HomePrimaryTab.CATEGORIES,
            onSelectTab = { onAction(FeedAction.SelectPrimaryTab(it)) },
            darkMode = false,
        )
        CategoryFilterSection(
            title = "Genres",
            options = categoriesState.genreFilters,
            selectedId = categoriesState.selectedGenreId,
            onClick = { onAction(FeedAction.SelectCategoryGenre(it)) },
        )
        CategoryFilterSection(
            title = "Access",
            options = categoriesState.availabilityFilters,
            selectedId = categoriesState.selectedAvailabilityId,
            onClick = { onAction(FeedAction.SelectCategoryAvailability(it)) },
        )
        CategoryFilterSection(
            title = "Sort",
            options = categoriesState.sortFilters,
            selectedId = categoriesState.selectedSortId,
            onClick = { onAction(FeedAction.SelectCategorySort(it)) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${categoriesState.resultCount} titles",
                style = DramaFlowThemeTokens.typography.titleMedium,
                color = colors.textPrimary,
            )
            Surface(
                modifier = Modifier
                    .clip(DramaFlowThemeTokens.shapes.pill)
                    .clickable { onAction(FeedAction.ResetCategoryFilters) },
                color = colors.surface,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, tint = colors.textPrimary)
                    Text("Reset", color = colors.textPrimary)
                }
            }
        }

        when (categoriesState.loadState) {
            DfLoadState.LOADING -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DfLoadingIndicator()
            }

            DfLoadState.EMPTY -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DfEmptyCard(
                    title = "No titles match these filters",
                    message = "Try widening the genre or access filters.",
                )
            }

            DfLoadState.ERROR -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                DfErrorCard(
                    message = categoriesState.errorMessage,
                    actionLabel = "Retry",
                    onAction = { onAction(FeedAction.Retry) },
                )
            }

            DfLoadState.SUCCESS -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(spacing.md),
                    contentPadding = PaddingValues(bottom = spacing.section),
                ) {
                    items(categoriesState.items, key = { it.drama.id }) { card ->
                        CategoryDramaCard(
                            card = card,
                            onDramaClick = onDramaClick,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DramaTitleEntry(
    title: String,
    tags: List<String>,
    onClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    Surface(
        modifier = Modifier
            .clip(DramaFlowThemeTokens.shapes.large)
            .clickable(onClick = onClick),
        color = Color.Black.copy(alpha = 0.26f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    tags.forEach { tag ->
                        Surface(
                            shape = DramaFlowThemeTokens.shapes.pill,
                            color = Color.White.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = tag,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
                            )
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = DramaFlowThemeTokens.typography.headlineMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Open drama details",
                    tint = Color.White.copy(alpha = 0.88f),
                )
            }
        }
    }
}

@Composable
private fun SideActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean,
    enabled: Boolean,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val surfaceColor = when {
        !enabled -> Color.Black.copy(alpha = 0.18f)
        selected -> DramaFlowThemeTokens.colors.accentStrong.copy(alpha = 0.82f)
        else -> Color.Black.copy(alpha = 0.3f)
    }
    val contentColor = if (enabled) Color.White else Color.White.copy(alpha = 0.45f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .size(52.dp)
                .clip(DramaFlowThemeTokens.shapes.pill)
                .clickable(enabled = enabled, onClick = onClick),
            color = surfaceColor,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = null, tint = contentColor)
            }
        }
        Spacer(modifier = Modifier.height(spacing.xs))
        Text(
            text = label,
            style = DramaFlowThemeTokens.typography.labelMedium,
            color = contentColor,
        )
    }
}

@Composable
private fun FeedTopBar(
    hint: String,
    darkMode: Boolean,
    onSearchClick: () -> Unit,
    onCategoryClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FeedSearchEntry(
            hint = hint,
            darkMode = darkMode,
            modifier = Modifier.weight(1f),
            onClick = onSearchClick,
        )
        FeedCategoryEntry(
            darkMode = darkMode,
            onClick = onCategoryClick,
        )
    }
}

@Composable
private fun FeedSearchEntry(
    hint: String,
    darkMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val background = if (darkMode) Color.White.copy(alpha = 0.22f) else DramaFlowThemeTokens.colors.surface
    val textColor = if (darkMode) Color.White.copy(alpha = 0.85f) else DramaFlowThemeTokens.colors.textSecondary
    Surface(
        modifier = modifier
            .clip(DramaFlowThemeTokens.shapes.pill)
            .clickable(onClick = onClick),
        color = background,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = textColor)
            Spacer(modifier = Modifier.width(spacing.sm))
            Text(
                text = hint,
                color = textColor,
                style = DramaFlowThemeTokens.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun FeedCategoryEntry(
    darkMode: Boolean,
    onClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val background = if (darkMode) Color.White.copy(alpha = 0.22f) else DramaFlowThemeTokens.colors.surface
    val textColor = if (darkMode) Color.White.copy(alpha = 0.92f) else DramaFlowThemeTokens.colors.textPrimary
    Surface(
        modifier = Modifier
            .clip(DramaFlowThemeTokens.shapes.pill)
            .clickable(onClick = onClick),
        color = background,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = null,
                tint = textColor,
            )
            Text(
                text = "Categories",
                color = textColor,
                style = DramaFlowThemeTokens.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun PrimaryTabRow(
    selectedTab: HomePrimaryTab,
    onSelectTab: (HomePrimaryTab) -> Unit,
    darkMode: Boolean,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val selectedColor = if (darkMode) Color.White else DramaFlowThemeTokens.colors.textPrimary
    val normalColor = if (darkMode) Color.White.copy(alpha = 0.65f) else DramaFlowThemeTokens.colors.textSecondary
    Row(horizontalArrangement = Arrangement.spacedBy(spacing.lg)) {
        HomePrimaryTab.entries.forEach { tab ->
            Text(
                text = tab.label,
                style = if (selectedTab == tab) {
                    DramaFlowThemeTokens.typography.titleLarge
                } else {
                    DramaFlowThemeTokens.typography.titleMedium
                },
                color = if (selectedTab == tab) selectedColor else normalColor,
                modifier = Modifier.clickable { onSelectTab(tab) },
            )
        }
    }
}

@Composable
private fun ChartRankCard(
    item: ChartRankItem,
    onClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DramaFlowThemeTokens.shapes.medium)
            .clickable(onClick = onClick),
        color = colors.whiteCard,
        shadowElevation = DramaFlowThemeTokens.elevation.low,
    ) {
        Row(
            modifier = Modifier.padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.rank.toString(),
                style = DramaFlowThemeTokens.typography.headlineMedium,
                color = colors.accentStrong,
                fontWeight = FontWeight.Bold,
            )
            AsyncImage(
                model = item.card.drama.portraitPosterUrl,
                contentDescription = item.card.drama.title,
                modifier = Modifier
                    .width(88.dp)
                    .height(124.dp)
                    .clip(DramaFlowThemeTokens.shapes.medium),
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.card.drama.tags.joinToString(" · ") { it.label },
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.heatText,
                    color = colors.accentStrong,
                    style = DramaFlowThemeTokens.typography.bodyMedium,
                )
                Text(
                    text = item.statusText,
                    color = colors.textSecondary,
                    style = DramaFlowThemeTokens.typography.labelMedium,
                )
                Surface(
                    shape = DramaFlowThemeTokens.shapes.pill,
                    color = colors.accentSoft,
                ) {
                    Text(
                        text = item.badgeText,
                        color = colors.accentStrong,
                        modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotlightDramaCard(
    card: DramaCard,
    onDramaClick: (String) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        modifier = Modifier
            .width(180.dp)
            .clip(DramaFlowThemeTokens.shapes.large)
            .clickable { onDramaClick(card.drama.id) },
        color = colors.whiteCard,
    ) {
        Column {
            AsyncImage(
                model = card.drama.heroImageUrl.ifBlank { card.drama.portraitPosterUrl },
                contentDescription = card.drama.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    text = card.drama.title,
                    style = DramaFlowThemeTokens.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = card.drama.channelStatusText(),
                    style = DramaFlowThemeTokens.typography.labelMedium,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterSection(
    title: String,
    options: List<CategoryFilterOption>,
    selectedId: String,
    onClick: (String) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = title,
            style = DramaFlowThemeTokens.typography.titleMedium,
            color = colors.textPrimary,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            items(options, key = { it.id }) { option ->
                DfCategoryChip(
                    label = option.label,
                    selected = selectedId == option.id,
                    onClick = { onClick(option.id) },
                )
            }
        }
    }
}

@Composable
private fun CategoryDramaCard(
    card: DramaCard,
    onDramaClick: (String) -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DramaFlowThemeTokens.shapes.medium)
            .clickable { onDramaClick(card.drama.id) },
        color = colors.whiteCard,
        shadowElevation = DramaFlowThemeTokens.elevation.low,
    ) {
        Column {
            AsyncImage(
                model = card.drama.portraitPosterUrl,
                contentDescription = card.drama.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Crop,
            )
            Column(
                modifier = Modifier.padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    text = card.drama.title,
                    style = DramaFlowThemeTokens.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = card.drama.tags.joinToString(" · ") { it.label },
                    style = DramaFlowThemeTokens.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${card.drama.heatScore} heat · ${card.drama.channelStatusText()}",
                    style = DramaFlowThemeTokens.typography.labelMedium,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatCount(value: Int): String {
    return if (value >= 10_000) String.format("%.1fk", value / 1_000f) else value.toString()
}

private fun Drama.channelStatusText(): String {
    val releaseState = if (isFeatured) "Ongoing" else "Completed"
    val accessState = if (isPremiumSeries) "Premium" else "Free"
    return "$totalEpisodes eps · $releaseState · $accessState"
}
