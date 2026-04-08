import { can, type AdminSession } from "@/lib/auth/permissions";

function buildSession(overrides?: Partial<AdminSession>): AdminSession {
  return {
    adminUser: {
      id: "admin-1",
      email: "admin@dramaflow.local",
      displayName: "Admin",
      role: "super_admin",
      status: "active"
    },
    permissions: [],
    ...overrides
  };
}

describe("can", () => {
  it("returns false when session is missing", () => {
    expect(can(null, "dashboard:view")).toBe(false);
  });

  it("allows all permissions when wildcard is present", () => {
    expect(can(buildSession({ permissions: ["*"] }), "drama:write")).toBe(true);
  });

  it("checks a concrete permission exactly", () => {
    expect(can(buildSession({ permissions: ["drama:read"] }), "drama:read")).toBe(true);
    expect(can(buildSession({ permissions: ["drama:read"] }), "drama:write")).toBe(false);
  });
});
