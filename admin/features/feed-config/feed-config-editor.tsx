"use client";

import React from "react";
import { useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { adminFetch } from "@/lib/api/client";
import { InlineNotice } from "@/components/feedback/inline-notice";
import { useActionMutation } from "@/lib/query/use-action-mutation";
import { feedConfigSchema } from "@/lib/schemas/forms";
import { FormSection } from "@/components/forms/section";
import { ConfirmDialog } from "@/components/dialogs/confirm-dialog";

type FeedFormValues = z.infer<typeof feedConfigSchema>;

export function FeedConfigEditor() {
  const configQuery = useQuery({
    queryKey: ["feed-config"],
    queryFn: () => adminFetch<any>("/feed-config/home?region=US&language=zh-CN")
  });

  const form = useForm<FeedFormValues>({
    resolver: zodResolver(feedConfigSchema),
    values: {
      region: configQuery.data?.region ?? "US",
      language: configQuery.data?.language ?? "zh-CN",
      draftPayload: JSON.stringify(
        configQuery.data?.draftPayload ?? {
          featured: [],
          trending: [],
          recommended: [],
          banners: []
        },
        null,
        2
      )
    }
  });

  const saveDraftAction = useActionMutation({
    action: async (values: FeedFormValues) => {
      // 2026-04-08:
      // 这里显式 JSON.parse 再提交，是故意把“文本编辑体验”和“结构化配置入库”拆开。
      // 运营同学在页面里看到的是可直接复制粘贴的 JSON，
      // 服务端真正接收的是对象结构，这样前后端边界清楚，线上回放问题也更好定位。
      return adminFetch("/feed-config/home", {
        method: "PUT",
        body: JSON.stringify({
          region: values.region,
          language: values.language,
          draftPayload: JSON.parse(values.draftPayload)
        })
      });
    },
    successMessage: "首页推荐草稿已保存。",
    errorMessage: ({ error }) =>
      error instanceof Error ? error.message : "保存草稿失败，请稍后重试。",
    invalidateKeys: [["feed-config"]]
  });

  async function saveDraft(values: FeedFormValues) {
    await saveDraftAction.executeAction(values);
  }

  return (
    <FormSection
      title="首页推荐配置"
      description="编辑草稿版推荐位，然后发布或回滚。feed-service 会读取已发布的内容。"
    >
      <form className="space-y-4" onSubmit={form.handleSubmit(saveDraft)}>
        {saveDraftAction.notice ? (
          <InlineNotice
            tone={saveDraftAction.notice.tone}
            message={saveDraftAction.notice.message}
          />
        ) : null}
        <div className="grid gap-4 md:grid-cols-2">
          <input className="input" {...form.register("region")} />
          <input className="input" {...form.register("language")} />
        </div>
        <textarea className="input min-h-80 font-mono text-xs" {...form.register("draftPayload")} />
        <div className="flex gap-3">
          <button className="button-primary" disabled={saveDraftAction.isSubmitting} type="submit">
            保存草稿
          </button>
          <ConfirmDialog
            label="发布"
            description="这会覆盖当前线上首页推荐配置。"
            invalidateKeys={[["feed-config"]]}
            successMessage="首页推荐配置已发布。"
            onConfirm={async () => {
              await adminFetch("/feed-config/home/publish?region=US&language=zh-CN", {
                method: "POST"
              });
            }}
          />
          <ConfirmDialog
            label="回滚"
            description="这会把草稿内容重置为当前已发布版本。"
            invalidateKeys={[["feed-config"]]}
            successMessage="首页推荐草稿已回滚到线上版本。"
            onConfirm={async () => {
              await adminFetch("/feed-config/home/rollback?region=US&language=zh-CN", {
                method: "POST"
              });
            }}
          />
        </div>
      </form>
    </FormSection>
  );
}
