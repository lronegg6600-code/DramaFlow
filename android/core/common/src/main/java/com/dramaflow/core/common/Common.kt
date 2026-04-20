package com.dramaflow.core.common

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dramaflow.core.database.DramaFlowDatabase
import com.dramaflow.core.database.DramaFlowPreferenceStore
import com.dramaflow.core.database.WatchHistoryEntity
import com.dramaflow.core.database.WatchProgressEntity
import com.dramaflow.core.common.entitlement.EntitlementStateStore
import com.dramaflow.core.model.DetailPayload
import com.dramaflow.core.model.Drama
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.model.DramaTag
import com.dramaflow.core.model.EntitlementState
import com.dramaflow.core.model.Episode
import com.dramaflow.core.model.EpisodeListItem
import com.dramaflow.core.model.FeedPayload
import com.dramaflow.core.model.PlaybackCompletion
import com.dramaflow.core.model.PlaybackDataMode
import com.dramaflow.core.model.PlaybackHeartbeat
import com.dramaflow.core.model.PlaybackSession
import com.dramaflow.core.model.ProfilePayload
import com.dramaflow.core.model.SubscriptionBenefit
import com.dramaflow.core.model.SubscriptionOffer
import com.dramaflow.core.model.SubscriptionPayload
import com.dramaflow.core.model.SubscriptionProduct
import com.dramaflow.core.model.UserProfile
import com.dramaflow.core.model.WatchHistoryItem
import com.dramaflow.core.model.WatchProgress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

sealed interface DataResult<out T> {
    data object Loading : DataResult<Nothing>
    data class Success<T>(val value: T) : DataResult<T>
    data class Error(val message: String) : DataResult<Nothing>
    data object Empty : DataResult<Nothing>
}

enum class MockBehavior {
    SUCCESS,
    LOADING,
    EMPTY,
    ERROR,
}

enum class FeatureKey {
    FEED,
    DETAIL,
    PLAYER,
    SUBSCRIPTION,
    PROFILE,
}

interface MockBehaviorProvider {
    fun behaviorFor(featureKey: FeatureKey): MockBehavior
}

@Singleton
class DefaultMockBehaviorProvider @Inject constructor() : MockBehaviorProvider {
    override fun behaviorFor(featureKey: FeatureKey): MockBehavior = MockBehavior.SUCCESS
}

@Qualifier
annotation class IoDispatcher

data class EnvironmentConfig(
    val environmentName: String,
    val useFakeData: Boolean,
    val playbackDataMode: PlaybackDataMode,
    val allowPlaybackFallback: Boolean,
    val useRealBilling: Boolean,
    val useRealEntitlements: Boolean,
)

object AppEnvironment {
    val current = EnvironmentConfig(
        environmentName = "debug-fake",
        useFakeData = true,
        playbackDataMode = runCatching { PlaybackDataMode.valueOf(BuildConfig.PLAYBACK_DATA_MODE) }.getOrDefault(PlaybackDataMode.HYBRID),
        allowPlaybackFallback = BuildConfig.ALLOW_PLAYBACK_FALLBACK,
        useRealBilling = BuildConfig.USE_REAL_BILLING,
        useRealEntitlements = BuildConfig.USE_REAL_ENTITLEMENTS,
    )
}

private const val WatchHistoryLogTag = "WatchHistory"
private const val EntitlementLogTag = "Entitlement"

interface FeedRepository {
    fun observeFeed(): Flow<DataResult<FeedPayload>>
}

interface CatalogRepository {
    fun observeDramaDetail(dramaId: String): Flow<DataResult<DetailPayload>>
}

interface PlaybackRepository {
    suspend fun loadPlaybackSession(episodeId: String): DataResult<PlaybackSession>
    suspend fun nextEpisodeFor(episodeId: String): Episode?
    suspend fun preloadPlaybackSession(episodeId: String)
    suspend fun sendHeartbeat(
        sessionId: String,
        positionSeconds: Int,
        bufferedPositionSeconds: Int,
        isPlaying: Boolean,
        playerState: String,
    ): PlaybackHeartbeat?
    suspend fun refreshPlaybackSession(sessionId: String): PlaybackSession?
    suspend fun completePlaybackSession(
        sessionId: String,
        finalPositionSeconds: Int,
        completed: Boolean,
        watchedSeconds: Int,
    ): PlaybackCompletion?
}

