package com.dramaflow.core.network.source

import com.dramaflow.core.model.Drama
import com.dramaflow.core.model.DramaCard
import com.dramaflow.core.model.Episode
import com.dramaflow.core.model.WatchHistoryItem
import com.dramaflow.core.model.WatchProgress
import com.dramaflow.core.network.DramaFlowNetworkModule
import com.dramaflow.core.network.api.AuthApi
import com.dramaflow.core.network.api.ContentApi
import com.dramaflow.core.network.api.FeedApi
import com.dramaflow.core.network.api.ProgressApi
import com.dramaflow.core.network.dto.RefreshRequestDto
import com.dramaflow.core.network.dto.UpsertEpisodeProgressRequestDto
import com.dramaflow.core.network.mapper.toDomain
import com.dramaflow.core.network.mapper.toDramaCard

data class AuthSessionPayload(
    val userId: String?,
    val accessToken: String?,
    val refreshToken: String?,
    val expiresAt: String?,
)

interface AuthRemoteDataSource {
    suspend fun createGuestSession(anonymousDeviceId: String): String?
    suspend fun refresh(refreshToken: String): String?
    suspend fun createGuestSessionPayload(anonymousDeviceId: String): AuthSessionPayload
    suspend fun refreshSessionPayload(refreshToken: String): AuthSessionPayload?
}

interface ContentRemoteDataSource {
    suspend fun getDramas(): List<Drama>
    suspend fun getDrama(dramaId: String): Drama?
    suspend fun getEpisodes(dramaId: String): List<Episode>
    suspend fun getEpisode(episodeId: String): Episode?
}

interface FeedRemoteDataSource {
    suspend fun getHomeFeed(): List<DramaCard>
    suspend fun getContinueWatching(): List<DramaCard>
}

interface ProgressRemoteDataSource {
    suspend fun getEpisodeProgress(episodeId: String): WatchProgress?
    suspend fun putEpisodeProgress(progress: WatchProgress): WatchProgress?
    suspend fun getRecentHistory(): List<WatchHistoryItem>
}

class RetrofitAuthRemoteDataSource(
    private val api: AuthApi =
        DramaFlowNetworkModule.createRetrofit(DramaFlowNetworkModule.currentEnvironment().authBaseUrl).create(AuthApi::class.java),
) : AuthRemoteDataSource {
    override suspend fun createGuestSession(anonymousDeviceId: String): String? =
        api.createGuestSession(mapOf("anonymousDeviceId" to anonymousDeviceId)).data?.accessToken

    override suspend fun refresh(refreshToken: String): String? =
        api.refresh(RefreshRequestDto(refreshToken)).data?.accessToken

    override suspend fun createGuestSessionPayload(anonymousDeviceId: String): AuthSessionPayload {
        val data = api.createGuestSession(mapOf("anonymousDeviceId" to anonymousDeviceId)).data
        return AuthSessionPayload(
            userId = data?.user?.id,
            accessToken = data?.accessToken,
            refreshToken = data?.refreshToken,
            expiresAt = data?.expiresAt,
        )
    }

    override suspend fun refreshSessionPayload(refreshToken: String): AuthSessionPayload? {
        val data = api.refresh(RefreshRequestDto(refreshToken)).data ?: return null
        return AuthSessionPayload(
            userId = data.user.id,
            accessToken = data.accessToken,
            refreshToken = data.refreshToken,
            expiresAt = data.expiresAt,
        )
    }
}

class RetrofitContentRemoteDataSource(
    private val api: ContentApi =
        DramaFlowNetworkModule.createRetrofit(DramaFlowNetworkModule.currentEnvironment().contentBaseUrl).create(ContentApi::class.java),
) : ContentRemoteDataSource {
    private val sampleStreamUrl = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"

    override suspend fun getDramas(): List<Drama> =
        api.getDramas().data.orEmpty().map { it.toDomain() }

    override suspend fun getDrama(dramaId: String): Drama? =
        api.getDramaDetail(dramaId).data?.toDomain()

    override suspend fun getEpisodes(dramaId: String): List<Episode> =
        api.getEpisodes(dramaId).data.orEmpty().map { it.toDomain(sampleStreamUrl) }

    override suspend fun getEpisode(episodeId: String): Episode? =
        api.getEpisode(episodeId).data?.toDomain(sampleStreamUrl)
}

class RetrofitFeedRemoteDataSource(
    private val api: FeedApi =
        DramaFlowNetworkModule.createRetrofit(DramaFlowNetworkModule.currentEnvironment().feedBaseUrl).create(FeedApi::class.java),
) : FeedRemoteDataSource {
    override suspend fun getHomeFeed(): List<DramaCard> {
        val payload = api.getHomeFeed().data ?: return emptyList()
        return buildList {
            addAll(payload.featured.map { it.toDramaCard() })
            addAll(payload.trending.map { it.toDramaCard() })
            addAll(payload.recommended.map { it.toDramaCard() })
        }
    }

    override suspend fun getContinueWatching(): List<DramaCard> =
        api.getContinueWatching().data.orEmpty().map { it.toDramaCard() }
}

class RetrofitProgressRemoteDataSource(
    accessTokenProvider: DramaFlowNetworkModule.AccessTokenProvider,
    private val api: ProgressApi =
        DramaFlowNetworkModule.createRetrofit(
            baseUrl = DramaFlowNetworkModule.currentEnvironment().progressBaseUrl,
            accessTokenProvider = accessTokenProvider,
        ).create(ProgressApi::class.java),
) : ProgressRemoteDataSource {
    override suspend fun getEpisodeProgress(episodeId: String): WatchProgress? =
        api.getEpisodeProgress(episodeId).data?.toDomain()

    override suspend fun putEpisodeProgress(progress: WatchProgress): WatchProgress? =
        api.putEpisodeProgress(
            episodeId = progress.episodeId,
            request = UpsertEpisodeProgressRequestDto(
                dramaId = progress.dramaId,
                positionSeconds = (progress.positionMs / 1000L).toInt(),
                durationSeconds = (progress.durationMs / 1000L).toInt(),
                completed = progress.completed,
            ),
        ).data?.toDomain()

    override suspend fun getRecentHistory(): List<WatchHistoryItem> =
        api.getRecentHistory().data.orEmpty().map {
            it.toDomain(title = "Recent episode", artworkUrl = "")
        }
}
