"use client";

import React from "react";
import { useQuery } from "@tanstack/react-query";
import { adminFetch } from "@/lib/api/client";
import { DataTable } from "@/components/tables/data-table";
import { ConfirmDialog } from "@/components/dialogs/confirm-dialog";
import { InlineNotice } from "@/components/feedback/inline-notice";
import { useActionMutation } from "@/lib/query/use-action-mutation";
import { useAdminSession } from "@/lib/auth/use-admin-session";
import { can } from "@/lib/auth/permissions";

type EntitlementRow = {
  userId: string;
  productId: string;
  state: string;
  sourcePurchaseToken: string;
  lastSyncedAt: string;
};

type EntitlementListPayload = {
  items: EntitlementRow[];
};

export function EntitlementSearch() {
  const session = useAdminSession();
  const canGrantTemp = can(session.data ?? null, "entitlement:grant_temp");
  const canRecompute = can(session.data ?? null, "entitlement:recompute");
  const canRevoke = can(session.data ?? null, "entitlement:revoke");
  const query = useQuery({
    queryKey: ["entitlements"],
    queryFn: () => adminFetch<EntitlementListPayload>("/entitlements")
  });

  const grantTempAction = useActionMutation({
    action: async (userId: string) =>
      adminFetch(`/entitlements/${userId}/grant-temp`, {
        method: "POST",
        body: JSON.stringify({
          productId: "premium_access",
          reason: "admin_console_temp_grant"
        })
      }),
    successMessage: ({ variables }) => `已给用户 ${variables} 发起临时授权。`,
    errorMessage: ({ error }) =>
      error instanceof Error ? error.message : "临时授权失败，请稍后重试。",
    invalidateKeys: [["entitlements"]]
  });

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold">权益</h2>
        {canGrantTemp ? (
          <button
            className="button-secondary"
            disabled={grantTempAction.isSubmitting}
            onClick={async () => {
              const userId = window.prompt("要给哪个 userId 临时授权？");
              if (!userId) {
                return;
              }
              await grantTempAction.executeAction(userId);
            }}
            type="button"
          >
            {grantTempAction.isSubmitting ? "处理中..." : "临时授权"}
          </button>
        ) : null}
      </div>
      {grantTempAction.notice ? (
        <InlineNotice
          tone={grantTempAction.notice.tone}
          message={grantTempAction.notice.message}
        />
      ) : null}
      <DataTable
        emptyLabel={query.isLoading ? "正在加载权益..." : "未找到权益记录。"}
        rows={query.data?.items ?? []}
        getRowKey={(row) => `${row.userId}:${row.sourcePurchaseToken}:${row.productId}`}
        columns={[
          { key: "userId", header: "用户 ID" },
          { key: "productId", header: "产品" },
          { key: "state", header: "状态", render: (row) => <span className="badge">{row.state}</span> },
          { key: "sourcePurchaseToken", header: "来源购买 Token" },
          { key: "lastSyncedAt", header: "最近同步" },
          {
            key: "actions",
            header: "操作",
            render: (row) => (
              <div className="flex gap-2">
                {canRecompute ? (
                  <ConfirmDialog
                    label="重算"
                    description="根据购买记录重新计算该用户的权益。"
                    invalidateKeys={[["entitlements"]]}
                    successMessage={`用户 ${row.userId} 的权益已触发重算。`}
                    onConfirm={async () => {
                      await adminFetch(`/entitlements/${row.userId}/recompute`, { method: "POST" });
                    }}
                  />
                ) : null}
                {canRevoke ? (
                  <ConfirmDialog
                    label="撤销"
                    description="按来源购买 Token 撤销该权益，请谨慎操作。"
                    invalidateKeys={[["entitlements"]]}
                    successMessage={`用户 ${row.userId} 的权益已触发撤销。`}
                    onConfirm={async () => {
                      await adminFetch(`/entitlements/${row.userId}/revoke`, {
                        method: "POST",
                        body: JSON.stringify({
                          sourcePurchaseToken: row.sourcePurchaseToken,
                          reason: "admin_console_revoke"
                        })
                      });
                    }}
                  />
                ) : null}
                {!canRecompute && !canRevoke ? <span className="text-xs text-muted">无权限</span> : null}
              </div>
            )
          }
        ]}
      />
    </div>
  );
}
