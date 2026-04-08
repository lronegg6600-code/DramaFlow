package com.dramaflow.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EntitlementDto(
    @SerialName("entitlementId")
    val entitlementId: String,
    @SerialName("userId")
    val userId: String,
    @SerialName("entitlementType")
    val entitlementType: String,
    @SerialName("productId")
    val productId: String,
    @SerialName("state")
    val entitlementState: String,
)

@Serializable
data class EntitlementSummaryDto(
    @SerialName("userId")
    val userId: String,
    @SerialName("entitlements")
    val entitlements: List<EntitlementDto> = emptyList(),
    @SerialName("isPremium")
    val isPremium: Boolean,
    @SerialName("activeProductId")
    val activeProductId: String? = null,
    val source: String,
)

typealias EntitlementSummaryEnvelopeDto = ResponseEnvelopeDto<EntitlementSummaryDto>
