import { NextRequest, NextResponse } from "next/server";

// 2026-04-08:
// 退出登录和登录是同一类问题：
// 这个文件跑在 Next.js 容器里，所以服务端代理必须优先走容器内服务地址，而不是 localhost。
const ADMIN_BASE_URL =
  process.env.ADMIN_API_INTERNAL_BASE_URL ??
  process.env.NEXT_PUBLIC_ADMIN_API_BASE_URL ??
  "http://localhost:8088";

export async function POST(request: NextRequest) {
  const upstream = await fetch(`${ADMIN_BASE_URL}/v1/admin/auth/logout`, {
    method: "POST",
    headers: {
      cookie: request.headers.get("cookie") ?? ""
    },
    cache: "no-store"
  });

  const text = await upstream.text();
  const response = new NextResponse(text || JSON.stringify({ data: { loggedOut: true } }), {
    status: upstream.status,
    headers: {
      "Content-Type": "application/json"
    }
  });

  const setCookie = upstream.headers.get("set-cookie");
  if (setCookie) {
    // 2026-04-08:
    // 退出登录要把上游返回的清理 cookie 指令继续写回浏览器。
    // 不然浏览器本地 session 还在，页面就会出现“看起来退出了，刷新又进来了”的脏状态。
    response.headers.set("set-cookie", setCookie);
  }

  return response;
}
