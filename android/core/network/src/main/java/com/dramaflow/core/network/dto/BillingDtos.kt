package com.dramaflow.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BillingPurchaseSyncRequestDto(
    @SerialName("purchaseToken")
    val purchaseToken: String,
    @SerialName("productId")
    val productId: String,
    @SerialName("basePlanId")
    val basePlanId: String? = null,
    @SerialName("offerId")
    val offerId: String? = null,
    @SerialName("packageName")
    val packageName: String,
    val source: String,
    @SerialName("clientContext")
    val clientContext: BillingClientContextDto? = null,
)

@Serializable
data class BillingClientContextDto(
    @SerialName("appVersion")
    val appVersion: String? = null,
    val country: String? = null,
    @SerialName("sourcePage")
    val sourcePage: String? = null,
    @SerialName("debugInfo")
    val debugInfo: Map<String, String>? = null,
)

@Serializable
data class BillingSyncResponseDto(
    @SerialName("syncAccepted")
    val syncAccepted: Boolean,
    @SerialName("purchaseState")
    val purchaseState: String,
    @SerialName("acknowledgementState")
    val acknowledgementState: String,
    @SerialName("entitlementState")
    val entitlementState: String,
    @SerialName("entitlementEffectiveAt")
    val entitlementEffectiveAt: String? = null,
    @SerialName("nextAction")
    val nextAction: String,
    @SerialName("traceId")
    val traceId: String? = null,
)

@Serializable
data class BillingSubscriptionStatusDto(
    @SerialName("userId")
    val userId: String,
    @SerialName("purchaseToken")
    val purchaseToken: String? = null,
    @SerialName("productId")
    val productId: String? = null,
    @SerialName("purchaseState")
    val purchaseState: String,
    @SerialName("acknowledgementState")
    val acknowledgementState: String,
    @SerialName("entitlementState")
    val entitlementState: String,
)

typealias BillingSyncEnvelopeDto = ResponseEnvelopeDto<BillingSyncResponseDto>
typealias BillingSubscriptionStatusEnvelopeDto = ResponseEnvelopeDto<BillingSubscriptionStatusDto>
