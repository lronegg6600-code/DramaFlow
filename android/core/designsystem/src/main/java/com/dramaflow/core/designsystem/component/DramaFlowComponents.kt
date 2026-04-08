package com.dramaflow.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dramaflow.core.designsystem.theme.DramaFlowThemeTokens
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.model.DramaTag
import com.dramaflow.core.model.SubscriptionBenefit
import com.dramaflow.core.model.SubscriptionOffer

@Composable
fun DfTopBar(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = DramaFlowThemeTokens.typography.headlineMedium, color = colors.textPrimary)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(subtitle, style = DramaFlowThemeTokens.typography.bodyMedium, color = colors.textSecondary)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun DfCategoryChip(
    label: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val colors = DramaFlowThemeTokens.colors
    AssistChip(
        onClick = { onClick?.invoke() },
        label = { Text(label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) colors.accentSoft else colors.surface,
            labelColor = if (selected) colors.accentStrong else colors.textSecondary,
        ),
        border = BorderStroke(1.dp, if (selected) Color.Transparent else colors.border),
    )
}

@Composable
fun DfDramaCard(
    card: DramaCard,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    val statusLabel = card.statusLabel
    Card(
        modifier = modifier.width(190.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = colors.whiteCard),
        shape = DramaFlowThemeTokens.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = DramaFlowThemeTokens.elevation.low),
    ) {
        Column {
            AsyncImage(
                model = card.drama.heroImageUrl,
                contentDescription = card.drama.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.78f).clip(DramaFlowThemeTokens.shapes.medium),
                contentScale = ContentScale.Crop,
            )
            Column(modifier = Modifier.padding(spacing.lg)) {
                if (statusLabel != null) {
                    Surface(
                        shape = DramaFlowThemeTokens.shapes.pill,
                        color = if (card.isLockedForUser) colors.warning.copy(alpha = 0.18f) else colors.accentSoft,
                    ) {
                        Text(
                            text = statusLabel,
                            style = DramaFlowThemeTokens.typography.labelMedium,
                            color = if (card.isLockedForUser) colors.warning else colors.accentStrong,
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = spacing.xs),
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.sm))
                }
                Text(
                    text = card.drama.title,
                    style = DramaFlowThemeTokens.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(spacing.xs))
                Text(
                    text = card.drama.shortDescription,
                    style = DramaFlowThemeTokens.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                card.lastProgress?.let { progress ->
                    Spacer(modifier = Modifier.height(spacing.sm))
                    Text(
                        text = "${(progress.progressPercent * 100).toInt()}% watched",
                        style = DramaFlowThemeTokens.typography.labelMedium,
                        color = colors.accentStrong,
                    )
                }
                Spacer(modifier = Modifier.height(spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, contentDescription = null, tint = colors.warning, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(spacing.xs))
                    Text("${card.drama.heatScore} heat", style = DramaFlowThemeTokens.typography.labelMedium, color = colors.textSecondary)
                }
            }
        }
    }
}

@Composable
fun DfTagRow(tags: List<DramaTag>) {
    val spacing = DramaFlowThemeTokens.spacing
    LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        items(tags) { tag ->
            DfCategoryChip(label = tag.label)
        }
    }
}

@Composable
fun DfPriceCard(
    offer: SubscriptionOffer,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = DramaFlowThemeTokens.colors
    val spacing = DramaFlowThemeTokens.spacing
    val badgeText = offer.badgeText
    val trialBadge = offer.trialBadge
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = DramaFlowThemeTokens.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = if (selected) colors.accentSoft else colors.surface),
        border = BorderStroke(1.dp, if (selected) colors.accentStrong else colors.border),
    ) {
        Column(modifier = Modifier.padding(spacing.lg)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(offer.title, style = DramaFlowThemeTokens.typography.titleMedium, color = colors.textPrimary)
                    Text(offer.billingPeriodLabel, style = DramaFlowThemeTokens.typography.bodyMedium, color = colors.textSecondary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(offer.priceText, style = DramaFlowThemeTokens.typography.titleLarge, color = colors.textPrimary)
                    Text(offer.monthlyEquivalentText, style = DramaFlowThemeTokens.typography.labelMedium, color = colors.textSecondary)
                }
            }
            if (badgeText != null) {
                Spacer(modifier = Modifier.height(spacing.md))
                Surface(shape = DramaFlowThemeTokens.shapes.pill, color = colors.accentStrong) {
                    Text(
                        text = badgeText,
                        color = colors.textInverse,
                        style = DramaFlowThemeTokens.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
                    )
                }
            }
            if (trialBadge != null) {
                Spacer(modifier = Modifier.height(spacing.sm))
                Text(trialBadge, style = DramaFlowThemeTokens.typography.bodyMedium, color = colors.textSecondary)
            }
        }
    }
}

