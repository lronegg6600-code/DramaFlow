import { adminLogin, authHeader, base, createPlaybackSession, ensureRun, guestSession, request } from "./_helpers.mjs";

ensureRun("billing_entitlement_playback_flow");

async function main() {
  const { accessToken, userId } = await guestSession("billing-flow");
  const purchaseToken = `${userId}:release-flow-${Date.now()}`;

  await request(`${base}:8087/v1/billing/google-play/purchases:sync`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeader(accessToken)
    },
    body: JSON.stringify({
      packageName: "com.dramaflow.app",
      productId: "premium_access",
      purchaseToken,
      basePlanId: "monthly",
      offerId: "trial_intro",
      source: "release_rehearsal"
    })
  });

  await request(`${base}:8086/v1/entitlements/recompute/${userId}`, {
    method: "POST"
  });

  const access = await request(`${base}:8086/v1/entitlements/me/playback-access?episodeId=df-neon-vows-e4&dramaId=df-neon-vows`, {
    headers: authHeader(accessToken)
  });

  const mode = access?.data?.accessLevel ?? access?.data?.playbackMode;
  if (!mode || !["full", "preview", "full_access", "preview_only"].includes(mode)) {
    throw new Error(`unexpected playback access mode: ${mode ?? "unknown"}`);
  }

  const descriptor = await createPlaybackSession(accessToken, "df-neon-vows-e4");
  if (!descriptor?.data?.sessionId) {
    throw new Error("playback session create did not return sessionId");
  }

  const { cookie } = await adminLogin();
  await request(`${base}:8088/v1/admin/purchases?q=${encodeURIComponent(purchaseToken)}`, {
    headers: { cookie }
  });

  console.log("[integration] billing -> entitlement -> playback flow passed");
}

main().catch((error) => {
  console.error(`[integration] billing_entitlement_playback_flow failed: ${error.message}`);
  process.exit(1);
});
