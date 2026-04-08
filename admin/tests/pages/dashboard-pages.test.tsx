import React from "react";
import { fireEvent, screen, waitFor } from "@testing-library/react";
import { renderWithProviders } from "./test-utils";
import { fullAccessSession, readOnlySession } from "../fixtures/session";
import { DramaList } from "@/features/dramas/drama-list";
import { DramaDetail } from "@/features/dramas/drama-detail";
import { FeedConfigEditor } from "@/features/feed-config/feed-config-editor";
import { PurchaseSearch } from "@/features/purchases/purchase-search";
import { EntitlementSearch } from "@/features/entitlements/entitlement-search";
import { RTDNEvents } from "@/features/rtdn/rtdn-events";
import { PlaybackSearch } from "@/features/playback/playback-search";
import { AuditLogList } from "@/features/audit/audit-log-list";

vi.mock("@/features/dramas/drama-form", () => ({
  DramaForm: ({ dramaId }: { dramaId: string }) => <div>DramaForm:{dramaId}</div>
}));

vi.mock("@/features/dramas/episode-editor", () => ({
  EpisodeEditor: ({ dramaId }: { dramaId: string }) => <div>EpisodeEditor:{dramaId}</div>
}));

const adminFetchMock = vi.fn();
const useAdminSessionMock = vi.fn();

vi.mock("@/lib/api/client", () => ({
  adminFetch: (...args: unknown[]) => adminFetchMock(...args)
}));

vi.mock("@/lib/auth/use-admin-session", () => ({
  useAdminSession: () => useAdminSessionMock()
}));

describe("dashboard pages", () => {
  beforeEach(() => {
    adminFetchMock.mockReset();
    useAdminSessionMock.mockReturnValue({ data: fullAccessSession });
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.spyOn(window, "alert").mockImplementation(() => undefined);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders dramas list page", async () => {
    adminFetchMock.mockResolvedValueOnce({
      items: [
        {
          id: "drama-1",
          title: "长安谜局",
          region: "CN",
          language: "zh-CN",
          publishStatus: "published",
          isFeatured: true
        }
      ]
    });

    renderWithProviders(<DramaList />);

    expect(screen.getByText("剧目库")).toBeTruthy();
    expect(await screen.findByText("长安谜局")).toBeTruthy();
    expect(screen.getByRole("link", { name: "新建剧目" })).toBeTruthy();
  });

  it("renders drama detail page and episode table", async () => {
    adminFetchMock
      .mockResolvedValueOnce({
        id: "drama-1",
        title: "长安谜局"
      })
      .mockResolvedValueOnce({
        items: [
          {
            episodeNo: 1,
            title: "第一集",
            previewSeconds: 30,
            isPremium: true,
            publishStatus: "published"
          }
        ]
      });

    renderWithProviders(<DramaDetail dramaId="drama-1" />);

    expect(await screen.findByText("DramaForm:drama-1")).toBeTruthy();
    expect(screen.getByText("EpisodeEditor:drama-1")).toBeTruthy();
    expect(screen.getByText("第一集")).toBeTruthy();
  });

  it("renders feed config page and publishes config", async () => {
    adminFetchMock
      .mockResolvedValueOnce({
        region: "US",
        language: "zh-CN",
        draftPayload: { featured: [{ dramaId: "drama-1" }] }
      })
      .mockResolvedValueOnce({ accepted: true })
      .mockResolvedValueOnce({
        region: "US",
        language: "zh-CN",
        draftPayload: { featured: [{ dramaId: "drama-1" }] }
      });

    renderWithProviders(<FeedConfigEditor />);

    expect(await screen.findByText("首页推荐配置")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "发布" }));

    await waitFor(() => {
      expect(adminFetchMock).toHaveBeenCalledWith(
        "/feed-config/home/publish?region=US&language=zh-CN",
        { method: "POST" }
      );
    });
  });

  it("renders purchases page and hides dangerous action without permission", async () => {
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

    expect(await screen.findByText("购买记录")).toBeTruthy();
    expect(await screen.findByText("purchase-1")).toBeTruthy();
    expect(screen.queryByRole("button", { name: "重新同步" })).toBeNull();
  });

  it("renders entitlements page and supports recompute/revoke actions", async () => {
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
      .mockResolvedValueOnce({ accepted: true })
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
      });

    renderWithProviders(<EntitlementSearch />);

    expect(await screen.findByText("权益")).toBeTruthy();
    expect(await screen.findByText("premium_access")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "重算" }));

    await waitFor(() => {
      expect(adminFetchMock).toHaveBeenCalledWith("/entitlements/user-1/recompute", {
        method: "POST"
      });
    });
  });

  it("renders rtdn, playback and audit pages", async () => {
    adminFetchMock
      .mockResolvedValueOnce({
        items: [
          {
            messageId: "msg-1",
            purchaseToken: "purchase-1",
            eventType: "SUBSCRIPTION_RECOVERED",
            processedState: "processed"
          }
        ]
      })
      .mockResolvedValueOnce({
        items: [
          {
            sessionId: "session-1",
            userId: "user-1",
            episodeId: "episode-1",
            playbackMode: "full",
            expiresAt: "2026-04-08T00:00:00Z",
            sessionStatus: "active"
          }
        ]
      })
      .mockResolvedValueOnce({
        items: [
          {
            createdAt: "2026-04-08T00:00:00Z",
            adminUserId: "admin-1",
            action: "purchase.resync",
            resourceType: "purchase",
            resourceId: "purchase-1",
            success: true
          }
        ]
      });

    const rtdnView = renderWithProviders(<RTDNEvents />);
    expect(await screen.findByText("RTDN 事件")).toBeTruthy();
    rtdnView.unmount();

    const playbackView = renderWithProviders(<PlaybackSearch />);
    expect(await screen.findByText("播放诊断")).toBeTruthy();
    playbackView.unmount();

    renderWithProviders(<AuditLogList />);
    expect(await screen.findByText("审计日志")).toBeTruthy();
  });
});
