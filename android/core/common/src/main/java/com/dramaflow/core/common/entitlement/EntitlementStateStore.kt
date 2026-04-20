package com.dramaflow.core.common.entitlement

import com.dramaflow.core.database.DramaFlowPreferenceStore
import com.dramaflow.core.model.EntitlementState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

@Singleton
class EntitlementStateStore @Inject constructor(
    private val preferences: DramaFlowPreferenceStore,
) {
    fun observe(): Flow<EntitlementState> {
        return combine(
            preferences.premiumState,
            preferences.premiumProductId,
            preferences.premiumSourceLabel,
            preferences.premiumUpdatedAtEpochMs,
            preferences.premiumExpiresAtEpochMs,
        ) { state, productId, sourceLabel, updatedAtEpochMs, expiresAtEpochMs ->
            EntitlementState(
                isPremium = state == "premium",
                activeProductId = productId,
                unlockedEpisodeIds = emptyList(),
                sourceLabel = sourceLabel,
                updatedAtEpochMs = updatedAtEpochMs,
                expiresAtEpochMs = expiresAtEpochMs,
            )
        }
    }

    suspend fun current(): EntitlementState = observe().first()

    suspend fun persist(state: EntitlementState) {
        preferences.setPremiumState(
            premiumState = if (state.isPremium) "premium" else "free",
            activeProductId = state.activeProductId,
            sourceLabel = state.sourceLabel,
            updatedAtEpochMs = state.updatedAtEpochMs,
            expiresAtEpochMs = state.expiresAtEpochMs,
        )
    }

    suspend fun reset() {
        preferences.resetPremiumState()
    }
}