interface ProgressRepository {
    fun observeHistory(): Flow<List<WatchHistoryItem>>
    fun observeContinueWatchingCards(): Flow<List<DramaCard>>
    suspend fun getLatestProgressForDrama(dramaId: String): WatchProgress?
    suspend fun getProgressForEpisode(episodeId: String): WatchProgress?
    suspend fun saveProgress(progress: WatchProgress)
    suspend fun saveEpisodeComplete(episode: Episode, drama: Drama, progressPercent: Float)
    suspend fun removeHistoryForDrama(dramaId: String)
    suspend fun clearHistory()
    suspend fun clearAll()
    fun observeLastPlayedEpisodeId(): Flow<String?>
    suspend fun setLastPlayedEpisodeId(episodeId: String?)
}

interface SubscriptionRepository {
    fun observeSubscription(): Flow<DataResult<SubscriptionPayload>>
    suspend fun setSelectedOffer(productId: String, offerId: String)
}

interface EntitlementRepository {
    fun observeEntitlement(): Flow<EntitlementState>
    suspend fun currentEntitlement(): EntitlementState
    suspend fun grantPremium(productId: String)
    suspend fun refresh(): EntitlementState
    suspend fun reset()
}

interface ProfileRepository {
    fun observeProfile(): Flow<DataResult<ProfilePayload>>
}

private fun <T> wrapWithBehavior(
    featureKey: FeatureKey,
    behaviorProvider: MockBehaviorProvider,
    source: Flow<T>,
): Flow<DataResult<T>> {
    return flow {
        emit(DataResult.Loading)
        delay(200)
        when (behaviorProvider.behaviorFor(featureKey)) {
            MockBehavior.SUCCESS -> source.collect { value -> emit(DataResult.Success(value)) }
            MockBehavior.LOADING -> emit(DataResult.Loading)
            MockBehavior.EMPTY -> emit(DataResult.Empty)
            MockBehavior.ERROR -> emit(DataResult.Error("We hit a temporary issue. Pull to retry or come back in a moment."))
        }
    }
}

