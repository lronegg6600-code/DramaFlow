package com.dramaflow.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodeProgressDto(
    @SerialName("userId")
    val userId: String,
    @SerialName("dramaId")
    val dramaId: String,
    @SerialName("episodeId")
    val episodeId: String,
    @SerialName("positionSeconds")
    val positionSeconds: Int,
    @SerialName("durationSeconds")
    val durationSeconds: Int,
    val completed: Boolean,
    @SerialName("updatedAt")
    val updatedAt: String,
)

@Serializable
data class RecentHistoryItemDto(
    @SerialName("dramaId")
    val dramaId: String,
    @SerialName("episodeId")
    val episodeId: String,
    @SerialName("watchedAt")
    val watchedAt: String,
)

@Serializable
data class UpsertEpisodeProgressRequestDto(
    @SerialName("dramaId")
    val dramaId: String,
    @SerialName("positionSeconds")
    val positionSeconds: Int,
    @SerialName("durationSeconds")
    val durationSeconds: Int,
    val completed: Boolean,
)

typealias ProgressEnvelopeDto = ResponseEnvelopeDto<EpisodeProgressDto>
typealias RecentHistoryEnvelopeDto = ResponseEnvelopeDto<List<RecentHistoryItemDto>>
