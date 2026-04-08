package com.dramaflow.core.network.api

import com.dramaflow.core.network.dto.DevEntitlementRequestDto
import com.dramaflow.core.network.dto.PlaybackCompleteRequestDto
import com.dramaflow.core.network.dto.PlaybackCompleteResponseEnvelopeDto
import com.dramaflow.core.network.dto.PlaybackDescriptorEnvelopeDto
import com.dramaflow.core.network.dto.PlaybackHeartbeatRequestDto
import com.dramaflow.core.network.dto.PlaybackHeartbeatResponseEnvelopeDto
import com.dramaflow.core.network.dto.PlaybackSessionDetailEnvelopeDto
import com.dramaflow.core.network.dto.PlaybackSessionRefreshEnvelopeDto
import com.dramaflow.core.network.dto.RequestPlaybackSessionDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PlaybackApi {
    @POST("v1/playback/sessions")
    suspend fun createPlaybackSession(
        @Body request: RequestPlaybackSessionDto,
    ): PlaybackDescriptorEnvelopeDto

    @GET("v1/playback/sessions/{sessionId}")
    suspend fun getSession(
        @Path("sessionId") sessionId: String,
    ): PlaybackSessionDetailEnvelopeDto

    @POST("v1/playback/sessions/{sessionId}/heartbeat")
    suspend fun sendHeartbeat(
        @Path("sessionId") sessionId: String,
        @Body request: PlaybackHeartbeatRequestDto,
    ): PlaybackHeartbeatResponseEnvelopeDto

    @POST("v1/playback/sessions/{sessionId}/complete")
    suspend fun complete(
        @Path("sessionId") sessionId: String,
        @Body request: PlaybackCompleteRequestDto,
    ): PlaybackCompleteResponseEnvelopeDto

    @POST("v1/playback/sessions/{sessionId}/refresh")
    suspend fun refresh(
        @Path("sessionId") sessionId: String,
    ): PlaybackSessionRefreshEnvelopeDto

    @POST("v1/dev/entitlements/grant-premium")
    suspend fun grantPremium(
        @Body request: DevEntitlementRequestDto,
    )

    @POST("v1/dev/entitlements/revoke-premium")
    suspend fun revokePremium(
        @Body request: DevEntitlementRequestDto,
    )
}
