package com.dramaflow.core.network.dto

import com.dramaflow.core.model.EntitlementState
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
    val entitlements: List<EntitlementDto>? = null,
    @SerialName("isPremium")
    val isPremium: Boolean,
    @SerialName("activeProductId")
    val activeProductId: String? = null,
    val source: String,
) {
    fun entitlementsOrEmpty(): List<EntitlementDto> = entitlements.orEmpty()
}

typealias EntitlementSummaryEnvelopeDto = ResponseEnvelopeDto<EntitlementSummaryDto>

fun EntitlementSummaryDto.toDomain(): EntitlementState {
    val normalizedEntitlements = entitlementsOrEmpty()
    val activeEntitlement = normalizedEntitlements.firstOrNull {
        it.entitlementState.equals("active", ignoreCase = true) || it.entitlementState.equals("grace", ignoreCase = true)
    }
    return EntitlementState(
        isPremium = isPremium || activeEntitlement != null,
        activeProductId = activeProductId ?: activeEntitlement?.productId,
        unlockedEpisodeIds = emptyList(),
        sourceLabel = source,
    )
}
