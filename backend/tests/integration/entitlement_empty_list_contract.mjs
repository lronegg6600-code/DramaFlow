const guestSessionResponse = await fetch("http://127.0.0.1:8081/v1/auth/guest-session", {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({ anonymousDeviceId: "phase28-entitlement-contract-probe" }),
});

const guestSessionBody = await guestSessionResponse.json();
const accessToken = guestSessionBody?.data?.accessToken ?? null;

let entitlementResponse = null;
let entitlementBody = null;
if (accessToken) {
  entitlementResponse = await fetch("http://127.0.0.1:8086/v1/entitlements/me", {
    headers: { authorization: `Bearer ${accessToken}` },
  });
  entitlementBody = await entitlementResponse.json();
}

const entitlements = entitlementBody?.data?.entitlements;
const summary = {
  probe: "entitlement_empty_list_contract",
  guestSessionStatus: guestSessionResponse.status,
  entitlementStatus: entitlementResponse?.status ?? null,
  userId: entitlementBody?.data?.userId ?? guestSessionBody?.data?.user?.id ?? null,
  entitlementsShape: Array.isArray(entitlements) ? "array" : entitlements === null ? "null" : typeof entitlements,
  passed: Array.isArray(entitlements),
  response: entitlementBody,
};

console.log(JSON.stringify(summary, null, 2));
process.exit(summary.passed ? 0 : 1);
