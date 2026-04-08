import { adminLogin, authHeader, base, createPlaybackSession, ensureRun, guestSession, request } from "../integration/_helpers.mjs";

ensureRun("smoke_release");

async function health(url) {
  await request(url);
}

async function main() {
  await health(`${base}:8081/health/live`);
  await health(`${base}:8082/health/live`);
  await health(`${base}:8083/health/live`);
  await health(`${base}:8084/health/live`);
  await health(`${base}:8085/health/live`);
  await health(`${base}:8086/health/live`);
  await health(`${base}:8087/health/live`);
  await health(`${base}:8088/health/live`);

  const { accessToken, userId } = await guestSession("release-smoke");
  await request(`${base}:8081/v1/auth/me`, {
    headers: authHeader(accessToken)
  });

  const { cookie } = await adminLogin();
  await request(`${base}:8088/v1/admin/dramas`, {
    headers: { cookie }
  });
  await request(`${base}:8088/v1/admin/audit-logs`, {
    headers: { cookie }
  });
  await request(`${base}:8083/v1/feed/home?region=US&language=en`);
  await request(`${base}:8086/v1/entitlements/me/playback-access?episodeId=df-neon-vows-e1&dramaId=df-neon-vows`, {
    headers: authHeader(accessToken)
  });
  await createPlaybackSession(accessToken, "df-neon-vows-e1");
  await request(`${base}:8088/v1/admin/entitlements/${userId}/recompute`, {
    method: "POST",
    headers: { cookie }
  });

  console.log("[release-smoke] release smoke passed");
}

main().catch((error) => {
  console.error(`[release-smoke] failed: ${error.message}`);
  process.exit(1);
});
