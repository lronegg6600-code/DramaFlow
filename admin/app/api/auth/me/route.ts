import { NextRequest, NextResponse } from "next/server";

// 2026-04-08:
// /me 是后台几乎所有页面加载时最先命中的会话检查接口。
// 这里一旦地址写错，最直观的表现就是登录页反复跳转、页面白屏、顶栏一直 loading。
const ADMIN_BASE_URL =
  process.env.ADMIN_API_INTERNAL_BASE_URL ??
  process.env.NEXT_PUBLIC_ADMIN_API_BASE_URL ??
  "http://localhost:8088";

export async function GET(request: NextRequest) {
  const upstream = await fetch(`${ADMIN_BASE_URL}/v1/admin/auth/me`, {
    headers: {
      cookie: request.headers.get("cookie") ?? ""
    },
    cache: "no-store"
  });

  const text = await upstream.text();
  return new NextResponse(text, {
    status: upstream.status,
    headers: {
      "Content-Type": "application/json"
    }
  });
}
