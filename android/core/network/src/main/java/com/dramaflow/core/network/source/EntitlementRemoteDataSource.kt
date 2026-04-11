package com.dramaflow.core.network.source

import com.dramaflow.core.model.EntitlementState
import com.dramaflow.core.network.DramaFlowNetworkModule
import com.dramaflow.core.network.api.EntitlementApi
import com.dramaflow.core.network.dto.toDomain

interface EntitlementRemoteDataSource {
    suspend fun getMyEntitlements(): EntitlementState
}

class RetrofitEntitlementRemoteDataSource(
    accessTokenProvider: DramaFlowNetworkModule.AccessTokenProvider,
    private val api: EntitlementApi = DramaFlowNetworkModule.createRetrofit(
        baseUrl = DramaFlowNetworkModule.currentEnvironment().entitlementBaseUrl,
        accessTokenProvider = accessTokenProvider,
    ).create(EntitlementApi::class.java),
) : EntitlementRemoteDataSource {
    override suspend fun getMyEntitlements(): EntitlementState {
        val data = api.getMyEntitlements().data ?: return EntitlementState(false, null, emptyList(), "remote_empty")
        return data.toDomain()
    }
}
