package com.dramaflow.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaybackDeviceContextDto(
    val platform: String,
    val appVersion: String,
    val networkType: String? = null,
)

@Serializable
data class RequestPlaybackSessionDto(
    @SerialName("episodeId")
    val episodeId: String,
    @SerialName("sourcePage")
    val sourcePage: String,
    @SerialName("autoNext")
    val autoNext: Boolean,
    @SerialName("preferredQuality")
    val preferredQuality: String? = null,
    @SerialName("deviceContext")
    val deviceContext: PlaybackDeviceContextDto,
)

@Serializable
data class PlaybackHeaderDto(
    val key: String,
    val value: String,
)

@Serializable
data class PlaybackNextEpisodeHintDto(
    @SerialName("episodeId")
    val episodeId: String,
    val title: String,
)

@Serializable
data class PlaybackAnalyticsContextDto(
    @SerialName("sessionId")
    val sessionId: String,
    @SerialName("dramaId")
    val dramaId: String,
    @SerialName("episodeId")
    val episodeId: String,
    @SerialName("playbackMode")
    val playbackMode: String,
    @SerialName("sourcePage")
    val sourcePage: String,
    @SerialName("signerMode")
    val signerMode: String,
)

@Serializable
data class PlaybackSessionDto(
    @SerialName("sessionId")
    val sessionId: String,
    @SerialName("playbackMode")
    val playbackMode: String,
    @SerialName("mediaUrl")
    val mediaUrl: String,
    @SerialName("requestHeaders")
    val requestHeaders: List<PlaybackHeaderDto>,
    @SerialName("expiresAt")
    val expiresAt: String,
    @SerialName("previewSeconds")
    val previewSeconds: Int,
    @SerialName("heartbeatIntervalSeconds")
    val heartbeatIntervalSeconds: Int,
    @SerialName("refreshAfterSeconds")
    val refreshAfterSeconds: Int,
    @SerialName("nextEpisodeHint")
    val nextEpisodeHint: PlaybackNextEpisodeHintDto? = null,
    @SerialName("analyticsContext")
    val analyticsContext: PlaybackAnalyticsContextDto,
)

@Serializable
data class PlaybackSessionDetailDto(
    @SerialName("sessionId")
    val sessionId: String,
    @SerialName("userId")
    val userId: String,
    @SerialName("dramaId")
    val dramaId: String,
    @SerialName("episodeId")
    val episodeId: String,
    @SerialName("playbackMode")
    val playbackMode: String,
    @SerialName("sessionStatus")
    val sessionStatus: String,
    @SerialName("mediaPath")
    val mediaPath: String,
    @SerialName("issuedAt")
    val issuedAt: String,
    @SerialName("expiresAt")
    val expiresAt: String,
)

@Serializable
data class PlaybackHeartbeatRequestDto(
    @SerialName("positionSeconds")
    val positionSeconds: Int,
    @SerialName("bufferedPositionSeconds")
    val bufferedPositionSeconds: Int,
    @SerialName("isPlaying")
    val isPlaying: Boolean,
    @SerialName("networkType")
    val networkType: String? = null,
    @SerialName("playerState")
    val playerState: String,
    @SerialName("clientTime")
    val clientTime: String,
)

@Serializable
data class HeartbeatDto(
    @SerialName("keepAlive")
    val keepAlive: Boolean,
    @SerialName("expiresAt")
    val expiresAt: String,
    @SerialName("shouldRefreshUrl")
    val shouldRefreshUrl: Boolean,
    @SerialName("previewRemainingSeconds")
    val previewRemainingSeconds: Int? = null,
    @SerialName("nextAction")
    val nextAction: String? = null,
)

@Serializable
data class PlaybackCompleteRequestDto(
    @SerialName("finalPositionSeconds")
    val finalPositionSeconds: Int,
    @SerialName("completed")
    val completed: Boolean,
    @SerialName("watchedSeconds")
    val watchedSeconds: Int,
    @SerialName("clientTime")
    val clientTime: String,
)

@Serializable
data class CompletionDto(
    @SerialName("accepted")
    val accepted: Boolean,
    @SerialName("nextEpisodeId")
    val nextEpisodeId: String? = null,
)

@Serializable
data class RefreshSessionDto(
    @SerialName("descriptor")
    val descriptor: PlaybackSessionDto,
)

@Serializable
data class DevEntitlementRequestDto(
    @SerialName("userId")
    val userId: String,
    @SerialName("entitlementType")
    val entitlementType: String = "premium",
)

typealias PlaybackDescriptorEnvelopeDto = ResponseEnvelopeDto<PlaybackSessionDto>
typealias PlaybackSessionDetailEnvelopeDto = ResponseEnvelopeDto<PlaybackSessionDetailDto>
typealias PlaybackHeartbeatResponseEnvelopeDto = ResponseEnvelopeDto<HeartbeatDto>
typealias PlaybackCompleteResponseEnvelopeDto = ResponseEnvelopeDto<CompletionDto>
typealias PlaybackSessionRefreshEnvelopeDto = ResponseEnvelopeDto<RefreshSessionDto>
