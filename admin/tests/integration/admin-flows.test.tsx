import React from "react";
import { fireEvent, screen, waitFor } from "@testing-library/react";
import { renderWithProviders } from "../pages/test-utils";
import { fullAccessSession } from "../fixtures/session";
import LoginPage from "@/app/(auth)/login/page";
import { FeedConfigEditor } from "@/features/feed-config/feed-config-editor";

const authFetchMock = vi.fn();
const adminFetchMock = vi.fn();
const pushMock = vi.fn();
const refreshMock = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({
    push: pushMock,
    refresh: refreshMock
  })
}));

vi.mock("@/lib/api/client", () => ({
  authFetch: (...args: unknown[]) => authFetchMock(...args),
  adminFetch: (...args: unknown[]) => adminFetchMock(...args)
}));

vi.mock("@/lib/auth/use-admin-session", () => ({
  useAdminSession: () => ({ data: fullAccessSession })
}));

describe("admin flows", () => {
  beforeEach(() => {
    authFetchMock.mockReset();
    adminFetchMock.mockReset();
    pushMock.mockReset();
    refreshMock.mockReset();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.spyOn(window, "alert").mockImplementation(() => undefined);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("handles login success", async () => {
    authFetchMock.mockResolvedValueOnce({
      adminUser: fullAccessSession.adminUser,
      permissions: fullAccessSession.permissions,
      expiresAt: "2026-04-08T00:00:00Z"
    });

    renderWithProviders(<LoginPage />);
    fireEvent.click(screen.getByRole("button", { name: "登录" }));

    await waitFor(() => {
      expect(authFetchMock).toHaveBeenCalled();
      expect(pushMock).toHaveBeenCalledWith("/");
      expect(refreshMock).toHaveBeenCalled();
    });
  });

  it("shows login failure feedback", async () => {
    authFetchMock.mockRejectedValueOnce(new Error("账号或密码错误"));

    renderWithProviders(<LoginPage />);
    fireEvent.click(screen.getByRole("button", { name: "登录" }));

    expect(await screen.findByText("账号或密码错误")).toBeTruthy();
  });

  it("blocks invalid login form submission", async () => {
    renderWithProviders(<LoginPage />);

    fireEvent.change(screen.getByPlaceholderText("邮箱"), {
      target: { value: "wrong" }
    });
    fireEvent.change(screen.getByPlaceholderText("密码"), {
      target: { value: "123" }
    });
    fireEvent.click(screen.getByRole("button", { name: "登录" }));

    await waitFor(() => {
      expect(authFetchMock).not.toHaveBeenCalled();
    });
  });

  it("shows mutation failure on feed publish", async () => {
    adminFetchMock
      .mockResolvedValueOnce({
        region: "US",
        language: "zh-CN",
        draftPayload: { featured: [] }
      })
      .mockRejectedValueOnce(new Error("发布失败"));

    renderWithProviders(<FeedConfigEditor />);
    expect(await screen.findByText("首页推荐配置")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "发布" }));

    await waitFor(() => {
      expect(window.alert).toHaveBeenCalledWith("发布失败");
    });
  });
});
