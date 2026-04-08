import React from "react";
import { fireEvent, screen, waitFor } from "@testing-library/react";
import { renderWithProviders } from "../pages/test-utils";
import { readOnlySession, fullAccessSession } from "../fixtures/session";
import { PurchaseSearch } from "@/features/purchases/purchase-search";
import { EntitlementSearch } from "@/features/entitlements/entitlement-search";

const adminFetchMock = vi.fn();
const useAdminSessionMock = vi.fn();

vi.mock("@/lib/api/client", () => ({
  adminFetch: (...args: unknown[]) => adminFetchMock(...args)
}));

vi.mock("@/lib/auth/use-admin-session", () => ({
  useAdminSession: () => useAdminSessionMock()
}));

describe("release guardrails", () => {
  beforeEach(() => {
    adminFetchMock.mockReset();
    useAdminSessionMock.mockReturnValue({ data: fullAccessSession });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.spyOn(window, "alert").mockImplementation(() => undefined);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("hides purchase resync for read-only session", async () => {
    useAdminSessionMock.mockReturnValue({ data: readOnlySession });
    adminFetchMock.mockResolvedValueOnce({
      items: [
        {
          purchaseToken: "purchase-1",
          userId: "user-1",
          productId: "premium_access",
          purchaseState: "active"
        }
      ]
    });

    renderWithProviders(<PurchaseSearch />);

    expect(await screen.findByText("purchase-1")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "重新同步" })).toBeNull();
  });

  it("shows failure feedback for dangerous entitlement action", async () => {
    adminFetchMock
      .mockResolvedValueOnce({
        items: [
          {
            userId: "user-1",
            productId: "premium_access",
            state: "active",
            sourcePurchaseToken: "purchase-1",
            lastSyncedAt: "2026-04-08T00:00:00Z"
          }
        ]
      })
      .mockRejectedValueOnce(new Error("撤销失败"));

    renderWithProviders(<EntitlementSearch />);

    expect(await screen.findByText("premium_access")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "撤销" }));

    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledWith("撤销失败");
    });
  });
});
