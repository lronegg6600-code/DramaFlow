package com.dramaflow.core.player

import com.dramaflow.core.common.PlaybackRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackPreloadCoordinator @Inject constructor(
    private val playbackRepository: PlaybackRepository,
) {
    suspend fun preloadNextEpisode(episodeId: String) {
        playbackRepository.preloadPlaybackSession(episodeId)
    }
}
