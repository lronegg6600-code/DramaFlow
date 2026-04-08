"use client";

import React from "react";
import { useQuery } from "@tanstack/react-query";
import { adminFetch } from "@/lib/api/client";
import { DataTable } from "@/components/tables/data-table";
import { ConfirmDialog } from "@/components/dialogs/confirm-dialog";
import { useAdminSession } from "@/lib/auth/use-admin-session";
import { can } from "@/lib/auth/permissions";

type PurchaseRow = {
  purchaseToken: string;
  userId: string;
  productId: string;
  purchaseState: string;
};

type PurchaseListPayload = {
  items: PurchaseRow[];
};

export function PurchaseSearch() {
  const session = useAdminSession();
  const canResync = can(session.data ?? null, "purchase:resync");
  const query = useQuery({
    queryKey: ["purchases"],
    queryFn: () => adminFetch<PurchaseListPayload>("/purchases")
  });

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">购买记录</h2>
      <DataTable
        emptyLabel={query.isLoading ? "正在加载购买记录..." : "未找到购买记录。"}
        rows={query.data?.items ?? []}
        getRowKey={(row) => row.purchaseToken}
        columns={[
          { key: "purchaseToken", header: "购买 Token" },
          { key: "userId", header: "用户 ID" },
          { key: "productId", header: "产品" },
          {
            key: "purchaseState",
            header: "状态",
            render: (row) => <span className="badge">{row.purchaseState}</span>
          },
          {
            key: "actions",
            header: "操作",
            render: (row) =>
              canResync ? (
                <ConfirmDialog
                  label="重新同步"
                  description="根据 billing-service 和 Google Play 校验状态，重新同步这条购买记录。"
                  invalidateKeys={[["purchases"]]}
                  successMessage="购买记录已触发重新同步。"
                  onConfirm={async () => {
                    // 2026-04-08:
                    // 购买重同步属于“高价值但可重复触发”的运维动作，必须挂确认框。
                    // 目的不是防呆，而是防止后台误触造成连续打上游账单链路。
                    await adminFetch(`/purchases/${row.purchaseToken}/resync`, { method: "POST" });
                  }}
                />
              ) : (
                <span className="text-xs text-muted">无权限</span>
              )
          }
        ]}
      />
    </div>
  );
}
