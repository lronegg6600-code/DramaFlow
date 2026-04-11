package com.dramaflow.core.network.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementDtoDecodeTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `entitlements null decodes without crashing`() {
        val payload = json.decodeFromString<EntitlementSummaryEnvelopeDto>(
            """
            {
              "data": {
                "userId": "user-1",
                "entitlements": null,
                "isPremium": false,
                "activeProductId": null,
                "source": "entitlement-service"
              }
            }
            """.trimIndent(),
        )

        val data = requireNotNull(payload.data)
        assertNull(data.entitlements)
        assertTrue(data.entitlementsOrEmpty().isEmpty())
        assertFalse(data.toDomain().isPremium)
    }

    @Test
    fun `empty entitlements array remains empty`() {
        val payload = json.decodeFromString<EntitlementSummaryEnvelopeDto>(
            """
            {
              "data": {
                "userId": "user-1",
                "entitlements": [],
                "isPremium": false,
                "activeProductId": null,
                "source": "entitlement-service"
              }
            }
            """.trimIndent(),
        )

        val data = requireNotNull(payload.data)
        assertEquals(0, data.entitlementsOrEmpty().size)
        assertFalse(data.toDomain().isPremium)
    }

    @Test
    fun `non empty entitlements keep premium access`() {
        val payload = json.decodeFromString<EntitlementSummaryEnvelopeDto>(
            """
            {
              "data": {
                "userId": "user-1",
                "entitlements": [
                  {
                    "entitlementId": "ent-1",
                    "userId": "user-1",
                    "entitlementType": "subscription",
                    "productId": "premium_access",
                    "state": "active"
                  }
                ],
                "isPremium": false,
                "activeProductId": null,
                "source": "entitlement-service"
              }
            }
            """.trimIndent(),
        )

        val data = requireNotNull(payload.data)
        assertEquals(1, data.entitlementsOrEmpty().size)
        assertTrue(data.toDomain().isPremium)
        assertEquals("premium_access", data.toDomain().activeProductId)
    }
}
