package com.dramaflow.core.common.entitlement

import com.dramaflow.core.database.DramaFlowPreferenceStore
import com.dramaflow.core.model.EntitlementState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class EntitlementStateStore @Inject constructor(
    private val preferences: DramaFlowPreferenceStore,
) {
    fun observe(): Flow<EntitlementState> {
        return combine(preferences.premiumState, preferences.premiumProductId) { state, productId ->
            EntitlementState(
                isPremium = state == "premium",
                activeProductId = productId,
                unlockedEpisodeIds = emptyList(),
                sourceLabel = if (state == "premium") "cached_remote_entitlement" else "free_tier",
            )
        }
    }

    suspend fun persist(state: EntitlementState) {
        preferences.setPremiumState(
            premiumState = if (state.isPremium) "premium" else "free",
            activeProductId = state.activeProductId,
        )
    }

    suspend fun reset() {
        preferences.resetPremiumState()
    }
}
