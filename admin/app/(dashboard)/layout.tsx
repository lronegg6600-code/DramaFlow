"use client";

import React from "react";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Sidebar } from "@/components/layout/sidebar";
import { Topbar } from "@/components/layout/topbar";
import { useAdminSession } from "@/lib/auth/use-admin-session";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const sessionQuery = useAdminSession();

  useEffect(() => {
    if (sessionQuery.isError) {
      // 2026-04-08:
      // 这里一旦拿不到会话，就直接回登录页，不在布局层硬撑。
      // 管理后台最怕的是“半登录态”：壳子出来了，但接口全是 401，
      // 运维和运营会以为页面坏了，实际只是会话失效。
      router.replace("/login");
    }
  }, [router, sessionQuery.isError]);

  if (sessionQuery.isLoading) {
    return <div className="p-10">正在加载管理会话...</div>;
  }

  return (
    <div className="grid min-h-screen grid-cols-[288px,1fr] gap-4 p-4">
      <Sidebar session={sessionQuery.data ?? null} />
      <div className="space-y-4">
        <Topbar session={sessionQuery.data ?? null} />
        <main className="space-y-6">{children}</main>
      </div>
    </div>
  );
}
