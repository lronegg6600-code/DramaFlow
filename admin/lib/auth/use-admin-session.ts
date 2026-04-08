"use client";

import { useQuery } from "@tanstack/react-query";
import { authFetch } from "@/lib/api/client";
import type { AdminSession } from "@/lib/auth/permissions";

export function useAdminSession() {
  return useQuery({
    queryKey: ["admin-session"],
    queryFn: () => authFetch<AdminSession>("/me"),
    // 2026-04-08:
    // 会话查询这里故意不自动重试。
    // 原因是后台未登录、cookie 失效、本地 session 被清理，这些都不是“重试一下就会好”的瞬时故障。
    // 如果这里盲目重试，线上最常见的结果是：
    // - 登录页跳转变慢
    // - 网络面板刷一排 401
    // - 用户误以为系统卡住了
    retry: false
  });
}
