package com.dramaflow.feature.subscription

import com.dramaflow.core.billing.BillingRepository
import com.dramaflow.core.model.BillingPurchasePayload
import com.dramaflow.core.model.BillingSyncResult
import com.dramaflow.core.model.EntitlementState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionSyncCoordinator @Inject constructor(
    private val billingRepository: BillingRepository,
    private val entitlementRepository: com.dramaflow.core.common.EntitlementRepository,
) {
    suspend fun syncPurchase(
        purchase: BillingPurchasePayload,
        sourcePage: String,
    ): Pair<BillingSyncResult, EntitlementState> {
        val result = billingRepository.syncPurchase(
            purchase = purchase,
            source = "purchase_flow",
            sourcePage = sourcePage,
        )
        val entitlement = entitlementRepository.refresh()
        return result to entitlement
    }

    suspend fun restorePurchases(sourcePage: String): Pair<List<BillingSyncResult>, EntitlementState> {
        val results = billingRepository.restorePurchases(sourcePage)
        val entitlement = entitlementRepository.refresh()
        return results to entitlement
    }
}
