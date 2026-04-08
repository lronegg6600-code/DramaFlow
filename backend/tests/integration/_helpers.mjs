const run = process.env.DRAMAFLOW_RUN_INTEGRATION === "1";

export const base = process.env.DRAMAFLOW_BASE_URL ?? "http://localhost";
export const adminEmail = process.env.DRAMAFLOW_ADMIN_EMAIL ?? "admin@dramaflow.local";
export const adminPassword = process.env.DRAMAFLOW_ADMIN_PASSWORD ?? "change-me-now";

export function ensureRun(label) {
  if (!run) {
    console.log(`[integration] skipped ${label}. set DRAMAFLOW_RUN_INTEGRATION=1 to execute.`);
    process.exit(0);
  }
}

export async function request(url, options = {}) {
  const response = await fetch(url, options);
  const text = await response.text();
  const payload = text ? JSON.parse(text) : {};
  if (!response.ok) {
    const message = payload?.error?.message ?? response.statusText;
    throw new Error(`${response.status} ${message} ${url}`);
  }
  return payload;
}

export async function adminLogin() {
  const response = await fetch(`${base}:8088/v1/admin/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      email: adminEmail,
      password: adminPassword
    })
  });

  const cookie = response.headers.get("set-cookie") ?? "";
  const text = await response.text();
  const payload = text ? JSON.parse(text) : {};
  if (!response.ok || !payload?.data) {
    throw new Error("admin login failed");
  }
  return { payload, cookie };
}

export async function guestSession(deviceSuffix = Date.now().toString()) {
  const payload = await request(`${base}:8081/v1/auth/guest-session`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      anonymousDeviceId: `release-device-${deviceSuffix}`
    })
  });

  const accessToken = payload?.data?.accessToken;
  const userId = payload?.data?.user?.id;
  if (!accessToken || !userId) {
    throw new Error("guest session response missing token/user");
  }
  return { accessToken, userId, payload };
}

export function authHeader(accessToken) {
  return { Authorization: `Bearer ${accessToken}` };
}

export async function createPlaybackSession(accessToken, episodeId) {
  return request(`${base}:8085/v1/playback/sessions`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...authHeader(accessToken)
    },
    body: JSON.stringify({
      episodeId,
      sourcePage: "release_smoke",
      preferredQuality: "auto"
    })
  });
}
