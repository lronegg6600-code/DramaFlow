package com.dramaflow.core.network.source

import com.dramaflow.core.model.PlaybackCompletion
import com.dramaflow.core.model.PlaybackDescriptor
import com.dramaflow.core.model.PlaybackHeartbeat
import com.dramaflow.core.network.DramaFlowNetworkModule
import com.dramaflow.core.network.api.PlaybackApi
import com.dramaflow.core.network.dto.PlaybackCompleteRequestDto
import com.dramaflow.core.network.dto.PlaybackDeviceContextDto
import com.dramaflow.core.network.dto.PlaybackHeartbeatRequestDto
import com.dramaflow.core.network.dto.RequestPlaybackSessionDto
import com.dramaflow.core.network.mapper.toDomain
import java.time.Instant

interface PlaybackRemoteDataSource {
    suspend fun createSession(
        episodeId: String,
        sourcePage: String,
        autoNext: Boolean,
    ): PlaybackDescriptor

    suspend fun heartbeat(
        sessionId: String,
        positionSeconds: Int,
        bufferedPositionSeconds: Int,
        isPlaying: Boolean,
        playerState: String,
        networkType: String?,
    ): PlaybackHeartbeat

    suspend fun complete(
        sessionId: String,
        finalPositionSeconds: Int,
        completed: Boolean,
        watchedSeconds: Int,
    ): PlaybackCompletion

    suspend fun refresh(sessionId: String): PlaybackDescriptor
}

class RetrofitPlaybackRemoteDataSource(
    accessTokenProvider: DramaFlowNetworkModule.AccessTokenProvider,
    private val api: PlaybackApi = DramaFlowNetworkModule.createRetrofit(
        baseUrl = DramaFlowNetworkModule.currentEnvironment().playbackBaseUrl,
        accessTokenProvider = accessTokenProvider,
    ).create(PlaybackApi::class.java),
) : PlaybackRemoteDataSource {
    override suspend fun createSession(
        episodeId: String,
        sourcePage: String,
        autoNext: Boolean,
    ): PlaybackDescriptor =
        api.createPlaybackSession(
            RequestPlaybackSessionDto(
                episodeId = episodeId,
                sourcePage = sourcePage,
                autoNext = autoNext,
                deviceContext = PlaybackDeviceContextDto(
                    platform = "android",
                    appVersion = "0.4.0-dev",
                    networkType = "unknown",
                ),
            ),
        ).data!!.toDomain()

    override suspend fun heartbeat(
        sessionId: String,
        positionSeconds: Int,
        bufferedPositionSeconds: Int,
        isPlaying: Boolean,
        playerState: String,
        networkType: String?,
    ): PlaybackHeartbeat =
        api.sendHeartbeat(
            sessionId = sessionId,
            request = PlaybackHeartbeatRequestDto(
                positionSeconds = positionSeconds,
                bufferedPositionSeconds = bufferedPositionSeconds,
                isPlaying = isPlaying,
                networkType = networkType,
                playerState = playerState,
                clientTime = Instant.now().toString(),
            ),
        ).data!!.toDomain()

    override suspend fun complete(
        sessionId: String,
        finalPositionSeconds: Int,
        completed: Boolean,
        watchedSeconds: Int,
    ): PlaybackCompletion =
        api.complete(
            sessionId = sessionId,
            request = PlaybackCompleteRequestDto(
                finalPositionSeconds = finalPositionSeconds,
                completed = completed,
                watchedSeconds = watchedSeconds,
                clientTime = Instant.now().toString(),
            ),
        ).data!!.toDomain()

    override suspend fun refresh(sessionId: String): PlaybackDescriptor =
        api.refresh(sessionId).data!!.descriptor.toDomain()
}
