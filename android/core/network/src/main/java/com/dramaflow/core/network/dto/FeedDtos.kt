package com.dramaflow.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedItemDto(
    @SerialName("dramaId")
    val dramaId: String,
    val title: String,
    @SerialName("shortDescription")
    val shortDescription: String,
    @SerialName("posterUrl")
    val posterUrl: String,
    @SerialName("coverUrl")
    val coverUrl: String,
    val tags: List<String>,
    @SerialName("isFeatured")
    val isFeatured: Boolean,
    @SerialName("isPremium")
    val isPremium: Boolean,
)

@Serializable
data class ContinueWatchingItemDto(
    @SerialName("dramaId")
    val dramaId: String,
    @SerialName("episodeId")
    val episodeId: String,
    val title: String,
    @SerialName("progressPercent")
    val progressPercent: Int,
    @SerialName("updatedAt")
    val updatedAt: String,
)

@Serializable
data class HomeFeedDto(
    val featured: List<FeedItemDto> = emptyList(),
    val trending: List<FeedItemDto> = emptyList(),
    val recommended: List<FeedItemDto> = emptyList(),
    @SerialName("continueWatching")
    val continueWatching: List<ContinueWatchingItemDto> = emptyList(),
)

typealias FeedHomeEnvelopeDto = ResponseEnvelopeDto<HomeFeedDto>
typealias FeedContinueWatchingEnvelopeDto = ResponseEnvelopeDto<List<ContinueWatchingItemDto>>
