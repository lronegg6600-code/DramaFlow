import type { AdminSession } from "@/lib/auth/permissions";

export const fullAccessSession: AdminSession = {
  adminUser: {
    id: "admin-1",
    email: "admin@dramaflow.local",
    displayName: "Admin",
    role: "super_admin",
    status: "active"
  },
  permissions: ["*"]
};

export const readOnlySession: AdminSession = {
  adminUser: {
    id: "support-1",
    email: "support@dramaflow.local",
    displayName: "Support",
    role: "support_agent",
    status: "active"
  },
  permissions: ["purchase:read", "entitlement:read", "rtdn:read", "playback:read", "audit:read"]
};