@Composable
fun DfBenefitCard(benefit: SubscriptionBenefit) {
    val colors = DramaFlowThemeTokens.colors
    val spacing = DramaFlowThemeTokens.spacing
    Surface(
        shape = DramaFlowThemeTokens.shapes.medium,
        color = colors.surfaceElevated,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(DramaFlowThemeTokens.shapes.small).background(colors.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = colors.accentStrong)
            }
            Spacer(modifier = Modifier.width(spacing.md))
            Column {
                Text(benefit.title, style = DramaFlowThemeTokens.typography.titleMedium, color = colors.textPrimary)
                Text(benefit.description, style = DramaFlowThemeTokens.typography.bodyMedium, color = colors.textSecondary)
            }
        }
    }
}

@Composable
fun DfErrorCard(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        shape = DramaFlowThemeTokens.shapes.large,
        color = colors.whiteCard,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.section),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = colors.danger)
            Spacer(modifier = Modifier.height(spacing.md))
            Text("Something slipped", style = DramaFlowThemeTokens.typography.titleLarge, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(message, style = DramaFlowThemeTokens.typography.bodyMedium, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(spacing.lg))
            DfPrimaryButton(label = actionLabel, onClick = onAction)
        }
    }
}

@Composable
fun DfEmptyCard(
    title: String,
    message: String,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        shape = DramaFlowThemeTokens.shapes.large,
        color = colors.whiteCard,
        border = BorderStroke(1.dp, colors.border),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(spacing.section),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = DramaFlowThemeTokens.typography.titleLarge, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(message, style = DramaFlowThemeTokens.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}

@Composable
fun DfLoadingSkeleton(modifier: Modifier = Modifier, lineCount: Int = 4) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        repeat(lineCount) {
            Box(
                modifier = Modifier.fillMaxWidth().height(18.dp).clip(DramaFlowThemeTokens.shapes.small).background(colors.surfaceMuted),
            )
        }
    }
}

@Composable
fun DfPrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val spacing = DramaFlowThemeTokens.spacing
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = DramaFlowThemeTokens.shapes.pill,
        color = DramaFlowThemeTokens.colors.accentStrong,
        shadowElevation = DramaFlowThemeTokens.elevation.medium,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.xl, vertical = spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = DramaFlowThemeTokens.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = DramaFlowThemeTokens.colors.textInverse,
            )
        }
    }
}

@Composable
fun DfWhiteMessageCard(
    title: String,
    body: String,
) {
    val spacing = DramaFlowThemeTokens.spacing
    val colors = DramaFlowThemeTokens.colors
    Surface(
        shape = DramaFlowThemeTokens.shapes.medium,
        color = colors.whiteCard,
        shadowElevation = DramaFlowThemeTokens.elevation.low,
    ) {
        Column(modifier = Modifier.padding(spacing.lg)) {
            Text(title, style = DramaFlowThemeTokens.typography.titleMedium, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(spacing.sm))
            Text(body, style = DramaFlowThemeTokens.typography.bodyMedium, color = colors.textSecondary)
        }
    }
}

@Composable
fun DfLoadingIndicator() {
    CircularProgressIndicator(color = DramaFlowThemeTokens.colors.accentStrong)
}
