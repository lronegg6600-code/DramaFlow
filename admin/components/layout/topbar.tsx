"use client";

import { usePathname, useRouter } from "next/navigation";
import { authFetch } from "@/lib/api/client";
import { useActionMutation } from "@/lib/query/use-action-mutation";
import type { AdminSession } from "@/lib/auth/permissions";

const TITLE_MAP: Record<string, string> = {
  "/": "仪表盘",
  "/dramas": "剧目库",
  "/feed-config": "首页推荐配置",
  "/users": "用户",
  "/purchases": "购买记录",
  "/entitlements": "权益",
  "/playback": "播放诊断",
  "/rtdn": "RTDN 事件",
  "/audit-logs": "审计日志",
  "/settings": "设置"
};

export function Topbar({ session }: { session: AdminSession | null }) {
  const pathname = usePathname();
  const router = useRouter();

  const logoutAction = useActionMutation({
    action: async () => authFetch("/logout", { method: "POST" }),
    errorMessage: ({ error }) => (error instanceof Error ? error.message : "退出登录失败，请稍后重试。"),
    onSuccess: async () => {
      router.push("/login");
      router.refresh();
    }
  });

  async function logout() {
    await logoutAction.executeAction(undefined);
  }

  return (
    <header className="panel flex items-center justify-between px-5 py-4">
      <div>
        <p className="text-xs uppercase tracking-[0.2em] text-muted">工作台</p>
        <h2 className="text-lg font-semibold">{TITLE_MAP[pathname] ?? pathname.replaceAll("/", " / ")}</h2>
      </div>
      <div className="flex items-center gap-4">
        <div className="text-right">
          <p className="text-sm font-medium">{session?.adminUser.displayName ?? "未知用户"}</p>
          <p className="text-xs text-muted">{session?.adminUser.role ?? "访客"}</p>
        </div>
        <button className="button-secondary" disabled={logoutAction.isSubmitting} onClick={logout} type="button">
          {logoutAction.isSubmitting ? "退出中..." : "退出登录"}
        </button>
      </div>
    </header>
  );
}
