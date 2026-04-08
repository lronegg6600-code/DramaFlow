package com.dramaflow.core.network.api

import com.dramaflow.core.network.dto.AuthEnvelopeDto
import com.dramaflow.core.network.dto.ContentDramaDetailEnvelopeDto
import com.dramaflow.core.network.dto.ContentDramaListEnvelopeDto
import com.dramaflow.core.network.dto.ContentEpisodeDetailEnvelopeDto
import com.dramaflow.core.network.dto.ContentEpisodeListEnvelopeDto
import com.dramaflow.core.network.dto.FeedContinueWatchingEnvelopeDto
import com.dramaflow.core.network.dto.FeedHomeEnvelopeDto
import com.dramaflow.core.network.dto.ProgressEnvelopeDto
import com.dramaflow.core.network.dto.RecentHistoryEnvelopeDto
import com.dramaflow.core.network.dto.RefreshRequestDto
import com.dramaflow.core.network.dto.UpsertEpisodeProgressRequestDto
import com.dramaflow.core.network.dto.UserSummaryEnvelopeDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AuthApi {
    @POST("v1/auth/guest-session")
    suspend fun createGuestSession(@Body body: Map<String, String>): AuthEnvelopeDto

    @POST("v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): AuthEnvelopeDto

    @GET("v1/auth/me")
    suspend fun me(): UserSummaryEnvelopeDto
}

interface ContentApi {
    @GET("v1/dramas")
    suspend fun getDramas(): ContentDramaListEnvelopeDto

    @GET("v1/dramas/{dramaId}")
    suspend fun getDramaDetail(@Path("dramaId") dramaId: String): ContentDramaDetailEnvelopeDto

    @GET("v1/dramas/{dramaId}/episodes")
    suspend fun getEpisodes(@Path("dramaId") dramaId: String): ContentEpisodeListEnvelopeDto

    @GET("v1/episodes/{episodeId}")
    suspend fun getEpisode(@Path("episodeId") episodeId: String): ContentEpisodeDetailEnvelopeDto
}

interface FeedApi {
    @GET("v1/feed/home")
    suspend fun getHomeFeed(): FeedHomeEnvelopeDto

    @GET("v1/feed/continue-watching")
    suspend fun getContinueWatching(): FeedContinueWatchingEnvelopeDto
}

interface ProgressApi {
    @GET("v1/progress/episodes/{episodeId}")
    suspend fun getEpisodeProgress(@Path("episodeId") episodeId: String): ProgressEnvelopeDto

    @PUT("v1/progress/episodes/{episodeId}")
    suspend fun putEpisodeProgress(
        @Path("episodeId") episodeId: String,
        @Body request: UpsertEpisodeProgressRequestDto,
    ): ProgressEnvelopeDto

    @GET("v1/history/recent")
    suspend fun getRecentHistory(): RecentHistoryEnvelopeDto
}
