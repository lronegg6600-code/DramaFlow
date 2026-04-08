"use client";

import React from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { z } from "zod";
import { authFetch } from "@/lib/api/client";
import { InlineNotice } from "@/components/feedback/inline-notice";
import { useActionMutation } from "@/lib/query/use-action-mutation";
import { loginSchema } from "@/lib/schemas/forms";

type LoginValues = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const router = useRouter();
  const form = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "admin@dramaflow.local",
      password: "change-me-now"
    }
  });

  const loginAction = useActionMutation({
    action: async (values: LoginValues) =>
      authFetch("/login", { method: "POST", body: JSON.stringify(values) }),
    errorMessage: ({ error }) =>
      error instanceof Error ? error.message : "登录失败，请稍后重试。",
    onSuccess: async () => {
      // 2026-04-08:
      // 登录成功后先跳首页，再主动 refresh 一次。
      // 这样能让布局层依赖的 /api/auth/me 和当前登录态快速对齐，
      // 避免接口已经 200，但页面还短暂停留在未登录状态。
      router.push("/");
      router.refresh();
    }
  });

  async function onSubmit(values: LoginValues) {
    try {
      await loginAction.executeAction(values);
    } catch {
      // 错误提示已经由 mutation notice 接管，这里不再把 rejected promise 往外抛。
    }
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-md items-center px-6">
      <div className="panel w-full p-8">
        <p className="text-xs uppercase tracking-[0.2em] text-muted">DramaFlow</p>
        <h1 className="mt-3 text-2xl font-semibold">管理后台登录</h1>
        <p className="mt-2 text-sm text-muted">
          访问内容运营、计费修复、权益诊断和播放支持工具。
        </p>
        <form className="mt-6 space-y-4" onSubmit={form.handleSubmit(onSubmit)}>
          {loginAction.notice ? (
            <InlineNotice
              tone={loginAction.notice.tone}
              message={loginAction.notice.message}
            />
          ) : null}
          <input className="input" placeholder="邮箱" {...form.register("email")} />
          <input
            className="input"
            placeholder="密码"
            type="password"
            {...form.register("password")}
          />
          <button
            className="button-primary w-full"
            disabled={loginAction.isSubmitting}
            type="submit"
          >
            {loginAction.isSubmitting ? "登录中..." : "登录"}
          </button>
        </form>
      </div>
    </main>
  );
}
