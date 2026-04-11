package com.dramaflow.core.common

import com.dramaflow.core.common.auth.AuthSessionManager
import com.dramaflow.core.model.DetailPayload
import com.dramaflow.core.model.Drama
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.model.DramaTag
import com.dramaflow.core.model.EpisodeListItem
import com.dramaflow.core.model.FeedPayload
import com.dramaflow.core.model.ProfilePayload
import com.dramaflow.core.model.UserProfile
import com.dramaflow.core.network.source.ContentRemoteDataSource
import com.dramaflow.core.network.source.FeedRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

@Singleton
class RemoteFeedRepository @Inject constructor(
    private val authSessionManager: AuthSessionManager,
    private val feedRemoteDataSource: FeedRemoteDataSource,
) : FeedRepository {
    override fun observeFeed(): Flow<DataResult<FeedPayload>> = flow {
        emit(DataResult.Loading)
        val payload = runCatching {
            authSessionManager.ensureGuestSession()

            val homeCards = feedRemoteDataSource.getHomeFeed()
            val continueCards = feedRemoteDataSource.getContinueWatching()
            val tags = homeCards.asSequence()
                .flatMap { it.drama.tags.asSequence() }
                .distinctBy { it.id }
                .toList()
                .ifEmpty { listOf(DramaTag(id = "all", label = "All")) }

            FeedPayload(
                featured = homeCards.firstOrNull { it.drama.isFeatured } ?: homeCards.firstOrNull(),
                banners = homeCards.take(5),
                tags = tags,
                continueWatching = continueCards,
                hotTitles = homeCards.take(8),
                recommendations = homeCards.drop(3).ifEmpty { homeCards },
            )
        }.getOrElse { error ->
            emit(DataResult.Error("Feed backend request failed: ${error.message ?: "unknown error"}"))
            return@flow
        }

        if (payload.banners.isEmpty() && payload.recommendations.isEmpty()) {
            emit(DataResult.Empty)
        } else {
            emit(DataResult.Success(payload))
        }
    }
}

@Singleton
class RemoteCatalogRepository @Inject constructor(
    private val authSessionManager: AuthSessionManager,
    private val contentRemoteDataSource: ContentRemoteDataSource,
    private val progressRepository: ProgressRepository,
    private val entitlementRepository: EntitlementRepository,
) : CatalogRepository {
    override fun observeDramaDetail(dramaId: String): Flow<DataResult<DetailPayload>> = flow {
        emit(DataResult.Loading)
        val payload = runCatching {
            authSessionManager.ensureGuestSession()

            val drama = contentRemoteDataSource.getDrama(dramaId) ?: return@runCatching null
            val entitlement = entitlementRepository.currentEntitlement()
            val episodes = contentRemoteDataSource.getEpisodes(dramaId).sortedBy { it.episodeNumber }
            val latestProgress = progressRepository.getLatestProgressForDrama(dramaId)

            val episodeItems = episodes.map { episode ->
                val progress = progressRepository.getProgressForEpisode(episode.id)
                EpisodeListItem(
                    episode = episode,
                    watchProgress = progress,
                    isLockedForUser = episode.requiresPremium && !entitlement.isPremium,
                    isCurrentEpisode = latestProgress?.episodeId == episode.id,
                )
            }

            val related = contentRemoteDataSource.getDramas()
                .filter { it.id != drama.id }
                .take(6)
                .map { relatedDrama ->
                    relatedDrama.toRemoteCard(
                        entitlementIsPremium = entitlement.isPremium,
                        progress = progressRepository.getLatestProgressForDrama(relatedDrama.id),
                    )
                }

            DetailPayload(
                drama = drama,
                primaryEpisode = latestProgress?.episodeId?.let { target -> episodes.firstOrNull { it.id == target } }
                    ?: episodes.firstOrNull(),
                watchProgress = latestProgress,
                entitlementState = entitlement,
                episodes = episodeItems,
                relatedTitles = related,
            )
        }.getOrElse { error ->
            emit(DataResult.Error("Detail backend request failed: ${error.message ?: "unknown error"}"))
            return@flow
        }

        if (payload == null) {
            emit(DataResult.Empty)
        } else {
            emit(DataResult.Success(payload))
        }
    }
}

@Singleton
class RemoteProfileRepository @Inject constructor(
    private val authSessionManager: AuthSessionManager,
    private val entitlementRepository: EntitlementRepository,
    private val progressRepository: ProgressRepository,
    private val contentRemoteDataSource: ContentRemoteDataSource,
) : ProfileRepository {
    override fun observeProfile(): Flow<DataResult<ProfilePayload>> = flow {
        emit(DataResult.Loading)
        val payload = runCatching {
            val session = authSessionManager.ensureGuestSession()
            val entitlement = entitlementRepository.currentEntitlement()
            val continueWatching = progressRepository.observeContinueWatchingCards().first()
            val history = progressRepository.observeHistory().first()

            // 这里不伪造账号信息：优先使用后端 guest session 给的 userId，拿不到才降级成 local 标识。
            val profile = UserProfile(
                id = session.userId ?: "local-guest",
                displayName = "Guest ${session.userId?.takeLast(6) ?: "local"}",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330",
                countryCode = "ZZ",
                languageCode = "en",
            )

            val favorites = runCatching {
                contentRemoteDataSource.getDramas()
                    .take(3)
                    .map { drama -> drama.toRemoteCard(entitlementIsPremium = entitlement.isPremium, progress = null) }
            }.getOrDefault(emptyList())

            ProfilePayload(
                profile = profile,
                entitlementState = entitlement,
                continueWatching = continueWatching.firstOrNull(),
                watchHistory = history,
                favorites = favorites,
                unlockedSummary = if (entitlement.isPremium) {
                    "Premium entitlement active from backend truth."
                } else {
                    "Free tier active. Premium episodes require successful entitlement grant."
                },
            )
        }.getOrElse { error ->
            emit(DataResult.Error("Profile backend request failed: ${error.message ?: "unknown error"}"))
            return@flow
        }

        emit(DataResult.Success(payload))
    }
}

private fun Drama.toRemoteCard(
    entitlementIsPremium: Boolean,
    progress: com.dramaflow.core.model.WatchProgress?,
): DramaCard {
    val locked = isPremiumSeries && !entitlementIsPremium
    return DramaCard(
        drama = this,
        lastProgress = progress,
        isUpdated = isFeatured,
        isLockedForUser = locked,
        statusLabel = if (locked) "Premium" else null,
    )
}
