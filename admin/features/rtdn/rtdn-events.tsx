"use client";

import React from "react";
import { useQuery } from "@tanstack/react-query";
import { adminFetch } from "@/lib/api/client";
import { DataTable } from "@/components/tables/data-table";
import { ConfirmDialog } from "@/components/dialogs/confirm-dialog";
import { useAdminSession } from "@/lib/auth/use-admin-session";
import { can } from "@/lib/auth/permissions";

type RTDNEventRow = {
  messageId: string;
  purchaseToken: string;
  eventType: string;
  processedState: string;
};

type RTDNEventListPayload = {
  items: RTDNEventRow[];
};

export function RTDNEvents() {
  const session = useAdminSession();
  const canReplay = can(session.data ?? null, "rtdn:replay");
  const query = useQuery({
    queryKey: ["rtdn-events"],
    queryFn: () => adminFetch<RTDNEventListPayload>("/rtdn-events")
  });

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">RTDN 事件</h2>
      <DataTable
        emptyLabel={query.isLoading ? "正在加载 RTDN 事件..." : "未找到 RTDN 事件。"}
        rows={query.data?.items ?? []}
        getRowKey={(row) => row.messageId}
        columns={[
          { key: "messageId", header: "消息 ID" },
          { key: "purchaseToken", header: "购买 Token" },
          { key: "eventType", header: "事件类型" },
          {
            key: "processedState",
            header: "处理状态",
            render: (row) => <span className="badge">{row.processedState}</span>
          },
          {
            key: "actions",
            header: "操作",
            render: (row) =>
              canReplay ? (
                <ConfirmDialog
                  label="重放"
                  description="通过 billing-service 重新处理这条 RTDN 事件。"
                  invalidateKeys={[["rtdn-events"]]}
                  successMessage={`RTDN 事件 ${row.messageId} 已触发重放。`}
                  onConfirm={async () => {
                    await adminFetch(`/rtdn-events/${row.messageId}/replay`, { method: "POST" });
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
