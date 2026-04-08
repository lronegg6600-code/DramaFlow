package com.dramaflow.core.network.api

import com.dramaflow.core.network.dto.BillingPurchaseSyncRequestDto
import com.dramaflow.core.network.dto.BillingSubscriptionStatusEnvelopeDto
import com.dramaflow.core.network.dto.BillingSyncEnvelopeDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BillingApi {
    @POST("v1/billing/google-play/purchases:sync")
    suspend fun syncPurchase(
        @Body request: BillingPurchaseSyncRequestDto,
    ): BillingSyncEnvelopeDto

    @POST("v1/billing/google-play/purchases/{purchaseToken}/resync")
    suspend fun resyncPurchase(
        @Path("purchaseToken") purchaseToken: String,
    ): BillingSyncEnvelopeDto

    @GET("v1/billing/me/subscription-status")
    suspend fun getSubscriptionStatus(): BillingSubscriptionStatusEnvelopeDto
}
