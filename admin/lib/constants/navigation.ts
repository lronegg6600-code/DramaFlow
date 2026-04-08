import { LayoutDashboard, Film, Home, Users, Receipt, ShieldCheck, Activity, BellRing, ScrollText, Settings } from "lucide-react";

// 2026-04-08:
// 导航文案和权限放在同一个常量里统一维护，不要散落在 Sidebar、Topbar、页面文件里各写一份。
// 这样做的直接收益是：
// 1. 权限变更时只改一处，避免菜单能看见但点进去 403。
// 2. 中英文切换、文案修订时不容易漏改。
// 3. 后续如果接 RBAC 配置中心，这里就是最自然的收口点。
export const NAV_ITEMS = [
  { href: "/", label: "仪表盘", icon: LayoutDashboard, permission: "dashboard:view" },
  { href: "/dramas", label: "剧目库", icon: Film, permission: "drama:read" },
  { href: "/feed-config", label: "首页推荐配置", icon: Home, permission: "feed_config:read" },
  { href: "/users", label: "用户", icon: Users, permission: "purchase:read" },
  { href: "/purchases", label: "购买记录", icon: Receipt, permission: "purchase:read" },
  { href: "/entitlements", label: "权益", icon: ShieldCheck, permission: "entitlement:read" },
  { href: "/playback", label: "播放诊断", icon: Activity, permission: "playback:read" },
  { href: "/rtdn", label: "RTDN 事件", icon: BellRing, permission: "rtdn:read" },
  { href: "/audit-logs", label: "审计日志", icon: ScrollText, permission: "audit:read" },
  { href: "/settings", label: "设置", icon: Settings, permission: "settings:view" }
] as const;
