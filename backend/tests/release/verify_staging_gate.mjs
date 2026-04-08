import { adminLogin, authHeader, base, createPlaybackSession, guestSession, request } from "../integration/_helpers.mjs";

async function main() {
  const healthPorts = [8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088];
  for (const port of healthPorts) {
    await request(`${base}:${port}/health/live`);
  }

  const { accessToken, userId } = await guestSession("verify-staging-gate");
  await request(`${base}:8081/v1/auth/me`, { headers: authHeader(accessToken) });
  await request(`${base}:8083/v1/feed/home?region=US&language=en`);
  await request(`${base}:8086/v1/entitlements/me/playback-access?episodeId=df-neon-vows-e1&dramaId=df-neon-vows`, {
    headers: authHeader(accessToken)
  });
  await createPlaybackSession(accessToken, "df-neon-vows-e1");

  const { cookie } = await adminLogin();
  await request(`${base}:8088/v1/admin/dramas`, { headers: { cookie } });
  await request(`${base}:8088/v1/admin/audit-logs`, { headers: { cookie } });
  await request(`${base}:8088/v1/admin/entitlements/${userId}/recompute`, {
    method: "POST",
    headers: { cookie }
  });

  console.log("[staging-gate] pass");
}

main().catch((error) => {
  console.error(`[staging-gate] fail: ${error.message}`);
  process.exit(1);
});
