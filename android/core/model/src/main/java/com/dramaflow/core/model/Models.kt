package com.dramaflow.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DramaTag(
    val id: String,
    val label: String,
)

@Serializable
data class Drama(
    val id: String,
    val title: String,
    val shortDescription: String,
    val longDescription: String,
    val portraitPosterUrl: String,
    val heroImageUrl: String,
    val heatScore: String,
    val totalEpisodes: Int,
    val tags: List<DramaTag>,
    val cast: List<String>,
    val labels: List<String>,
    val heroNote: String,
    val isFeatured: Boolean,
    val isPremiumSeries: Boolean,
)

@Serializable
data class Episode(
    val id: String,
    val dramaId: String,
    val episodeNumber: Int,
    val title: String,
    val synopsis: String,
    val durationSeconds: Long,
    val streamUrl: String,
    val artworkUrl: String,
    val isPreviewEnabled: Boolean,
    val requiresPremium: Boolean,
    val previewWindowSeconds: Int?,
)

@Serializable
data class WatchProgress(
    val dramaId: String,
    val episodeId: String,
    val positionMs: Long,
    val durationMs: Long,
    val progressPercent: Float,
    val lastUpdatedEpochMs: Long,
    val completed: Boolean,
)

@Serializable
data class PlaybackSession(
    val drama: Drama,
    val episode: Episode,
    val nextEpisode: Episode?,
    val savedProgress: WatchProgress?,
    val entitlementState: EntitlementState,
    val playbackDescriptor: PlaybackDescriptor? = null,
    val isRemotePlayback: Boolean = false,
)

@Serializable
data class PreviewLimit(
    val previewWindowSeconds: Int,
    val remainingSeconds: Int,
    val blockReason: String,
)

@Serializable
data class NextEpisodeHint(
    val episodeId: String,
    val title: String,
    val autoPlayInSeconds: Int,
)

@Serializable
data class SubscriptionBenefit(
    val id: String,
    val title: String,
    val description: String,
)

@Serializable
data class SubscriptionOffer(
    val id: String,
    val title: String,
    val offerToken: String,
    val billingPeriodLabel: String,
    val priceText: String,
    val monthlyEquivalentText: String,
    val trialBadge: String?,
    val badgeText: String?,
    val isDefault: Boolean,
)

@Serializable
data class SubscriptionProduct(
    val id: String,
    val title: String,
    val subtitle: String,
    val benefits: List<SubscriptionBenefit>,
    val offers: List<SubscriptionOffer>,
)

@Serializable
data class EntitlementState(
    val isPremium: Boolean,
    val activeProductId: String?,
    val unlockedEpisodeIds: List<String>,
    val sourceLabel: String,
)

@Serializable
data class BillingSyncResult(
    val syncAccepted: Boolean,
    val purchaseState: String,
    val acknowledgementState: String,
    val entitlementState: String,
    val entitlementEffectiveAt: String?,
    val nextAction: String,
    val traceId: String?,
)

@Serializable
data class BillingSubscriptionStatus(
    val userId: String,
    val purchaseToken: String?,
    val productId: String?,
    val purchaseState: String,
    val acknowledgementState: String,
    val entitlementState: String,
)

@Serializable
data class BillingPurchasePayload(
    val productId: String,
    val basePlanId: String?,
    val offerId: String?,
    val offerToken: String?,
    val purchaseToken: String,
    val purchaseState: String,
    val accountId: String?,
    val profileId: String?,
)

@Serializable
sealed interface PurchaseResult {
    @Serializable
    data class Success(
        val purchase: BillingPurchasePayload,
    ) : PurchaseResult

    @Serializable
    data object Cancelled : PurchaseResult

    @Serializable
    data class Error(val message: String) : PurchaseResult
}

@Serializable
data class UserProfile(
    val id: String,
    val displayName: String,
    val avatarUrl: String,
    val countryCode: String,
    val languageCode: String,
)

@Serializable
data class WatchHistoryItem(
    val dramaId: String,
    val episodeId: String,
    val title: String,
    val artworkUrl: String,
    val watchedAtEpochMs: Long,
    val progressPercent: Float,
)

@Serializable
data class DramaCard(
    val drama: Drama,
    val lastProgress: WatchProgress?,
    val isUpdated: Boolean,
    val isLockedForUser: Boolean,
    val statusLabel: String?,
)

@Serializable
data class FeedPayload(
    val featured: DramaCard?,
    val banners: List<DramaCard>,
    val tags: List<DramaTag>,
    val continueWatching: List<DramaCard>,
    val hotTitles: List<DramaCard>,
    val recommendations: List<DramaCard>,
)

@Serializable
data class EpisodeListItem(
    val episode: Episode,
    val watchProgress: WatchProgress?,
    val isLockedForUser: Boolean,
    val isCurrentEpisode: Boolean,
)

@Serializable
data class DetailPayload(
    val drama: Drama,
    val primaryEpisode: Episode?,
    val watchProgress: WatchProgress?,
    val entitlementState: EntitlementState,
    val episodes: List<EpisodeListItem>,
    val relatedTitles: List<DramaCard>,
)

@Serializable
data class SubscriptionPayload(
    val heroTitle: String,
    val heroSubtitle: String,
    val products: List<SubscriptionProduct>,
    val selectedProductId: String?,
    val selectedOfferId: String?,
    val entitlementState: EntitlementState,
)

@Serializable
data class ProfilePayload(
    val profile: UserProfile,
    val entitlementState: EntitlementState,
    val continueWatching: DramaCard?,
    val watchHistory: List<WatchHistoryItem>,
    val favorites: List<DramaCard>,
    val unlockedSummary: String,
)
