package com.dramaflow.core.common

import android.util.Log
import com.dramaflow.core.common.auth.AuthSessionManager
import com.dramaflow.core.common.entitlement.EntitlementStateStore
import com.dramaflow.core.model.EntitlementState
import com.dramaflow.core.model.SubscriptionPayload
import com.dramaflow.core.network.source.EntitlementRemoteDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val EntitlementLogTag = "Entitlement"

@Singleton
class RemoteEntitlementRepository @Inject constructor(
    appScopeHolder: AppScopeHolder,
    private val authSessionManager: AuthSessionManager,
    private val remoteDataSource: EntitlementRemoteDataSource,
    private val stateStore: EntitlementStateStore,
) : EntitlementRepository {
    init {
        appScopeHolder.scope.launch {
            runCatching { refresh() }
        }
    }

    override fun observeEntitlement(): Flow<EntitlementState> = stateStore.observe()

    override suspend fun currentEntitlement(): EntitlementState = observeEntitlement().first()

    override suspend fun grantPremium(productId: String) {
        val state = EntitlementState(
            isPremium = true,
            activeProductId = productId,
            unlockedEpisodeIds = emptyList(),
            sourceLabel = "client_pending_sync",
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        stateStore.persist(state)
        Log.d(EntitlementLogTag, "entitlement_grant product=$productId source=${state.sourceLabel}")
    }

    override suspend fun refresh(): EntitlementState {
        authSessionManager.ensureGuestSession()
        val state = remoteDataSource.getMyEntitlements()
        stateStore.persist(state)
        Log.d(
            EntitlementLogTag,
            "entitlement_state_refresh premium=${state.isPremium} product=${state.activeProductId} source=${state.sourceLabel}",
        )
        return state
    }

    override suspend fun reset() {
        stateStore.reset()
        Log.d(EntitlementLogTag, "entitlement_revoke source=remote_reset")
    }
}

@Singleton
class RemoteSubscriptionRepository @Inject constructor(
    private val entitlementRepository: EntitlementRepository,
    private val preferences: com.dramaflow.core.database.DramaFlowPreferenceStore,
) : SubscriptionRepository {
    override fun observeSubscription(): Flow<DataResult<SubscriptionPayload>> {
        return combine(
            entitlementRepository.observeEntitlement(),
            preferences.lastSelectedProductId,
            preferences.lastSelectedOfferId,
        ) { entitlement, selectedProductId, selectedOfferId ->
            DataResult.Success(
                SubscriptionPayload(
                    heroTitle = "Google Play subscription gates premium playback",
                    heroSubtitle = "Purchase result is synced to backend billing, then entitlement-service decides the final access state.",
                    products = DramaFlowMockData.subscriptionProducts,
                    selectedProductId = selectedProductId ?: DramaFlowMockData.subscriptionProducts.firstOrNull()?.id,
                    selectedOfferId = selectedOfferId ?: DramaFlowMockData.subscriptionProducts.firstOrNull()?.offers?.firstOrNull()?.id,
                    entitlementState = entitlement,
                ),
            )
        }
    }

    override suspend fun setSelectedOffer(productId: String, offerId: String) {
        preferences.setLastSelectedProduct(productId, offerId)
    }
}
