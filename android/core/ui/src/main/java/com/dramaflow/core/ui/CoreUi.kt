package com.dramaflow.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dramaflow.core.designsystem.component.DfEmptyCard
import com.dramaflow.core.designsystem.component.DfErrorCard
import com.dramaflow.core.designsystem.component.DfLoadingIndicator
import com.dramaflow.core.designsystem.component.DfLoadingSkeleton
import com.dramaflow.core.designsystem.theme.DramaFlowThemeTokens

enum class DfLoadState {
    LOADING,
    EMPTY,
    ERROR,
    SUCCESS,
}

@Composable
fun DfScreenScaffold(
    topBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = DramaFlowThemeTokens.colors.background,
        topBar = topBar,
        content = content,
    )
}

@Composable
fun DfStateLayout(
    state: DfLoadState,
    modifier: Modifier = Modifier,
    errorMessage: String = "Something went wrong.",
    emptyTitle: String = "Nothing here yet",
    emptyMessage: String = "Content will show up here once your catalog is ready.",
    onRetry: () -> Unit = {},
    success: @Composable () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    when (state) {
        DfLoadState.LOADING -> Box(
            modifier = modifier.fillMaxSize().padding(spacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                DfLoadingIndicator()
                DfLoadingSkeleton()
            }
        }
        DfLoadState.EMPTY -> Box(
            modifier = modifier.fillMaxSize().padding(spacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            DfEmptyCard(title = emptyTitle, message = emptyMessage)
        }
        DfLoadState.ERROR -> Box(
            modifier = modifier.fillMaxSize().padding(spacing.xl),
            contentAlignment = Alignment.Center,
        ) {
            DfErrorCard(message = errorMessage, actionLabel = "Try again", onAction = onRetry)
        }
        DfLoadState.SUCCESS -> success()
    }
}

@Composable
fun DfScrollableColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DramaFlowThemeTokens.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.xl, vertical = spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.section),
        content = content,
    )
}
