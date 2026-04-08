import { adminLogin, base, ensureRun, request } from "./_helpers.mjs";

ensureRun("admin_audit_flow");

async function main() {
  const { cookie } = await adminLogin();
  const userId = process.env.DRAMAFLOW_AUDIT_TEST_USER_ID ?? "user-release-audit";

  await request(`${base}:8088/v1/admin/entitlements/${userId}/recompute`, {
    method: "POST",
    headers: { cookie }
  });

  const audit = await request(`${base}:8088/v1/admin/audit-logs?query=entitlement.recompute`, {
    headers: { cookie }
  });

  const items = audit?.data?.items ?? audit?.items ?? [];
  const hit = items.find((item) => item.action === "entitlement.recompute");
  if (!hit) {
    throw new Error("audit log does not contain entitlement.recompute");
  }

  console.log("[integration] admin dangerous action -> audit log flow passed");
}

main().catch((error) => {
  console.error(`[integration] admin_audit_flow failed: ${error.message}`);
  process.exit(1);
});
