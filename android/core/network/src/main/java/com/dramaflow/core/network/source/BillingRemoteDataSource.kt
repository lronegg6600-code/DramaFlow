package com.dramaflow.core.network.source

import com.dramaflow.core.model.BillingPurchasePayload
import com.dramaflow.core.model.BillingSubscriptionStatus
import com.dramaflow.core.model.BillingSyncResult
import com.dramaflow.core.network.DramaFlowNetworkModule
import com.dramaflow.core.network.api.BillingApi
import com.dramaflow.core.network.dto.BillingClientContextDto
import com.dramaflow.core.network.dto.BillingPurchaseSyncRequestDto

interface BillingRemoteDataSource {
    suspend fun syncPurchase(
        purchase: BillingPurchasePayload,
        source: String,
        sourcePage: String,
    ): BillingSyncResult

    suspend fun resyncPurchase(purchaseToken: String): BillingSyncResult
    suspend fun getSubscriptionStatus(): BillingSubscriptionStatus?
}

class RetrofitBillingRemoteDataSource(
    accessTokenProvider: DramaFlowNetworkModule.AccessTokenProvider,
    private val api: BillingApi = DramaFlowNetworkModule.createRetrofit(
        baseUrl = DramaFlowNetworkModule.currentEnvironment().billingBaseUrl,
        accessTokenProvider = accessTokenProvider,
    ).create(BillingApi::class.java),
) : BillingRemoteDataSource {
    override suspend fun syncPurchase(
        purchase: BillingPurchasePayload,
        source: String,
        sourcePage: String,
    ): BillingSyncResult {
        val data = api.syncPurchase(
            BillingPurchaseSyncRequestDto(
                purchaseToken = purchase.purchaseToken,
                productId = purchase.productId,
                basePlanId = purchase.basePlanId,
                offerId = purchase.offerId,
                packageName = "com.dramaflow.app",
                source = source,
                clientContext = BillingClientContextDto(
                    appVersion = "0.5.0",
                    sourcePage = sourcePage,
                ),
            ),
        ).data ?: error("billing sync returned empty payload")
        return BillingSyncResult(
            syncAccepted = data.syncAccepted,
            purchaseState = data.purchaseState,
            acknowledgementState = data.acknowledgementState,
            entitlementState = data.entitlementState,
            entitlementEffectiveAt = data.entitlementEffectiveAt,
            nextAction = data.nextAction,
            traceId = data.traceId,
        )
    }

    override suspend fun resyncPurchase(purchaseToken: String): BillingSyncResult {
        val data = api.resyncPurchase(purchaseToken).data ?: error("billing resync returned empty payload")
        return BillingSyncResult(
            syncAccepted = data.syncAccepted,
            purchaseState = data.purchaseState,
            acknowledgementState = data.acknowledgementState,
            entitlementState = data.entitlementState,
            entitlementEffectiveAt = data.entitlementEffectiveAt,
            nextAction = data.nextAction,
            traceId = data.traceId,
        )
    }

    override suspend fun getSubscriptionStatus(): BillingSubscriptionStatus? {
        val data = api.getSubscriptionStatus().data ?: return null
        return BillingSubscriptionStatus(
            userId = data.userId,
            purchaseToken = data.purchaseToken,
            productId = data.productId,
            purchaseState = data.purchaseState,
            acknowledgementState = data.acknowledgementState,
            entitlementState = data.entitlementState,
        )
    }
}
