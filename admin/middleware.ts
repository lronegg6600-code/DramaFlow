import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const PROTECTED_PREFIXES = [
  "/dramas",
  "/feed-config",
  "/users",
  "/purchases",
  "/entitlements",
  "/playback",
  "/rtdn",
  "/audit-logs",
  "/settings"
];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const hasSession = Boolean(request.cookies.get("dramaflow_admin_session")?.value);
  const isProtected = pathname === "/" || PROTECTED_PREFIXES.some((prefix) => pathname.startsWith(prefix));

  if (pathname.startsWith("/login") && hasSession) {
    return NextResponse.redirect(new URL("/", request.url));
  }

  if (isProtected && !hasSession) {
    return NextResponse.redirect(new URL("/login", request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico).*)"]
};
