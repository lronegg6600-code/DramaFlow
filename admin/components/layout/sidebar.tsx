"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { NAV_ITEMS } from "@/lib/constants/navigation";
import { can, type AdminSession } from "@/lib/auth/permissions";
import { cn } from "@/lib/utils";

export function Sidebar({ session }: { session: AdminSession | null }) {
  const pathname = usePathname();

  return (
    <aside className="panel sticky top-4 h-[calc(100vh-2rem)] w-72 p-4">
      <div className="mb-6">
        <p className="text-xs uppercase tracking-[0.2em] text-muted">DramaFlow</p>
        <h1 className="mt-2 text-xl font-semibold">管理后台</h1>
      </div>
      <nav className="space-y-2">
        {NAV_ITEMS.filter((item) => can(session, item.permission)).map((item) => {
          const Icon = item.icon;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-xl px-3 py-2 text-sm transition",
                pathname === item.href ? "bg-accent text-white" : "text-slate-700 hover:bg-slate-100"
              )}
            >
              <Icon className="h-4 w-4" />
              <span>{item.label}</span>
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
