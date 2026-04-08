import { NextRequest, NextResponse } from "next/server";

// 2026-04-08:
// 这个通配代理承接了后台绝大多数管理接口。
// 这里一旦地址配错，不会只坏一个页面，而是剧目、用户、购买、权益、推荐配置会一起挂。
// 所以这里和 auth 代理一样，必须优先走容器内地址。
const ADMIN_BASE_URL =
  process.env.ADMIN_API_INTERNAL_BASE_URL ??
  process.env.NEXT_PUBLIC_ADMIN_API_BASE_URL ??
  "http://localhost:8088";

type RouteContext = { params: Promise<{ path: string[] }> };

async function proxy(request: NextRequest, params: { path: string[] }) {
  const query = request.nextUrl.search;
  const url = `${ADMIN_BASE_URL}/v1/admin/${params.path.join("/")}${query}`;
  const body = request.method === "GET" ? undefined : await request.text();

  // 2026-04-08:
  // 这里的原则是“只转发业务真正需要的内容”：
  // - Content-Type：让上游知道怎么解析 body
  // - cookie：让上游能做会话鉴权
  // 其他 header 暂时不全量透传，避免后面接 CDN、反向代理或安全网关时引入双写、伪造、污染问题。
  const upstream = await fetch(url, {
    method: request.method,
    headers: {
      "Content-Type": request.headers.get("content-type") ?? "application/json",
      cookie: request.headers.get("cookie") ?? ""
    },
    body,
    cache: "no-store"
  });

  const text = await upstream.text();
  return new NextResponse(text, {
    status: upstream.status,
    headers: {
      "Content-Type": upstream.headers.get("content-type") ?? "application/json"
    }
  });
}

export async function GET(request: NextRequest, context: RouteContext) {
  return proxy(request, await context.params);
}

export async function POST(request: NextRequest, context: RouteContext) {
  return proxy(request, await context.params);
}

export async function PUT(request: NextRequest, context: RouteContext) {
  return proxy(request, await context.params);
}
