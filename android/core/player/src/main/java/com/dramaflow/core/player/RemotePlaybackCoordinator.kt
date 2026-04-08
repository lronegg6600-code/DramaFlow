package com.dramaflow.core.player

import com.dramaflow.core.common.PlaybackRepository
import com.dramaflow.core.model.PlaybackCompletion
import com.dramaflow.core.model.PlaybackHeartbeat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemotePlaybackCoordinator @Inject constructor(
    private val playbackRepository: PlaybackRepository,
) {
    suspend fun heartbeat(
        sessionId: String,
        positionSeconds: Int,
        bufferedPositionSeconds: Int,
        isPlaying: Boolean,
        playerState: String,
    ): PlaybackHeartbeat? = playbackRepository.sendHeartbeat(
        sessionId = sessionId,
        positionSeconds = positionSeconds,
        bufferedPositionSeconds = bufferedPositionSeconds,
        isPlaying = isPlaying,
        playerState = playerState,
    )

    suspend fun complete(
        sessionId: String,
        finalPositionSeconds: Int,
        completed: Boolean,
        watchedSeconds: Int,
    ): PlaybackCompletion? = playbackRepository.completePlaybackSession(
        sessionId = sessionId,
        finalPositionSeconds = finalPositionSeconds,
        completed = completed,
        watchedSeconds = watchedSeconds,
    )
}
