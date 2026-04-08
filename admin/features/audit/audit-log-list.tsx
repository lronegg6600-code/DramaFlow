"use client";

import React from "react";
import { useQuery } from "@tanstack/react-query";
import { adminFetch } from "@/lib/api/client";
import { DataTable } from "@/components/tables/data-table";

type AuditLogRow = {
  createdAt: string;
  adminUserId: string;
  action: string;
  resourceType: string;
  resourceId: string;
  success: boolean;
};

type AuditLogListPayload = {
  items: AuditLogRow[];
};

export function AuditLogList() {
  const query = useQuery({
    queryKey: ["audit-logs"],
    queryFn: () => adminFetch<AuditLogListPayload>("/audit-logs")
  });

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">审计日志</h2>
      <DataTable
        emptyLabel={query.isLoading ? "正在加载审计日志..." : "未找到审计日志。"}
        rows={query.data?.items ?? []}
        columns={[
          { key: "createdAt", header: "创建时间" },
          { key: "adminUserId", header: "管理员" },
          { key: "action", header: "动作" },
          { key: "resourceType", header: "资源类型" },
          { key: "resourceId", header: "资源 ID" },
          { key: "success", header: "成功", render: (row) => (row.success ? "是" : "否") }
        ]}
      />
    </div>
  );
}
