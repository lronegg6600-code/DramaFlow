import "./globals.css";
import React from "react";
import type { Metadata } from "next";
import { QueryProvider } from "@/lib/query/provider";

export const metadata: Metadata = {
  title: "DramaFlow 管理后台",
  description: "用于内容运营、计费修复和播放诊断的管理后台。"
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body>
        <QueryProvider>{children}</QueryProvider>
      </body>
    </html>
  );
}
