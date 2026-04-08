package com.dramaflow.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class PlaybackAccessMode {
    PREVIEW,
    FULL,
}

@Serializable
data class PlaybackHeader(
    val key: String,
    val value: String,
)

@Serializable
data class PlaybackAnalyticsContext(
    val sessionId: String,
    val dramaId: String,
    val episodeId: String,
    val playbackMode: String,
    val sourcePage: String,
    val signerMode: String,
)

@Serializable
data class PlaybackDescriptor(
    val sessionId: String,
    val playbackMode: PlaybackAccessMode,
    val mediaUrl: String,
    val requestHeaders: List<PlaybackHeader>,
    val expiresAtEpochMs: Long,
    val previewSeconds: Int,
    val heartbeatIntervalSeconds: Int,
    val refreshAfterSeconds: Int,
    val nextEpisodeHint: NextEpisodeHint?,
    val analyticsContext: PlaybackAnalyticsContext,
)

@Serializable
data class PlaybackHeartbeat(
    val keepAlive: Boolean,
    val expiresAtEpochMs: Long,
    val shouldRefreshUrl: Boolean,
    val previewRemainingSeconds: Int?,
    val nextAction: String?,
)

@Serializable
data class PlaybackCompletion(
    val accepted: Boolean,
    val nextEpisodeId: String?,
)

@Serializable
enum class PlaybackDataMode {
    FAKE_ONLY,
    HYBRID,
    REMOTE_PLAYBACK,
}
