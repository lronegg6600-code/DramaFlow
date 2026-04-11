import { writeFileSync } from "node:fs";
import path from "node:path";

const root = "Z:\\Projects\\DramaFlow";
const output = path.join(root, "release-evidence", "entitlement-runtime-contract-probe.json");

const guest = await fetch("http://127.0.0.1:8081/v1/auth/guest-session", {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({ anonymousDeviceId: "phase29-contract-probe" }),
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
  userId: entitlementBody?.data?.userId ?? guestBody?.data?.user?.id ?? null,
  entitlementsShape: Array.isArray(entitlements) ? "array" : entitlements === null ? "null" : typeof entitlements,
  entitlementsValue: entitlements,
  passed: Array.isArray(entitlements),
  interpretation: Array.isArray(entitlements)
    ? "live runtime has converged to array semantics"
    : "live runtime still returns entitlements=null and has not converged",
};

writeFileSync(output, JSON.stringify(payload, null, 2));
console.log(JSON.stringify(payload, null, 2));
process.exit(payload.passed ? 0 : 1);
