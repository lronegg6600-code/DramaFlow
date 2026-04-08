package com.dramaflow.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DramaDto(
    val id: String,
    val title: String,
    @SerialName("shortDescription")
    val shortDescription: String,
    @SerialName("longDescription")
    val longDescription: String,
    @SerialName("posterUrl")
    val posterUrl: String,
    @SerialName("coverUrl")
    val coverUrl: String,
    val tags: List<String>,
    val region: String,
    val language: String,
    @SerialName("publishStatus")
    val publishStatus: String,
    @SerialName("isFeatured")
    val isFeatured: Boolean,
)

@Serializable
data class EpisodeDto(
    val id: String,
    @SerialName("dramaId")
    val dramaId: String,
    @SerialName("episodeNo")
    val episodeNo: Int,
    val title: String,
    val description: String,
    @SerialName("durationSeconds")
    val durationSeconds: Int,
    @SerialName("previewSeconds")
    val previewSeconds: Int,
    @SerialName("isPremium")
    val isPremium: Boolean,
    @SerialName("streamKeyPlaceholder")
    val streamKeyPlaceholder: String,
    @SerialName("publishStatus")
    val publishStatus: String,
    @SerialName("sortOrder")
    val sortOrder: Int,
)

typealias ContentDramaListEnvelopeDto = ResponseEnvelopeDto<List<DramaDto>>
typealias ContentDramaDetailEnvelopeDto = ResponseEnvelopeDto<DramaDto>
typealias ContentEpisodeListEnvelopeDto = ResponseEnvelopeDto<List<EpisodeDto>>
typealias ContentEpisodeDetailEnvelopeDto = ResponseEnvelopeDto<EpisodeDto>
