import { authHeader, base, createPlaybackSession, ensureRun, guestSession, request } from "./_helpers.mjs";

ensureRun("entitlement_revoke_access_downgrade");

async function main() {
  const { accessToken, userId } = await guestSession("revoke-flow");
  const purchaseToken = `${userId}:revoke-flow-${Date.now()}`;

  await request(`${base}:8086/v1/entitlements/grants`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      userId,
      entitlementType: "subscription",
      productId: "premium_access",
      scopeType: "global",
      state: "active",
      startsAt: new Date().toISOString(),
      endsAt: new Date(Date.now() + 30 * 60 * 1000).toISOString(),
      sourcePurchaseToken: purchaseToken,
      reason: "release_rehearsal_grant",
      payloadSnapshot: { scenario: "entitlement_revoke_access_downgrade" }
    })
  });

  await createPlaybackSession(accessToken, "df-neon-vows-e4");

  await request(`${base}:8086/v1/entitlements/revoke`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      userId,
      sourcePurchaseToken: purchaseToken,
      state: "expired",
      reason: "release_rehearsal_revoke",
      payloadSnapshot: { scenario: "entitlement_revoke_access_downgrade" }
    })
  });

  const access = await request(`${base}:8086/v1/entitlements/me/playback-access?episodeId=df-neon-vows-e4&dramaId=df-neon-vows`, {
    headers: authHeader(accessToken)
  });

  const mode = access?.data?.accessLevel ?? access?.data?.playbackMode;
  if (!mode || !["preview", "none", "preview_only", "no_access"].includes(mode)) {
    throw new Error(`expected downgraded access, got ${mode ?? "unknown"}`);
  }

  console.log("[integration] entitlement revoke -> playback downgrade flow passed");
}

main().catch((error) => {
  console.error(`[integration] entitlement_revoke_access_downgrade failed: ${error.message}`);
  process.exit(1);
});
