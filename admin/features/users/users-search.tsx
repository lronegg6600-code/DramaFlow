"use client";

import { useQuery } from "@tanstack/react-query";
import { adminFetch } from "@/lib/api/client";
import { DataTable } from "@/components/tables/data-table";

export function UsersSearch() {
  const query = useQuery({
    queryKey: ["users"],
    queryFn: () => adminFetch<any>("/users")
  });

  return (
    <div className="space-y-4">
      <h2 className="text-xl font-semibold">用户</h2>
      <DataTable
        emptyLabel={query.isLoading ? "正在加载用户..." : "未找到用户。"}
        rows={query.data?.items ?? []}
        columns={[
          { key: "id", header: "用户 ID" },
          { key: "anonymousDeviceId", header: "匿名设备 ID" },
          { key: "status", header: "状态" },
          { key: "createdAt", header: "创建时间" }
        ]}
      />
    </div>
  );
}