@Singleton
class FakeEntitlementRepository @Inject constructor(
    private val stateStore: EntitlementStateStore,
) : EntitlementRepository {
    override fun observeEntitlement(): Flow<EntitlementState> = stateStore.observe()

    override suspend fun currentEntitlement(): EntitlementState = stateStore.current()

    override suspend fun grantPremium(productId: String) {
        val state = EntitlementState(
            isPremium = true,
            activeProductId = productId,
            unlockedEpisodeIds = emptyList(),
            sourceLabel = "mock_purchase",
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        stateStore.persist(state)
        Log.d(EntitlementLogTag, "entitlement_grant product=$productId source=${state.sourceLabel}")
    }

    override suspend fun refresh(): EntitlementState {
        val state = currentEntitlement()
        Log.d(
            EntitlementLogTag,
            "entitlement_state_refresh premium=${state.isPremium} product=${state.activeProductId} source=${state.sourceLabel}",
        )
        return state
    }

    override suspend fun reset() {
        stateStore.reset()
        Log.d(EntitlementLogTag, "entitlement_revoke source=fake_reset")
    }
}

@Singleton
class FakeProgressRepository @Inject constructor(
    private val database: DramaFlowDatabase,
    private val preferences: DramaFlowPreferenceStore,
) : ProgressRepository {
    private val dao = database.watchStateDao()

    override fun observeHistory(): Flow<List<WatchHistoryItem>> {
        return combine(dao.observeHistory(), dao.observeAllProgress()) { history, progressList ->
            history.sortedByDescending { it.watchedAtEpochMs }
                .distinctBy { it.dramaId }
                .mapNotNull { entity ->
                    entity.toWatchHistoryItem(progressList.firstOrNull { it.episodeId == entity.episodeId })
                }
        }
    }

    override fun observeContinueWatchingCards(): Flow<List<DramaCard>> {
        return dao.observeAllProgress().map { progressList ->
            progressList.sortedByDescending { it.lastUpdatedEpochMs }
                .mapNotNull { progress -> DramaFlowMockData.findDrama(progress.dramaId)?.toCard(progress.toModel(), false, false) }
                .distinctBy { it.drama.id }
        }
    }

    override suspend fun getLatestProgressForDrama(dramaId: String): WatchProgress? {
        return dao.getLatestProgressForDrama(dramaId)?.toModel()
    }

    override suspend fun getProgressForEpisode(episodeId: String): WatchProgress? {
        return dao.getProgressByEpisode(episodeId)?.toModel()
    }

    override suspend fun saveProgress(progress: WatchProgress) {
        runCatching {
            dao.upsertProgress(progress.toEntity())
            upsertHistoryRecord(progress)
        }.onFailure { error ->
            Log.e(
                WatchHistoryLogTag,
                "history_persist_failed action=upsert drama=${progress.dramaId} episode=${progress.episodeId} message=${error.message}",
                error,
            )
        }
    }

    override suspend fun saveEpisodeComplete(episode: Episode, drama: Drama, progressPercent: Float) {
        runCatching {
            dao.upsertHistory(
                WatchHistoryEntity(
                episodeId = episode.id,
                dramaId = drama.id,
                title = "${drama.title} · ${episode.title}",
                artworkUrl = drama.portraitPosterUrl,
                watchedAtEpochMs = System.currentTimeMillis(),
                progressPercent = progressPercent,
                ),
            )
            Log.d(
                WatchHistoryLogTag,
                "history_record_upsert drama=${drama.id} episode=${episode.id} progress=$progressPercent completed=true",
            )
        }.onFailure { error ->
            Log.e(
                WatchHistoryLogTag,
                "history_persist_failed action=complete drama=${drama.id} episode=${episode.id} message=${error.message}",
                error,
            )
        }
    }

    override suspend fun removeHistoryForDrama(dramaId: String) {
        runCatching {
            dao.deleteDramaWatchState(dramaId)
            val lastPlayedEpisodeId = preferences.lastPlayedEpisodeId.first()
            if (lastPlayedEpisodeId?.let { DramaFlowMockData.findEpisode(it)?.dramaId } == dramaId) {
                preferences.setLastPlayedEpisode(null)
            }
            Log.d(WatchHistoryLogTag, "history_record_remove drama=$dramaId")
        }.getOrElse { error ->
            Log.e(
                WatchHistoryLogTag,
                "history_persist_failed action=remove drama=$dramaId message=${error.message}",
                error,
            )
            throw error
        }
    }

    override suspend fun clearHistory() {
        runCatching {
            dao.clearAllWatchState()
            preferences.setLastPlayedEpisode(null)
            Log.d(WatchHistoryLogTag, "history_record_clear scope=all")
        }.getOrElse { error ->
            Log.e(
                WatchHistoryLogTag,
                "history_persist_failed action=clear_all message=${error.message}",
                error,
            )
            throw error
        }
    }

    override suspend fun clearAll() {
        clearHistory()
    }

    override fun observeLastPlayedEpisodeId(): Flow<String?> = preferences.lastPlayedEpisodeId

    override suspend fun setLastPlayedEpisodeId(episodeId: String?) {
        preferences.setLastPlayedEpisode(episodeId)
    }

    private suspend fun upsertHistoryRecord(progress: WatchProgress) {
        val episode = DramaFlowMockData.findEpisode(progress.episodeId) ?: return
        val drama = DramaFlowMockData.findDrama(progress.dramaId) ?: return
        dao.upsertHistory(
            WatchHistoryEntity(
                episodeId = episode.id,
                dramaId = drama.id,
                title = "${drama.title} - ${episode.title}",
                artworkUrl = drama.portraitPosterUrl,
                watchedAtEpochMs = progress.lastUpdatedEpochMs,
                progressPercent = progress.progressPercent,
            ),
        )
        Log.d(
            WatchHistoryLogTag,
            "history_record_upsert drama=${drama.id} episode=${episode.id} progress=${progress.progressPercent}",
        )
    }
}

@Singleton
class FakeFeedRepository @Inject constructor(
    private val behaviorProvider: MockBehaviorProvider,
    private val progressRepository: ProgressRepository,
    private val entitlementRepository: EntitlementRepository,
) : FeedRepository {
    override fun observeFeed(): Flow<DataResult<FeedPayload>> {
        val source = combine(
            progressRepository.observeContinueWatchingCards(),
            entitlementRepository.observeEntitlement(),
        ) { continueWatching, entitlement ->
            val cards = DramaFlowMockData.dramas.map { drama ->
                val progress = continueWatching.firstOrNull { it.drama.id == drama.id }?.lastProgress
                drama.toCard(progress = progress, isUpdated = drama.isFeatured, isLockedForUser = drama.isPremiumSeries && !entitlement.isPremium)
            }
            FeedPayload(
                featured = cards.firstOrNull { it.drama.isFeatured },
                banners = cards.take(3),
                tags = DramaFlowMockData.tags,
                continueWatching = continueWatching,
                hotTitles = cards.take(4),
                recommendations = cards.shuffled(),
            )
        }
        return wrapWithBehavior(FeatureKey.FEED, behaviorProvider, source)
    }
}

@Singleton
class FakeCatalogRepository @Inject constructor(
    private val behaviorProvider: MockBehaviorProvider,
    private val progressRepository: ProgressRepository,
    private val entitlementRepository: EntitlementRepository,
) : CatalogRepository {
    override fun observeDramaDetail(dramaId: String): Flow<DataResult<DetailPayload>> {
        val source = combine(
            entitlementRepository.observeEntitlement(),
            progressRepository.observeHistory(),
            progressRepository.observeContinueWatchingCards(),
        ) { entitlement, _, _ ->
            val drama = DramaFlowMockData.findDrama(dramaId) ?: DramaFlowMockData.dramas.first()
            val latestProgress = progressRepository.getLatestProgressForDrama(drama.id)
            val episodes = DramaFlowMockData.episodesForDrama(drama.id).map { episode ->
                val progress = progressRepository.getProgressForEpisode(episode.id)
                EpisodeListItem(
                    episode = episode,
                    watchProgress = progress,
                    isLockedForUser = episode.requiresPremium && !entitlement.isPremium,
                    isCurrentEpisode = latestProgress?.episodeId == episode.id,
                )
            }
            val primaryEpisode = latestProgress?.let { saved ->
                DramaFlowMockData.findEpisode(saved.episodeId)
            } ?: DramaFlowMockData.episodesForDrama(drama.id).firstOrNull()
            DetailPayload(
                drama = drama,
                primaryEpisode = primaryEpisode,
                watchProgress = latestProgress,
                entitlementState = entitlement,
                episodes = episodes,
                relatedTitles = DramaFlowMockData.dramas.filter { it.id != drama.id }.take(3).map {
                    it.toCard(
                        progress = null,
                        isUpdated = false,
                        isLockedForUser = it.isPremiumSeries && !entitlement.isPremium,
                    )
                },
            )
        }
        return wrapWithBehavior(FeatureKey.DETAIL, behaviorProvider, source)
    }
}

@Singleton
class FakePlaybackRepository @Inject constructor(
    private val behaviorProvider: MockBehaviorProvider,
    private val progressRepository: ProgressRepository,
    private val entitlementRepository: EntitlementRepository,
) : PlaybackRepository {
    override suspend fun loadPlaybackSession(episodeId: String): DataResult<PlaybackSession> {
        delay(180)
        return when (behaviorProvider.behaviorFor(FeatureKey.PLAYER)) {
            MockBehavior.SUCCESS -> {
                val episode = DramaFlowMockData.findEpisode(episodeId) ?: return DataResult.Empty
                val drama = DramaFlowMockData.findDrama(episode.dramaId) ?: return DataResult.Empty
                DataResult.Success(
                    PlaybackSession(
                        drama = drama,
                        episode = episode,
                        nextEpisode = DramaFlowMockData.nextEpisode(episode.id),
                        savedProgress = progressRepository.getProgressForEpisode(episode.id),
                        entitlementState = entitlementRepository.currentEntitlement(),
                    ),
                )
            }
            MockBehavior.LOADING -> DataResult.Loading
            MockBehavior.EMPTY -> DataResult.Empty
            MockBehavior.ERROR -> DataResult.Error("This episode could not be loaded right now.")
        }
    }

    override suspend fun nextEpisodeFor(episodeId: String): Episode? = DramaFlowMockData.nextEpisode(episodeId)

    override suspend fun preloadPlaybackSession(episodeId: String) = Unit

    override suspend fun sendHeartbeat(
        sessionId: String,
        positionSeconds: Int,
        bufferedPositionSeconds: Int,
        isPlaying: Boolean,
        playerState: String,
    ): PlaybackHeartbeat? = null

    override suspend fun refreshPlaybackSession(sessionId: String): PlaybackSession? = null

    override suspend fun completePlaybackSession(
        sessionId: String,
        finalPositionSeconds: Int,
        completed: Boolean,
        watchedSeconds: Int,
    ): PlaybackCompletion? = null
}

@Singleton
class FakeSubscriptionRepository @Inject constructor(
    private val behaviorProvider: MockBehaviorProvider,
    private val entitlementRepository: EntitlementRepository,
    private val preferences: DramaFlowPreferenceStore,
) : SubscriptionRepository {
    override fun observeSubscription(): Flow<DataResult<SubscriptionPayload>> {
        val source = combine(
            entitlementRepository.observeEntitlement(),
            preferences.lastSelectedProductId,
            preferences.lastSelectedOfferId,
        ) { entitlement, selectedProductId, selectedOfferId ->
            SubscriptionPayload(
                heroTitle = "Stay inside the scene instead of hitting paywalls",
                heroSubtitle = "Subscription keeps auto-play smooth, unlocks premium-only episodes, and syncs your resume state across sessions.",
                products = DramaFlowMockData.subscriptionProducts,
                selectedProductId = selectedProductId ?: DramaFlowMockData.subscriptionProducts.firstOrNull()?.id,
                selectedOfferId = selectedOfferId ?: DramaFlowMockData.subscriptionProducts.firstOrNull()?.offers?.firstOrNull()?.id,
                entitlementState = entitlement,
            )
        }
        return wrapWithBehavior(FeatureKey.SUBSCRIPTION, behaviorProvider, source)
    }

    override suspend fun setSelectedOffer(productId: String, offerId: String) {
        preferences.setLastSelectedProduct(productId, offerId)
    }
}

@Singleton
class FakeProfileRepository @Inject constructor(
    private val behaviorProvider: MockBehaviorProvider,
    private val entitlementRepository: EntitlementRepository,
    private val progressRepository: ProgressRepository,
) : ProfileRepository {
    override fun observeProfile(): Flow<DataResult<ProfilePayload>> {
        val source = combine(
            entitlementRepository.observeEntitlement(),
            progressRepository.observeContinueWatchingCards(),
            progressRepository.observeHistory(),
        ) { entitlement, continueWatching, history ->
            ProfilePayload(
                profile = DramaFlowMockData.profile,
                entitlementState = entitlement,
                continueWatching = continueWatching.firstOrNull(),
                watchHistory = history,
                favorites = DramaFlowMockData.dramas.takeLast(3).map {
                    it.toCard(progress = null, isUpdated = false, isLockedForUser = it.isPremiumSeries && !entitlement.isPremium)
                },
                unlockedSummary = if (entitlement.isPremium) "Premium unlocked across all eligible episodes" else "Free tier active",
            )
        }
        return wrapWithBehavior(FeatureKey.PROFILE, behaviorProvider, source)
    }
}

object DramaFlowMockData {
    val tags = listOf(
        DramaTag("revenge", "Revenge"),
        DramaTag("ceo", "CEO"),
        DramaTag("romance", "Romance"),
        DramaTag("twist", "Twist"),
        DramaTag("family", "Family"),
    )

    private const val sampleVideoUrl =
        "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"

    val dramas = listOf(
        Drama(
            id = "df-neon-vows",
            title = "Neon Vows",
            shortDescription = "A fake engagement turns into a power game inside a luxury tech empire.",
            longDescription = "A sharp, fast-burn vertical drama where every boardroom promise becomes public leverage. The emotional engine is built around cliffhangers, premium reveals, and rapid reversals.",
            portraitPosterUrl = "https://images.unsplash.com/photo-1515886657613-9f3515b0c78f",
            heroImageUrl = "https://images.unsplash.com/photo-1515169067868-5387ec356754",
            heatScore = "9.6",
            totalEpisodes = 6,
            tags = listOf(tags[0], tags[1], tags[2]),
            cast = listOf("Aria Vale", "Lucas Hart", "Mina Cole"),
            labels = listOf("Binge-worthy", "Fast burn", "Power clash"),
            heroNote = "New episodes every Friday",
            isFeatured = true,
            isPremiumSeries = true,
        ),
        Drama(
            id = "df-last-heiress",
            title = "The Last Heiress Signal",
            shortDescription = "A banished daughter hijacks a media launch to expose a hidden inheritance war.",
            longDescription = "An inheritance scandal delivered in fast vertical beats with escalating reveals and strategic cliffhangers.",
            portraitPosterUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1",
            heroImageUrl = "https://images.unsplash.com/photo-1521119989659-a83eee488004",
            heatScore = "9.3",
            totalEpisodes = 5,
            tags = listOf(tags[0], tags[4]),
            cast = listOf("Mila Shaw", "Jordan Wylde"),
            labels = listOf("Family feud", "Power return"),
            heroNote = "Updated every Wednesday",
            isFeatured = true,
            isPremiumSeries = false,
        ),
        Drama(
            id = "df-midnight-contract",
            title = "Midnight Contract",
            shortDescription = "An overnight marriage clause saves a company and ruins two guarded hearts.",
            longDescription = "A cleaner romance-forward title to broaden the feed and provide a non-premium alternative.",
            portraitPosterUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9",
            heroImageUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d",
            heatScore = "9.1",
            totalEpisodes = 5,
            tags = listOf(tags[1], tags[2]),
            cast = listOf("Eden Ross", "Theo Hale"),
            labels = listOf("Marriage deal", "Office sparks"),
            heroNote = "Free opening arc now live",
            isFeatured = false,
            isPremiumSeries = true,
        ),
    )

    private val episodes = listOf(
        Episode("df-neon-vows-e1", "df-neon-vows", 1, "Episode 1", "A press event goes off-script.", 33, sampleVideoUrl, dramas[0].portraitPosterUrl, true, false, null),
        Episode("df-neon-vows-e2", "df-neon-vows", 2, "Episode 2", "A fake ring triggers a real scandal.", 33, sampleVideoUrl, dramas[0].portraitPosterUrl, true, false, null),
        Episode("df-neon-vows-e3", "df-neon-vows", 3, "Episode 3", "The contract leaks to the board.", 33, sampleVideoUrl, dramas[0].portraitPosterUrl, true, false, null),
        Episode("df-neon-vows-e4", "df-neon-vows", 4, "Episode 4", "The wedding clause activates.", 33, sampleVideoUrl, dramas[0].portraitPosterUrl, true, true, 15),
        Episode("df-neon-vows-e5", "df-neon-vows", 5, "Episode 5", "A public betrayal changes the deal.", 33, sampleVideoUrl, dramas[0].portraitPosterUrl, true, true, 15),
        Episode("df-neon-vows-e6", "df-neon-vows", 6, "Episode 6", "The season's power balance flips.", 33, sampleVideoUrl, dramas[0].portraitPosterUrl, true, true, 15),
        Episode("df-last-heiress-e1", "df-last-heiress", 1, "Episode 1", "A return nobody expected.", 33, sampleVideoUrl, dramas[1].portraitPosterUrl, true, false, null),
        Episode("df-last-heiress-e2", "df-last-heiress", 2, "Episode 2", "The hidden clause comes out.", 33, sampleVideoUrl, dramas[1].portraitPosterUrl, true, false, null),
        Episode("df-midnight-contract-e1", "df-midnight-contract", 1, "Episode 1", "The marriage paper lands at midnight.", 33, sampleVideoUrl, dramas[2].portraitPosterUrl, true, false, null),
    )

    val profile = UserProfile(
        id = "df-user-001",
        displayName = "Avery Blake",
        avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2",
        countryCode = "US",
        languageCode = "en",
    )

    val subscriptionProducts = listOf(
        SubscriptionProduct(
            id = "premium_access",
            title = "DramaFlow Premium",
            subtitle = "Unlock every premium episode and keep auto-play smooth",
            benefits = listOf(
                SubscriptionBenefit("playback", "Premium playback", "Continue through paywalled episodes without stopping."),
                SubscriptionBenefit("resume", "Resume sync", "Keep your last watched episode and saved progress."),
                SubscriptionBenefit("drops", "Early drops", "Get access to newly released premium chapters sooner."),
            ),
            offers = listOf(
                SubscriptionOffer(
                    id = "monthly_premium",
                    title = "Monthly",
                    offerToken = "monthly_premium_token",
                    billingPeriodLabel = "Billed every month",
                    priceText = "$12.99",
                    monthlyEquivalentText = "$12.99/mo",
                    trialBadge = "3-day trial",
                    badgeText = "Most Popular",
                    isDefault = true,
                ),
                SubscriptionOffer(
                    id = "quarterly_premium",
                    title = "Quarterly",
                    offerToken = "quarterly_premium_token",
                    billingPeriodLabel = "Billed every 3 months",
                    priceText = "$29.99",
                    monthlyEquivalentText = "$9.99/mo",
                    trialBadge = null,
                    badgeText = "Save 23%",
                    isDefault = false,
                ),
            ),
        ),
    )

    fun findDrama(dramaId: String): Drama? = dramas.firstOrNull { it.id == dramaId }

    fun findEpisode(episodeId: String): Episode? = episodes.firstOrNull { it.id == episodeId }

    fun episodesForDrama(dramaId: String): List<Episode> = episodes.filter { it.dramaId == dramaId }

    fun nextEpisode(episodeId: String): Episode? {
        val episode = findEpisode(episodeId) ?: return null
        return episodesForDrama(episode.dramaId).firstOrNull { it.episodeNumber == episode.episodeNumber + 1 }
    }
}

private fun Drama.toCard(
    progress: WatchProgress?,
    isUpdated: Boolean,
    isLockedForUser: Boolean,
): DramaCard {
    val status = when {
        progress != null && !progress.completed -> "Continue watching"
        isLockedForUser -> "Premium"
        isUpdated -> "Updated"
        else -> null
    }
    return DramaCard(
        drama = this,
        lastProgress = progress,
        isUpdated = isUpdated,
        isLockedForUser = isLockedForUser,
        statusLabel = status,
    )
}

private fun WatchProgress.toEntity(): WatchProgressEntity {
    return WatchProgressEntity(
        episodeId = episodeId,
        dramaId = dramaId,
        positionMs = positionMs,
        durationMs = durationMs,
        progressPercent = progressPercent,
        lastUpdatedEpochMs = lastUpdatedEpochMs,
        completed = completed,
    )
}

private fun WatchProgressEntity.toModel(): WatchProgress {
    return WatchProgress(
        dramaId = dramaId,
        episodeId = episodeId,
        positionMs = positionMs,
        durationMs = durationMs,
        progressPercent = progressPercent,
        lastUpdatedEpochMs = lastUpdatedEpochMs,
        completed = completed,
    )
}

private fun WatchHistoryEntity.toWatchHistoryItem(progress: WatchProgressEntity?): WatchHistoryItem? {
    val episode = DramaFlowMockData.findEpisode(episodeId)
    val drama = DramaFlowMockData.findDrama(dramaId)
    val titleParts = title.split(" - ", limit = 2)
    val resolvedDramaTitle = drama?.title ?: titleParts.firstOrNull().orEmpty()
    val resolvedEpisodeTitle = episode?.title ?: titleParts.getOrNull(1) ?: "Episode"
    val resolvedEpisodeNumber = episode?.episodeNumber ?: progress?.let { DramaFlowMockData.findEpisode(it.episodeId)?.episodeNumber } ?: 0
    val resolvedDurationMs = progress?.durationMs ?: episode?.durationSeconds?.times(1000L) ?: 0L
    return resolvedDramaTitle.takeIf { it.isNotBlank() }?.let {
        WatchHistoryItem(
            dramaId = dramaId,
            episodeId = episodeId,
            dramaTitle = resolvedDramaTitle,
            episodeTitle = resolvedEpisodeTitle,
            episodeNumber = resolvedEpisodeNumber,
            artworkUrl = drama?.portraitPosterUrl ?: artworkUrl,
            watchedAtEpochMs = watchedAtEpochMs,
            progressPercent = progress?.progressPercent ?: progressPercent,
            positionMs = progress?.positionMs ?: 0L,
            durationMs = resolvedDurationMs,
        )
    }
}

fun ViewModel.launchDataLoad(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: suspend () -> Unit,
) {
    viewModelScope.launch(dispatcher) { block() }
}

class StateHolder<T>(initialValue: T) {
    private val backingFlow = MutableStateFlow(initialValue)
    val state: StateFlow<T> = backingFlow.asStateFlow()

    fun update(value: T) {
        backingFlow.value = value
    }
}

@Singleton
class AppScopeHolder @Inject constructor() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
