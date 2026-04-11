const guestSessionResponse = await fetch("http://127.0.0.1:8081/v1/auth/guest-session", {
  method: "POST",
  headers: { "content-type": "application/json" },
  body: JSON.stringify({ anonymousDeviceId: "phase28-billing-restore-probe" }),
});

const guestSessionBody = await guestSessionResponse.json();
const accessToken = guestSessionBody?.data?.accessToken ?? null;

let billingStatus = null;
let billingBody = null;
let entitlementStatus = null;
let entitlementBody = null;

if (accessToken) {
  billingStatus = await fetch("http://127.0.0.1:8087/v1/billing/me/subscription-status", {
    headers: { authorization: `Bearer ${accessToken}` },
  });
  billingBody = await billingStatus.json();

  entitlementStatus = await fetch("http://127.0.0.1:8086/v1/entitlements/me", {
    headers: { authorization: `Bearer ${accessToken}` },
  });
  entitlementBody = await entitlementStatus.json();
}

const summary = {
  probe: "billing_restore_no_crash_flow",
  guestSessionStatus: guestSessionResponse.status,
  billingStatus: billingStatus?.status ?? null,
  entitlementStatus: entitlementStatus?.status ?? null,
  restoreSimulation: "no_real_purchase_token_available_in_backend_probe",
  entitlementShape: Array.isArray(entitlementBody?.data?.entitlements)
    ? "array"
    : entitlementBody?.data?.entitlements === null
      ? "null"
      : typeof entitlementBody?.data?.entitlements,
  passed:
    guestSessionResponse.ok &&
    (billingStatus?.ok ?? false) &&
    (entitlementStatus?.ok ?? false),
  billingResponse: billingBody,
  entitlementResponse: entitlementBody,
};

console.log(JSON.stringify(summary, null, 2));
process.exit(summary.passed ? 0 : 1);
