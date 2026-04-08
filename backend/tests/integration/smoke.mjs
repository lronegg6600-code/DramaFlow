const run = process.env.DRAMAFLOW_RUN_INTEGRATION === "1";
const base = process.env.DRAMAFLOW_BASE_URL ?? "http://localhost";

if (!run) {
  console.log("[integration] skipped. set DRAMAFLOW_RUN_INTEGRATION=1 to execute.");
  process.exit(0);
}

async function request(url, options = {}) {
  const response = await fetch(url, options);
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText} ${url}`);
  }

  const text = await response.text();
  return text ? JSON.parse(text) : {};
}

async function main() {
  await request(`${base}:8088/health/live`);
  await request(`${base}:8083/health/live`);
  await request(`${base}:8085/health/live`);

  const loginPayload = await request(`${base}:8088/v1/admin/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      email: "admin@dramaflow.local",
      password: "change-me-now"
    })
  });

  if (!loginPayload.data) {
    throw new Error("admin login failed");
  }

  await request(`${base}:8083/v1/feed/home`);
  console.log("[integration] smoke checks passed");
}

main().catch((error) => {
  console.error(`[integration] failed: ${error.message}`);
  process.exit(1);
});
