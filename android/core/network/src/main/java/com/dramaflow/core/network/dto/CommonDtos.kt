package com.dramaflow.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ErrorEnvelopeDto(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap(),
)

@Serializable
data class ResponseEnvelopeDto<T>(
    val data: T? = null,
    val error: ErrorEnvelopeDto? = null,
)

@Serializable
data class UserSummaryDto(
    val id: String,
    val status: String,
)

@Serializable
data class SessionDto(
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("refreshToken")
    val refreshToken: String,
    @SerialName("expiresAt")
    val expiresAt: String,
    val user: UserSummaryDto,
)

typealias AuthEnvelopeDto = ResponseEnvelopeDto<SessionDto>
typealias UserSummaryEnvelopeDto = ResponseEnvelopeDto<UserSummaryDto>

@Serializable
data class RefreshRequestDto(
    @SerialName("refreshToken")
    val refreshToken: String,
)
