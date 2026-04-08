package com.dramaflow.core.common

import com.dramaflow.core.common.auth.AuthSessionManager
import com.dramaflow.core.model.PlaybackAccessMode
import com.dramaflow.core.model.PlaybackCompletion
import com.dramaflow.core.model.PlaybackDescriptor
import com.dramaflow.core.model.PlaybackHeartbeat
import com.dramaflow.core.model.PlaybackSession
import com.dramaflow.core.model.WatchProgress
import com.dramaflow.core.network.source.PlaybackRemoteDataSource
import com.dramaflow.core.network.source.ProgressRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HybridProgressRepository @Inject constructor(
    private val localRepository: FakeProgressRepository,
    private val authSessionManager: AuthSessionManager,
    private val remoteDataSource: ProgressRemoteDataSource,
) : ProgressRepository by localRepository {
    override suspend fun saveProgress(progress: WatchProgress) {
        localRepository.saveProgress(progress)
        if (AppEnvironment.current.playbackDataMode == com.dramaflow.core.model.PlaybackDataMode.FAKE_ONLY) return
        runCatching {
            authSessionManager.ensureGuestSession()
            remoteDataSource.putEpisodeProgress(progress)
        }
    }
}

@Singleton
class HybridPlaybackRepository @Inject constructor(
    private val localRepository: FakePlaybackRepository,
    private val authSessionManager: AuthSessionManager,
    private val remoteDataSource: PlaybackRemoteDataSource,
    private val progressRepository: ProgressRepository,
    private val entitlementRepository: EntitlementRepository,
) : PlaybackRepository {
    private val preloadCache = mutableMapOf<String, PlaybackDescriptor>()
    private val sessionCache = mutableMapOf<String, PlaybackSession>()

    override suspend fun loadPlaybackSession(episodeId: String): DataResult<PlaybackSession> {
        if (AppEnvironment.current.playbackDataMode == com.dramaflow.core.model.PlaybackDataMode.FAKE_ONLY) {
            return localRepository.loadPlaybackSession(episodeId)
        }

        return runCatching {
            authSessionManager.ensureGuestSession()
            val descriptor = preloadCache.remove(episodeId)
                ?: remoteDataSource.createSession(episodeId = episodeId, sourcePage = "player", autoNext = false)
            val session = buildRemoteSession(episodeId, descriptor)
            sessionCache[descriptor.sessionId] = session
            DataResult.Success(session)
        }.getOrElse {
            if (AppEnvironment.current.allowPlaybackFallback) {
                localRepository.loadPlaybackSession(episodeId)
            } else {
                DataResult.Error("Playback authorization failed. Please retry in a moment.")
            }
        }
    }

    override suspend fun nextEpisodeFor(episodeId: String) = localRepository.nextEpisodeFor(episodeId)

    override suspend fun preloadPlaybackSession(episodeId: String) {
        if (AppEnvironment.current.playbackDataMode == com.dramaflow.core.model.PlaybackDataMode.FAKE_ONLY) return
        runCatching {
            authSessionManager.ensureGuestSession()
            preloadCache[episodeId] = remoteDataSource.createSession(
                episodeId = episodeId,
                sourcePage = "player",
                autoNext = true,
            )
        }
    }

    override suspend fun sendHeartbeat(
        sessionId: String,
        positionSeconds: Int,
        bufferedPositionSeconds: Int,
        isPlaying: Boolean,
        playerState: String,
    ): PlaybackHeartbeat? {
        if (AppEnvironment.current.playbackDataMode == com.dramaflow.core.model.PlaybackDataMode.FAKE_ONLY) return null
        return runCatching {
            remoteDataSource.heartbeat(
                sessionId = sessionId,
                positionSeconds = positionSeconds,
                bufferedPositionSeconds = bufferedPositionSeconds,
                isPlaying = isPlaying,
                playerState = playerState,
                networkType = null,
            )
        }.getOrNull()
    }

    override suspend fun refreshPlaybackSession(sessionId: String): PlaybackSession? {
        if (AppEnvironment.current.playbackDataMode == com.dramaflow.core.model.PlaybackDataMode.FAKE_ONLY) return null
        val current = sessionCache[sessionId] ?: return null
        val descriptor = runCatching { remoteDataSource.refresh(sessionId) }.getOrNull() ?: return null
        val refreshed = current.copy(
            episode = current.episode.copy(streamUrl = descriptor.mediaUrl),
            playbackDescriptor = descriptor,
        )
        sessionCache[sessionId] = refreshed
        return refreshed
    }

    override suspend fun completePlaybackSession(
        sessionId: String,
        finalPositionSeconds: Int,
        completed: Boolean,
        watchedSeconds: Int,
    ): PlaybackCompletion? {
        if (AppEnvironment.current.playbackDataMode == com.dramaflow.core.model.PlaybackDataMode.FAKE_ONLY) return null
        return runCatching {
            remoteDataSource.complete(
                sessionId = sessionId,
                finalPositionSeconds = finalPositionSeconds,
                completed = completed,
                watchedSeconds = watchedSeconds,
            )
        }.getOrNull()
    }

    private suspend fun buildRemoteSession(
        episodeId: String,
        descriptor: PlaybackDescriptor,
    ): PlaybackSession {
        val fallback = when (val result = localRepository.loadPlaybackSession(episodeId)) {
            is DataResult.Success -> result.value
            else -> {
                val episode = DramaFlowMockData.findEpisode(episodeId) ?: DramaFlowMockData.episodesForDrama("df-neon-vows").first()
                val drama = DramaFlowMockData.findDrama(episode.dramaId) ?: DramaFlowMockData.dramas.first()
                PlaybackSession(
                    drama = drama,
                    episode = episode,
                    nextEpisode = DramaFlowMockData.nextEpisode(episode.id),
                    savedProgress = null,
                    entitlementState = entitlementRepository.currentEntitlement(),
                )
            }
        }
        val remoteEpisode = fallback.episode.copy(
            streamUrl = descriptor.mediaUrl,
            requiresPremium = descriptor.playbackMode == PlaybackAccessMode.PREVIEW,
            previewWindowSeconds = descriptor.previewSeconds.takeIf { descriptor.playbackMode == PlaybackAccessMode.PREVIEW && it > 0 },
        )
        return fallback.copy(
            episode = remoteEpisode,
            nextEpisode = descriptor.nextEpisodeHint?.episodeId?.let { DramaFlowMockData.findEpisode(it) } ?: fallback.nextEpisode,
            savedProgress = progressRepository.getProgressForEpisode(episodeId),
            playbackDescriptor = descriptor,
            isRemotePlayback = true,
        )
    }
}
