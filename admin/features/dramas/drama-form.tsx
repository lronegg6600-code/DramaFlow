"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { dramaSchema } from "@/lib/schemas/forms";
import { adminFetch } from "@/lib/api/client";
import { InlineNotice } from "@/components/feedback/inline-notice";
import { FormSection } from "@/components/forms/section";
import { useActionMutation } from "@/lib/query/use-action-mutation";
import { z } from "zod";

type DramaFormValues = z.infer<typeof dramaSchema>;

export function DramaForm({
  dramaId,
  initialValues
}: {
  dramaId?: string;
  initialValues?: Partial<DramaFormValues>;
}) {
  const router = useRouter();
  const form = useForm<DramaFormValues>({
    resolver: zodResolver(dramaSchema),
    defaultValues: {
      title: initialValues?.title ?? "",
      shortDescription: initialValues?.shortDescription ?? "",
      longDescription: initialValues?.longDescription ?? "",
      posterUrl: initialValues?.posterUrl ?? "https://example.com/poster.jpg",
      coverUrl: initialValues?.coverUrl ?? "https://example.com/cover.jpg",
      tags: Array.isArray(initialValues?.tags) ? initialValues?.tags.join(", ") : initialValues?.tags ?? "",
      region: initialValues?.region ?? "US",
      language: initialValues?.language ?? "zh-CN",
      publishStatus: (initialValues?.publishStatus as "draft" | "published" | "archived") ?? "draft",
      isFeatured: initialValues?.isFeatured ?? false
    }
  });

  const saveDramaAction = useActionMutation({
    action: async (values: DramaFormValues) => {
      // 2026-04-08:
      // 表单层先把 tags 从“逗号分隔字符串”收敛成标准数组，再交给后端。
      // 原因很实际：运营录入时最顺手的是一串文本，但接口和数据库需要的是结构化数组。
      // 这个转换放在提交边界做，能保证页面状态简单，同时避免把后端表单细节泄漏到输入控件层。
      const payload = {
        ...values,
        tags: values.tags.split(",").map((value) => value.trim()).filter(Boolean)
      };

      if (dramaId) {
        return adminFetch(`/dramas/${dramaId}`, { method: "PUT", body: JSON.stringify(payload) });
      }

      return adminFetch("/dramas", { method: "POST", body: JSON.stringify(payload) });
    },
    successMessage: dramaId ? "剧目已更新。" : "剧目已创建，正在返回列表页。",
    errorMessage: ({ error }) => (error instanceof Error ? error.message : "保存剧目失败，请稍后重试。"),
    invalidateKeys: [["dramas"], ...(dramaId ? [["drama", dramaId]] : [])],
    onSuccess: async () => {
      router.push("/dramas");
      router.refresh();
    }
  });

  async function onSubmit(values: DramaFormValues) {
    await saveDramaAction.executeAction(values);
  }

  return (
    <FormSection title={dramaId ? "编辑剧目" : "创建剧目"} description="管理发布状态、地区、付费元数据和内容可发现性。">
      <form className="grid gap-4 md:grid-cols-2" onSubmit={form.handleSubmit(onSubmit)}>
        {saveDramaAction.notice ? <div className="md:col-span-2"><InlineNotice tone={saveDramaAction.notice.tone} message={saveDramaAction.notice.message} /></div> : null}
        <input className="input" placeholder="标题" {...form.register("title")} />
        <input className="input" placeholder="短描述" {...form.register("shortDescription")} />
        <textarea className="input min-h-28 md:col-span-2" placeholder="长描述" {...form.register("longDescription")} />
        <input className="input" placeholder="海报 URL" {...form.register("posterUrl")} />
        <input className="input" placeholder="封面 URL" {...form.register("coverUrl")} />
        <input className="input" placeholder="标签（逗号分隔）" {...form.register("tags")} />
        <input className="input" placeholder="地区" {...form.register("region")} />
        <input className="input" placeholder="语言" {...form.register("language")} />
        <select className="input" {...form.register("publishStatus")}>
          <option value="draft">草稿</option>
          <option value="published">已发布</option>
          <option value="archived">已归档</option>
        </select>
        <label className="flex items-center gap-2 rounded-xl border border-border px-3 py-2 text-sm">
          <input type="checkbox" {...form.register("isFeatured")} />
          推荐内容
        </label>
        <div className="md:col-span-2">
          <button className="button-primary" disabled={saveDramaAction.isSubmitting} type="submit">
            {saveDramaAction.isSubmitting ? "保存中..." : "保存剧目"}
          </button>
        </div>
      </form>
    </FormSection>
  );
}
