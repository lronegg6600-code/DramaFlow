package com.dramaflow.core.network.api

import com.dramaflow.core.network.dto.EntitlementSummaryEnvelopeDto
import retrofit2.http.GET

interface EntitlementApi {
    @GET("v1/entitlements/me")
    suspend fun getMyEntitlements(): EntitlementSummaryEnvelopeDto
}
