import { NextRequest, NextResponse } from "next/server";

// 2026-04-08:
// 这里必须优先走容器内地址，而不是直接拿浏览器里的 localhost。
// 原因很简单：
// 1. 这个 route.ts 运行在 Next.js 服务端容器里，不是在用户浏览器里。
// 2. 如果服务端容器去请求 http://localhost:8088，请到的是 admin-web 容器自己，不是 admin-service。
// 3. 线上高流量场景里，这类“容器内 localhost 指错目标”的问题会直接放大成登录、鉴权、代理接口连锁失败。
// 所以这里固定采用双地址策略：
// - 服务端代理链路：ADMIN_API_INTERNAL_BASE_URL -> http://admin-service:8088
// - 浏览器直连链路：NEXT_PUBLIC_ADMIN_API_BASE_URL -> http://localhost:8088
const ADMIN_BASE_URL =
  process.env.ADMIN_API_INTERNAL_BASE_URL ??
  process.env.NEXT_PUBLIC_ADMIN_API_BASE_URL ??
  "http://localhost:8088";

export async function POST(request: NextRequest) {
  const body = await request.text();

  // 2026-04-08:
  // 登录这里刻意只透传最小必要头，避免把浏览器侧一堆无关 header 原样带进后台。
  // 这么做有两个现实好处：
  // 1. 上游 admin-service 更容易做审计和问题排查。
  // 2. 少带无关 header，后面接 CDN、网关、WAF 时更不容易出现代理歧义。
  const upstream = await fetch(`${ADMIN_BASE_URL}/v1/admin/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body,
    cache: "no-store"
  });

  const text = await upstream.text();
  const response = new NextResponse(text, {
    status: upstream.status,
    headers: {
      "Content-Type": "application/json"
    }
  });

  const setCookie = upstream.headers.get("set-cookie");
  if (setCookie) {
    // 2026-04-08:
    // 登录成功后，必须把 admin-service 下发的会话 cookie 原样转回浏览器。
    // 这里不能自己重拼 cookie，也不能丢掉 Path、HttpOnly、SameSite 这些属性。
    // 否则最典型的线上现象就是：接口明明 200，但页面实际还是未登录。
    response.headers.set("set-cookie", setCookie);
  }
  return response;
}
