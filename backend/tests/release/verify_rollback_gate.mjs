import { adminLogin, authHeader, base, createPlaybackSession, guestSession, request } from "../integration/_helpers.mjs";

async function main() {
  for (const port of [8085, 8086, 8087, 8088]) {
    await request(`${base}:${port}/health/live`);
  }

  const { accessToken } = await guestSession("verify-rollback-gate");
  await request(`${base}:8086/v1/entitlements/me/playback-access?episodeId=df-neon-vows-e1&dramaId=df-neon-vows`, {
    headers: authHeader(accessToken)
  });
  await createPlaybackSession(accessToken, "df-neon-vows-e1");

  const { cookie } = await adminLogin();
  await request(`${base}:8088/v1/admin/dramas`, { headers: { cookie } });

  console.log("[rollback-gate] pass");
}

main().catch((error) => {
  console.error(`[rollback-gate] fail: ${error.message}`);
  process.exit(1);
});
