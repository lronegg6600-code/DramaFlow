import { writeFileSync } from "node:fs";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "entitlement-live-contract-reprobe.json");

async function main() {
  try {
    const guest = await fetch("http://127.0.0.1:8081/v1/auth/guest-session", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ anonymousDeviceId: "phase30-live-contract-reprobe" }),
    });
    const guestBody = await guest.json();
    const token = guestBody?.data?.accessToken ?? "";

    let entitlementStatus = null;
    let entitlementBody = null;
    if (token) {
      const entitlement = await fetch("http://127.0.0.1:8086/v1/entitlements/me", {
        headers: { authorization: `Bearer ${token}` },
      });
      entitlementStatus = entitlement.status;
      entitlementBody = await entitlement.json();
    }

    const entitlements = entitlementBody?.data?.entitlements;
    const payload = {
      generatedAt: new Date().toISOString(),
      guestSessionStatus: guest.status,
      entitlementStatus,
      entitlementsShape: Array.isArray(entitlements) ? "array" : entitlements === null ? "null" : typeof entitlements,
      passed: Array.isArray(entitlements),
      response: entitlementBody,
      interpretation: Array.isArray(entitlements)
        ? "runtime reprobe passed"
        : "runtime reprobe still failed",
    };
    writeFileSync(output, JSON.stringify(payload, null, 2));
    console.log(JSON.stringify(payload, null, 2));
    process.exit(payload.passed ? 0 : 1);
  } catch (error) {
    const payload = {
      generatedAt: new Date().toISOString(),
      status: "failed",
      error: String(error?.message ?? error),
      interpretation: "Live reprobe could not connect to the local backend stack after Docker Desktop restart attempt.",
    };
    writeFileSync(output, JSON.stringify(payload, null, 2));
    console.log(JSON.stringify(payload, null, 2));
    process.exit(1);
  }
}

await main();
