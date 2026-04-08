import { performance } from "node:perf_hooks";
import { authHeader, base, createPlaybackSession, guestSession, request } from "../integration/_helpers.mjs";

const durationSeconds = Number(process.env.DRAMAFLOW_SOAK_DURATION_SECONDS ?? "120");
const stopAt = Date.now() + durationSeconds * 1000;
const stats = {
  feedHome: [],
  playbackCreate: [],
  adminDramas: [],
  failures: []
};

function percentile(values, ratio) {
  if (values.length === 0) {
    return 0;
  }
  const sorted = [...values].sort((a, b) => a - b);
  const index = Math.min(sorted.length - 1, Math.floor(sorted.length * ratio));
  return Number(sorted[index].toFixed(2));
}

async function timed(label, fn) {
  const start = performance.now();
  try {
    await fn();
    stats[label].push(performance.now() - start);
  } catch (error) {
    stats.failures.push(`${label}: ${error.message}`);
  }
}

async function main() {
  const { accessToken } = await guestSession("soak");
  const adminLogin = await fetch(`${base}:8088/v1/admin/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      email: process.env.DRAMAFLOW_ADMIN_EMAIL ?? "admin@dramaflow.local",
      password: process.env.DRAMAFLOW_ADMIN_PASSWORD ?? "change-me-now"
    })
  });
  const cookie = adminLogin.headers.get("set-cookie") ?? "";

  while (Date.now() < stopAt) {
    await timed("feedHome", () => request(`${base}:8083/v1/feed/home?region=US&language=en`));
    await timed("playbackCreate", () => createPlaybackSession(accessToken, "df-neon-vows-e1"));
    await timed("adminDramas", () => request(`${base}:8088/v1/admin/dramas`, { headers: { cookie } }));
  }

  const summary = {
    durationSeconds,
    feedHomeCount: stats.feedHome.length,
    playbackCreateCount: stats.playbackCreate.length,
    adminDramasCount: stats.adminDramas.length,
    feedHomeP95Ms: percentile(stats.feedHome, 0.95),
    playbackCreateP95Ms: percentile(stats.playbackCreate, 0.95),
    adminDramasP95Ms: percentile(stats.adminDramas, 0.95),
    failureCount: stats.failures.length,
    failures: stats.failures.slice(0, 10)
  };

  console.log(JSON.stringify(summary, null, 2));

  if (summary.failureCount > 0) {
    process.exit(1);
  }
}

main().catch((error) => {
  console.error(`[soak-substitute] fail: ${error.message}`);
  process.exit(1);
});
